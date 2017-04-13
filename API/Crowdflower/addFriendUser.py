import crowdflower
import pymysql
import json

def getData(user, poi, url, keywords):
	user_img = url

	sql="SELECT venueid, venuename FROM venue WHERE venueid IN (SELECT venueid FROM user_history WHERE userid='" + user + "')"
	cursor.execute(sql)
	history = cursor.fetchall()

	user_history = ""
	for h in history:
		user_history = user_history + "<br/><a href='https://foursquare.com/v/" + h[0] +"'>" + h[1] + "</a>"

	sql = "SELECT venuename, venueaddress, venuephoto, venuedescription FROM venue WHERE venueid='" + poi +"'"
	cursor.execute(sql)
	venueInfo = cursor.fetchone()
	poi_name = venueInfo[0]
	poi_location = venueInfo[1]
	poi_img = venueInfo[2]
	poi_desc = venueInfo[3]


	sql="SELECT venuetags FROM venue_tags WHERE venueid='" + poi + "'"
	cursor.execute(sql)
	tags = cursor.fetchall()
	poi_tags = ""
	for t in tags:
		poi_tags = poi_tags + t[0] + ", "
	poi_tags = poi_tags[:-2]


	user_tags = keywords

	sql = "SELECT venuecomments,venuephrases FROM venue_comments WHERE venueid='" + poi + "'" 
	cursor.execute(sql)
	venueComments = cursor.fetchall()

	#venueComments = crowdflower.findBestComments(venuecomments, user_tags)

	poi_text = ""
	i = 1
	for c in venueComments:
		poi_text = poi_text + "<p>" + str(i) + ": " + c[0] + "</p>" 
		i = i + 1

	data = {
		"poi_name":poi_name, 
		"poi_tags":poi_tags, 
		"poi_img":poi_img, 
		"poi_location":poi_location, 
	    "user_tags":user_tags,
	    "user_img":user_img, 
	    "user_history":user_history, 
	    "poi_text":poi_text,
	    "poi_desc":poi_desc,
	}
	return data


db=pymysql.connect("localhost","Lei"," .","recommendation")
#create cursor
cursor = db.cursor() 
#create new table

print("\n############## User1 ##############")
user = "Friend1"
poi1 = "4af47862f964a520d6f221e3" # London Liverpool Street Railway Station 
poi2 = "4c9c6a059c48236a1cb14dee" # Piccadilly Circus
poi3 = "4ac518d1f964a520c3a620e3" # Leicester Square
url = "dl.dropboxusercontent.com/s/w9yj336i812ln35/447544316.jpg"
keywords = "Art, Museum, Scene"
data = getData(user, poi3, url, keywords)
print(data)
crowdflower.createAndLaunchNewJob(data)

print("\n############## User2 ##############")
user = "Friend2"
poi1 = "4ac518d1f964a520c3a620e3" # Leicester Square
poi2 = "4acbc300f964a52058c520e3" # London Euston Railway Station (EUS)
poi3 = "4c9c6a059c48236a1cb14dee" # Piccadlly Circus
url = "dl.dropboxusercontent.com/s/mqx8aoshmgvobuw/73415006.jpg"
keywords = "Football, Shopping, Study"
data = getData(user, poi1, url, keywords)
crowdflower.createAndLaunchNewJob(data)

print("\n############## User3 ##############")
user = "Friend3"
poi1 = "52d71ac611d2dae852a0be1f" # Chapati & Karak
poi2 = "4ac518d1f964a520c3a620e3" # Leicester Square
poi3 = "4c9c6a059c48236a1cb14dee" # Piccadlly Circus
url = "dl.dropboxusercontent.com/s/nub4t9z6g6cdukx/webwxgetmsgimg.jpg"
keywords = "Nature, Museum, Study"
data = getData(user, poi1, url, keywords)
crowdflower.createAndLaunchNewJob(data)

print("\n############## User4 ##############")
user = "Friend4"
poi1 = "4af47862f964a520d6f221e3" # London Liverpool Street Railway Station (LST)
poi2 = "4c9c6a059c48236a1cb14dee" # Piccadlly Circus
poi3 = "4bcc08273740b7139c756365" # London St Pancras International Eurostar Terminal
url = "https://scontent-ams3-1.xx.fbcdn.net/v/t1.0-9/10421098_10206752795802729_3108415449634716857_n.jpg?oh=fbc5b2b5259572234642283578d70b96&oe=5996FE96"
keywords = "Computer Science, Nature, Sightseeing" 	 
data = getData(user, poi2, url, keywords)
crowdflower.createAndLaunchNewJob(data)

print("\n############## User5 ##############")
user = "Friend5"
poi1 = "4c9c6a059c48236a1cb14dee" # Piccadlly Circus
poi2 = "4af47862f964a520d6f221e3" # London Liverpool Street Railway Station (LST)
poi3 = "4ac518d1f964a520c3a620e3" # Leicester Square
url = "dl.dropboxusercontent.com/s/fiiyhyexy3uek5v/1997987600.jpg"
keywords = "Shopping, Afternoon tea, Museum"
data = getData(user, poi1, url, keywords)
crowdflower.createAndLaunchNewJob(data)

print("\n############## User6 ##############")
user = "Friend6"
poi1 = "4ac518cef964a52021a620e3" # The London Eye
poi2 = "4ac518eff964a52064ad20e3" # Borough Market
poi3 = "4ac518f4f964a520fbae20e3" # Camden Lock Market
url = "dl.dropboxusercontent.com/s/f9td3z4m83g3723/344087515.jpg"
keywords = "Museum, Study, History"
data = getData(user, poi1, url, keywords)
crowdflower.createAndLaunchNewJob(data)

cursor.close()
db.close()
