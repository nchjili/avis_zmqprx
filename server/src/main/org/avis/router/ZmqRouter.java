/*
 *  ZMQ<->AVIS proxy.
 *  
 *  Copyright (C) 2013 Lukas Vacek <lucas.vacek@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 3 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.avis.router;

import java.util.*;
import java.lang.*;
import java.math.BigInteger;
import java.io.*;
import org.avis.io.messages.*;
import org.avis.security.Keys;
import static org.avis.logging.Log.*;

import org.zeromq.ZMQ;
import org.codehaus.jackson.map.ObjectMapper;

public class ZmqRouter
{
  class ZmqNotifyListener implements NotifyListener {
      private ObjectMapper objectMapper = new ObjectMapper();
      @Override
      public void notifyReceived(Notify message, Keys keys) {
          trace ("Publishing notification on ZMQ.",ZmqRouter.class);
          try {
              Map<String,Object> as_map = new HashMap<String,Object>();
              // we want to change fields a bit
              // so writeVAlueAsString works nicely (for example substitute NaN with null)
              // so we need to copy attributes to a new map so we are not
              // messing with the original message
              for (Map.Entry<String, Object> entry: message.attributes.entrySet()) {
                  if (entry.getValue() instanceof Double &&
                       ((Double)entry.getValue()).isNaN() ) {
                      as_map.put(entry.getKey(),null);
                  } else if (entry.getValue() instanceof byte[]) {
                      // the reason to wrap this field in an array is
                      // so that the client can determine original avis type
                      // of the field - JSON String = AVIS String, JSON list containg
                      // one String = AVIS Opaque
                      List a = new ArrayList<byte[]>();
                      a.add(entry.getValue());
                      as_map.put(entry.getKey(), a);
                  } else {
                    as_map.put(entry.getKey(), entry.getValue());
                  }
              }
              String result = objectMapper.writeValueAsString(as_map);
              publisher.send(result);
          } catch(IOException exc) {
              warn ("Couldn't convert notification to JSON, ignoring",ZmqRouter.class);
          }
      }
  }

  class ZmqSubscriber extends Thread {
      private ObjectMapper objectMapper = new ObjectMapper();
      private boolean stopMe = false;

      public void close() {
          stopMe = true;
          try {
              join();
          } catch (InterruptedException exc) {
          }
      }

      public void run() {
          while (true) {
              if (stopMe) { break; }
              String as_string = subscriber.recvStr(ZMQ.NOBLOCK);
              if (as_string != null) {
                  try {
                      Map<String, Object> as_map = objectMapper.readValue(as_string, Map.class);
                      for (Map.Entry<String, Object> entry: as_map.entrySet() ) {
                          if (entry.getValue() == null) {
                              as_map.put(entry.getKey(),Double.NaN);
                          } else if (entry.getValue() instanceof List) {
                              List l = (List)entry.getValue();
                              byte[] data = objectMapper.convertValue(l.get(0) ,byte[].class);
                              as_map.put(entry.getKey(), data );
                          } else if (entry.getValue() instanceof BigInteger) {
                              warn("Too big Integer received on ZMQ, truncating.",ZmqRouter.class);
                              BigInteger orig_val = (BigInteger)entry.getValue();
                              Double as_double = (Double)(orig_val.doubleValue());
                              Long as_long = (Long)(as_double.longValue());
                              as_map.put(entry.getKey(), as_long);
                          }
                      }
                      Notify notify = new NotifyEmit(as_map);
                      router.injectNotify(notify);
                      trace ("ZMQ notification received.",ZmqRouter.class);
                  } catch(IOException exc) {
                      warn ("Invalid ZMQ msg received, ignoring",ZmqRouter.class);
                  }
              }
              // sleep for 1 ms
              try {
                Thread.sleep(1);
              } catch (InterruptedException exc) {
              }
          }
          subscriber.close();
      }
  }

  static ZMQ.Context context = ZMQ.context(1);
  static String DEFAULT_PUB_ADDRESS = "tcp://127.0.0.1:5555";
  static String DEFAULT_SUB_ADDRESS = "tcp://127.0.0.1:5556";

  private Router router;
  private NotifyListener notifyListener = new ZmqNotifyListener();
  private ZmqSubscriber subscriberThread = new ZmqSubscriber();  
  private ZMQ.Socket publisher;
  private ZMQ.Socket subscriber;

  public ZmqRouter (Router router, String zmq_pub_address, boolean pub_bind,
                                   String zmq_sub_address, boolean sub_bind)
    throws IOException
  {
      this.router = router;

      publisher = context.socket(ZMQ.PUB);
      if (pub_bind) {
        publisher.bind(zmq_pub_address);
      } else {
        publisher.connect(zmq_pub_address);
      }

      subscriber = context.socket(ZMQ.SUB);
      if (sub_bind) {
          subscriber.bind(zmq_sub_address);
      } else {
          subscriber.connect(zmq_sub_address);
      }
      subscriber.subscribe("".getBytes());
  }
  
  public ZmqRouter (Router router)
    throws IOException
  {
    this (router, DEFAULT_PUB_ADDRESS, true, DEFAULT_SUB_ADDRESS, false);
  }

  public void start() {
      router.addNotifyListener(notifyListener);
      subscriberThread.start();
  }
  
  public void close() {
      subscriberThread.close();
      publisher.close();
      context.term();
  }
}
