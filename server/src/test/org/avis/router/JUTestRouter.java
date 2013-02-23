/*
 *  Avis event router.
 *  
 *  Copyright (C) 2008 Matthew Phillips <avis@mattp.name>
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.io.Closeable;

import org.avis.io.messages.ConfConn;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRply;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.SecRqst;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubDelRqst;
import org.avis.io.messages.SubModRqst;
import org.avis.io.messages.SubRply;
import org.avis.io.messages.TestConn;
import org.avis.io.messages.UNotify;
import org.avis.security.Key;
import org.avis.security.KeyScheme;
import org.avis.security.Keys;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;

import static org.avis.io.messages.Nack.EXP_IS_TRIVIAL;
import static org.avis.io.messages.Nack.PARSE_ERROR;
import static org.avis.logging.Log.ALARM;
import static org.avis.logging.Log.WARNING;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.enableLogging;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;
import static org.avis.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the router server.
 * 
 * @author Matthew Phillips
 */
public class JUTestRouter
{
  static final int PORT = 29170;

  private Router router;
  private Random random;
  private LogFailTester logTester;
  private ArrayList<Closeable> autoClose;

  @Before
  public void setup ()
  {
    enableLogging (WARNING, true);
    enableLogging (ALARM, true);
    
    random = new Random ();
    logTester = new LogFailTester ();
    autoClose = new ArrayList<Closeable> ();
  }
  
  @After
  public void tearDown ()
  {
    if (router != null)
      router.close ();
    
    for (int i = autoClose.size () - 1; i >= 0; i--)
    {
      try
      {
        autoClose.get (i).close ();
      } catch (Throwable ex)
      {
        alarm ("Failed to close", this, ex);
      }
    }
    
    logTester.assertOkAndDispose ();
  }

  
  @Test
  public void connect ()
    throws Exception
  {
    router = new Router (PORT);
    SimpleClient client = new SimpleClient ();
    
    ConnRqst connRqst = new ConnRqst (4, 0);
    client.send (connRqst);
    
    ConnRply reply = (ConnRply)client.receive ();
    
    assertEquals (connRqst.xid, reply.xid);
    
    DisconnRqst disconnRqst = new DisconnRqst ();
    client.send (disconnRqst);
    
    DisconnRply disconnRply = (DisconnRply)client.receive ();
    assertEquals (disconnRqst.xid, disconnRply.xid);
    
    client.close ();
    router.close ();
  }
  
