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

import java.util.Timer;
import java.util.TimerTask;

import java.io.Closeable;
import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.avis.config.Options;
import org.avis.federation.io.FederationFrameCodec;
import org.avis.federation.io.messages.FedConnRply;
import org.avis.federation.io.messages.FedConnRqst;
import org.avis.io.LivenessFilter;
import org.avis.io.RequestTrackingFilter;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.RequestTimeoutMessage;
import org.avis.router.Router;
import org.avis.util.Filter;
import org.avis.util.IllegalConfigOptionException;

import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;
import static org.avis.federation.Federation.logError;
import static org.avis.federation.Federation.logMessageReceived;
import static org.avis.federation.Federation.logMinaException;
import static org.avis.federation.Federation.logSessionOpened;
import static org.avis.io.FrameCodec.setMaxFrameLengthFor;
import static org.avis.io.Net.hostIdFor;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.info;
import static org.avis.logging.Log.internalError;
import static org.avis.logging.Log.warn;
import static org.avis.util.Text.className;
import static org.avis.util.Text.shortException;

/**
 * Initiates a federation connection to a remote host. After
 * connection and successfully handshaking with a FedConnRqst, it
 * creates and hands over processing to a Link.
 * 
 * @see Link
 * @see Acceptor
 * 
 * @author Matthew Phillips
 */
public class Connector implements IoHandler, Closeable
{
  private EwafURI uri;
  private Options options;
  private Router router;
  private SocketConnector connector;
  private SocketConnectorConfig connectorConfig;
  private InetSocketAddress remoteAddress;
  private IoSession session;
  private String serverDomain;
  private FederationClass federationClass;
  private Link link;
  private Timer asyncConnectTimer;
  protected volatile boolean closing;
  
  @SuppressWarnings("unchecked")
  public Connector (Router router, String serverDomain,
                    EwafURI uri, FederationClass federationClass,
                    Options options) 
    throws IllegalConfigOptionException, IOException
  {
    this.router = router;
    this.uri = uri;
    this.serverDomain = serverDomain;
    this.federationClass = federationClass;
    this.options = options;
    this.connector = new SocketConnector (1, router.executor ());
    this.connectorConfig = new SocketConnectorConfig ();
    this.remoteAddress = new InetSocketAddress (uri.host, uri.port);
    
    long requestTimeout = options.getInt ("Federation.Request-Timeout") * 1000;
    long keepaliveInterval = 
      options.getInt ("Federation.Keepalive-Interval") * 1000;

    /* Change the worker timeout to make the I/O thread quit soon
     * when there's no connection to manage. */
    connector.setWorkerTimeout (0);
    
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout ((int)(requestTimeout / 1000));
    
    DefaultIoFilterChainBuilder filters = new DefaultIoFilterChainBuilder ();
    
    filters.addLast ("codec", FederationFrameCodec.FILTER);
    
    filters.addLast
      ("requestTracker", new RequestTrackingFilter (requestTimeout));
    
    filters.addLast
      ("liveness", new LivenessFilter (keepaliveInterval, requestTimeout));

    Filter<InetAddress> authRequired = 
      (Filter<InetAddress>)options.get ("Federation.Require-Authenticated");
    
    if (uri.isSecure ())
      filters = router.createSecureFilters (filters, authRequired, true);
    else
      filters = router.createStandardFilters (filters, authRequired);
    
    connectorConfig.setFilterChainBuilder (filters);
    
    connect ();
  }
  
  public Link link ()
  {
    return link;
  }

  public boolean isWaitingForAsyncConnection ()
  {
    return asyncConnectTimer != null;
  }
  
  /**
   * True if the link has been connected plus successful handshake and
   * initial subscription.
   */
  public boolean isConnected ()
  {
    return link != null && link.isLive ();
  }
  
  /**
   * Kick off a connection attempt.
   */
  synchronized void connect ()
  {
    closing = false;
    
    cancelAsyncConnect ();
    
    connector.connect 
      (remoteAddress, this, connectorConfig).addListener 
        (new IoFutureListener ()
        {
          public void operationComplete (IoFuture future)
          {
            connectFutureComplete (future);
          }
        });
  }
  
  /**
   * Called by future created by connect () when complete.
   */
  protected void connectFutureComplete (IoFuture future)
  {
    if (closing)
      return;
    
    try
    {
      if (!future.isReady ())
      {
        diagnostic ("Connection attempt to federator at " + uri + 
                    " timed out, retrying", this);
        
        asyncConnect ();
      } else
      {
        open (future.getSession ());
      }
    } catch (RuntimeIOException ex)
    {
      diagnostic ("Failed to connect to federator at " + uri + ", retrying: " + 
                  shortException (ex.getCause ()), this, ex);
      
      asyncConnect ();
    }
  }

