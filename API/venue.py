#this is to create a table in the database and insert data into it. THE category and tags will be added soon with the same method. The phrases and comments has not been done.
import pprint
import json
import pymysql
from foursquareAPI import get_venue_name
from foursquareAPI import get_venue_description
from foursquareAPI import get_venue_address
from foursquareAPI import get_venue_city
from foursquareAPI import get_venue_categories
from foursquareAPI import get_venue_price_tier
from foursquareAPI import get_venue_price_message
from foursquareAPI import get_venue_rating
from foursquareAPI import get_venue_likes
from foursquareAPI import get_venue_tags
from foursquareAPI import get_venue_phrases
from foursquareAPI import get_venue_photo    
#def remove_non_utf8(text):
  #  text=text.replace('\n','').replace('\r','')
   # return ''.join([i if ord(i) < 128 else ' ' for i in text])
#connect to database,you need to create database in the mysql first. The second one is the username, the third one is the password, the fourth one is the name of dataset
db=pymysql.connect("localhost","Lei"," .","recommendation",use_unicode=True, charset="utf8mb4")
cursor=db.cursor()
#db.set_character_set('utf8')
cursor.execute('SET NAMES utf8mb4;')
cursor.execute('SET CHARACTER SET utf8mb4;')
cursor.execute('SET character_set_connection=utf8mb4;')
#create cursor
cursor = db.cursor() 
cursor.execute("SELECT VERSION()")
version = cursor.fetchone()
print ("Database version : %s " % version)
#create new table
cursor.execute("DROP TABLE IF EXISTS VENUE")
sql="""CREATE TABLE VENUE(venueid char(255) PRIMARY KEY,
       venuename LONGTEXT,
       venueaddress LONGTEXT, 
       venuecity LONGTEXT, 
       venuerating LONGTEXT, 
       venuephoto LONGTEXT, 
       venuedescription LONGTEXT, 
       venuepricetier LONGTEXT, 
       venuepricemessage LONGTEXT,
       venuelikes FLOAT)"""
       #venuetags LONGTEXT, 
       #venuecategories LONGTEXT)
cursor.execute(sql)
#read data
flag=1
with open('4s_venues_twitter.json','r') as data_file:
    data= json.load(data_file)
for key, value in data.items():
    cursor=db.cursor()
    venue_name=get_venue_name(data,key)
    venue_description=get_venue_description(data,key)
    venue_address=get_venue_address(data,key)
    venue_city=get_venue_city(data,key)
    venue_price_tier=get_venue_price_tier(data,key)
    venue_price_message=get_venue_price_message(data,key)
    venue_rating=get_venue_rating(data,key)
    venue_likes=get_venue_likes(data,key)
    venue_photo=get_venue_photo(data,key,'320')
    #venue_categories=get_venue_categories(data,key)
    #venue_tags=get_venue_tags(data,key)
    #insert data into table
    try:
        sql1="INSERT INTO VENUE(venueid,venuename,venueaddress,venuecity,venuerating,venuephoto,venuedescription,venuepricetier,venuepricemessage,venuelikes) Values(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)"
        cursor.execute(sql1,(key,venue_name,venue_address,venue_city,venue_rating,venue_photo,venue_description,venue_price_tier,venue_price_message,venue_likes))
        db.commit()
    except:
        print(key,venue_name,venue_address,venue_city,venue_rating,venue_photo,venue_description,venue_price_tier,venue_price_message,venue_likes)
        db.rollback()
cursor.close()
db.close()