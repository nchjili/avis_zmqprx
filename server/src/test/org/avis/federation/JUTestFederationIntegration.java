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

import java.util.Collection;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.avis.io.messages.NotifyEmit;
import org.avis.logging.Log;
import org.avis.router.Main;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.util.IllegalConfigOptionException;
import org.avis.util.LogFailTester;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;

import static org.avis.federation.FederationManager.federationManagerFor;
import static org.avis.federation.TestUtils.MAX_WAIT;
import static org.avis.federation.TestUtils.waitForConnect;
import static org.avis.logging.Log.INFO;
import static org.avis.logging.Log.enableLogging;
import static org.avis.logging.Log.shouldLog;
import static org.avis.util.Streams.close;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Full federation integration test between three routers. Starts each
 * router as if started from command line and federates in a V:
 * router1 -> router2 and router1 -> router3.
 * 
 * @author Matthew Phillips
 */
public class JUTestFederationIntegration
{
  private LogFailTester logTester;
  private boolean oldLogInfoState;
  
  @Before
  public void setup ()
  {
    oldLogInfoState = shouldLog (INFO);
    
    enableLogging (INFO, false);
    
    logTester = new LogFailTester ();
  }
  
  @After
  public void tearDown ()
    throws Exception
  {
    enableLogging (INFO, oldLogInfoState);
    
    logTester.assertOkAndDispose ();
  }
  
  @Test
  public void integration ()
    throws Exception
  {
    File config1 = configFile
    (
      "Listen=elvin://127.0.0.1:29170\n" + 
      "Federation.Activated=yes\n" + 
      "Federation.Connect[Test]=" +
        "ewaf://127.0.0.1:29181 ewaf://127.0.0.1:29191\n" + 
      "Federation.Subscribe[Test]=require (test)\n" + 
      "Federation.Provide[Test]=require (test)\n" +
      "Federation.Request-Timeout=2"
    );
    
    File config2 = configFile
    (
      "Listen=elvin://127.0.0.1:29180\n" + 
      "Federation.Activated=yes\n" + 
      "Federation.Listen=ewaf://127.0.0.1:29181\n" + 
      "Federation.Subscribe[Test]=require (test)\n" + 
      "Federation.Provide[Test]=require (test)\n" +
      "Federation.Apply-Class[Test]=@127.0.0.1"
    );
    
    File config3 = configFile
    (
      "Listen=elvin://127.0.0.1:29190\n" + 
      "Federation.Activated=yes\n" + 
      "Federation.Listen=ewaf://127.0.0.1:29191\n" + 
      "Federation.Subscribe[Test]=require (test)\n" + 
      "Federation.Provide[Test]=require (test)\n" +
      "Federation.Apply-Class[Test]=127.0.0.1"
    );

    Log.enableLogging (Log.INFO, false);
    // Log.enableLogging (Log.TRACE, true);
    // Log.enableLogging (Log.DIAGNOSTIC, true);

    Router router2 = startRouter (config2);
    Router router3 = startRouter (config3);
    Router router1 = startRouter (config1);

    for (Connector connector : federationManagerFor (router1).connectors ())
      waitForConnect (connector);
    
    SimpleClient client1 = new SimpleClient ("127.0.0.1", 29180);
    SimpleClient client2 = new SimpleClient ("127.0.0.1", 29190);
    
    client1.connect ();
    client1.subscribe ("test == 1");
    
    client2.connect ();
    client2.subscribe ("test == 2");
    
    client1.send (new NotifyEmit ("test", 2));
    
    client2.receive ();
    
    client2.send (new NotifyEmit ("test", 1));
    
    client1.receive ();
    
    client1.close ();
    client2.close ();
    
    router1.close ();
    router2.close ();
    router3.close ();
  }
  
