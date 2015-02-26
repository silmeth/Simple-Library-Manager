from django.shortcuts import render
from django.http import HttpResponse
from django.db import transaction, connection
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth import authenticate, login
from slm_db_interface.models import Book, Author, Publisher, Borrower, SLMUser
from bisect import bisect_left  # to do binary search on sorted lists
import re
import json


def create_json_from_books(books, additional_dic=None):
    books_list = []
    for i, book in enumerate(books):
        book_dict = {
            'title': book.title,
            'author': book.author.name,
            'author_id': book.author.id,
            'isbn10': book.isbn10,
            'isbn13': book.isbn13,
            'publisher': book.publisher.name,
            'publisher_id': book.publisher.id,
            'pub_date': book.published_year,
            'book_id': book.id
        }

        if additional_dic is not None:  # add additional non-standard fields
            for key in additional_dic[i]:
                book_dict[key] = additional_dic[i][key]

        books_list.append(book_dict)

    return json.JSONEncoder(indent=2, ensure_ascii=False).encode(books_list)


def create_book_from_json(json_obj):
    book = None
    book_author = None
    book_publisher = None
    with transaction.atomic():
        json_decoded = json.JSONDecoder().decode(json_obj)
        if 'author' in json_decoded and json_decoded['author'] is None:
            return None
        elif 'author_new' in json_decoded and json_decoded['author_new']:
            book_author = Author(name=json_decoded['author_name'])
            book_author.save()
        elif 'author_id' in json_decoded:
            book_author = Author.objects.get(id=json_decoded['author_id'])

        if 'publisher' in json_decoded and json_decoded['publisher'] is None:
            return None
        elif 'publisher_new' in json_decoded and json_decoded['publisher_new']:
            book_publisher = Publisher(name=json_decoded['publisher_name'])
            book_publisher.save()
        elif 'publisher_id' in json_decoded:
            book_publisher = Publisher.objects.get(id=json_decoded['publisher_id'])

        if 'title' not in json_decoded:
            return None
        book = Book(title=json_decoded['title'], author=book_author, publisher=book_publisher,
                    borrower=None, borrow_date=None, return_date=None)

        if 'isbn10' in json_decoded:
            book.isbn10 = json_decoded['isbn10']
        if 'isbn13' in json_decoded:
            book.isbn13 = json_decoded['isbn13']
        if 'pub_date' in json_decoded:
            book.published_year = json_decoded['pub_date']
        book.save()

    return book


def create_3grams(s):
    assert(type(s) is str)
    list_3grams = []
    for pos in range(len(s)-2):
        list_3grams.append(s[pos:pos+3])
    list_3grams.sort()
    return list_3grams


def compare_3grams(first, second):  # Jaccard's similarity
    assert(type(first) is list and type(second) is list)
    intersect = 0
    len1 = len(first)
    len2 = len(second)
    for val in first:  # find number of elements in the intersection of two lists of 3-grams
        pos = bisect_left(second, val, 0, len2)
        if pos != len2 and second[pos] == val:
            intersect += 1
    return float(intersect)/(len1+len2-intersect)


def get_books_by_isbn(request, isbn): # no login required ATM, may change
    sisbn = str(isbn)
    results = None
    if len(sisbn) == 10:
        results = Book.objects.filter(isbn10=sisbn)
    elif len(sisbn) == 13:
        results = Book.objects.filter(isbn13=sisbn)
    return HttpResponse(content=create_json_from_books(results), content_type='application/json; charset=utf-8')


def search(request, attr, attr_val): # no login required ATM, may change
    regexp_whitespace = re.compile('\s+')
    regexp_punctuation = re.compile('[^\w\s]+')

    attr_val = regexp_whitespace.sub(' ', attr_val.lower())
    attr_val = regexp_punctuation.sub('', attr_val)

    query_3grams = create_3grams(attr_val)
    results = []
    similarities = []

    for book in Book.objects.all():
        if attr == 'title':
            book_attr_val = book.title.lower()
        elif attr == 'author':
            book_attr_val = book.author.name.lower()
        else:
            return HttpResponse(content='cannot search by this attribute', status=404)

        book_attr_val = regexp_whitespace.sub(' ', book_attr_val)
        book_attr_val = regexp_punctuation.sub('', book_attr_val)
        book_3grams = create_3grams(book_attr_val)
        similarity = compare_3grams(query_3grams, book_3grams)
        if similarity > 0.21:
            pos = bisect_left(similarities, similarity, 0, len(similarities))
            results.insert(pos, book)
            similarities.insert(pos, similarity)

    sim_dic_list = []
    for sim in similarities:
        sim_dic_list.append({'similarity': sim})

    return HttpResponse(content=create_json_from_books(results[::-1], sim_dic_list[::-1]),
                        content_type='application/json; charset=utf-8')


