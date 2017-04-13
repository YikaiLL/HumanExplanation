import pprint
import json
import pymysql
from foursquareAPI import get_venue_categories
db=pymysql.connect("localhost","Lei"," .","recommendation")
cursor = db.cursor() 
cursor.execute("DROP TABLE IF EXISTS venue_categories")
sql="""CREATE TABLE venue_categories(venueid char(255), 
       venuecategories LONGTEXT)"""
cursor.execute(sql)
with open('4s_venues_twitter.json','r') as data_file:
	data= json.load(data_file)
for key, value in data.items():
    venue_categories=get_venue_categories(data,key)
    if venue_categories!=None:
      for string in venue_categories:
        sql1="INSERT INTO venue_categories(venueid,venuecategories) Values(%s,%s)"
        try:
          cursor.execute(sql1,(key,string))
          db.commit()
          flag=flag+1
        except:
          db.rollback()


cursor.close()
db.close()