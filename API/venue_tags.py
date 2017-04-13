import pprint
import json
import pymysql
from foursquareAPI import get_venue_tags
db=pymysql.connect("localhost","Lei"," .","recommendation")
cursor = db.cursor() 
cursor.execute("DROP TABLE IF EXISTS venue_tags")
sql="""CREATE TABLE venue_tags(venueid char(255), 
       venuetags LONGTEXT)"""
cursor.execute(sql)
with open('4s_venues_twitter.json','r') as data_file:
	data= json.load(data_file)
for key, value in data.items():
    venue_tags=get_venue_tags(data,key)
    if venue_tags!=None:
      for string in venue_tags:
        sql1="INSERT INTO venue_tags(venueid,venuetags) Values(%s,%s)"
        try:
          cursor.execute(sql1,(key,string))
          db.commit()
          flag=flag+1
        except:
          db.rollback()


cursor.close()
db.close()