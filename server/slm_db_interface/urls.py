from django.conf.urls import patterns, url
from slm_db_interface import views


urlpatterns = patterns('',
    url(r'^get_books/isbn/(?P<isbn>\d+)$', views.get_books_by_isbn, name='get_books_by_isbn'),
    url(r'^search/(?P<attr>[a-z]+)=(?P<attr_val>(\w|\+|,|\.|\s|:)+)$', views.search, name='search'),
    url(r'^add_book$', views.add_book, name='add_book'),
    url(r'^search_authors/(?P<name>(\w|\+|,|\.|\s|:)+)$', views.search_authors, name='search_authors'),
    url(r'^search_publishers/(?P<name>(\w|\+|,|\.|\s|:)+)$', views.search_publishers, name='search_publishers'),
    url(r'^login', views.log_user_in, name='log_user_in'),
)