  /**
   * Test connection options.
   * 
   * @see JUTestClientConnectionOptions
   */
  @Test
  public void connectionOptions ()
    throws Exception
  {
    router = new Router (PORT);
    SimpleClient client = new SimpleClient ("localhost", PORT);
    
    HashMap<String, Object> options = new HashMap<String, Object> ();
    options.put ("Packet.Max-Length", 1024);
    options.put ("Subscription.Max-Count", 16);
    options.put ("Subscription.Max-Length", 1024);
    options.put ("Attribute.Opaque.Max-Length", 2048 * 1024);
    options.put ("TCP.Send-Immediately", 1);
    options.put ("Bogus", "not valid");
    
    ConnRply connReply = client.connect (options);
    
    // todo: when Attribute.Opaque.Max-Length supported, switch lines below
    // assertEquals (2048 * 1024, reply.options.get ("Attribute.Opaque.Max-Length"));
    assertEquals (Integer.MAX_VALUE, connReply.options.get ("Attribute.Opaque.Max-Length"));
    assertEquals (1, connReply.options.get ("TCP.Send-Immediately"));
    assertEquals (1024, connReply.options.get ("Packet.Max-Length"));
    assertEquals (16, connReply.options.get ("Subscription.Max-Count"));
    assertEquals (1024, connReply.options.get ("Subscription.Max-Length"));
    assertNull (connReply.options.get ("Bogus"));
    
    // try to send a frame bigger than 1K, check server rejects
    SecRqst secRqst = new SecRqst ();
    secRqst.addNtfnKeys = new Keys ();
    secRqst.addNtfnKeys.add (SHA1_PRODUCER, new Key (new byte [1025]));
    
    logTester.pause ();
    
    client.send (secRqst);
    Message reply = client.receive ();
    
    assertTrue (reply instanceof Disconn);
    // check the disconnect message advises looking at Packet.Max-Length
    assertTrue (((Disconn)reply).args.contains ("Packet.Max-Length"));
    
    client.closeImmediately ();
    
    router.close ();
    
    logTester.unpause ();
    
    router = new Router (PORT);
    
    // remove packet length restriction for following tests
    options.remove ("Packet.Max-Length");
    
    // test Subscription.Max-Count enforcement
    
    client = new SimpleClient ("localhost", PORT);
    client.connect (options);
    
    for (int i = 0; i < 16; i++)
      client.subscribe ("Count == " + i);
    
    SubAddRqst subAddRqst = new SubAddRqst ("Invalid == 1");
    client.send (subAddRqst);
    Nack nack = (Nack)client.receive ();
    
    assertEquals (subAddRqst.xid, nack.xid);
    client.close ();
    
    // test Subscription.Max-Length enforcement
    
    client = new SimpleClient ("localhost", PORT);
    client.connect (options);
    
    subAddRqst = new SubAddRqst (dummySubscription (2048));
    client.send (subAddRqst);
    nack = (Nack)client.receive ();
    
    assertEquals (subAddRqst.xid, nack.xid);
    client.close ();
    
    // test Subscription.Max-Keys and Connection.Max-Keys enforcement
    
    int maxConnKeys = CONNECTION_OPTION_SET.getMaxValue ("Connection.Max-Keys");
    int maxSubKeys = CONNECTION_OPTION_SET.getMaxValue ("Subscription.Max-Keys");

    options = new HashMap<String, Object> ();
    options.put ("Connection.Max-Keys", maxConnKeys);
    options.put ("Subscription.Max-Keys", maxSubKeys);
    
    client = new SimpleClient ("localhost", PORT);
    client.connect (options);
    
    Keys keys = new Keys ();
    
    for (int i = 0; i < maxConnKeys + 1; i++)
      keys.add (KeyScheme.SHA1_CONSUMER, new Key (randomBytes (128)));
    
    secRqst = new SecRqst (keys, EMPTY_KEYS, EMPTY_KEYS, EMPTY_KEYS);
    client.send (secRqst);
    nack = (Nack)client.receive ();
    assertEquals (secRqst.xid, nack.xid);
    
    subAddRqst = new SubAddRqst ("n == 1");
    subAddRqst.keys = keys;
    
    client.send (subAddRqst);
    nack = (Nack)client.receive ();
    assertEquals (subAddRqst.xid, nack.xid);
    
    client.close ();
    
    router.close ();
  }
  
  /**
   * Test connection options specified in router config.
   */
  @Test
  public void routerOptions ()
    throws Exception
  {
    RouterOptions options = new RouterOptions (PORT);
 
    options.set ("Packet.Max-Length", 1024);
    options.set ("Subscription.Max-Count", 16);
    options.set ("Subscription.Max-Length", 1024);
    //options.set ("Attribute.Opaque.Max-Length", 2048 * 1024);
    
    router = new Router (options);
    SimpleClient client = new SimpleClient ("localhost", PORT);

    client.connect ();
    
    // try to send a frame bigger than 1K, check server rejects
    SecRqst secRqst = new SecRqst ();
    secRqst.addNtfnKeys = new Keys ();
    secRqst.addNtfnKeys.add (SHA1_PRODUCER, new Key (new byte [1025]));
    
    logTester.pause ();
    
    client.send (secRqst);
    Message reply = client.receive ();
    
    router.close ();
    
    logTester.unpause ();
    
    assertTrue ("Expected a Disconn", reply instanceof Disconn);
    
    client.closeImmediately ();
  }
  
