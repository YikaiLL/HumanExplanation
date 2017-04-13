#!/usr/bin/env python
# -*- coding: utf-8 -*-

import csv
import json
import getpass
import requests
import optparse
import cookielib
import foursquare
from bs4 import BeautifulSoup as BS

usage = 'Usage: %prog [options] arg1 arg2'
parser = optparse.OptionParser(usage=usage, version='%prog 1.0')
parser.add_option('-t', '--get-token', action='store_true', dest='gettoken', default=False, help='get one 4s token')
parser.add_option('-l', '--input', dest='inputfile', type='string', help='input a file, returns a list of tokens')
parser.add_option('-o', '--output', dest='outputfile', type='string', help='write tokens to file')
#todo
parser.add_option('-c', '--crawl-app', dest='number', type='int', help='crawl a list of client ID and client secert to file')
parser.add_option('-v', '--get-venues', action='store_true', dest='getvenue', default=False, help='get 4s venues')
parser.add_option('-p', '--get-tips', action='store_true', dest='getvenue', default=False, help='get 4s tips')

opts, args = parser.parse_args()

#automatically crawl client id and secret
def crawl_app(session):
    app_url = 'https://foursquare.com/developers/apps'     
    

    return

def read_file(filename, session):
    print 'read ', filename
    cli_ids = []
    cli_sec = [] 
    token_list = []
    with open(filename, 'r') as fr:
        for i, line in enumerate(fr):
            line = line.split('\n')[0]
            if(i % 2 == 0):
                cli_ids.append(line)
            else:
                cli_sec.append(line)

    for i in range(len(cli_ids)):
        token = get_token(cli_ids[i], cli_sec[i], session)
        token_list.append(token)
    print 'token list:', [ x.encode('utf-8') for x in token_list] 
    return token_list

def write_file(token_list, filename):
    with open(filename, 'w') as fw:
        writer = csv.writer(fw)
        for token in token_list:
            writer.writerow([token])
    print 'write tokens to', filename

#todo
def login():
    username = raw_input('Foursquare username: ')
    password = getpass.getpass('Foursquare password: ')

    home_url = 'https://foursquare.com/login'

    #todo    
    session = requests.session()
    session.cookies = cookielib.LWPCookieJar(filename = 'cookies')
    print session.cookies
#    session.cookies.load(ignore_discard = True)

    headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36",
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Encoding": "gzip, deflate",
            "Host": "foursquare.com",
            "Upgrade-Insecure-Requests": "1",
         }

    html = session.get(home_url).content
    homepage = BS(html, 'html.parser')
    _xsrf = homepage.find('input', {'name': 'fs-request-signature'})['value']
    
    data = {
            'emailOrPhone':username,
            'password':password,
            'fs-request-signature': _xsrf,
            }

    #login
    resp = session.post(home_url, data, headers)

    if(resp.status_code != 200):
        print 'login failure'

#    session.cookies.save()

    return session

def get_token(cli_id, cli_sec, session):
    client_id = cli_id
    client_secret = cli_sec
    client = foursquare.Foursquare(client_id=client_id, client_secret=client_secret, redirect_uri='http://127.0.0.1:3000/auth/return')
    
    auth_url = client.oauth.auth_url()
    
    print 'get access token...'
    
    token = ''
    try:
        # for those first time get access token ,and for those already have the token will throw an exception
        html = session.get(auth_url).content
        authpage = BS(html, 'html.parser')
        _xsrf = authpage.find('input', {'name': 'fs-request-signature'})['value']
        data = {
                'shouldAuthorize': 'true',
                'fs-request-signature': _xsrf
                }
        # the stmt will throw a exception when send data
        resp = session.post(auth_url, data, headers)
    except requests.exceptions.ConnectionError, e:
        print 'access_token:'
        code = str(e).split('?')[1][5:53]
        access_token = client.oauth.get_token(code)
        token = access_token
        print access_token 

    return token 
    
def main():
    if opts.gettoken:
        session = login()
        client_id = raw_input('Client ID: ')
        client_secret = raw_input('Client Secret: ')
        redirect_uri = raw_input('Redirect uri: ')
        get_token(client_id, client_secret, session)

    if(opts.inputfile != None):
        session = login()
        token_list = read_file(opts.inputfile, session)        

    if(opts.outputfile != None):
        write_file(token_list, opts.outputfile)

if __name__ =='__main__':
    main()
