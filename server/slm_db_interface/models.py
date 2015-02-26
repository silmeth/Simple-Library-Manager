from django.db import models
from django.contrib.auth.models import User

class Borrower(models.Model):
    """
    Borrower's data, used to
    """
    id = models.AutoField(db_column='borrower_id', primary_key=True)
    nick = models.CharField(max_length=40, blank=True, null=False)
    name = models.CharField(max_length=40, blank=False, null=False)
    surname = models.CharField(max_length=40, blank=True, null=False)
    email = models.EmailField(blank=False, null=False)

    def __str__(self):
        res = str(self.name)
        if self.nick:
            res += ' ‘' + str(self.nick) + '’'
        if self.surname:
            res += ' ' + str(self.surname)
        return res

    class Meta:
        db_table = 'borrowers'


class SLMUser(models.Model):
    """
    SLM User with field referring to the Borrower record appropriate
    for given user, and with field to refer Django user.
    """
    id = models.AutoField(db_column='slm_user_id', primary_key=True)
    django_user = models.OneToOneField(User, db_column='django_user_id', related_name='slm_user')
                                                                                    # system uses Django authentication
    borrower = models.OneToOneField(Borrower, db_column='borrower_id', blank=False, null=True, related_name='slm_user')
                                                                                                # user has to have
                                                                                                # appropriate borrower
    can_borrow = models.BooleanField(default=True)
    can_lend = models.BooleanField(default=False)
    can_manage_books = models.BooleanField(default=False)
#    nickname = models.CharField(max_length=40, db_column='nickname')

    def __str__(self):
        return str(self.django_user.username)

    class Meta:
        db_table = 'slm_users'


class Author(models.Model):
    id = models.AutoField(db_column='author_id', primary_key=True)
    name = models.CharField(max_length=100, db_index=True, blank=False, null=False, unique=False)

    def __str__(self):
        return str(self.name)

    class Meta:
        db_table = 'authors'


class Publisher(models.Model):
    id = models.AutoField(db_column='publisher_id', primary_key=True)
    name = models.CharField(max_length=100, db_index=True, blank=False, null=False, unique=False)

    def __str__(self):
        return str(self.name)

    class Meta:
        db_table = 'publishers'


class Book(models.Model):
    id = models.AutoField(db_column='book_id', primary_key=True)
    borrower = models.ForeignKey(Borrower, db_column='borrower_id', null=True, blank=True)
    author = models.ForeignKey(Author, db_column='author_id')
    title = models.CharField(max_length=200)
    isbn10 = models.CharField(max_length=10, db_index=True, null=True, blank=True)
    isbn13 = models.CharField(max_length=13, db_index=True, null=True, blank=True)
    publisher = models.ForeignKey(Publisher, db_column='publisher_id')
    published_year = models.SmallIntegerField(db_index=True, null=True, blank=True)
    borrow_date = models.DateField(db_index=True, null=True, blank=True)
    return_date = models.DateField(db_index=True, null=True, blank=True)

    def __str__(self):
        return '„' + str(self.title) + '”, ' + str(self.author.name)

    class Meta:
        db_table = 'books'
