import pymysql
import json
db=pymysql.connect("localhost","Lei"," .","recommendation")
#create cursor
cursor = db.cursor() 
#create new table
cursor.execute("DROP TABLE IF EXISTS venue_comments")
sql="""CREATE TABLE venue_comments(venueid char(255),
       venuecomments LONGTEXT,
       venuephrases LONGTEXT)"""
cursor.execute(sql)
with open('4s_tips_twitter_based_phrases.json','r') as data_file:
    data=json.load(data_file)
#flag=1
for key,values in data.items():
    for phrases,comments in values.items():
        if phrases.isdigit():
            phrases=None
        sql1="INSERT INTO venue_comments(venueid,venuecomments,venuephrases) Values(%s,%s,%s)"
        try:
            cursor.execute(sql1,(key,comments,phrases))
            db.commit()
            flag=flag+1
        except:
            db.rollback()
   # if flag>10:
    #     break
cursor.close()
db.close()