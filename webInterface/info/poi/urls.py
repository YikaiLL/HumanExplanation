from django.conf.urls import url
from . import views
from django.contrib import admin

urlpatterns = (
    # url(r'^home', views.home, name='home'),
    url(r'^index', views.recommend, name='recommend'),
    url(r'^home', views.selectuser, name='selectuser'),
    url(r'^typical', views.select_typical_user, name='select_typical_user'),
    url(r'^index',views.write_exp,name='write_exp'),
    url(r'^search/$', views.search, name='search'),

)