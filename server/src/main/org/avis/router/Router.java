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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.ReadThrottleFilterBuilder;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

import org.avis.common.ElvinURI;
import org.avis.config.Options;
import org.avis.io.ClientFrameCodec;
import org.avis.io.ExceptionMonitorLogger;
import org.avis.io.FrameTooLargeException;
import org.avis.io.messages.ConfConn;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRply;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.Notify;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.QuenchPlaceHolder;
import org.avis.io.messages.SecRply;
import org.avis.io.messages.SecRqst;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubDelRqst;
import org.avis.io.messages.SubModRqst;
import org.avis.io.messages.SubRply;
import org.avis.io.messages.TestConn;
import org.avis.io.messages.UNotify;
import org.avis.io.messages.XidMessage;
import org.avis.security.Keys;
import org.avis.subscription.parser.ConstantExpressionException;
import org.avis.subscription.parser.ParseException;
import org.avis.util.ConcurrentHashSet;
import org.avis.util.Filter;
import org.avis.util.IllegalConfigOptionException;
import org.avis.util.ListenerList;
import org.avis.util.Text;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.mina.common.ByteBuffer.setUseDirectBuffers;
import static org.apache.mina.common.IdleStatus.READER_IDLE;
import static org.apache.mina.common.IoFutureListener.CLOSE;

import static org.avis.common.Common.CLIENT_VERSION_MAJOR;
import static org.avis.common.Common.CLIENT_VERSION_MINOR;
import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.io.FrameCodec.setMaxFrameLengthFor;
import static org.avis.io.LegacyConnectionOptions.setWithLegacy;
import static org.avis.io.Net.addressesFor;
import static org.avis.io.Net.enableTcpNoDelay;
import static org.avis.io.TLS.toPassphrase;
import static org.avis.io.messages.Disconn.REASON_PROTOCOL_VIOLATION;
import static org.avis.io.messages.Disconn.REASON_SHUTDOWN;
import static org.avis.io.messages.Nack.EMPTY_ARGS;
import static org.avis.io.messages.Nack.EXP_IS_TRIVIAL;
import static org.avis.io.messages.Nack.IMPL_LIMIT;
import static org.avis.io.messages.Nack.NOT_IMPL;
import static org.avis.io.messages.Nack.NO_SUCH_SUB;
import static org.avis.io.messages.Nack.PARSE_ERROR;
import static org.avis.io.messages.Nack.PROT_INCOMPAT;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.trace;
import static org.avis.logging.Log.warn;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;
import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;
import static org.avis.subscription.parser.SubscriptionParserBase.expectedTokensFor;
import static org.avis.util.Text.formatNotification;
import static org.avis.util.Text.idFor;

public class Router implements IoHandler, Closeable
{
  static
  {
    // route MINA exceptions to log
    ExceptionMonitor.setInstance (ExceptionMonitorLogger.INSTANCE);
  }
  
  private static final String ROUTER_VERSION =
    System.getProperty ("avis.router.version", "<unknown>");
  
  private RouterOptions routerOptions;
  private ExecutorService executor;
  private SocketAcceptor acceptor;
  private KeyStore keystore;
  private volatile boolean closing;
  
  private ConcurrentHashSet<IoSession> sessions;

  private ListenerList<NotifyListener> notifyListeners;
  private ListenerList<CloseListener> closeListeners;

  /**
   * Create an instance with default configuration.
   */
  public Router ()
    throws IOException
  {
    this (DEFAULT_PORT);
  }
  
  /**
   * Shortcut to create an instance listening to localhost:port.
   */
  public Router (int port)
    throws IOException
  {
    this (new RouterOptions (port));
  }
  