  /**
   * Use the simple client to run through a connect, subscribe, emit,
   * change sub, disconnect sequence.
   */
  @Test
  public void subscribe ()
    throws Exception
  {
    router = new Router (PORT);
    SimpleClient client = new SimpleClient ();
    
    client.connect ();
    
    SubAddRqst subAddRqst = new SubAddRqst ("number == 1");
    client.send (subAddRqst);
    
    SubRply subReply = (SubRply)client.receive ();
    assertEquals (subAddRqst.xid, subReply.xid);
    
    // check NACK on bad subscription
    subAddRqst = new SubAddRqst ("(1 + 1");
    client.send (subAddRqst);
    
    Nack nackReply = (Nack)client.receive ();
    assertEquals (PARSE_ERROR, nackReply.error);
    
    subAddRqst = new SubAddRqst ("1 == 1");
    client.send (subAddRqst);
    
    nackReply = (Nack)client.receive ();
    assertEquals (EXP_IS_TRIVIAL, nackReply.error);
    
    // send notification
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("number", 1);
    
    client.send (new NotifyEmit (ntfn));
    NotifyDeliver notifyDeliver = (NotifyDeliver)client.receive ();
    
    assertEquals (0, notifyDeliver.secureMatches.length);
    assertEquals (1, notifyDeliver.insecureMatches.length);
    assertEquals (subReply.subscriptionId, notifyDeliver.insecureMatches [0]);
    assertEquals ("foobar", notifyDeliver.attributes.get ("name"));
    assertEquals (1, notifyDeliver.attributes.get ("number"));
    
    // send non-matching ntfn
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("number", 2);
    
    // should get no reply to next: following tests will fail if not
    client.send (new NotifyEmit (ntfn));
    
    // modify subscription
    SubModRqst subModRqst =
      new SubModRqst (subReply.subscriptionId, "number == 2", true);
    client.send (subModRqst);
    
    subReply = (SubRply)client.receive ();
    assertEquals (subReply.xid, subModRqst.xid);
    assertEquals (subReply.subscriptionId, subModRqst.subscriptionId);
    
    // remove subscription
    SubDelRqst delRqst = new SubDelRqst (subReply.subscriptionId);
    client.send (delRqst);
    subReply = (SubRply)client.receive ();
    assertEquals (subReply.subscriptionId, delRqst.subscriptionId);
    
    // check NACK on remove invalid subscription
    delRqst = new SubDelRqst (subReply.subscriptionId);
    client.send (delRqst);
    nackReply = (Nack)client.receive ();
    assertEquals (Nack.NO_SUCH_SUB, nackReply.error);
    
    // send a connection test
    client.send (TestConn.INSTANCE);
    assertTrue (client.receive () instanceof ConfConn);
    
    client.close ();
    router.close ();
  }
  
  /**
   * Test multiple clients sending messages between each other.
   */
  @Test
  public void multiClient ()
    throws Exception
  {
    router = new Router (PORT);
    
    // client 1
    SimpleClient client1 = new SimpleClient ();
    
    client1.connect ();
    
    SubAddRqst subAddRqst1 = new SubAddRqst ("client == 1 || all == 1");
    client1.send (subAddRqst1);
    
    SubRply subReply1 = (SubRply)client1.receive ();
    assertEquals (subAddRqst1.xid, subReply1.xid);
    
    // client 2
    SimpleClient client2 = new SimpleClient ();
    
    client2.connect ();
    
    SubAddRqst subAddRqst2 = new SubAddRqst ("client == 2 || all == 1");
    client2.send (subAddRqst2);
    
    SubRply subReply2 = (SubRply)client2.receive ();
    assertEquals (subAddRqst2.xid, subReply2.xid);
    
    // client 1 send message to client 2
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("client", 2);
    ntfn.put ("payload", "hello from client 1");
    
    client1.send (new NotifyEmit (ntfn));
    
    NotifyDeliver client2Notify = (NotifyDeliver)client2.receive ();
    assertEquals ("hello from client 1", client2Notify.attributes.get ("payload"));
    
    // client 2 send message to client 1
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("client", 1);
    ntfn.put ("payload", "hello from client 2");
    
    client2.send (new NotifyEmit (ntfn));
    
    NotifyDeliver client1Notify = (NotifyDeliver)client1.receive ();
    assertEquals ("hello from client 2", client1Notify.attributes.get ("payload"));
    
    // client 1 sends message to all
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("all", 1);
    ntfn.put ("payload", "hello all");
    
    client1.send (new NotifyEmit (ntfn));
    
    client2Notify = (NotifyDeliver)client2.receive ();
    assertEquals ("hello all", client2Notify.attributes.get ("payload"));
    
    client1Notify = (NotifyDeliver)client1.receive ();
    assertEquals ("hello all", client1Notify.attributes.get ("payload"));
    
    client1.close ();
    client2.close ();
    router.close ();
  }
  
