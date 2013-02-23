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

import java.net.InetSocketAddress;
import java.net.URI;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.common.ExceptionMonitor;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.SSLFilter;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;

import org.junit.Before;
import org.junit.Test;

import org.avis.io.ClientFrameCodec;
import org.avis.io.ExceptionMonitorLogger;

public class JUTestRouterTLS
{
  private static final int DEFAULT_PORT = 29170;
  private static final int SECURE_PORT = 29171;

  @Before
  public void setup ()
  {
    // route MINA exceptions to log
    ExceptionMonitor.setInstance (ExceptionMonitorLogger.INSTANCE);
  }
  
  /*
   * Command to generate the test key store:
   *   keytool -genkey -alias test -keysize 512 -validity 3650 -keyalg RSA \
   *     -dname "CN=test.com, OU=test, O=Test Inc, L=Adelaide, \
   *     S=South Australia, C=AU" -keypass testing -storepass testing \
   *     -keystore tls_test.ks
   */
  @Test
  public void connect () 
    throws Exception
  {
    ExceptionMonitor.setInstance (ExceptionMonitorLogger.INSTANCE);
    
    RouterOptions options = new RouterOptions ();
    URI keystore = getClass ().getResource ("tls_test.ks").toURI ();
    
    options.set ("Listen", 
                 "elvin://127.0.0.1:" + DEFAULT_PORT + " " + 
                 "elvin:/secure/127.0.0.1:" + SECURE_PORT);
    options.set ("TLS.Keystore", keystore);
    options.set ("TLS.Keystore-Passphrase", "testing");
   
    Router router = new Router (options);
    
    SimpleClient standardClient = 
      new SimpleClient (new InetSocketAddress ("127.0.0.1", DEFAULT_PORT), 
                        createStandardConfig ());
    
    standardClient.connect ();
    
    standardClient.close ();
    
    SimpleClient secureClient = 
      new SimpleClient (new InetSocketAddress ("127.0.0.1", SECURE_PORT), 
                        createTLSConfig ());
    
    secureClient.connect ();
    
    secureClient.close ();
    
    router.close ();
  }

  private static SocketConnectorConfig createTLSConfig () 
    throws Exception
  {
    SocketConnectorConfig connectorConfig = createStandardConfig ();
    
    SSLContext sslContext = SSLContext.getInstance ("TLS");
    sslContext.init (null, new TrustManager [] {ACCEPT_ALL_MANAGER}, null);
    
    SSLFilter sslFilter = new SSLFilter (sslContext);
    sslFilter.setUseClientMode (true);
    
    connectorConfig.getFilterChain ().addFirst  ("ssl", sslFilter);
                                
    return connectorConfig;
  }
  
  private static SocketConnectorConfig createStandardConfig () 
    throws Exception
  {
    SocketConnectorConfig connectorConfig = new SocketConnectorConfig ();
    connectorConfig.setThreadModel (ThreadModel.MANUAL);
    connectorConfig.setConnectTimeout (10);
    
    connectorConfig.getFilterChain ().addLast 
      ("codec", ClientFrameCodec.FILTER);
    
    return connectorConfig;
  }
  
  static final X509TrustManager ACCEPT_ALL_MANAGER = new X509TrustManager ()
  {
    public void checkClientTrusted (X509Certificate [] x509Certificates,
                                    String s)
      throws CertificateException
    {
      // zip: allow anything
    }

    public void checkServerTrusted (X509Certificate [] x509Certificates,
                                    String s)
      throws CertificateException
    {
      // zip: allow anything
    }

    public X509Certificate [] getAcceptedIssuers ()
    {
      return new X509Certificate [0];
    }
  };
}
