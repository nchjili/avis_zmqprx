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

import org.apache.mina.common.ByteBuffer;

import org.avis.io.messages.NotifyEmit;

import static java.lang.System.currentTimeMillis;

/**
 * Send/receive a stream of notifications to the router in order to
 * watch heap activity.
 * 
 * @author Matthew Phillips
 */
public class MemoryLeak
{
  public static void main ()
    throws Exception
  {
    ByteBuffer.setUseDirectBuffers (true);
    
    Router router = new Router (29170);
    SimpleClient client = new SimpleClient ();

    client.connect ();
    
    client.subscribe ("require (name)");
    
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("data", new byte [64 * 1024]);
    
    long start = currentTimeMillis ();
    
    int count = 0;
    
    while (currentTimeMillis () - start < 30 * 60 * 1000)
    {
      client.send (new NotifyEmit (ntfn));
      
      client.receive ();
      
      count++;
      
      if ((currentTimeMillis () - start) % (10 * 1000) == 0)
      {
        System.gc ();
        System.out.println ("Heap = " + Runtime.getRuntime ().freeMemory ());
      }
    }
    
    System.out.println ("Messages = " + count);
    
    client.close ();
    router.close ();
  }
}
