from django.contrib import admin
from slm_db_interface.models import SLMUser, Borrower, Book, Author, Publisher


class SLMUserInline(admin.TabularInline):
    model = SLMUser


class BorrowerAdmin(admin.ModelAdmin):
    fieldsets = [
        (None, {'fields': ['name', 'nick', 'surname']}),
    ]
    inlines = [SLMUserInline]


class BookAdmin(admin.ModelAdmin):
    fieldsets = [
        (None, {'fields': ['title', 'author', 'publisher', 'published_year']}),
        ('Identifiers', {'fields': ['isbn10', 'isbn13']}),
        ('Lending/borrowing', {'fields': ['borrow_date', 'return_date']}),
    ]


class BookInline(admin.TabularInline):
    model = Book
    extra = 2


class AuthorAdmin(admin.ModelAdmin):
    fields = ['name']
    inlines = [BookInline]


class PublisherAdmin(admin.ModelAdmin):
    fields = ['name']
    inlines = [BookInline]

admin.site.register(Borrower, BorrowerAdmin)
admin.site.register(SLMUser)
admin.site.register(Book, BookAdmin)
admin.site.register(Author, AuthorAdmin)
admin.site.register(Publisher, PublisherAdmin)

admin.site.site_header = 'Simple Library Manager Admin Panel'
