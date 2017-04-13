import pymysql
import json

recommendations = dict()

#i = 5
#file = open("BPRResults\BPR-top-10-items fold ["+str(i)+"].txt", 'r');
file = open("BPRResults\FriendSummary.txt");
file.readline() # userId itemId rating prediction line
for line in file:
    splitline = line.split(" ")
    userID = splitline[0][0:-1]
    for it in range (0, 10):
        itemID = splitline[1 + 2*it][1:-1]
        if not(itemID[-1:] == '*'):
            if not(userID in recommendations):
                recommendations[userID] = [itemID]
            else:
                recommendations[userID].append(itemID)
        else:
            print(userID + " " + itemID)
file.close()



db=pymysql.connect("localhost","Lei","Lay930905.","recommendation")
cursor = db.cursor() 
#cursor.execute("DROP TABLE IF EXISTS poi_list")
#sql="""CREATE TABLE poi_list(
#       userid char(255) PRIMARY KEY,
#       venue1 char(255),
#       venue2 char(255),
#       venue3 char(255))"""
#cursor.execute(sql)
#db.commit()
flag=0


#for user, item in recommendations.iteritems(): #Python 2
for user, item in recommendations.items():
    userid = str(user)

    venue_id1 = str(item[0]) if len(item) >= 1 else None
    venue_id2 = str(item[1]) if len(item) >= 2 else None
    venue_id3 = str(item[2]) if len(item) >= 3 else None

    #print userid + " " + str(venue_id1) + " " + str(venue_id2) + " " + str(venue_id3)

    sql1="INSERT INTO poi_list(userid,venue1,venue2,venue3) Values(%s,%s,%s,%s)"
    cursor.execute(sql1,(userid,venue_id1,venue_id2,venue_id3))
    db.commit()
    flag=flag+1
    print(flag)
cursor.close()
db.close()
print(flag)