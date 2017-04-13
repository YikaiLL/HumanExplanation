#!/usr/bin/env python
# -*- coding : utf-8 -*-
# Create a json file from a bunch of tar.gz processed by tweetf0rm 

import tarfile,os
import sys
import json

tarf = ['1489874178.tar.gz', '1489878050.tar.gz', '1489881920.tar.gz', '1489885806.tar.gz', '1489889688.tar.gz', '1489930953.tar.gz']

wdic = {}
for fname in tarf:
    tar = tarfile.open(fname)
    dic = {}
    print "processing " , fname
    for counter, member in enumerate(tar.getmembers()):
        f = tar.extractfile(member)
        name = member.name[11:]
        print "processing ", name
        out = {}
        for i, line in enumerate(f):
            try:
                tweet = json.loads(line)
            except Exception, e:
                print i , line
                continue
            try:
                output = tweet['text']
            except Exception, e:
                print "empty text in ", fname, " file: ", name 
                continue
            out[i] = output
            if(i >= 200):
                break
        dic[name] = out
    wdic.update(dic)

print "write to file"

with open('ofilename.json', 'w') as fj:
    fj.write(json.dumps(wdic))
tar.close()