  /**
   * Schedule a delayed connect () in Federation.Request-Timeout
   * seconds from now.
   * 
   * @see #cancelAsyncConnect()
   */
  synchronized void asyncConnect ()
  {
    if (asyncConnectTimer != null)
      throw new Error ();
    
    asyncConnectTimer = new Timer ("Federation connector");
      
    TimerTask connectTask = new TimerTask ()
    {
      @Override
      public void run ()
      {
        synchronized (Connector.this)
        {
          if (!closing)
            connect ();
        }
      }
    };
    
    asyncConnectTimer.schedule 
      (connectTask, options.getInt ("Federation.Request-Timeout") * 1000L);
  }
  
  /**
   * Canncel an async connect and its associated timer.
   * 
   * @see #asyncConnect()
   */
  private void cancelAsyncConnect ()
  {
    if (asyncConnectTimer != null)
    {
      asyncConnectTimer.cancel ();
      
      asyncConnectTimer = null;
    }
  }

  /**
   * Called to open a federation connection when a connection and
   * session has been established.
   */
  synchronized void open (IoSession newSession)
  {
    this.session = newSession;
    
    cancelAsyncConnect ();
    
    send (new FedConnRqst (VERSION_MAJOR, VERSION_MINOR, serverDomain));
  }
  
  public void close ()
  {
    synchronized (this)
    {
      closing = true;
      
      cancelAsyncConnect ();
      
      if (session != null)
      {
        if (session.isConnected ())
        {
          if (link != null)
            link.close ();  // closes session after disconn
          else
            session.close ();
        } else
        {
          if (link != null && !link.initiatedSessionClose ())
          {
            warn ("Remote federator at " + uri + " " + 
                  "closed outgoing link with no warning", this);
            
            link.close ();
          }        
        }
        
        session = null;
      }
      
      link = null;
    }
  }
  
  private void reopen ()
  {
    if (closing)
      return;
    
    close ();

    diagnostic ("Scheduling reconnection for outgoing federation link to " + 
                uri, this);

    closing = false;
    
    asyncConnect ();
  }
  
  private void handleMessage (Message message)
  {
    switch (message.typeId ())
    {
      case FedConnRply.ID:
        createFederationLink (((FedConnRply)message).serverDomain);
        break;
      case Nack.ID:
        handleFedConnNack ((Nack)message);
        break;
      case RequestTimeoutMessage.ID:
        handleRequestTimeout (((RequestTimeoutMessage)message).request);
        break;
      case ErrorMessage.ID:
        logError ((ErrorMessage)message, this);
        close ();
        break;
      default:
        warn ("Unexpected message during handshake from remote federator at " +  
              uri + ": " + message.name (), this);
        reopen ();
    }
  }
  
  private void handleRequestTimeout (RequestMessage<?> request)
  {
    if (request.getClass () == FedConnRqst.class)
    {
      warn ("Federation connection request to remote federator at " + 
            uri + " timed out, reconnecting", this);
      
      reopen ();
    } else
    {
     // this shouldn't happen, FedConnRqst is the only request we send
     internalError ("Request timeout for unexpected message type: " + 
                    className (request), this);
    }
  }
  
  private void handleFedConnNack (Nack nack)
  {
    warn ("Closing connection to remote router at " + uri + 
          " after it rejected federation connect request: " + 
          nack.formattedMessage (), this);
    
    reopen ();
  }

  private void createFederationLink (String remoteServerDomain)
  {
    info ("Federation outgoing link for " + uri + " established with \"" + 
          hostIdFor (session) + "\", remote server domain \"" + 
          remoteServerDomain + "\"", this);
    
    link =
      new Link (router, session,
                federationClass, serverDomain, 
                remoteServerDomain, 
                remoteAddress.getAddress ().getCanonicalHostName ());
  }
  
  private void send (Message message)
  {
    Federation.send (session, serverDomain, message);
  }
  
  // IoHandler  
  
  public void sessionOpened (IoSession theSession)
    throws Exception
  {
    logSessionOpened (theSession, "outgoing", this);
  }
  
  public void sessionClosed (IoSession theSession)
    throws Exception
  {
    info ("Federation link for " + uri + " with \"" + hostIdFor (theSession) + 
          "\" disconnected", this);
    
    reopen ();
  }
  
  public void sessionCreated (IoSession theSession)
    throws Exception
  {
    setMaxFrameLengthFor (theSession, options.getInt ("Packet.Max-Length"));
  }
  
  public void messageReceived (IoSession theSession, Object theMessage)
    throws Exception
  {
    if (closing)
      return;
   
    Message message = (Message)theMessage;
    
    logMessageReceived (message, theSession, this);
    
    if (link == null)
      handleMessage (message);
    else if (!link.isClosed ())
      link.handleMessage (message);
  }
  
  public void messageSent (IoSession theSession, Object message)
    throws Exception
  {
    // zip
  }

  public void sessionIdle (IoSession theSession, IdleStatus status)
    throws Exception
  {
    // zip
  }
  
  public void exceptionCaught (IoSession theSession, Throwable cause)
    throws Exception
  {
    logMinaException (cause, this);
  }
}
