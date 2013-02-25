#!/usr/bin/env python3
# license: LGPL3
import sys
import zmq
import json
import base64

if len(sys.argv) != 2 and len(sys.argv) != 3:
    print("USAGE: %s <zmq_url> [connect|bind]" % sys.argv[0])
    print("subscribe to zmq publisher on zmq_url (connect by default)")
    print("assume format of msgs used by avis-zmqprx")
    sys.exit(2)

# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.SUB)

print("Subscribing to zmq_avis_sub ...")
if len(sys.argv) == 3 and sys.argv[2] == "bind":
    socket.bind(sys.argv[1])
else:
    socket.connect(sys.argv[1])

socket.setsockopt(zmq.SUBSCRIBE, b'')

while True:
    string = socket.recv_unicode()
    try:
        o = json.loads(string)
    except ValueError:
        continue
    for k, val in o.items():
        if type(val) == list:
            s = base64.b64decode(val[0].encode('ascii'));
            print("%s: " % k,end="")
            sys.stdout.flush()
            sys.stdout.buffer.write(s);
            sys.stdout.flush()
            print()
        else:
            print("%s: %r" % (k,val))
    print("---------------")
