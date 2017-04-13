#!/usr/bin/env python 
# -*- coding: utf-8 -*-
# Get foursquare venues using a bunch of tokens

import urllib2
import json
from datetime import datetime
import time
import csv
from itertools import cycle

token_list = ['list_of_token'] 
print len(token_list), 'tokens'
pool = cycle(token_list)

id_list = [] 
with open('london.txt') as ft:
    for line in ft:
        str = line.split(' ')[1]
        if not str.endswith('_home'):
            id_list.append(str)

with open('londonins.txt') as fi:
    for line in fi:
        str = line.split(' ')[1]
        if not str.endswith('_home'):
            id_list.append(str)

id_set = set(id_list)
#around 15740
print 'venue length' , len(id_set)

#with open('missing_venue.csv', 'w') as fc, 
with open('4s_venues.json', 'w') as fj:

    #fc = csv.writer(fc)
    mlist = []

    vtime = datetime.now().strftime('%Y%m%d')

    access_token = next(pool) 
    print access_token

    for counter, vid in enumerate(id_set):
        url = 'https://api.foursquare.com/v2/venues/' + vid + '?oauth_token=' + access_token +'&v=' +vtime
        print counter , url
        try:
           data = urllib2.urlopen(url)
           data = json.loads(data.read())  
        except Exception, e:
    #        fc.writerow([vid])
            mlist.append(vid)
            access_token = next(pool)
            print '----line: ',counter, url, ' Ops..sth wrong..quota excceed, change acc token to ', access_token
            continue 
        venues = json.dumps(data['response']) + '\n'
        try:
            fj.write(venues)
        except Exception, e:
            print 'write file exception'

    #deal with missing list
    print 'now processing missing venues:', mlist

    if not mlist:
        print 'mlist is empty'
    else:
        for counter, vid in enumerate(mlist):
            url = 'https://api.foursquare.com/v2/venues/' + vid + '?oauth_token=token&v=' +vtime
            print url
            try:
                data = urllib2.urlopen(url)
                data = json.loads(data.read())
            except Exception, e:
                print 'Ops'
                continue
            venues = json.dumps(data['response']) + '\n'
            fj.write(venues)
