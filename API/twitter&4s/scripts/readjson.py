#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json

filename = raw_input('filename:')
key = raw_input("input key:")
with open(filename, 'r') as fr:
    data = json.load(fr)
    print data[key]
