import keyconfig# Create the file and add API_KEY = "XXX"
import requests
import json
# remove here
API_KEY =  '';

job_id_generate = " "
job_id_evaluate = " "

GENERATIONS_PER_RECOMMENDATION = 3
EVALUATIONS_PER_RECOMMENDATION = 3


# Create a new row on the job with the data
# @return The ID of the job
def addData(job_id, data):

    # Can use data directly
    #data = {
    #    "poi_name":data["poi_name"],
    #    "poi_tags":data["poi_tags"],
    #    "poi_img":data["poi_img"],
    #    "poi_text":data["poi_text"],
    #    "poi_location":data["poi_location"],
    #    "user_tags":data["user_tags"],
    #    "user_img":data["user_img"],
    #    "user_history":data["user_history"]
    #}

    request_url = "https://api.crowdflower.com/v1/jobs/{}/units.json".format(job_id)
    headers = {'content-type': 'application/json'}

    payload = {
        'key': API_KEY,
        'unit': {
            'data': data
        }
    }

    try:
        result = requests.post(request_url, data=json.dumps(payload), headers=headers, timeout=1)
    except Exception as e:
        print(e)
        return -2

    if (result.status_code == requests.codes.ok):
        parsed = json.loads(result.text)
        return parsed["id"]
    else:
        return 0



# Send a job to evaluate step if the fist step is done
def sendToEvaluate(job_id, unit_id):
    request_url = "https://api.crowdflower.com/v1/jobs/{}/units/{}/judgments.json".format(job_id, unit_id)
    payload = {
        'key': API_KEY,
    }

    rowResult = requests.get(request_url, data=payload)

    parsed = json.loads(rowResult.text)

    if len(parsed) == GENERATIONS_PER_RECOMMENDATION:
        nlExplanation = [];
        for x in parsed:
            nlExplanation.append(x["data"]["nl"])
    else: 
        # Job not done
        return 0

    generateData = parsed[0]['unit_data']
    #print generateData

    data = {
        "poi_name":generateData["poi_name"],
        "poi_tags":generateData["poi_tags"],
        "poi_img":generateData["poi_img"],
        "poi_location":generateData["poi_location"],
        "user_tags":generateData["user_tags"],
        "user_img":generateData["user_img"],
        "user_history":generateData["user_history"],
        "poi_text":generateData["poi_text"],
        "poi_desc":generateData["poi_desc"],
    }

    for i in range(0, GENERATIONS_PER_RECOMMENDATION):
        data["nl_" + str(i+1)] = nlExplanation[i]

    request_url = "https://api.crowdflower.com/v1/jobs/{}/units.json".format(job_id_evaluate)
    headers = {'content-type': 'application/json'}

    payload = {
        'key': API_KEY,
        'unit': {
            'data': data
        }
    }
    try:
        result = requests.post(request_url, data=json.dumps(payload), headers=headers, timeout=5)
    except Exception as e:
        print(e)
        return -2

    if (result.status_code == requests.codes.ok):
        parsed = json.loads(result.text)
        return parsed["id"]
    else:
        return -1





# Get the result of the job if the result is available
def getResult(unit_id):
    request_url = "https://api.crowdflower.com/v1/jobs/{}/units/{}/judgments.json".format(job_id_evaluate, unit_id)
    payload = {
        'key': API_KEY,
    }

    try:
        rowResult = requests.get(request_url, data=payload, timeout=0.1)
    except Exception as e:
        print(e)
        return -2

    

    parsed = json.loads(rowResult.text)

    score1 = 0;
    score2 = 0;
    score3 = 0;

    if len(parsed) == EVALUATIONS_PER_RECOMMENDATION:
        for x in parsed:
            if (x["data"]["sample_radio_buttons"] == "Explanation 1"):
                score1 += 1
            elif (x["data"]["sample_radio_buttons"] == "Explanation 2"):
                score2 += 1
            elif (x["data"]["sample_radio_buttons"] == "Explanation 3"):
                score3 += 1


        if (score1 >= score2 and score1 >= score3):
            return parsed[0]["unit_data"]["nl_1"]
        elif (score2 >= score3):
            return parsed[0]["unit_data"]["nl_2"]
        else:
            return parsed[0]["unit_data"]["nl_3"]
    else: 
        # Job not done
        return None

# Copy the natural language generation job
# After just add data and launch the job
def copyJob():
    request_url = "https://api.crowdflower.com/v1/jobs/{}/copy.json".format(job_id_generate)
    headers = {'content-type': 'application/json'}
    payload = {
        'key': API_KEY,
    }

    r = requests.get(request_url, data=json.dumps(payload), headers=headers)
    parsed = json.loads(r.text)
    new_job_id = parsed["id"]

    return new_job_id

def createAndLaunchNewJob(data):
    new_job_id = copyJob()

    print("Job copied ID: " + str(new_job_id))
    addData(new_job_id, data)
    print("Data added")
    #data_id = launchJob(new_job_id)
    print("Job launched Data ID: " + str(data_id))
    

def launchJob(job_id, amount = 1):
    request_url = "https://api.crowdflower.com/v1/jobs/{}/orders.json".format(job_id)
    headers = {'content-type': 'application/json'}
    payload = {
        'key': API_KEY,
    }

    data = [
        ('channels[0]', 'cf_internal'),
        ('debit[units_count]', amount),
    ]

    r = requests.post(request_url, params=payload, data=data)
    parsed = json.loads(r.text)
    data_id = parsed["id"]
    return data_id

def ping(job_id):
    request_url = "https://api.crowdflower.com/v1/jobs/{}/ping.json".format(job_id)
    headers = {'content-type': 'application/json'}
    payload = {
        'key':  ,
    }

    r = requests.get(request_url, params=payload, headers=headers)
    parsed = json.loads(r.text)
    print(r.text)

    if parsed['needed_judgments'] == 0:
        return True

    return False


import retinasdk

# Return the 3 best comments based on similarity between comment phrases and user topics
def findBestComments(venueComments, topics):
    comments = []

    top = topics.replace(",", "").split(" ")
    # remove here
    liteClient = retinasdk.LiteClient(" ")
    for c in venueComments:
        score = 0
        if not(c[1] == None):
            for topic in top:
                #core = score + liteClient.compare(c[1], topic)
                score = max(score, liteClient.compare(c[1], topic))
        comments.append( (c[0], c[1], score) )

    comments_sorted = sorted(comments, key=lambda tup: tup[2], reverse=True)

    return comments_sorted[:3]