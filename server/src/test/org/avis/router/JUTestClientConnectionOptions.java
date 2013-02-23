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

import org.junit.Test;

import static org.avis.io.LegacyConnectionOptions.legacyToNew;
import static org.avis.router.ClientConnectionOptionSet.CLIENT_CONNECTION_OPTION_SET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test the {@link ClientConnectionOptions} class.
 * 
 * @author Matthew Phillips
 */
public class JUTestClientConnectionOptions
{
  @Test
  public void validation ()
  {
    HashMap<String, Object> requested = new HashMap<String, Object> ();
    
    requested.put ("Packet.Max-Length", 8 * 1024 * 1024); // valid
    requested.put ("Attribute.Max-Count", 63); // bogus value
    requested.put ("Bogus", 42); // bogus
    requested.put ("Receive-Queue.Drop-Policy", "bogus"); // bogus
    requested.put ("Send-Queue.Drop-Policy", "fail"); // valid
    requested.put ("Receive-Queue.Drop-Policy", "oldest"); // valid, default
    requested.put ("Send-Queue.Max-Length", "bogus"); // bogus
    
    ClientConnectionOptions options;
    try
    {
      options = new ClientConnectionOptions (requested);
      Map<String, Object> valid = options.accepted ();
      
      assertRequested (requested, valid, "Packet.Max-Length");
      assertDefault (valid, "Attribute.Max-Count");
      assertNull (valid.get ("Bogus"));
      assertDefault (valid, "Receive-Queue.Drop-Policy");
      assertRequested (requested, valid, "Send-Queue.Drop-Policy");
      assertRequested (requested, valid, "Receive-Queue.Drop-Policy");
      assertDefault (valid, "Send-Queue.Max-Length");
    } catch (ExceptionInInitializerError ex)
    {
      ex.getCause ().printStackTrace ();
    }
  }
  
  @Test
  public void compatibility ()
  {
    HashMap<String, Object> requested = new HashMap<String, Object> ();
    
    requested.put ("router.attribute.string.max-length", 8 * 1024 * 1024); // valid
    requested.put ("router.send-queue.drop-policy", "fail"); // valid
    requested.put ("router.attribute.max-count", 63); // bogus
    requested.put ("router.coalesce-delay", 0); // valid
    
    ClientConnectionOptions options = new ClientConnectionOptions (requested);
    Map<String, Object> accepted = options.accepted ();
    
    // todo: when Attribute.String.Max-Length supported, switch lines below
    // assertRequested (requested, valid, "router.attribute.string.max-length");
    assertDefault (accepted, "router.attribute.string.max-length");
    assertDefault (accepted, "router.attribute.max-count");
    assertRequested (requested, accepted, "router.send-queue.drop-policy");
    assertRequested (requested, accepted, "router.coalesce-delay");
  }

  private static void assertDefault (Map<String, Object> accepted,
                                     String name)
  {
    assertEquals
      (CLIENT_CONNECTION_OPTION_SET.defaults.get
        (legacyToNew (name)), accepted.get (name));
  }

  private static void assertRequested (Map<String, Object> requested,
                                       Map<String, Object> valid,
                                       String name)
  {
    assertEquals (requested.get (name), valid.get (name));
  }
}