  /**
   * Create a new instance.
   * 
   * @param options The router configuration options. Note that, due
   *                to a MINA limitation, the IO.Use-Direct-Buffers
   *                option applies globally, so using multiple router
   *                instances with this option set to different values
   *                will clash.
   * 
   * @throws IOException if an network error during router
   *                 initialisation.
   * @throws IllegalConfigOptionException If an option in the configuratiion
   *                 options is invalid.
   */
  @SuppressWarnings ("unchecked")
  public Router (RouterOptions options)
    throws IOException, IllegalConfigOptionException
  {
    this.notifyListeners = 
      new ListenerList<NotifyListener>
        (NotifyListener.class, "notifyReceived", Notify.class, Keys.class);
    this.closeListeners = 
      new ListenerList<CloseListener>
        (CloseListener.class, "routerClosing", Router.class);
    
    this.routerOptions = options;
    this.sessions = new ConcurrentHashSet<IoSession> ();
    this.executor = newCachedThreadPool ();
    this.acceptor =
      new SocketAcceptor (getRuntime ().availableProcessors () + 1,
                          executor);
    
    setUseDirectBuffers (options.getBoolean ("IO.Use-Direct-Buffers"));
    
    /*
     * Setup IO filter chain with codec and then thread pool. NOTE:
     * The read throttling system needs an ExecutorFilter to glom
     * onto: it's not clear that we gain any other benefit from it
     * since notification processing is non-blocking. See
     * http://mina.apache.org/configuring-thread-model.html.
     */
    DefaultIoFilterChainBuilder filters = new DefaultIoFilterChainBuilder ();

    filters.addLast ("codec", ClientFrameCodec.FILTER);
    filters.addLast ("threadPool", new ExecutorFilter (executor));
    
    bind (options.listenURIs (), this, filters, 
          (Filter<InetAddress>)routerOptions.get ("Require-Authenticated"));
  }

  /**
   * Bind to a set of URI's. This can be used by plugins to bind to
   * network addresses using the same network setup as the router
   * would, including TLS parameters.
   * 
   * @param uris The URI's to listen to.
   * @param handler The IO handler.
   * @param baseFilters The IO filters used by all connection types.
   * @param authRequired Hosts matched by this filter must be
   *                successfully authenticated via TLS or will be
   *                refused access.
   * 
   * @throws IOException if an error occurred during binding.
   */
  public void bind (Set<? extends ElvinURI> uris, IoHandler handler,
                    DefaultIoFilterChainBuilder baseFilters, 
                    Filter<InetAddress> authRequired) 
    throws IOException
  {
    SocketAcceptorConfig defaultAcceptorConfig = 
      createAcceptorConfig 
        (createStandardFilters (baseFilters, authRequired));
    SocketAcceptorConfig secureAcceptorConfig = null; // lazy init'ed
    
    for (ElvinURI uri : uris)
    {
      SocketAcceptorConfig bindConfig;
      
      if (uri.isSecure ())
      {
        if (secureAcceptorConfig == null)
        {
          secureAcceptorConfig = 
            createAcceptorConfig 
              (createSecureFilters (baseFilters, authRequired, false));
        }
        
        bindConfig = secureAcceptorConfig;
      } else
      {
        bindConfig = defaultAcceptorConfig;
      }
      
      for (InetSocketAddress address : addressesFor (uri))
        acceptor.bind (address, handler, bindConfig);
    }
  }

  /**
   * Create default MINA config for incoming connections.
   * 
   * @param filters The base set of common filters.
   * @param authRequired The set of hosts that must connect via
   *                authenticated connections and should be
   *                blacklisted from non authenticated access.
   */
  private SocketAcceptorConfig createAcceptorConfig 
    (DefaultIoFilterChainBuilder filters)
  {
    SocketAcceptorConfig defaultAcceptorConfig = new SocketAcceptorConfig ();
    
    defaultAcceptorConfig.setReuseAddress (true);
    defaultAcceptorConfig.setThreadModel (ThreadModel.MANUAL);
    defaultAcceptorConfig.setFilterChainBuilder (filters);
    
    return defaultAcceptorConfig;
  }
  
  /**
   * Create the filters used for standard, non secured links.
   * 
   * @param commonFilters The initial set of filters to add to.
   * @param authRequired The hosts for which authentication is
   *                required. For standard link these hosts are denied
   *                connection.
   * @return The new filter set.
   */
  public DefaultIoFilterChainBuilder createStandardFilters 
    (DefaultIoFilterChainBuilder commonFilters, 
     Filter<InetAddress> authRequired)
  {
    if (authRequired != Filter.MATCH_NONE)
    {
      commonFilters = (DefaultIoFilterChainBuilder)commonFilters.clone (); 
      
      commonFilters.addFirst ("blacklist", new BlacklistFilter (authRequired));
    }
    
    return commonFilters;
  }
  
