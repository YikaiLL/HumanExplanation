#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Update existing JSON

import json
import urllib2

mlist = ['4ac518cef964a52021a620e3', '4ce5029cb43737040374f6ce', '51125cd8e4b0042c1a2e030b', '5098e77be4b0eecea4e5a309', '4e0f135bb0fb59de67dc7671', '4de9be977d8b6c7a532d5730', '4acdabe2f964a5209fcc20e3', '4ff4d40be4b04f7676e24f99', '4ad22051f964a52089df20e3', '4ad73549f964a520fd0821e3', '4ac518b6f964a52009a120e3', '52483ccd498ee2dda9ced486', '4aceff2cf964a5203cd220e3', '4cadd5dc628cb1f7ffd92e15', '4ccee2b0c9b846883431cbc3', '4c47e8ec972c0f47b66a2521', '4e19f551ae6092c276608626', '4b17ee62f964a520d5c923e3', '4fa33e54e4b0fd4c3d634664', '4f0b0491e4b09a8a418e36be', '500347cee4b086066f8c933b', '4ace3e99f964a52060cf20e3', '4b600d26f964a52040d429e3', '525e8c3f11d2d879b0fbc3eb', '4f76d6b4e4b015b23c712239', '4e4fe2796365e1419d02f181', '4bbdbc268ec3d13a07581c28', '4f3a2cace4b059425a4a5959', '50043e7ee4b0d5040faa429d', '4b9df0b2f964a52097c336e3', '4de8814252b1741cdb046dbe', '4ae3634ff964a5205a9421e3']  

#with open('missing_venue.txt') as f:
#    for line in f:
#        mlist.append(line)

print len(mlist)
vtime = '20170315'

with open('4s_venues.json', 'r') as fp:
    json_data = json.load(fp)
    print 'finish load'
    for vid in mlist:
        url = 'https://api.foursquare.com/v2/venues/' + vid + '?oauth_token=token&v=' +vtime
        print url
        #try:
        data = urllib2.urlopen(url)
        data = json.loads(data.read())
        #except Exception, e:
          #  print 'Ops'
           # continue
        result = data['response']
        json_data[vid] = result


with open('4s_venues.json', 'r+') as fi:
    fi.write(json.dumps(json_data))
