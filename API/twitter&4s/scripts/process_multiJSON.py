#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Processing multiple root json as custom json or csv

import json
import csv

with open('4s_venues.json', 'r') as f, open('output_file.json', 'w') as fp:

    out = {}

    for line in f:
        tweet = json.loads(line)
        print tweet, type(tweet)

        uid = tweet['id'] 
        output = {
            'text': tweet['verified'],
            'id': tweet['id'],
        }

        out[uid] = output
    
    fp.write(json.dumps(out))
        
with open('twitter_id_json', 'r') as f, open('output_file.csv', 'w') as fp:

    writer = csv.writer(fp) 

    for line in f:
        tweet = json.loads(line)

        uid = tweet['id']
        text = tweet['verified']

        writer.writerow([uid, text])