  /**
   * Create the filters used for TLS-secured links.
   * 
   * @param commonFilters The initial set of filters to add to.
   * @param authRequired The hosts for which authentication is
   *                required.
   * @param clientMode True if the TLS filter should be in client mode.
   * 
   * @return The new filter set.
   */
  public DefaultIoFilterChainBuilder createSecureFilters 
    (DefaultIoFilterChainBuilder commonFilters, 
     Filter<InetAddress> authRequired, 
     boolean clientMode) throws IOException
  {
    DefaultIoFilterChainBuilder secureFilters = 
      (DefaultIoFilterChainBuilder)commonFilters.clone ();
    
    secureFilters.addFirst 
      ("security", 
       new SecurityFilter (keystore (), 
                           routerOptions.getString ("TLS.Keystore-Passphrase"), 
                           authRequired, clientMode));
    
    return secureFilters;
  }

  /**
   * Lazy load the router's keystore.
   */
  private KeyStore keystore () 
    throws IOException
  {
    if (keystore == null)
    {
      URI keystoreUri = (URI)routerOptions.get ("TLS.Keystore");
      
      if (keystoreUri.toString ().length () == 0)
      {
        throw new IOException 
          ("Cannot use TLS without a keystore: " +
           "see TLS.Keystore configuration option");
      }
      
      InputStream keystoreStream = 
        routerOptions.toAbsoluteURI (keystoreUri).toURL ().openStream ();

      try
      {
        keystore = KeyStore.getInstance ("JKS");
        
        keystore.load 
          (keystoreStream, 
           toPassphrase (routerOptions.getString ("TLS.Keystore-Passphrase")));
      } catch (GeneralSecurityException ex)
      {
        throw new IOException ("Failed to load TLS keystore: " + 
                               ex.getMessage ());
      } finally
      {
        keystoreStream.close ();
      }
    }
    
    return keystore;
  }
  
  /**
   * Close all connections synchronously. Close listeners are notified
   * before shutdown commences. May be called more than once with no
   * effect.
   */
  public void close ()
  {
    synchronized (this)
    {
      if (closing)
        return;
      
      closing = true; 
    }
    
    closeListeners.fire (this);
    closeListeners = null;
    
    Disconn disconnMessage = new Disconn (REASON_SHUTDOWN);
    
    for (IoSession session : sessions)
    {
      Connection connection = peekConnectionFor (session);
     
      session.suspendRead ();
      
      if (connection != null)
      {
        connection.lockWrite ();

        try
        {
          if (connection.isOpen ())
          {
            send (session, disconnMessage).addListener (CLOSE);

            connection.close ();
          }
        } finally
        {
          connection.unlockWrite ();
        }
      }
    }
    
    waitForAllSessionsClosed ();
    
    acceptor.unbindAll ();
    executor.shutdown ();
    
    try
    {
      if (!executor.awaitTermination (15, SECONDS))
        warn ("Failed to cleanly shut down thread pool", this);
    } catch (InterruptedException ex)
    {
      diagnostic ("Interrupted while waiting for shutdown", this, ex);
    }
  }

  private void waitForAllSessionsClosed ()
  {
    long finish = currentTimeMillis () + 20000;
    
    try
    {
      while (!sessions.isEmpty () && currentTimeMillis () < finish)
        sleep (100);
    } catch (InterruptedException ex)
    {
      Thread.currentThread ().interrupt ();
    }
    
    if (!sessions.isEmpty ())
    {
      warn ("Sessions took too long to close " +
      		"(" + sessions.size () + " still open)", this);
    }
    
    sessions.clear ();
  }
  
  /**
   * The shared executor thread pool used by the router. Plugins may
   * share this.
   */
  public ExecutorService executor ()
  {
    return executor;
  }
  
  /**
   * The router's MINA socket acceptor. Plugins may share this.
   */
  public SocketAcceptor socketAcceptor ()
  {
    return acceptor;
  }
  
  public Set<ElvinURI> listenURIs ()
  {
    return routerOptions.listenURIs ();
  }
  
  public Options options ()
  {
    return routerOptions;
  }
  