  /**
   * Test secure messaging using the producer key scheme. Other
   * schemes should really be tested, but the key matching logic for
   * all the schemes supported in the server is done by the security
   * tests, so not bothering for now.
   */
  @Test
  public void security ()
    throws Exception
  {
    router = new Router (PORT);
    
//    SimpleClient alice = new SimpleClient ("localhost", 2917);
//    SimpleClient bob = new SimpleClient ("localhost", 2917);
//    SimpleClient eve = new SimpleClient ("localhost", 2917);
    
    SimpleClient alice = new SimpleClient ("alice");
    SimpleClient bob = new SimpleClient ("bob");
    SimpleClient eve = new SimpleClient ("eve");
    
    alice.connect ();
    bob.connect ();
    eve.connect ();

    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_PRODUCER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePublic);
    
    Keys eveSubKeys = new Keys ();
    eveSubKeys.add (SHA1_PRODUCER,
                    new Key ("Not alice's key").publicKeyFor (SHA1_PRODUCER));
    
    bob.subscribe ("require (From-Alice)", bobSubKeys);
    
    eve.subscribe ("require (From-Alice)", eveSubKeys);
    
    checkAliceBobEve (alice, bob, eve, aliceNtfnKeys);
    
    alice.close ();
    bob.close ();
    eve.close ();
  }
  
  /**
   * Test that global keys set via SecModify work.
   */
  @Test
  public void securitySecModify ()
    throws Exception
  {
    router = new Router (PORT);
    
    SimpleClient alice = new SimpleClient ("alice");
    SimpleClient bob = new SimpleClient ("bob");
    SimpleClient eve = new SimpleClient ("eve");
    
    alice.connect ();
    bob.connect ();
    eve.connect ();

    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_PRODUCER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePublic);
    
    Keys eveSubKeys = new Keys ();
    eveSubKeys.add (SHA1_PRODUCER,
                    new Key ("Not alice's key").publicKeyFor (SHA1_PRODUCER));
    
    bob.subscribe ("require (From-Alice)");
    
    eve.subscribe ("require (From-Alice)");
    
    alice.sendAndReceive
      (new SecRqst (aliceNtfnKeys, EMPTY_KEYS, EMPTY_KEYS, EMPTY_KEYS));
    
    bob.sendAndReceive
      (new SecRqst (EMPTY_KEYS, EMPTY_KEYS, bobSubKeys, EMPTY_KEYS));
    
    eve.sendAndReceive
      (new SecRqst (EMPTY_KEYS, EMPTY_KEYS, eveSubKeys, EMPTY_KEYS));
    
    checkAliceBobEve (alice, bob, eve, EMPTY_KEYS);
    
    alice.close ();
    bob.close ();
    eve.close ();
  }

  /**
   * Check that alice and bob receive securely, eve doesn't.
   */
  private static void checkAliceBobEve (SimpleClient alice,
                                        SimpleClient bob, 
                                        SimpleClient eve,
                                        Keys ntfnKeys)
    throws Exception
  {
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("From-Alice", 1);
    
    alice.sendNotify (ntfn, ntfnKeys);
    
    NotifyDeliver bobNtfn = (NotifyDeliver)bob.receive ();
    
    assertEquals (1, bobNtfn.secureMatches.length);
    assertEquals (0, bobNtfn.insecureMatches.length);
    
    assertEquals (1, bobNtfn.attributes.get ("From-Alice"));
    
    try
    {
      NotifyDeliver eveNtfn = (NotifyDeliver)eve.receive (2000);
      
      assertEquals (1, eveNtfn.attributes.get ("From-Alice"));
      
      fail ("Eve foiled our super secret scheme");
    } catch (MessageTimeoutException ex)
    {
      // ok
    }
  }
  
  /**
   * Test changing subscription security settings on the fly.
   */
  @Test
  public void securitySubModify ()
    throws Exception
  {
    router = new Router (PORT);
    
    SimpleClient alice = new SimpleClient ("alice");
    SimpleClient bob = new SimpleClient ("bob");
    
    alice.connect ();
    bob.connect ();

    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_PRODUCER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePublic);

    SubAddRqst subAddRqst =
      new SubAddRqst ("require (From-Alice)", bobSubKeys, true);
    
    bob.send (subAddRqst);
    SubRply subRply = bob.receive (SubRply.class);
    
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("From-Alice", 1);
    
    // send secure
    alice.sendNotify (ntfn, aliceNtfnKeys);
    
    NotifyDeliver bobNtfn = (NotifyDeliver)bob.receive ();
    assertEquals (1, bobNtfn.secureMatches.length);
    assertEquals (0, bobNtfn.insecureMatches.length);
    assertEquals (subRply.subscriptionId, bobNtfn.secureMatches [0]);
    
    // send insecure
    alice.sendNotify (ntfn);
    
    bobNtfn = (NotifyDeliver)bob.receive ();
    assertEquals (0, bobNtfn.secureMatches.length);
    assertEquals (1, bobNtfn.insecureMatches.length);
    assertEquals (subRply.subscriptionId, bobNtfn.insecureMatches [0]);
    
    // change bob to require secure
    SubModRqst subModRqst = new SubModRqst (subRply.subscriptionId, "", false);

    bob.send (subModRqst);
    bob.receive (SubRply.class);
    
    // send insecure again, bob should not get it
    alice.sendNotify (ntfn);
    
    try
    {
      bobNtfn = (NotifyDeliver)bob.receive (2000);
      
      fail ("Server delivered message insecurely");
    } catch (MessageTimeoutException ex)
    {
      // ok
    }
    
    // change bob's keys so they do not match
    subModRqst = new SubModRqst (subRply.subscriptionId, "", false);
    subModRqst.delKeys = new Keys ();
    subModRqst.delKeys.add (SHA1_PRODUCER, alicePublic);
    
    bob.send (subModRqst);
    bob.receive (SubRply.class);
    
    // send secure again, bob should not get it
    alice.sendNotify (ntfn, aliceNtfnKeys);
    
    try
    {
      bobNtfn = (NotifyDeliver)bob.receive (2000);
      
      fail ("Server delivered message insecurely");
    } catch (MessageTimeoutException ex)
    {
      // ok
    }
    
    alice.close ();
    bob.close ();
  }
  
  @Test
  public void unotify ()
    throws Exception
  {
    router = new Router (PORT);

    SimpleClient client1 = new SimpleClient ("client1");
    SimpleClient client2 = new SimpleClient ("client2");
    
    client2.connect ();
    client2.subscribe ("number == 1");
    
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("number", 1);
    ntfn.put ("client", "client 1");
    
    client1.send (new UNotify (4, 0, ntfn));
    
    // todo MINA close can eat messages in queue: fix this
    sleep (1000);
    
    client1.close ();
    
    NotifyDeliver reply = (NotifyDeliver)client2.receive ();
    assertEquals ("client 1", reply.attributes.get ("client"));
    
    client2.close ();
  }
  
  /**
   * Test handling of client that does Bad Things.
   */
  @Test
  public void badClient ()
    throws Exception
  {
    // this test generates warnings by design: turn off checking
    logTester.assertOkAndDispose ();
    
    enableLogging (WARNING, false);
    
    router = new Router (PORT);
    SimpleClient client = new SimpleClient ();
    SimpleClient badClient = new SimpleClient ();
    
    client.connect ();
    client.subscribe ("number == 1");
    
    // try to send a notification with no ConnRqst
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("number", 1);
    
    badClient.send (new NotifyEmit (ntfn));
    
    try
    {
      client.receive (2000);
      
      fail ("Server allowed client with no connection to notify");
    } catch (MessageTimeoutException ex)
    {
      // ok
    }
    
    // try change security with no connection
    badClient.close ();
    badClient = new SimpleClient ();
    
    SecRqst secRqst = new SecRqst ();
    badClient.send (secRqst);
    Message reply = badClient.receive ();
    assertTrue (reply instanceof Disconn);
    
    badClient.close ();

    // try subscription with no connection
    badClient = new SimpleClient ();
    
    SubAddRqst subAddRqst =
      new SubAddRqst ("require (hello)", EMPTY_KEYS, true);
    badClient.send (subAddRqst);
    reply = badClient.receive ();
    assertTrue (reply instanceof Disconn);
    badClient.close ();
    
    // try to connect twice
    badClient = new SimpleClient ();
    badClient.connect ();

    ConnRqst connRqst = new ConnRqst (4, 0);
    badClient.send (connRqst);
    reply = badClient.receive ();
    assertTrue (reply instanceof Disconn);
    
    // server will have disconnected us for being Bad, so just kill socket
    badClient.closeImmediately ();

    // modify non-existent sub
    badClient = new SimpleClient ();
    badClient.connect ();
    
    SubModRqst subModRqst = new SubModRqst (123456, "", true);
    badClient.send (subModRqst);
    reply = badClient.receive ();
    assertTrue (reply instanceof Nack);
    assertEquals (subModRqst.xid, ((Nack)reply).xid);
    
    badClient.close ();
    client.close ();
  }
  
  /**
   * Test events are delivered in the order they're sent.
   */
  @Test
  public void inorder () 
    throws Exception
  {
    router = new Router (PORT);

    final SimpleClient client1 = new SimpleClient ("client1");
    final SimpleClient client2 = new SimpleClient ("client2");
    
    autoClose.add (client1);
    autoClose.add (client2);
    
    client1.connect ();
    
    client2.connect ();
    client2.subscribe ("int32 (serial)");
    
    Thread notifyThread = new Thread ()
    {
      @Override
      public void run ()
      {
        Map<String, Object> ntfn = new HashMap<String, Object> ();

        try
        {
          for (int serial = 0; serial < 1000 && !isInterrupted (); serial++)
          {
            ntfn.put ("serial", serial);
            
            client1.sendNotify (ntfn);
          }
        } catch (Exception ex)
        {
          ex.printStackTrace ();
        }
      }
    };
    
    notifyThread.start ();
    
    for (int i = 0; i < 1000 ; i++)
    {
      NotifyDeliver ntfn = (NotifyDeliver)client2.receive ();
      
      int serial = (Integer)ntfn.attributes.get ("serial");
      
      if (serial != i)
      {
        notifyThread.interrupt ();
        notifyThread.join (10000);
        
        fail ("Events not in order: " +
              "serial was " + serial + ", should have been " + i);
      }
    }
  }

  private static String dummySubscription (int length)
  {
    StringBuilder str = new StringBuilder ("i == -1");
    
    for (int i = 0; str.length () + 15 < length; i++)
      str.append (" && i == " + i);
    
    return str.toString ();
  }
  
  private byte [] randomBytes (int length)
  {
    byte [] data = new byte [length];
    
    random.nextBytes (data);
    
    return data;
  }
}
