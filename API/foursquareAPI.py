import json
import pprint

def print_all_venueId(data):
    for key, value in data.items():
        print(key)
    return


def print_one_item(data,venue_id):
    pprint.pprint(data[venue_id])
    return

def print_one_item_key(data,venue_id):
    for key, value in data[venue_id]['venue'].items():
        print(key)
    return


def get_venue_name(data,venue_id):
	try:
		return data[venue_id]['venue']['name']
	except:
		return None

def get_venue_description(data,venue_id):
	try:
		return data[venue_id]['venue']['description']
	except:
		return None

def get_venue_address(data,venue_id):
	try:
		return data[venue_id]['venue']['location']['address']
	except:
		return None

def get_venue_city(data,venue_id):
	try:
		return data[venue_id]['venue']['location']['city']
	except:
		return None

# Return the list of category
def get_venue_categories(data,venue_id):
	try:
		return [one_category['name'] for one_category in data[venue_id]['venue']['categories']]
	except:
		return None

# An object containing the price tier from 1 (least pricey) - 4 (most pricey)
# and a message describing the price tier.
def get_venue_price_tier(data,venue_id):
	try:
		return data[venue_id]['venue']['price']['tier']
	except:
		return None

def get_venue_price_message(data,venue_id):
	try:
		return data[venue_id]['venue']['price']['message']
	except:
		return None

# Numerical rating of the venue (0 through 10). 
# Returned as part of an explore result, excluded in search results. 
# Not all venues will have a rating.
def get_venue_rating(data,venue_id):
	try:
		return data[venue_id]['venue']['rating']
	except:
		return None


# number of likes by the users
def get_venue_likes(data,venue_id):
	try:
		return data[venue_id]['venue']['likes']['count']
	except:
		return None

# Return list of strings containing the tags
def get_venue_tags(data,venue_id):
	try:
		return data[venue_id]['venue']['tags']
	except:
		return None

# List of phrases commonly seen in this venue's tips, as well as a sample 
# tip snippet and the number of tips this phrase appears in
# return the dict using phrases as the keys,
# and the count number/occurance in the tips as the values.
def get_venue_phrases(data,venue_id):
	# pprint.pprint(data[venue_id]['venue']['phrases'])
	try:
		result={}
		for phrase_item in data[venue_id]['venue']['phrases']:
			result[phrase_item['phrase']]=phrase_item['count']
		return result
	except:
		return None

# Pic_width should be string!!!
def get_venue_photo(data,venue_id,pic_width):
	# Get the best photo first
	try:
		prefix=data[venue_id]['venue']['bestPhoto']['prefix']
		suffix=data[venue_id]['venue']['bestPhoto']['suffix']
		link=prefix+'width'+pic_width+suffix
		return link
	except :
		pass
		# If no bestPhoto, we get the first photo
		try:
			prefix=data[venue_id]['venue']['photos']['groups'][0]['items'][0]['prefix']
			suffix=data[venue_id]['venue']['photos']['groups'][0]['items'][0]['suffix']
			link=prefix+'width'+pic_width+suffix
			return link
		except :
			return None




# Demo..
# should be json_file string here.
# 1. Open the correct json file...
# with open('twitter&4s/4s_venues_twitter.json') as data_file:
# 	data= json.load(data_file)
# 2. Call function...

# print_all_venueId(data)
# print_one_item_key(data,'4ac518b5f964a5208fa020e3')
# pprint.pprint(get_venue_photo(data,'4ac518e2f964a5207eaa20e3','320'))


