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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import java.io.IOException;

import java.lang.management.ManagementFactory;

import org.avis.config.Options;
import org.avis.router.CloseListener;
import org.avis.router.Router;
import org.avis.subscription.ast.Node;
import org.avis.util.IllegalConfigOptionException;

import static java.lang.Integer.toHexString;
import static java.lang.System.identityHashCode;
import static java.util.Collections.emptySet;

import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.common.ElvinURI.secureProtocol;
import static org.avis.io.Net.localHostName;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;
import static org.avis.util.Text.shortException;

/**
 * Constructs the federation setup by reading Federation.*
 * configuration items from a config and setting up federation
 * classes, listeners and acceptors to match.
 * 
 * @author Matthew Phillips
 */
public class FederationManager implements CloseListener
{
  protected Router router;
  protected FederationClasses classes;
  protected Acceptor acceptor;
  protected List<Connector> connectors;

  public FederationManager (Router router, Options federationConfig) 
    throws IllegalConfigOptionException, IOException
  {
    this.router = router;
    
    String serverDomain = initServerDomain (federationConfig);
    
    classes = initClasses (federationConfig);
    
    initAddAttributes (federationConfig, classes);
      
    connectors = initConnectors (router, serverDomain, classes, 
                                 federationConfig);

    acceptor =
      initAcceptor (router, serverDomain, classes, federationConfig);
    
    router.addCloseListener (this);
  }
  
  /**
   * Find the federation manager for a router.
   * 
   * @param router The router.
   * @return The last federation manager created for the router.
   * 
   * @throws IllegalArgumentException if no manager found.
   */
  public static FederationManager federationManagerFor (Router router)
  {
    for (Object listener : router.closeListeners ())
    {
      if (listener instanceof FederationManager)
        return (FederationManager)listener;
    }
    
    throw new IllegalArgumentException ("No federation manager");
  }
  
  public Acceptor acceptor ()
  {
    return acceptor;
  }
  
  public Collection<Connector> connectors ()
  {
    return connectors;
  }

  public Set<EwafURI> listenURIs ()
  {
    if (acceptor == null)
      return emptySet ();
    else
      return acceptor.listenURIs ();
  }

  public void routerClosing (Router theRouter)
  {
    close ();
  }
  
  public void close ()
  {
    router.removeCloseListener (this);
    
    if (acceptor != null)
      acceptor.close ();
    
    for (Connector connector : connectors)
      connector.close ();
    
    acceptor = null;
    connectors = null;
  }
  
  public boolean isClosed ()
  {
    return connectors == null;
  }
  
  @SuppressWarnings("unchecked")
  private static List<Connector> initConnectors
    (Router router,
     String serverDomain,
     FederationClasses classes, 
     Options config) 
     throws IllegalConfigOptionException, IOException
  {
    Map<String, Set<EwafURI>> connect = 
      (Map<String, Set<EwafURI>>)config.getParamOption ("Federation.Connect");
   
    // check federation classes and URI's make sense
    for (Entry<String, Set<EwafURI>> entry : connect.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      if (fedClass.allowsNothing ())
      {
        throw new IllegalConfigOptionException
          ("Federation.Connect[" + entry.getKey () + "]",
           "No federation subscribe/provide defined: " +
            "this connection cannot import or export any notifications");
      }
      
      for (EwafURI uri : entry.getValue ())
        checkUri ("Federation.Connect[" + entry.getKey () + "]", uri);
    }
    
    List<Connector> connectors = new ArrayList<Connector> (connect.size ());
    
    for (Entry<String, Set<EwafURI>> entry : connect.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      for (EwafURI uri : entry.getValue ())
      {
        connectors.add
          (new Connector (router, serverDomain, uri, fedClass, config));
      }
    }
    
    return connectors;
  }

  private String initServerDomain (Options federationConfig)
  {
    String domain = federationConfig.getString ("Federation.Router-Name");
    
    if (domain.length () == 0)
    {
      try
      {
        domain = defaultServerDomain ();
      } catch (IOException ex)
      {
        throw new IllegalConfigOptionException
          ("Federation.Router-Name", 
           "Cannot auto detect default router name, " +
           "please set this manually: " + shortException (ex));
      }
    }
    
    return domain;
  }