  /**
   * Used for testing to simulate server hanging: server stops
   * responding to messages but keeps connection open.
   * 
   * @see #testSimulateUnhang()
   */
  public void testSimulateHang ()
  {
    // cause messageReceived () to stop processing
    closing = true;
  }
  
  /**
   * Undo the effect of {@link #testSimulateHang()}.
   */
  public void testSimulateUnhang ()
  {
    closing = false;
  }
  
  /**
   * Add a listener that will be invoked when the router is about to
   * close down.
   * 
   * @see #removeCloseListener(CloseListener)
   */
  public void addCloseListener (CloseListener listener)
  {
    synchronized (closeListeners)
    {      
      closeListeners.add (listener);
    }
  }
  
  /**
   * Undo the effect of {@link #addCloseListener(CloseListener)}.
   */
  public void removeCloseListener (CloseListener listener)
  {
    synchronized (closeListeners)
    {
      closeListeners.remove (listener);
    }
  }
  
  /**
   * Get a list of the current close event listeners.
   */
  public List<CloseListener> closeListeners ()
  {
    return closeListeners.asList ();
  }
  
  /**
   * Add a listener that will be invoked whenever a Notify message is
   * handled for delivery.
   * 
   * @see #removeNotifyListener(NotifyListener)
   * @see #injectNotify(Notify)
   */
  public void addNotifyListener (NotifyListener listener)
  {
    synchronized (notifyListeners)
    {
      notifyListeners.add (listener);
    }
  }
  
  /**
   * Undo the effect of {@link #addNotifyListener(NotifyListener)}.
   */
  public void removeNotifyListener (NotifyListener listener)
  {
    synchronized (notifyListeners)
    {
      notifyListeners.remove (listener);
    }
  }
  
  // IoHandler interface

  public void messageReceived (IoSession session, Object messageObject)
    throws Exception
  {
    if (closing || connectionClosed (session))
      return;
    
    if (shouldLog (TRACE))
    {
      trace ("Server got message from " + Text.idFor (session) +
             ": " + messageObject, this);
    }
    
    Message message = (Message)messageObject;

    try
    {
      switch (message.typeId ())
      {
        case ConnRqst.ID:
          handleConnRqst (session, (ConnRqst)message);
          break;
        case DisconnRqst.ID:
          handleDisconnRqst (session, (DisconnRqst)message);
          break;
        case SubAddRqst.ID:
          handleSubAddRqst (session, (SubAddRqst)message);
          break;
        case SubModRqst.ID:
          handleSubModRqst (session, (SubModRqst)message);
          break;
        case SubDelRqst.ID:
          handleSubDelRqst (session, (SubDelRqst)message);
          break;
        case NotifyEmit.ID:
          handleNotifyEmit (session, (NotifyEmit)message);
          break;
        case SecRqst.ID:
          handleSecRqst (session, (SecRqst)message);
          break;
        case TestConn.ID:
          handleTestConn (session);
          break;
        case UNotify.ID:
          handleUnotify ((UNotify)message);
          break;
        case ErrorMessage.ID:
          handleError (session, (ErrorMessage)message);
          break;
        case QuenchPlaceHolder.ID:
          handleQuench (session, (QuenchPlaceHolder)message);
          break;
        default:
          warn
            ("Server got an unhandleable message type: " + message, this);
      }
    } catch (ProtocolCodecException ex)
    {
      /*
       * A message processing method detected a protocol violation
       * e.g. attempt to remove non existent subscription.
       */
      disconnectProtocolViolation (session, message, ex.getMessage (), ex);
    }
  }

