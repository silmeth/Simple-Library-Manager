from django.conf.urls import patterns, url
from slm_db_interface import views


urlpatterns = patterns('',
    url(r'^get_books/isbn/(?P<isbn>\d+)$', views.get_books_by_isbn, name='get_books_by_isbn'),
    url(r'^search/(?P<attr>[a-z]+)=(?P<attr_val>(\w|\+|,|\.|\s|:)+)$', views.search, name='search')
)