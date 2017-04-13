import pymysql
import json
def remove_non_utf8(text):
    text=text.replace('\n','').replace('\r','')
    return ''.join([i if ord(i) < 128 else ' ' for i in text])

db=pymysql.connect("localhost","Lei"," .","recommendation")
cursor = db.cursor() 
cursor.execute("DROP TABLE IF EXISTS user")
sql="""CREATE TABLE user(userid char(255) PRIMARY KEY,
       username LONGTEXT,
       description LONGTEXT, 
       usercity LONGTEXT, 
       usercountry LONGTEXT, 
       profile_image LONGTEXT)"""
cursor.execute(sql)
flag=0
with open ('twitter_id.json') as data:
    for line in data:
        tweet=json.loads(line)
        try:
            userid=tweet['id']
        except:
            userid=None
            continue
        try:    
            description=remove_non_utf8(tweet['description'])
        except:
            description=None
        try:
            profile_image=remove_non_utf8(tweet['profile_image_url'])
        except:
            profile_image=None
        try:
            name=remove_non_utf8(tweet['screen_name'])
        except:
            name=None
        city='London'
        country='UK'
        sql1="INSERT INTO user(userid,description,profile_image,username,usercity,usercountry) Values(%s,%s,%s,%s,%s,%s)"
        cursor.execute(sql1,(userid,description,profile_image,name,city,country))
        db.commit()
        flag=flag+1
        #print(flag)
cursor.close()
db.close()
print(flag)