#!/usr/bin/env python3
# license: LGPL3
import sys
import zmq
import json

if len(sys.argv) != 3:
    print("USAGE: %s <sub_zmq_url> <pub_zmq_url>" % sys.argv[0])
    print("subscribe to zmq publisher on sub_zmq_url (connect)")
    print("publish every msgs received on pub_zmq_url (bind)")
    print("assume format of msgs used by avis-zmqprx")
    sys.exit(2)

# Socket to talk to server
context = zmq.Context()
suscriber = context.socket(zmq.SUB)
publisher = context.socket(zmq.PUB)

print("Subscribing to %s, publishing on %s" % (sys.argv[1],sys.argv[2]) )

suscriber.connect(sys.argv[1])
publisher.bind(sys.argv[2])

suscriber.setsockopt(zmq.SUBSCRIBE, b'')

while True:
    string = suscriber.recv()
    print("%s" % string.decode('utf-8'))
    publisher.send(string)
    publisher.send(b'invalid message')