def search_authors(request, name):
    if request.user.is_authenticated():
        regexp_whitespace = re.compile('\s+')
        regexp_punctuation = re.compile('[^\w\s]+')

        name = regexp_whitespace.sub(' ', name.lower())
        name = regexp_punctuation.sub('', name)

        query_3grams = create_3grams(name)
        results = []
        similarities = []

        for author in Author.objects.all():
            result = author.name.lower()

            result = regexp_whitespace.sub(' ', result)
            result = regexp_punctuation.sub('', result)
            result_3grams = create_3grams(result)
            similarity = compare_3grams(query_3grams, result_3grams)
            if similarity > 0.21:
                pos = bisect_left(similarities, similarity, 0, len(similarities))
                results.insert(pos, author)
                similarities.insert(pos, similarity)

        results = results[::-1]
        similarities = similarities[::-1]
        json_results_list = []

        for i, res in enumerate(results):
            json_results_list.append({'name': res.name, 'author_id': res.id, 'similarity': similarities[i]})

        json_results = json.JSONEncoder(indent=2, ensure_ascii=False).encode(json_results_list)

        return HttpResponse(content=json_results,
                            content_type='application/json; charset=utf-8')
    else:
        return HttpResponse(content='error: not authenticated', content_type='text/plain')  # TODO change to error dict


def search_publishers(request, name):
    if request.user.is_authenticated():
        regexp_whitespace = re.compile('\s+')
        regexp_punctuation = re.compile('[^\w\s]+')

        name = regexp_whitespace.sub(' ', name.lower())
        name = regexp_punctuation.sub('', name)

        query_3grams = create_3grams(name)
        results = []
        similarities = []

        for publisher in Publisher.objects.all():
            result = publisher.name.lower()

            result = regexp_whitespace.sub(' ', result)
            result = regexp_punctuation.sub('', result)
            result_3grams = create_3grams(result)
            similarity = compare_3grams(query_3grams, result_3grams)
#            if similarity > 0.21:  # listing all publishers makes more sense
            pos = bisect_left(similarities, similarity, 0, len(similarities))
            results.insert(pos, publisher)
            similarities.insert(pos, similarity)

        results = results[::-1]
        similarities = similarities[::-1]
        json_results_list = []

        for i, res in enumerate(results):
            json_results_list.append({'name': res.name, 'publisher_id': res.id, 'similarity': similarities[i]})

        json_results = json.JSONEncoder(indent=2, ensure_ascii=False).encode(json_results_list)

        return HttpResponse(content=json_results,
                            content_type='application/json; charset=utf-8')
    else:
        return HttpResponse(content='error: not authenticated', content_type='text/plain')


@csrf_exempt
def add_book(request):
    if request.user.is_authenticated():
        if request.user.slm_user.can_manage_books:
            # book data comes in json through a POST request
            if request.method == 'POST':
                try:
                    print(request.body.decode('utf8'))
                    book = create_book_from_json(request.body.decode('utf8'))
                    return HttpResponse(content=create_json_from_books([book]),
                                        content_type='application/json; charset=utf-8')
                except ValueError as err:  # TODO change to error dict
                    return HttpResponse(
                        content='error: request not a valid json\n' + str(err),
                        content_type='text/plain'
                    )
            else:
                return HttpResponse(content='error: something went wrong', content_type='text/plain')
        else:
            return HttpResponse(content='error: lack of manage book permission')
    else:
        return HttpResponse(content='error: not authenticated', content_type='text/plain')


@csrf_exempt
def log_user_in(request):
    if request.method == 'POST':
        try:
            credentials = json.JSONDecoder().decode(request.body.decode('utf8'))
            user = authenticate(username=credentials['username'], password=credentials['password'])
            if user is not None:
                if user.is_active:
                    login(request, user)
                    resp_json = {'logged_in': True,
                                 'username': str(user)}
                    if user.slm_user.can_manage_books:
                        resp_json['can_manage_books'] = True
                    if user.slm_user.can_lend:
                        resp_json['can_lend'] = True
                    if user.slm_user.can_borrow:
                        resp_json['can_borrow'] = True
                    resp = json.JSONEncoder(indent=2, ensure_ascii=False).encode(resp_json)
                    return HttpResponse(content=resp, content_type='application/json; charset=utf-8')
                else:  # TODO change to error dict
                    return HttpResponse(content='error: user inactive', content_type='text/plain')
            else:
                return HttpResponse(content='error: wrong credentials', content_type='text/plain')
        except ValueError:
            return HttpResponse(content='error: request not a valid json', content_type='text/plain')

