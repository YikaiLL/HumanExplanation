from django.shortcuts import render
from models import *
from django.forms import *
from django.http import HttpResponse
import operator
from random import randint
from django.utils.html import escape
from elasticsearch import Elasticsearch
import json
es = Elasticsearch()

# Create your views here.
def recommend(request):
    context = {}
    if request.session.get('loginusername') is not None:
        context['names'] = request.session.get('loginusername')
    else:
        context['names'] = request.POST['user_name']
        request.session.modified = True
        request.session['loginusername'] = context['names']

    our_user = User.objects.get(username=context['names'])
    context['id'] = our_user.userid
    context['pic'] = our_user.profile_image.replace("_normal.jpg", ".jpg").replace("_normal.jpeg", ".jpeg")
    context['usercity'] = our_user.usercity
    context['usercountry'] = our_user.usercountry
    context['descrption'] = our_user.description

    one_user_poilist = PoiList.objects.get(userid=context['id'])

    random_scope_for_num = 0
    # Number of the rows in PoiList
    random_scope_for_recom = PoiList.objects.all().count()
    recom = ""
    temp = ""

    # While loop to make sure it has something in the venue column
    while (random_scope_for_num == 0):
        #     Provide random user and random venue for the user
        recom = PoiList.objects.get(id=randint(1, random_scope_for_recom))
        # recom = PoiList.objects.get(id=2)
        user_id__ = recom.userid
        context['exp_userid'] = recom.userid
        if recom.venue3 is not None and recom.venue2 is not None and recom.venue1 is not None:
            random_scope_for_num = 3
        elif recom.venue2 is not None and recom.venue1 is not None:
            random_scope_for_num = 2
        elif recom.venue1 is not None:
            random_scope_for_num = 1

    num = randint(1, random_scope_for_num)
    if num == 1:
        context['exp_venueid'] = recom.venue1
        temp = recom.venue1
    elif num == 2:
        context['exp_venueid'] = recom.venue2
        temp = recom.venue2
    elif num == 3:
        context['exp_venueid'] = recom.venue3
        temp = recom.venue3

    context['temp'] = temp
    context['bug'] = "..."
    try:
        exp_user = User.objects.get(userid=recom.userid)
        twitter(recom.userid, context, 'exp_topiclist')
        history(recom.userid, context, 'exp_history')
        context['exp_username'] = exp_user.username
        try:
            context['exp_usercity'] = exp_user.usercity
        except:
            context['exp_usercity'] = ""
        try:
            context['exp_usercountry'] = exp_user.usercountry
        except:
            context['exp_usercountry'] = ""
        try:
            context['exp_userpic'] = exp_user.profile_image.replace("_normal.jpg", ".jpg").replace("_normal.jpeg",
                                                                                                   ".jpeg")
        except:
            context['exp_userpic'] = ""


    except:
        context['bug'] = context['bug'] + "get recom userid wrong...."

    try:
        exp_venue = Venue.objects.get(venueid=temp)
        context['exp_venuename'] = exp_venue.venuename
        try:
            context['exp_venueadd'] = exp_venue.venueaddress
        except:
            context['exp_venueadd'] = ""
        try:
            context['exp_venuephoto'] = exp_venue.venuephoto
        except:
            context['exp_venuephoto'] = ""

    except:
        context['bug'] = context['bug'] + "get temp wrong...."

    # For the topic extraction
    history(context['id'], context, 'uservenuelist')
    twitter(context['id'], context, 'topic_list')

    context['POIs'] = {}

    venue_id = [one_user_poilist.venue1, one_user_poilist.venue2, one_user_poilist.venue3]
    for venue_id_ in venue_id:
        context['POIs'][venue_id_] = {}

        one_venue_object = Venue.objects.get(venueid=venue_id_)
        context['POIs'][venue_id_]['name'] = one_venue_object.venuename
        context['POIs'][venue_id_]['address'] = one_venue_object.venueaddress
        context['POIs'][venue_id_]['picture'] = one_venue_object.venuephoto

        # Need to change here in order to find better comments
        one_venue_comment_object = VenueComments.objects.filter(venueid=venue_id_)

        for comment_venue in one_venue_comment_object:
            context['POIs'][venue_id_]['comment'] = comment_venue.venuecomments
            context['POIs'][venue_id_]['phrase'] = str(comment_venue.venuephrases) + ": "
            break

        one_venue_types = VenueCategories.objects.filter(venueid=venue_id_)
        context['POIs'][venue_id_]['type'] = [type.venuecategories for type in one_venue_types]
    request.session.modified = True
    request.session['userid'] = recom.userid
    request.session.modified = True
    request.session['venueid'] = temp

    cand_list = CandExp.objects.filter(user_id='2262294550')
    cand_list = CandExp.objects.filter(venue_id='50ceee35e4b0d870dd7fa024')
    context['cand_explain'] = []
    for exps in cand_list:
        context['cand_explain'].append(exps.cand_exp)

    return render(request, 'poi/index.html', context)


