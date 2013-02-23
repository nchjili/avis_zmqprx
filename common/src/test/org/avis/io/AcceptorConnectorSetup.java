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
package org.avis.io;

import java.util.concurrent.ExecutorService;

import java.io.IOException;

import java.net.InetSocketAddress;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * Test setup that creates a connected SocketAcceptor/SocketConnector
 * pair.
 * 
 * @author Matthew Phillips
 */
public class AcceptorConnectorSetup
{
  public SocketAcceptor acceptor;
  public SocketAcceptorConfig acceptorConfig;
  public IoSession session;
  public ExecutorService executor;
  public SocketConnector connector;
  public SocketConnectorConfig connectorConfig;

  public AcceptorConnectorSetup ()
    throws IOException
  {
    executor = newCachedThreadPool ();
    
    // listener
    acceptor = new SocketAcceptor (1, executor);
    acceptorConfig = new SocketAcceptorConfig ();
    
    acceptorConfig.setReuseAddress (true);
    acceptorConfig.setThreadModel (ThreadModel.MANUAL);
    
    DefaultIoFilterChainBuilder filterChainBuilder =
      acceptorConfig.getFilterChain ();

    filterChainBuilder.addLast ("codec", ClientFrameCodec.FILTER);
    
    // connector
    connector = new SocketConnector (1, executor);
    connectorConfig = new SocketConnectorConfig ();
    
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (20);
    
    connectorConfig.getFilterChain ().addLast   
      ("codec", ClientFrameCodec.FILTER);
  }
  
  public void connect (IoHandler acceptorListener, IoHandler connectorListener)
    throws IOException
  {
    InetSocketAddress remoteAddress = new InetSocketAddress ("127.0.0.1", 29170);
    
    acceptor.bind (remoteAddress, acceptorListener, acceptorConfig);
    
    ConnectFuture future = 
      connector.connect (remoteAddress, connectorListener, connectorConfig);
    
    future.join ();
    
    session = future.getSession ();
  }
  
  
  public void close ()
  {
    session.close ();
    acceptor.unbindAll ();
    executor.shutdown ();
  }
}
