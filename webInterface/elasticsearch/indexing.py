from datetime import datetime
from elasticsearch import Elasticsearch
import json
es = Elasticsearch()

with open('4s_venues.json') as f:
    for i, line in enumerate(f):
        venue = json.loads(line)
        vid = venue['venue']['id']
        res = es.index(index="testvenue", doc_type='venue', id=vid, body=json.dumps(venue))
print(res['created'])

#test indexing
res = es.get(index="testvenue", doc_type='venue', id='5297984a498e59fe04d4784e')
print(type(json.dumps(res['_source'])))

#es.indices.refresh(index="test-index")
#test query
res = es.search(index="testvenue", body={"query": {"query_string": {"query":"restaurant","query":"India"}}})
#return 10 values each time
#for hit in range(res['hits']['total']):
#    print len(res['hits']['hits'])
#    print res['hits']['hits'][hit]['_source']['venue']['name']
#print("Got %d Hits:" % res['hits']['total'])
#for hit in res['hits']['hits']:
#    print("%(timestamp)s %(author)s: %(text)s" % hit["_source"])
