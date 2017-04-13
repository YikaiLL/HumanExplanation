#/usr/bin/env python
# -*- coding: utf-8 -*-
# processing multiple root json as custom json or csv
import json
import csv
from UnicodeWriter import UnicodeWriter
        
with open('4s_venues.json', 'r') as f, open('output_file.csv', 'w') as fp:

    writer = UnicodeWriter(fp) 

    for i, line in enumerate(f):
        response = json.loads(line)

        uid = response['venue']['id']
        name = response['venue']['name']
        address = response['venue']['location']['address']
        city = reponse['venue']['location']

        writer.writerow([uid, text])
