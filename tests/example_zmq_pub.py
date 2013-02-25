#!/usr/bin/env python3
# license: LGPL3
import sys
import zmq
import json
import time
import base64

if len(sys.argv) != 2 and len(sys.argv) != 3:
    print("USAGE: %s <zmq_url> [connect|bind]" % sys.argv[0])
    print("publish on zmq_url (bind by default)")
    print("assume format of msgs used by avis-zmqprx")
    sys.exit(2)

# Socket to talk to server
context = zmq.Context()
socket = context.socket(zmq.PUB)

print("Publishing ...")
if len(sys.argv) == 3 and sys.argv[2] == "connect":
    socket.connect(sys.argv[1])
else:
    socket.bind(sys.argv[1])

time.sleep(0.2) # play it safe
data = {
        'Opaque':5425423523544523524523525245,
        'big minus':-5425423523544523524523525245,
        'minus1':-1,
        'Naan':None,
        '_opaque':[base64.b64encode(b"hola").decode('ascii')],
        '_real':452354.5,
        '_string':"yo",
}
socket.send_unicode( json.dumps(data) )
print("done")
socket.close()
context.term()
