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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;

import org.avis.common.ElvinURI;
import org.avis.io.messages.NotifyEmit;

import static java.lang.System.currentTimeMillis;

import static org.avis.logging.Log.info;

public class DosAttack
{
  public static void main (String [] args)
    throws Exception
  {
    if (args.length < 1)
    {
      System.err.println ("Usage: DosAttack <elvin_uri>");
      System.exit (1);
    }
    
    final Collection<SimpleClient> receiveClients = new ArrayList<SimpleClient> ();
    ElvinURI uri = new ElvinURI (args [0]);
    
    ByteBuffer.setUseDirectBuffers (false);
    
    final SimpleClient sendClient = new SimpleClient (uri.host, uri.port);

    sendClient.connect ();
    
    for (int i = 0; i < 10; i++)
    {
      SimpleClient receiveClient = new SimpleClient (uri.host, uri.port);
      receiveClient.connect ();
      receiveClient.subscribe ("require (name)");
      receiveClients.add (receiveClient);
    }
    
    sendClient.subscribe ("require (name)");
    
    final Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    ntfn.put ("data", new byte [64 * 1024]);
    
    new Thread ()
    {
      @Override
      public void run ()
      {
        info ("Sender running...", this);
        
        long start = currentTimeMillis ();
        int count = 0;
        
        try
        {
          while (currentTimeMillis () - start < 30 * 60 * 1000)
          {
            sendClient.send (new NotifyEmit (ntfn));
            
            count++;

            if (count % 20 == 0)
              info (count + " messages sent", this);
          }
        } catch (Exception ex)
        {
          ex.printStackTrace ();
        }
      } 
    }.start ();

    new Thread ()
    {
      @Override
      public void run ()
      {
        int count = 0;
        
        info ("Receiver running...", this);
        
        try
        {
          while (true)
          {
            for (SimpleClient receiveClient : receiveClients)
              receiveClient.receive ();
            
            count++;
            
            if (count % 20 == 0)
              info (count + " messages received", this);
          }
        } catch (Exception ex)
        {
          ex.printStackTrace ();
        }
      }
    }.start ();
  }
}
