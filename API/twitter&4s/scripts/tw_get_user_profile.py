#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Get user profile

import tweepy
import json
import jsonpickle
import csv
import sys

#authorization
auth = tweepy.OAuthHandler('', '')
auth.set_access_token('', '')
api = tweepy.API(auth, wait_on_rate_limit = True, wait_on_rate_limit_notify = True)

#create a id list
with open("london.txt") as fp:
    id_list = [] 
    for line in fp:
        str = line.split(' ')[0]
        id_list.append(str)

id_set = set(id_list)

with open("twitter_id.csv", "w") as fc, open("twitter_id.json", "w") as fj:

    writer = csv.writer(fc)
    
    for counter, uid in enumerate(id_set):
        print counter , ":" , uid
        writer.writerow([uid])
        try:
            user = api.get_user(uid)
        except tweepy.TweepError, e:
            print uid, e.message[0]['code']
            continue 
        json_data = json.dumps(user._json)+'\n'
        fj.write(json_data)
        sys.stdout.flush()

print("mission complete")
