import json
from django.shortcuts import render
from django.http import HttpResponse
from slm_db_interface.models import Book, Author, Publisher, Borrower, SLMUser
from bisect import bisect_left  # to do binary search on sorted lists
import re


def create_books_json(books, additional_dic=None):
    books_list = []
    for i, book in enumerate(books):
        book_dict = {
            'title': book.title,
            'author': book.author.name,
            'isbn10': book.isbn10,
            'isbn13': book.isbn13
        }

        if additional_dic is not None:  # add additional non-standard fields
            for key in additional_dic[i]:
                book_dict[key] = additional_dic[i][key]

        books_list.append(book_dict)

    return json.JSONEncoder(indent=2, ensure_ascii=False).encode(books_list)


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


def get_books_by_isbn(request, isbn):
    sisbn = str(isbn)
    if len(sisbn) == 10:
        results = Book.objects.filter(isbn10=sisbn)
    elif len(sisbn) == 13:
        results = Book.objects.filter(isbn13=sisbn)
    return HttpResponse(content=create_books_json(results), content_type='application/json; charset=utf-8')


def search(request, attr, attr_val):
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
        if attr == 'author':
            book_attr_val = book.author.name.lower()

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

    return HttpResponse(content=create_books_json(results[::-1], sim_dic_list[::-1]),
                        content_type='application/json; charset=utf-8')