  /**
   * Do the best we can to guess a good server domain based on
   * identity hashcode.PID.hostname.
   */
  private String defaultServerDomain ()
    throws IOException
  {
    String instanceId = toHexString (identityHashCode (this));
    String runtimeName = ManagementFactory.getRuntimeMXBean ().getName ();
 
    /*
     * RuntimeMXBean.getName () returns pid@hostname on many VM's: if
     * it looks like this is the case, use it otherwise fall back on
     * hashcode + hostname.
     */
    if (runtimeName.matches ("\\d+@.+"))
      return instanceId + '.' + runtimeName;
    else
      return instanceId + '@' + localHostName ();
  }

  @SuppressWarnings("unchecked")
  private static Acceptor initAcceptor (Router router, 
                                        String serverDomain,
                                        FederationClasses classes,
                                        Options config)
  {
    Set<EwafURI> uris = (Set<EwafURI>)config.get ("Federation.Listen");
    
    if (uris.isEmpty ())
    {
      return null;
    } else
    {
      try
      {
        for (EwafURI uri : uris)
          checkUri ("Federation.Listen", uri);
        
        return new Acceptor (router, serverDomain, classes, uris, config);
      } catch (IOException ex)
      {
        throw new IllegalConfigOptionException ("Federation.Listen", 
                                          shortException (ex));
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static FederationClasses initClasses (Options federationConfig)
  {
    FederationClasses classes = new FederationClasses ();
    
    Map<String, ?> provide = 
      federationConfig.getParamOption ("Federation.Provide");
    
    for (Entry<String, ?> entry : provide.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      fedClass.outgoingFilter = (Node)entry.getValue ();
    }
    
    Map<String, ?> subscribe = 
      federationConfig.getParamOption ("Federation.Subscribe");
    
    for (Entry<String, ?> entry : subscribe.entrySet ())
    {
      Node incomingFilter = (Node)entry.getValue ();

      /*
       * Cannot sub TRUE right now. When we support 1.1-level
       * federation this will be possible as CONST_TRUE will be
       * &&'d with the current consolidated subscription.
       */ 
      if (incomingFilter == CONST_TRUE)
      {
        throw new IllegalConfigOptionException 
          ("Federation.Subscribe[" + entry.getKey () + "]", 
           "Federation with TRUE is not currently supported");
      }
      
      classes.define (entry.getKey ()).incomingFilter = incomingFilter;
    }
    
    Map<String, ?> applyClass = 
      federationConfig.getParamOption ("Federation.Apply-Class");
    
    for (Entry<String, ?> entry : applyClass.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      for (String hostPatterns : (Set<String>)entry.getValue ())
      {
        // compatibility with Avis 1.1
        if (hostPatterns.contains ("@"))
          hostPatterns = hostPatterns.replaceAll ("@", "");
        
        classes.map (hostPatterns, fedClass);
      }
    }
    
    classes.setDefaultClass 
      (classes.define 
        (federationConfig.getString ("Federation.Default-Class")));
    
    return classes;
  }
  
  @SuppressWarnings("unchecked")
  private static void initAddAttributes (Options config,
                                         FederationClasses classes)
  {
    Map<String, ?> incoming = 
      config.getParamOption ("Federation.Add-Incoming-Attribute");
    
    for (Entry<String, ?> entry : incoming.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      fedClass.incomingAttributes = (Map<String, Object>)entry.getValue ();
    }
    
    Map<String, ?> outgoing = 
      config.getParamOption ("Federation.Add-Outgoing-Attribute");
    
    for (Entry<String, ?> entry : outgoing.entrySet ())
    {
      FederationClass fedClass = classes.define (entry.getKey ());
      
      fedClass.outgoingAttributes = (Map<String, Object>)entry.getValue ();
    }
  }
  
  private static void checkUri (String option, EwafURI uri)
  {
    if (!uri.protocol.equals (defaultProtocol ()) && 
        !uri.protocol.equals (secureProtocol ()))
    {
      throw new IllegalConfigOptionException
        (option, "Avis only supports " + defaultProtocol () + 
         " and " + secureProtocol () + " protocols: " + uri);
    }
  }
}