  private void handleConnRqst (IoSession session, ConnRqst message)
    throws ProtocolCodecException
  {
    if (peekConnectionFor (session) != null)
      throw new ProtocolCodecException ("Already connected");
    
    Connection connection =
      new Connection (routerOptions, message.options,
                      message.subscriptionKeys, message.notificationKeys);
    
    int maxKeys = connection.options.getInt ("Connection.Max-Keys");
    
    if (message.versionMajor != CLIENT_VERSION_MAJOR ||
        message.versionMinor > CLIENT_VERSION_MINOR)
    {
      send (session,
            new Nack (message, PROT_INCOMPAT,
                      "Max supported protocol version is " +
                       + CLIENT_VERSION_MAJOR + '.' + CLIENT_VERSION_MINOR +
                       ": use a connection URI like " +
                       "elvin:" + CLIENT_VERSION_MAJOR + '.' + 
                       CLIENT_VERSION_MINOR + "//hostname to specify " +
                       "protocol version"));
    } else if (message.notificationKeys.size () > maxKeys ||
               message.subscriptionKeys.size () > maxKeys)
    {
      nackLimit (session, message, "Too many keys");
    } else
    {
      updateTcpSendImmediately (session, connection.options);
      updateQueueLength (session, connection);
      
      Map<String, Object> options = connection.options.accepted ();

      // add router ID
      setWithLegacy (options, 
                     "Vendor-Identification", "Avis " + ROUTER_VERSION);
      
      connection.lockWrite ();
      
      try
      {
        setConnection (session, connection);
        
        send (session, new ConnRply (message, options));
      } finally
      {
        connection.unlockWrite ();
      }
    }
  }

  /**
   * NOTE: the spec says it's a violation to add the same key more
   * than once or to remove a non-existent key (sec 7.4.8) and
   * should be reported as a protocol violation. Avis currently
   * doesn't enforce this since neither of these cases has any
   * effect on its key collections and the check would add overhead.
   */
  private void handleSecRqst (IoSession session, SecRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);
    
    try
    {
      message.addNtfnKeys.hashPrivateKeysForRole (PRODUCER);
      message.delNtfnKeys.hashPrivateKeysForRole (PRODUCER);
      
      message.addSubKeys.hashPrivateKeysForRole (CONSUMER);
      message.delSubKeys.hashPrivateKeysForRole (CONSUMER);
      
      Keys newNtfnKeys = connection.notificationKeys.delta
        (message.addNtfnKeys, message.delNtfnKeys);
  
      Keys newSubKeys = connection.subscriptionKeys.delta
        (message.addSubKeys, message.delSubKeys);

      if (connection.connectionKeysFull (newNtfnKeys, newSubKeys))
      {
        nackLimit (session, message, "Too many keys");
      } else
      {
        connection.notificationKeys = newNtfnKeys;
        connection.subscriptionKeys = newSubKeys;
      
        send (session, new SecRply (message));
      }
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleDisconnRqst (IoSession session, DisconnRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      connection.close ();
      
      send (session, new DisconnRply (message)).addListener (CLOSE);
    } finally
    {
      connection.unlockWrite ();
    }
  }
  
