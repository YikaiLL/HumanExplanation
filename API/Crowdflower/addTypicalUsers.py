import crowdflower
import pymysql
import json


typicalUsers = {
	'100057597':'4c9c6a059c48236a1cb14dee',
	'100595096':'4bcbd53a68f976b027dd6183',
	'1012917350':'4d5ed23c149637047e12cf94',
	'1014120596':'4ad7a04bf964a520fe0c21e3',
	'1014916676':'4ad06c8ef964a52009d820e3',
	'1017078158':'4acf5c05f964a52036d320e3',
	'19499747':'4b7bf8a3f964a52068762fe3',
	'328607423':'4ad8c647f964a520851421e3',
	'34465303':'4ac518d1f964a520c3a620e3',
}

db=pymysql.connect("localhost","Lei"," .","recommendation")
#create cursor
cursor = db.cursor() 
#create new table

new_job_id = crowdflower.copyJob()
print("Job copied ID: " + str(new_job_id))

#for user, poi in typicalUsers.iteritems(): #Python 2
for user, poi in typicalUsers.items(): #Python 3
	print("\n############## Typical user " + str(user) + " ##############")

	sql="SELECT profile_image FROM user WHERE userid='" + user + "'"
	cursor.execute(sql)
	user_img = cursor.fetchone()[0]

	user_img = user_img.replace("_normal.jpg", ".jpg").replace("_normal.jpeg", ".jpeg")

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

	#sql = "SELECT topic FROM twitter_typical_user_topic WHERE userid='" + user + "' ORDER BY frequency DESC LIMIT 5" #TODO Order
	#cursor.execute(sql)
	#topics = cursor.fetchall()
	#user_tags = ""
	#for t in topics:
	#	user_tags = user_tags + t[0] + ", "
	#user_tags = user_tags[:-2]

	sql = "SELECT keyword1, keyword2, keyword3 FROM twitter_typical_user_topic WHERE userid='" + user + "'"
	cursor.execute(sql)
	topics = cursor.fetchall()

	user_tags = ""
	for a in topics:
		user_tags = user_tags + str(a[0]) + ", " + str(a[1]) + ", " + str(a[2]) + "<br/>"

	sql = "SELECT venuecomments,venuephrases FROM venue_comments WHERE venueid='" + poi + "'" 
	cursor.execute(sql)
	venueComments = cursor.fetchall()

	#venueComments = crowdflower.findBestComments(venuecomments, user_tags)

	poi_text = ""
	i = 1
	for c in venueComments:
		if not(c[0] == None):
			poi_text = poi_text + "<p>" + str(i) + ": " + c[0] + "</p>" 
			i = i + 1
			#poi_tags=poi_tags+c[1]+" "

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


	print(data)

	crowdflower.addData(new_job_id, data)

cursor.close()
db.close()

print("Launch Job...")
crowdflower.launchJob(new_job_id, 10)
print("Launch Job... Done")
