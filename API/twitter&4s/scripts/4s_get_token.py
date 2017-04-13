#!/usr/bin/env python
# -*- coding: utf-8 -*-
# Get foursquare access tokens using selenium and chromedriver

import json
import foursquare
import urllib 
from selenium import webdriver
import time
import getpass

client_id = raw_input('Client ID: ')
client_secret = raw_input('Client Secret: ')
redirect_uri = raw_input('Redirect URI: ')
# Construct the client object
client = foursquare.Foursquare(client_id=client_id, client_secret=client_secret, redirect_uri=redirect_uri)

# Build the authorization url for your app
auth_uri = client.oauth.auth_url()

print auth_uri

user_name = raw_input('Fsquare username: ')
user_pass = getpass.getpass('Password(hide): ') 
# Path to chromedriver
browser = webdriver.Chrome(‘path’)
browser.get('https://foursquare.com/login?continue=%2F&clicked=true')
browser.find_element_by_name("emailOrPhone").send_keys(user_name)
browser.find_element_by_name("password").send_keys(user_pass)
# Find login button
browser.find_element_by_xpath("/html/body[@class='notrans withHeaderSearch']/div[@id='wrapper']/div[@id='container']/div/div[@id='loginBox']/div[@id='loginForm']/form[@id='loginToFoursquare']/p[@class='loginOrSignup']/input[@id='loginFormButton']").click()

browser.get(auth_uri)
code = browser.current_url
code = code.encode('ascii','ignore')
# The case for first time confirm using 4s app 
if "oauth2" in code: 
    browser.find_element_by_xpath("/html/body[@class='notrans withHeaderSearch withGetTheAppBar']/div[@id='wrapper']/div[@id='container']/div[@id='oauthAuthorize']/div[@class='footer']/div[@class='right']/form[@id='responseForm']/span[@id='allowButton']").click() 
    code = browser.current_url
code = code[-52:-4]
print "code:"+code
browser.quit()

code = code.encode('ascii','ignore')
# Interrogate foursquare's servers to get the user's access_token
access_token = client.oauth.get_token(code)

print 'access_token:'+ access_token

# Apply the returned access token to the client
client.set_access_token(access_token)

# Get the user's data
user = client.users('1183247')
user = json.dumps(user, indent=4, separators=(',',': '))