  private void handleSubAddRqst (IoSession session, SubAddRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      if (connection.subscriptionsFull ())
      {
        nackLimit (session, message, "Too many subscriptions");
      } else if (connection.subscriptionTooLong (message.subscriptionExpr))
      {
        nackLimit (session, message, "Subscription too long");
      } else if (connection.subscriptionKeysFull (message.keys))
      {
        nackLimit (session, message, "Too many keys");
      } else
      {
        Subscription subscription =
          new Subscription (message.subscriptionExpr,
                            message.keys, message.acceptInsecure);
       
        connection.addSubscription (subscription);
  
        send (session, new SubRply (message, subscription.id));
      }
    } catch (ParseException ex)
    {
      nackParseError (session, message, message.subscriptionExpr, ex);
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleSubModRqst (IoSession session, SubModRqst message)
    throws NoConnectionException  
  {
    Connection connection = writeableConnectionFor (session);

    try
    {
      Subscription subscription =
        connection.subscriptionFor (message.subscriptionId);
      
      message.addKeys.hashPrivateKeysForRole (CONSUMER);
      message.delKeys.hashPrivateKeysForRole (CONSUMER);
      
      Keys newKeys = 
        subscription.keys.delta (message.addKeys, message.delKeys);

      if (connection.subscriptionKeysFull (newKeys))
      {
        nackLimit (session, message, "Too many keys");
      } else if (connection.subscriptionTooLong (message.subscriptionExpr))
      {
        nackLimit (session, message, "Subscription too long");
      } else
      {
        if (message.subscriptionExpr.length () > 0)
          subscription.updateExpression (message.subscriptionExpr);
  
        subscription.keys = newKeys;
        subscription.acceptInsecure = message.acceptInsecure;
        
        send (session, new SubRply (message, subscription.id));
      }
    } catch (ParseException ex)
    {
      nackParseError (session, message, message.subscriptionExpr, ex);
    } catch (InvalidSubscriptionException ex)
    {
      nackNoSub (session, message, message.subscriptionId, ex.getMessage ());
    } finally
    {
      connection.unlockWrite ();
    }
  }

  private void handleSubDelRqst (IoSession session, SubDelRqst message)
    throws NoConnectionException
  {
    Connection connection = writeableConnectionFor (session);
    
    try
    {
      if (connection.removeSubscription (message.subscriptionId) != null)
        send (session, new SubRply (message, message.subscriptionId));
      else
        nackNoSub (session, message, message.subscriptionId,
                   "Invalid subscription ID");
    } finally
    {
      connection.unlockWrite ();
    }
  }
  
  private void handleNotifyEmit (IoSession session, NotifyEmit message)
    throws NoConnectionException
  {
    if (shouldLog (TRACE))
      logNotification (session, message);
    
    message.keys.hashPrivateKeysForRole (PRODUCER);
    
    deliverNotification (message, connectionFor (session).notificationKeys);
  }

  private void handleUnotify (UNotify message)
  {
    if (shouldLog (TRACE))
      logNotification (null, message);
    
    message.keys.hashPrivateKeysForRole (PRODUCER);
    
    deliverNotification (message, EMPTY_KEYS);
  }
  
  /**
   * Inject a notification from an outside producer.
   */
  public void injectNotify (Notify message)
  {
    deliverNotification (message, EMPTY_KEYS);
  }

  /**
   * Deliver a notification message to subscribers.
   * 
   * @param message The message (a UNotify or a NotifyEmit).
   * @param notificationKeys The global notification keys that apply
   *          to the message. These are in addition to any keys
   *          attached to the message itself.
   */
  private void deliverNotification (Notify message, Keys notificationKeys)
  {
    for (IoSession session : sessions)
    {
      Connection connection = peekConnectionFor (session);
      
      if (connection == null)
        continue;
      
      connection.lockRead ();

      try
      {        
        if (!connection.isOpen ())
          continue;
        
        SubscriptionMatch matches =
          connection.matchSubscriptions (message.attributes,
                                         notificationKeys,
                                         message.keys,
                                         message.deliverInsecure);

        if (matches.matched ())
        {
          if (shouldLog (TRACE))
          {
            trace ("Delivering notification " + idFor (message) + 
                   " to client " + idFor (session), this);
          }
          
          send (session, new NotifyDeliver (message.attributes,
                                            matches.secure (),
                                            matches.insecure ()));
        }
      } catch (RuntimeException ex)
      {
        /*
         * Do not allow "normal" runtime exceptions to abort delivery
         * to other clients. Log and continue to next client.
         */
        alarm ("Exception while delivering notification", this, ex);
      } finally
      {
        connection.unlockRead ();
      }
    }
    
    if (notifyListeners.hasListeners ())
      notifyListeners.fire (message, notificationKeys);
  }

  private static void handleTestConn (IoSession session)
  {
    // if no other outgoing messages are waiting, send a confirm message
    if (session.getScheduledWriteRequests () == 0)
      send (session, ConfConn.INSTANCE);
  }
  
  private static void handleQuench (IoSession session,
                                    QuenchPlaceHolder message)
  {
    diagnostic
      ("Rejecting quench request from client: quench is not supported",
       Router.class);
    
    send (session, new Nack (message, NOT_IMPL, "Quench not supported"));
  }

  private static void handleError (IoSession session, 
                                   ErrorMessage errorMessage)
  {
    String message;
    
    if (errorMessage.error instanceof FrameTooLargeException)
    {
      // add helpful advisory for client that exceeds max frame size
      message = 
        errorMessage.error.getMessage () + 
        ". Use the Packet.Max-Length connection option to increase the " +
        "maximum notification size.";
    } else
    {
      message = errorMessage.error.getMessage ();
    }
    
    disconnectProtocolViolation (session, errorMessage.cause, message, null);
  }

  /**
   * Handle a protocol violation by a client disconnecting with the
   * REASON_PROTOCOL_VIOLATION code.
   * 
   * @param session The client session.
   * @param cause The message that caused the violation.
   * @param diagnosticMessage The diagnostic sent back to the client.
   * @throws NoConnectionException
   */
  private static void disconnectProtocolViolation (IoSession session,
                                                   Message cause,
                                                   String diagnosticMessage,
                                                   Throwable error)
  {
    if (diagnosticMessage == null)
      diagnosticMessage = "Frame format error";
    
    warn ("Disconnecting client " + idFor (session) + 
          " due to protocol violation: " +
          diagnosticMessage, Router.class);

    if (error != null)
      diagnostic ("Decode stack trace", Router.class, error);
    
    Connection connection = peekConnectionFor (session);
    
    if (connection != null)
    {
      connection.lockWrite ();
      
      try
      {
        connection.close ();
      } finally
      {
        connection.unlockWrite ();
      }
    }
    
    // send Disconn and close
    send (session,
          new Disconn (REASON_PROTOCOL_VIOLATION,
                       diagnosticMessage)).addListener (CLOSE);
  }

  /**
   * Handle the TCP.Send-Immediately connection option if set.
   */
  private static void updateTcpSendImmediately (IoSession session,
                                                Options options)
  {
    if (!enableTcpNoDelay (session, 
                           options.getInt ("TCP.Send-Immediately") != 0))
    {
      options.remove ("TCP.Send-Immediately"); 
    }
  }
  
  /**
   * Update the receive/send queue lengths based on connection
   * options. Currently only implements Receive-Queue.Max-Length using
   * MINA's ReadThrottleFilterBuilder filter.
   */
  private static void updateQueueLength (IoSession session,
                                         Connection connection)
  {
    ReadThrottleFilterBuilder readThrottle =
      (ReadThrottleFilterBuilder)session.getAttribute ("readThrottle");
    
    readThrottle.setMaximumConnectionBufferSize
      (connection.options.getInt ("Receive-Queue.Max-Length"));
  }

  /**
   * Send a NACK response for a parse error with error info.
   */
  private static void nackParseError (IoSession session,
                                      XidMessage inReplyTo,
                                      String expr,
                                      ParseException ex)
  {
    int code;
    Object [] args = EMPTY_ARGS;
    String message;
    
    if (ex instanceof ConstantExpressionException)
    {
      code = EXP_IS_TRIVIAL;
      message = ex.getMessage ();
    } else
    {
      code = PARSE_ERROR;
      
      if (ex.currentToken == null)
      {
        // handle ParseException with no token info
        
        message = ex.getMessage ();
        args = new Object [] {0, ""};
      } else
      {
        // use token info to generate a better error message
        
        args = new Object [] {ex.currentToken.next.beginColumn,
                              ex.currentToken.next.image};
        
        /*
         * NB: we could use %1 and %2 to refer to args in the message
         * here, but why make it harder for the client?
         */
        message = "Parse error at column " + args [0] + 
                  ", token \"" + args [1] + "\": expected: " +
                  expectedTokensFor (ex);
      }
    }
    
    diagnostic ("Subscription add/modify failed with parse error: " +
                message, Router.class);
    diagnostic ("Subscription was: " + expr, Router.class);
    
    send (session, new Nack (inReplyTo, code, message, args));
  }
  
  /**
   * Send a NACK due to a blown limit, e.g. Subscription.Max-Count.
   */
  private static void nackLimit (IoSession session, XidMessage inReplyTo,
                                 String message)
  {
    send (session, new Nack (inReplyTo, IMPL_LIMIT, message));
  }
  
  /**
   * Send a NACK due to an invalid subscription ID.
   */
  private static void nackNoSub (IoSession session, XidMessage inReplyTo,
                                 long subscriptionId, String message)
  {
    send (session, new Nack (inReplyTo, NO_SUCH_SUB, message,
                             subscriptionId));
  }
  
  public void exceptionCaught (IoSession session, Throwable ex)
    throws Exception
  {
    if (ex instanceof IOException)
      diagnostic ("IO exception while processing message", this, ex);
    else
      alarm ("Server exception", this, ex);
  }
  
  public void messageSent (IoSession session, Object message)
    throws Exception
  {
    // zip
  }

  /**
   * NB this can be called *after* close () is completed sometimes.
   */
  public void sessionClosed (IoSession session)
    throws Exception
  {
    if (shouldLog (TRACE))
      trace ("Server session " + Text.idFor (session) + " closed", this);
    
    sessions.remove (session);

    Connection connection = peekConnectionFor (session);
    
    if (connection != null)
    {
      connection.lockWrite ();
     
      try
      {
        if (connection.isOpen ())
        {
          diagnostic ("Client disconnected without warning", this);
          
          connection.close ();
        }
      } finally
      {
        connection.unlockWrite ();
      }
    }
  }

  public void sessionCreated (IoSession session)
    throws Exception
  {
    // client has this long to connect or UNotify
    session.setIdleTime
      (READER_IDLE, 
       routerOptions.getInt ("IO.Idle-Connection-Timeout"));
    
    // install read throttle
    ReadThrottleFilterBuilder readThrottle = new ReadThrottleFilterBuilder ();
    
    readThrottle.setMaximumConnectionBufferSize
      (CONNECTION_OPTION_SET.defaults.getInt ("Receive-Queue.Max-Length"));
    
    readThrottle.attach (session.getFilterChain ());
    
    session.setAttribute ("readThrottle", readThrottle);
    
    // set default max length for connectionless sessions
    setMaxFrameLengthFor
      (session,
       CONNECTION_OPTION_SET.defaults.getInt ("Packet.Max-Length"));
    
    sessions.add (session);
  }

  public void sessionIdle (IoSession session, IdleStatus status)
    throws Exception
  {
    // close idle sessions that we haven't seen a ConnRqst for yet
    if (status == READER_IDLE && peekConnectionFor (session) == null)
    {
      diagnostic
        ("Client " + Text.idFor (session) +
         " waited too long to connect: closing session", this);
      
      session.close ();
    }
  }

  public void sessionOpened (IoSession session)
    throws Exception
  {
    diagnostic ("Server session " + Text.idFor (session) + 
                " opened for connection on " + session.getServiceAddress () + 
                (isSecure (session) ? " (using TLS)" : ""), this);
    
  }
  
  private static void setConnection (IoSession session,
                                     Connection connection)
  {
    session.setAttachment (connection);
    
    setMaxFrameLengthFor
      (session, connection.options.getInt ("Packet.Max-Length"));
  }
  
  /**
   * Get the (open) connection associated with a session or throw
   * NoConnectionException.
   */
  private static Connection connectionFor (IoSession session)
    throws NoConnectionException
  {
    Connection connection = (Connection)session.getAttachment ();
    
    if (connection == null)
      throw new NoConnectionException ("No connection established for session");
    else if (!connection.isOpen ())
      throw new NoConnectionException ("Connection is closed");
    else
      return connection;
  }
  
  /**
   * Like connectionFor () but also acquires a write lock.
   * 
   * @throws NoConnectionException if there is no connection for the
   *           session or the connection is not open.
   */
  private static Connection writeableConnectionFor (IoSession session)
    throws NoConnectionException
  {
    Connection connection = connectionFor (session);
    
    connection.lockWrite ();
    
    if (!connection.isOpen ())
    {
      connection.unlockWrite ();
      
      throw new NoConnectionException ("Connection is closed");
    }
    
    return connection;
  }
  
  private static WriteFuture send (IoSession session, Message message)
  {    
    if (shouldLog (TRACE))
    {
      trace ("Server sent message to " + Text.idFor (session) + ": " + message,
             Router.class);
    }
    
    return session.write (message);
  }

  private void logNotification (IoSession session, Notify message)
  {
    trace ("Notification " + idFor (message) + 
           " from client " + idFor (session) + ":\n" + 
           formatNotification (message.attributes), this);
  }
  
  /**
   * Get the connection associated with a session or null for no connection.
   */
  private static Connection peekConnectionFor (IoSession session)
  {
    return (Connection)session.getAttachment ();
  }
  
  /**
   * Test if connection is closed or underlying session is closing.
   */
  private static boolean connectionClosed (IoSession session)
  {
    Connection connection = peekConnectionFor (session);
    
    return session.isClosing () || 
            (connection != null && !connection.isOpen ());
  }

  public static boolean isSecure (IoSession session)
  {
    return session.getServiceConfig ().getFilterChain ().contains 
      (SecurityFilter.class);
  }
}
