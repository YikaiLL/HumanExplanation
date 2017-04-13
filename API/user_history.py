import pymysql
import json
db=pymysql.connect("localhost","Lei"," .","recommendation")
#create cursor
cursor = db.cursor() 
#create new table
cursor.execute("DROP TABLE IF EXISTS user_history")
sql="""CREATE TABLE user_history(userid char(255),
       venueid LONGTEXT)"""
cursor.execute(sql)
with open('4s_venues_twitter.json','r') as data_file:
    data= json.load(data_file)
with open('london.txt','r') as data_file1:
    for line in data_file1:
        value=line.split(' ')
        user_id=value[0]
        venue_id=value[1]
        sql1="INSERT INTO user_history(userid,venueid) Values(%s,%s)"
        cursor.execute(sql1,(user_id,venue_id))
        db.commit()
cursor.close()
db.close()