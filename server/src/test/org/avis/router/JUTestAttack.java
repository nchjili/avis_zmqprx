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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import java.io.IOException;

import org.avis.io.messages.SubAddRqst;
import org.avis.router.Router;
import org.avis.security.Key;
import org.avis.security.Keys;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.info;
import static org.avis.logging.Log.enableLogging;

import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;
import static org.avis.router.JUTestRouter.PORT;
import static org.avis.security.KeyScheme.SHA1_CONSUMER;

/**
 * Test attacks deliberately designed to DOS/crash the server by
 * exhausting resources.
 * 
 * @author Matthew Phillips
 */
public class JUTestAttack
{
  private Random random;
  private Router server;

  @Before
  public void setup ()
    throws IOException
  {
    enableLogging (TRACE, false);
    enableLogging (DIAGNOSTIC, false);
    
    random = new Random ();
    server = new Router (PORT);
  }
  
  @After
  public void teardown ()
  {
    if (server != null)
      server.close ();
  }
  
  /**
   * Attack the server's heap space by adding the default max
   * keys/subs. Will pass with -Xmx120M setting.
   */
  @Test
  @Ignore
  public void attackKeys ()
    throws Exception
  {
    int numKeys = CONNECTION_OPTION_SET.defaults.getInt ("Subscription.Max-Keys");
    int numSubs = CONNECTION_OPTION_SET.defaults.getInt ("Subscription.Max-Count");
    SimpleClient client = new SimpleClient ("localhost", PORT);
    
    client.connect ();
    
    info ("Sending keys...", this);
    
    for (int i = 0; i < numSubs; i++)
    {
      Keys keys = new Keys ();
      
      for (int j = 0; j < numKeys; j++)
        keys.add (SHA1_CONSUMER, new Key (randomBytes (128)));
      
      SubAddRqst subAddRqst = new SubAddRqst ("number == " + i, keys, true);
      
      client.send (subAddRqst);
      client.receive (60 * 1000);
    }
    
    client.close ();
  }

  /**
   * Attack server by sending the max number of subs with very long
   * expressions. Takes about 2 mins to kill server on Powerbook G4.
   * Adding -Xmx120M allows it to pass.
   */
  @Test
  @Ignore
  public void attackSubscriptions ()
    throws Exception
  {
    int maxSubs = CONNECTION_OPTION_SET.getMaxValue ("Subscription.Max-Count");
    int maxLength = CONNECTION_OPTION_SET.getMaxValue ("Subscription.Max-Length");

    SimpleClient client = new SimpleClient ("localhost", PORT);
    
    Map<String, Object> options = new HashMap<String, Object> ();
    options.put ("Subscription.Max-Count", maxSubs);
    options.put ("Subscription.Max-Length", maxLength);
    
    client.connect (options);
    
    info ("Subscribing...", this);
    
    String subscriptionExpr = dummySubscription (maxLength);
    
    for (int i = 0; i < maxSubs; i++)
      client.subscribe (subscriptionExpr);
    
    client.close ();
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
