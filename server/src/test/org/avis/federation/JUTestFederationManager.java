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
package org.avis.federation;

import java.util.HashMap;
import java.util.Map;

import java.net.InetAddress;

import org.avis.config.Options;
import org.avis.io.messages.NotifyDeliver;
import org.avis.logging.Log;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.avis.federation.TestUtils.waitForConnect;
import static org.avis.io.Net.addressesFor;
import static org.avis.logging.Log.INFO;
import static org.avis.logging.Log.enableLogging;
import static org.avis.util.Collections.set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JUTestFederationManager
{
  private static final int PORT1 = 29170;
  private static final int PORT2 = PORT1 + 10;

  private LogFailTester logTester;
  private boolean oldLogInfoState;

  @Before
  public void setup ()
  {
     // Log.enableLogging (Log.TRACE, true);
     // Log.enableLogging (Log.DIAGNOSTIC, true);

    oldLogInfoState = Log.shouldLog (INFO);
    
    enableLogging (INFO, false);
    
    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
  {
    enableLogging (INFO, oldLogInfoState);
    
    logTester.assertOkAndDispose ();
  }
  
  @Test
  public void basicListen () 
    throws Exception
  {
    InetAddress localHost = InetAddress.getLocalHost ();
    EwafURI federationUri = new EwafURI ("ewaf://127.0.0.1:" + (PORT1 - 1));

    Options options = new Options (FederationOptionSet.OPTION_SET);
    
    options.set ("Federation.Router-Name", "router1");
    options.set ("Federation.Listen", federationUri);
    options.set ("Federation.Apply-Class[Test]", 
                 localHost.getHostAddress () + " localhost bogus");
    options.set ("Federation.Subscribe[Test]", "require (federated)");
    options.set ("Federation.Provide[Test]", "require (federated)");
    options.set ("Federation.Add-Incoming-Attribute[Test][Added-Incoming]", 
                 "'incoming'");
    options.set ("Federation.Add-Outgoing-Attribute[Test][Added-Outgoing]", 
                 "'outgoing'");
    
    Router router1 = new Router (PORT1);
    
    FederationManager manager = new FederationManager (router1, options);
    
    FederationClass testClass = manager.classes.classFor (localHost);
    
    assertFalse (testClass.allowsNothing ());
    
    assertEquals (addressesFor (set (federationUri)),
                  manager.acceptor.listenAddresses);
    
    assertEquals ("incoming", 
                  testClass.incomingAttributes.get ("Added-Incoming"));
    assertEquals ("outgoing", 
                  testClass.outgoingAttributes.get ("Added-Outgoing"));
    
    Router router2 = new Router (PORT2);
    
    FederationClass fedClass =
      new FederationClass ("require (federated)", "require (federated)");
    
    Connector connector =
      new Connector (router2, "router2", federationUri, fedClass,
                     new Options (FederationOptionSet.OPTION_SET));
    
    waitForConnect (connector);
    
    assertEquals (1, manager.acceptor.links.size ());
    
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    client1.connect ();
    client2.connect ();
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");
    
    client1.sendNotify 
      (map ("federated", "router1", "from", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)client2.receive ();
    
    assertEquals (0, notification.secureMatches.length);
    assertEquals (1, notification.insecureMatches.length);
    assertEquals ("client1", notification.attributes.get ("from"));
    
    client1.close ();
    client2.close ();
    
    connector.close ();
    router2.close ();
    
    router1.close ();
    
    assertTrue (manager.isClosed ());
  }
  
  @Test
  public void fullConnectAccept () 
    throws Exception
  {
    EwafURI federationUri = new EwafURI ("ewaf://127.0.0.1:" + (PORT1 - 1));

    // router 1 (acceptor)
    Options options1 = new Options (FederationOptionSet.OPTION_SET);
    
    options1.set ("Federation.Router-Name", "router1");
    options1.set ("Federation.Listen", federationUri);
    options1.set ("Federation.Apply-Class[Test]", "localhost");
    options1.set ("Federation.Subscribe[Test]", "require (federated)");
    options1.set ("Federation.Provide[Test]", "require (federated)");
    
    Router router1 = new Router (PORT1);
    
    FederationManager manager1 = new FederationManager (router1, options1);

    // router 2 (connector)
    Options options2 = new Options (FederationOptionSet.OPTION_SET);
    
    options2.set ("Federation.Router-Name", "router2");
    options2.set ("Federation.Connect[Test]", federationUri);
    options2.set ("Federation.Subscribe[Test]", "require (federated)");
    options2.set ("Federation.Provide[Test]", "require (federated)");
    
    Router router2 = new Router (PORT2);

    FederationManager manager2 = new FederationManager (router2, options2);

    waitForConnect (manager2.connectors.get (0));
    
    assertEquals (1, manager1.acceptor.links.size ());
    
    SimpleClient client1 = new SimpleClient ("client1", "localhost", PORT1);
    SimpleClient client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    client1.connect ();
    client2.connect ();
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");
    
    client1.sendNotify 
      (map ("federated", "router1", "from", "client1"));
    
    NotifyDeliver notification = (NotifyDeliver)client2.receive ();
    
    assertEquals (0, notification.secureMatches.length);
    assertEquals (1, notification.insecureMatches.length);
    assertEquals ("client1", notification.attributes.get ("from"));
    
    client1.close ();
    client2.close ();
    
    router2.close ();
    router1.close ();
    
    assertTrue (manager1.isClosed ());
    assertTrue (manager2.isClosed ());
  }
  
  @Test
  public void defaultApplyClass () 
    throws Exception
  {
    EwafURI federationUri = new EwafURI ("ewaf://127.0.0.1:" + (PORT1 - 1));

    // router 1 (acceptor)
    Options options1 = new Options (FederationOptionSet.OPTION_SET);
    
    options1.set ("Federation.Router-Name", "router1");
    options1.set ("Federation.Listen", federationUri);
    options1.set ("Federation.Subscribe[Test]", "require (federated)");
    options1.set ("Federation.Provide[Test]", "require (federated)");
    options1.set ("Federation.Default-Class", "Test");
    
    Router router1 = new Router (PORT1);
    
    FederationManager manager1 = new FederationManager (router1, options1);

    // router 2 (connector)
    Options options2 = new Options (FederationOptionSet.OPTION_SET);
    
    options2.set ("Federation.Router-Name", "router2");
    options2.set ("Federation.Connect[Test]", federationUri);
    options2.set ("Federation.Subscribe[Test]", "require (federated)");
    options2.set ("Federation.Provide[Test]", "require (federated)");
    
    Router router2 = new Router (PORT2);

    FederationManager manager2 = new FederationManager (router2, options2);

    waitForConnect (manager2.connectors.get (0));
    
    assertEquals (1, manager1.acceptor.links.size ());
    
    router2.close ();
    router1.close ();
  }
  
  private static Map<String, Object> map (String... nameValues)
  {
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < nameValues.length; i += 2)
      map.put (nameValues [i], nameValues [i + 1]);
    
    return map;
  }
}
