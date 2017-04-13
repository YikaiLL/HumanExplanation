from django.conf.urls import url, include
from . import views
from django.contrib import admin

urlpatterns = (
    # url(r'^home', views.home, name='home'),
    url(r'^index', views.recommend, name='recommend'),
    url(r'^home', views.selectuser, name='selectuser'),
    url(r'^search/$', views.search, name='search'),

)
