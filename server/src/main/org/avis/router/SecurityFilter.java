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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.security.KeyStore;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.SSLFilter;

import org.avis.util.Filter;

import static org.avis.io.TLS.sslContextFor;
import static org.avis.logging.Log.diagnostic;

/**
 * A front end filter that wraps a MINA SSL filter for secure
 * connections. This allows the policy for determining which hosts
 * must be authenticated to be determined on a per host basis.
 * 
 * @author Matthew Phillips
 */
public class SecurityFilter implements IoFilter
{
  private KeyStore keystore;
  private String keystorePassphrase;
  private Filter<InetAddress> authRequired;
  private boolean clientMode;

  public SecurityFilter (KeyStore keystore,
                         String keystorePassphrase,
                         Filter<InetAddress> authRequired, 
                         boolean clientMode)
  {
    this.keystore = keystore;
    this.keystorePassphrase = keystorePassphrase;
    this.authRequired = authRequired;
    this.clientMode = clientMode;
  }
  
  private SSLFilter ssfFilterFor (IoSession session)
    throws Exception
  {
    SSLFilter filter = (SSLFilter)session.getAttribute ("securityFilterSSL");
    
    if (filter == null)
    {
      InetAddress address = 
        ((InetSocketAddress)session.getRemoteAddress ()).getAddress ();
      
      boolean needAuth = authRequired.matches (address);
      
      diagnostic ("Host " + address + " connecting via TLS " +
                  (needAuth ? "needs authentication" : 
                               "does not require authentication"), this);
      
      filter = 
        new SSLFilter (sslContextFor (keystore, keystorePassphrase, needAuth));
    
      filter.setUseClientMode (clientMode);
      filter.setNeedClientAuth (needAuth);
      
      session.setAttribute ("securityFilterSSL", filter);
    }
    
    return filter;
  }

  public void init () 
    throws Exception
  {
    // zip
  }
  
  public void destroy ()  
    throws Exception
  {
    // zip
  }

  public void sessionCreated (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    ssfFilterFor (session).sessionCreated (nextFilter, session);
  }

  public void exceptionCaught (NextFilter nextFilter,
                               IoSession session, Throwable cause)
    throws Exception
  {
    ssfFilterFor (session).exceptionCaught (nextFilter, session, cause);
  }

  public void filterClose (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    ssfFilterFor (session).filterClose (nextFilter, session);
  }

  public void filterWrite (NextFilter nextFilter, IoSession session,
                           WriteRequest writeRequest)
    throws Exception
  {
    ssfFilterFor (session).filterWrite (nextFilter, session, writeRequest);
  }

  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    ssfFilterFor (session).messageReceived (nextFilter, session, message);
  }

  public void messageSent (NextFilter nextFilter, IoSession session,
                           Object message) throws Exception
  {
    ssfFilterFor (session).messageSent (nextFilter, session, message);
  }

  public void onPostAdd (IoFilterChain parent, String name,
                         NextFilter nextFilter) throws Exception
  {
    ssfFilterFor (parent.getSession ()).onPostAdd (parent, name, nextFilter);
  }

  public void onPostRemove (IoFilterChain parent, String name,
                            NextFilter nextFilter) throws Exception
  {
    ssfFilterFor (parent.getSession ()).onPostRemove (parent, name, nextFilter);
  }

  public void onPreAdd (IoFilterChain parent, String name,
                        NextFilter nextFilter) throws Exception
  {
    ssfFilterFor (parent.getSession ()).onPreAdd (parent, name, nextFilter);
  }

  public void onPreRemove (IoFilterChain parent, String name,
                           NextFilter nextFilter) throws Exception
  {
    ssfFilterFor (parent.getSession ()).onPreRemove (parent, name, nextFilter);
  }

  public void sessionClosed (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    ssfFilterFor (session).sessionClosed (nextFilter, session);
  }

  public void sessionIdle (NextFilter nextFilter, IoSession session,
                           IdleStatus status) throws Exception
  {
    ssfFilterFor (session).sessionIdle (nextFilter, session, status);
  }

  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    ssfFilterFor (session).sessionOpened (nextFilter, session);
  }
}