  /**
   * Full integration test including TLS in connector/acceptor.
   * 
   * Router 1 connects -> Router 2
   *                   -> Router 3
   */
  @Test
  public void integrationTLS ()
    throws Exception
  {
    String commonOptions = 
      "TLS.Keystore=" + getClass ().getResource ("router.ks") + "\n" +
      "TLS.Keystore-Passphrase=testing\n" +
      "Federation.Activated=yes\n" +
      "Federation.Subscribe[Test]=require (test)\n" + 
      "Federation.Provide[Test]=require (test)\n" +
      "Federation.Apply-Class[Test]=127.0.0.1\n" +
      "Federation.Request-Timeout=2\n" +
      "Federation.Require-Authenticated=127.0.0.1\n";
    
    File config1 = configFile
    (
      "Listen=elvin:/secure/127.0.0.1:29170\n" +
      commonOptions +
      "Federation.Connect[Test]=" +
        "ewaf:/secure/127.0.0.1:29181 ewaf:/secure/127.0.0.1:29191\n" 
    );
    
    File config2 = configFile
    (
      "Listen=elvin://127.0.0.1:29180\n" +
      commonOptions +
      "Federation.Listen=" +
        "ewaf:/secure/127.0.0.1:29181 " +
        "ewaf://127.0.0.1:29182\n"
    );
    
    File config3 = configFile
    (
      "Listen=elvin://127.0.0.1:29190\n" +
      commonOptions +
      "Federation.Listen=ewaf:/secure/127.0.0.1:29191\n"
    );

    // config with untrusted cert
    File config4 = configFile
    (
      "Listen=elvin:/secure/127.0.0.1:29200\n" +
      commonOptions +
      "TLS.Keystore=" + getClass ().getResource ("untrusted.ks") + "\n" +
      "Federation.Connect[Test]=ewaf:/secure/127.0.0.1:29181" 
    );
    
    // config with trusted cert but not using TLS
    File config5 = configFile
    (
      "Listen=elvin:/secure/127.0.0.1:29300\n" +
      commonOptions +
      "Federation.Connect[Test]=ewaf://127.0.0.1:29182" 
    );
    
//    Log.enableLogging (Log.INFO, true);
//    Log.enableLogging (Log.TRACE, true);
//    Log.enableLogging (Log.DIAGNOSTIC, true);

    Router router2 = startRouter (config2);
    Router router3 = startRouter (config3);
    Router router1 = startRouter (config1);

    Collection<Connector> connectors = 
      federationManagerFor (router1).connectors ();
    
    assertEquals (2, connectors.size ());
    
    for (Connector connector : connectors)
      waitForConnect (connector);
    
    SimpleClient client1 = new SimpleClient ("127.0.0.1", 29180);
    SimpleClient client2 = new SimpleClient ("127.0.0.1", 29190);
    
    client1.connect ();
    client1.subscribe ("test == 1");
    
    client2.connect ();
    client2.subscribe ("test == 2");
    
    client1.send (new NotifyEmit ("test", 2));
    
    client2.receive ();
    
    client2.send (new NotifyEmit ("test", 1));
    
    client1.receive ();
    
    client1.close ();
    client2.close ();
    
    // check that federator with untrusted cert cannot connect
    logTester.pause ();
    
    Router router4 = startRouter (config4);
    
    assertFailsToConnect (router4);
    
    router4.close ();
    
    // check that federator cannot connect insecurely
    logTester.pause ();
    
    Router router5 = startRouter (config5);
    
    assertFailsToConnect (router5);
    
    router5.close ();
    
    logTester.unpause ();
    
    router3.close ();
    router2.close ();
    router1.close ();
  }

  private static void assertFailsToConnect (Router router)
    throws InterruptedException
  {
    sleep (MAX_WAIT);
    
    for (Connector connector : federationManagerFor (router).connectors ())
      assertFalse (connector.isConnected ());
  }

  private static Router startRouter (File config) 
    throws IllegalConfigOptionException, IOException
  {
    return Main.start ("-c", config.getAbsolutePath ());
  }

  private static File configFile (String contents)
    throws IOException
  {
    File configFile = File.createTempFile ("avis", ".conf");
    
    configFile.deleteOnExit ();
    
    PrintWriter configStream = new PrintWriter (configFile);
    
    try
    {
      configStream.append (contents);
    } finally
    {
      close (configStream);
    }
    
    return configFile;
  }
}