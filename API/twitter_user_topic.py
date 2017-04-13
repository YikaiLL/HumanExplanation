import json
import pymysql
db=pymysql.connect(" "," "," ."," ")
#create cursor
cursor = db.cursor() 
#create new table
cursor.execute("DROP TABLE IF EXISTS twitter_user_topic")
sql="""CREATE TABLE twitter_user_topic(userid char(255),
       topic char(255),
       frequency int)"""
cursor.execute(sql)
#read data
with open('twitter_user_topic.json') as data_file:
    data_topic=json.load(data_file)
for key,value in data_topic.items():
	for topic,frequency in value.items():
		sql1="INSERT INTO twitter_user_topic(userid,topic,frequency) Values(%s,%s,%s)"
		cursor.execute(sql1,(key,topic,frequency))
		db.commit() 

cursor.close()
db.close()