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

import java.util.Random;
import java.util.concurrent.ExecutorService;

import java.net.InetSocketAddress;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * Fuzz tests an Elvin router by writing message frames with random payloads.
 * 
 * @author Matthew Phillips
 */
public class Fuzz
{
  final static int [] MESSAGES = new int []
    {32, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,
     64, 80, 81, 82, 83};
  
  public static void main (String [] args)
    throws InterruptedException
  {
    new Fuzz ((args.length > 0 ? args [0] : "127.0.0.1")).run ();
  }

  private SocketConnector connector;
  private ExecutorService executor;
  private InetSocketAddress remoteAddress;
  private Random random;
  
  public Fuzz (String host)
  {
    executor = newCachedThreadPool ();
    
    // connector
    connector = new SocketConnector (1, executor);
    connector.setWorkerTimeout (0);
    
    remoteAddress = new InetSocketAddress (host, 2917);
    
    random = new Random (hashCode ());
  }

  public void run ()
    throws InterruptedException
  {
    while (true)
    {
      IoSession session = connect ();
     
      ByteBuffer buffer = ByteBuffer.allocate (128 * 1024);
      buffer.acquire ();
      
      int bytes = random.nextInt (buffer.capacity () - 8);
      
      bytes = bytes - (bytes % 4);
      
      buffer.clear ();

      buffer.putInt (bytes + 4);
      buffer.putInt (MESSAGES [random.nextInt (MESSAGES.length)]);
      
      for (int i = bytes; i > 0; i--)
        buffer.put ((byte)(random.nextInt (256) - 127));
      
      buffer.flip ();
      session.write (buffer).addListener (IoFutureListener.CLOSE);
      
      System.out.println ("Wrote " + bytes + " bytes");
      
      sleep (10);
    }
  }
  
  private IoSession connect ()
  {
    SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    ConnectFuture future = 
      connector.connect (remoteAddress, new IoHandlerAdapter (),
                         connectorConfig);
    
    future.join ();
    
    return future.getSession ();
  }
}
