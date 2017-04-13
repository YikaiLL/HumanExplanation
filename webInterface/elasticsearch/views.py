from django.shortcuts import render
from models import *
from django.forms import *

from django.http import HttpResponse
from django.utils.html import escape
from elasticsearch import Elasticsearch
import json
es = Elasticsearch()

# Create your views here.
def recommend(request):
    context = {}
    context['names'] = request.POST['user_name']
    all_user = User.objects.all()
    all_poilist = PoiList.objects.all()
    all_venue = Venue.objects.all()
    all_type = VenueCategories.objects.all()
    all_userhistory = UserHistory.objects.all()


    for user in all_user:
        if str(user.username) == context['names']:
            context['id'] = user.userid
            context['pic'] = user.profile_image


    context['POIs']={}
    context['uservenuelist'] = {}
    # context['Users']['venue_name'] = {}
    # context['Users']['venue_link'] = []
    for userhistory in all_userhistory:
        if str(userhistory.userid) == context['id']:
            for venue in all_venue:
                if venue.venueid == userhistory.venueid:
                    context['uservenuelist'][venue.venueid]=venue.venuename
                    break


    for list in all_poilist:
        if str(list.userid)==context['id']:
            venue_id = [list.venue1,list.venue2,list.venue3]
            for venue_id_ in venue_id:
                context['POIs'][venue_id_] = {}
                for venue in all_venue:
                    if venue.venueid == venue_id_:
                        context['POIs'][venue_id_]['name'] = venue.venuename
                        context['POIs'][venue_id_]['address'] = venue.venueaddress
                        context['POIs'][venue_id_]['picture'] = venue.venuephoto

                        break
                context['POIs'][venue_id_]['type'] = [type.venuecategories for type in all_type if type.venueid == venue_id_]

    return render(request,'poi/index.html',context)

def search(request):
    if request.method == 'POST':
        response_data = {}
        search_query = request.POST.get('query')
        res = es.search(index="testvenue", body={"query": {"query_string": {"query": search_query}}})
        ls = []
        for hit in range(len(res['hits']['hits'])):
            ls.append(res['hits']['hits'][hit]['_source']['venue'])
        response_data['searchbox'] = ls

        return HttpResponse(json.dumps(response_data))
    return HttpResponse('404 not found')


def selectuser(request):
    # all_user = User.objects.all()
    # if request.method == 'GET':

    #     context['id'] = PoiList.objects.all()
    #     for userid in context['id']:
    #         if userid.venue3 is not None :
    #             for user in all_user:
    #                 if userid==user.userid:
    #                     context['name'].append(user.username)
        context = {}
        context['name'] = User.objects.all()
        return render(request,'poi/home.html',context)
def home(request):
    return render(request, 'poi/home.html')
def index(request):
    return render(request, 'poi/index.html')