def selectuser(request):
    context = {}
    context['names'] = []
    # Get only the list
    user_name_list = PoiList.objects.values_list('userid', flat=True)
    for one_name in user_name_list:
        try:
            one_user = User.objects.get(userid=one_name)
            context['names'].append(one_user.username)
        except:
            continue
    user___name = None
    request.session.modified = True
    request.session.flush()
    request.session.clear()
    for key in request.session.keys():
        del request.session[key]
    return render(request, 'poi/home.html', context)


def select_typical_user(request):
    context = {}
    context['names'] = []

    typical_user_id = [100057597, 100595096, 1012917350, 1014120596, 1014916676, 1017078158, 19499747, 328607423,
                       34465303, 358464905]
    # typical_user_list=[]
    # Get only the list
    # for one_user_id in typical_user_id:
    #     typical_user_list.append(PoiList.objects.get(userid=one_user_id))
    for one_user_id in typical_user_id:
        try:
            one_user = User.objects.get(userid=one_user_id)
            context['names'].append(one_user.username)
        except:
            continue
    user___name = None
    request.session.modified = True
    request.session.flush()
    request.session.clear()
    for key in request.session.keys():
        del request.session[key]
    return render(request, 'poi/typical.html', context)


def twitter(user_id_, context, dicitem):
    # Get top ten frequency topic append it to the list
    temp_list_sorted_topic = TwitterUserTopic.objects.filter(userid=user_id_).values_list('topic', flat=True).order_by(
        '-frequency')[:10]

    context[dicitem] = ""
    for i in range(len(temp_list_sorted_topic)):
        if i != len(temp_list_sorted_topic) - 1:
            context[dicitem] += temp_list_sorted_topic[i] + ", "
        else:
            context[dicitem] += temp_list_sorted_topic[i]
    return


def history(user_id_, context, dicitem):
    one_user_history = UserHistory.objects.filter(userid=user_id_)

    context[dicitem] = {}

    for userhistory in one_user_history:
        one_user_venue = Venue.objects.get(venueid=userhistory.venueid)
        context[dicitem][userhistory.venueid] = one_user_venue.venuename

    return


#  Write into database
# Problem is how to refresh the index.html and pass the userid
def write_exp(request):
    if 'message2' in request.POST:
        explanation = request.POST['message2']
    else:
        explanation = False
    user___id = request.session.get('userid')
    venue___id = request.session.get('venueid')
    nowname = request.session.get('loginusername')
    idnum = CandExp.objects.all().count()
    if idnum is None:
        idnum = 1
    else:
        idnum += 1

    b = CandExp(id=idnum, user_id=user___id, venue_id=venue___id, cand_exp=explanation)
    b.save()
    request.session.flush()
    request.session.clear()
    for key in request.session.keys():
        if key == 'userid' or key == 'venueid':
            del request.session[key]
    request.session.modified = True
    request.session['loginusername'] = nowname
    return render(request, 'poi/index.html')


def choose_best(request):
    if 'bestexp' in request.POST:
        explanation = request.POST['message2']
    else:
        explanation = False
    user___id = request.session.get('userid')
    venue___id = request.session.get('venueid')
    nowname = request.session.get('loginusername')
    idnum = CandExp.objects.all().count()
    if idnum is None:
        idnum = 1
    else:
        idnum += 1

    b = CandExp(id=idnum, user_id=user___id, venue_id=venue___id, cand_exp=explanation)
    b.save()
    request.session.flush()
    request.session.clear()
    for key in request.session.keys():
        if key == 'userid' or key == 'venueid':
            del request.session[key]
    request.session.modified = True
    request.session['loginusername'] = nowname
    return render(request, 'poi/index.html')


def logout(request):
    response = HttpResponse('home.html')
    return response

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
    return HttpResponse('error')

