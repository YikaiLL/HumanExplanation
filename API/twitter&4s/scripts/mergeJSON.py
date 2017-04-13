#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Merge JSON from different files

import json
import csv
import os
from os import walk
import re

f =[]
for (dirpath, dirnames, filenames) in walk("dirpath"):
    f.extend(filenames)
    break

for i, n in enumerate(f):
    if(re.match('[0-9]', n) is None):
        f.pop(i)
f.pop()

dic = {}
with open('out.json', 'w') as fp:
    for index, uid in enumerate(f):
        with open(f[index], 'r') as fa:
            out = {} 
            for i, line in enumerate(fa):
                tweet = json.loads(line)
                output = tweet['text']
                out[i] = output
        dic[uid] = out 
    fp.write(json.dumps(dic))
