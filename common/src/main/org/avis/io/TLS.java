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

import java.io.IOException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Utilities for TLS/SSL.
 * 
 * @author Matthew Phillips
 */
public final class TLS
{
  private TLS ()
  {
    // zip
  }

  /**
   * Generate an array of characters for a passphrase, or null if
   * passphrase is empty (i.e. allow empty string to stand for no
   * passphrase).
   */
  public static char [] toPassphrase (String passphrase)
  {
    return passphrase.length () > 0 ? passphrase.toCharArray () : null;
  }
  
  /**
   * Create a default SSL context with the default keystore and trust
   * chain.
   * 
   * @return A new SSL context.
   * 
   * @throws IOException if an error occurred initialising TLS.
   */
  public static SSLContext defaultSSLContext () 
    throws IOException
  {
    return sslContextFor (null, null, false);
  }
  
  /**
   * Create a new SSL context.
   * 
   * @param keystore The keystore to use for keys and trusted
   *                certificates. This may be null to use the default
   *                store.
   * @param keystorePassphrase The passphrase for the keystore. Null
   *                if no keystore.
   * @param requireTrustedServer True if only servers that are in the
   *                trusted certificate chain are acceptable. Has no
   *                effect when used in server mode.
   * 
   * @return The SSL context.
   * 
   * @throws IllegalArgumentException if a keystore is specified and
   *                 no passphrase is set.
   * @throws IOException if an error occurred initialising TLS.
   */
  public static SSLContext sslContextFor (KeyStore keystore, 
                                          String keystorePassphrase,
                                          boolean requireTrustedServer) 
    throws IOException, IllegalArgumentException
  {
    if (keystore != null && keystorePassphrase == null)
    {
      throw new IllegalArgumentException 
        ("Passphrase must be set when using a keystore");
    }
    
    try
    {
      KeyManager [] keyManagers = null;
      
      if (keystore != null)
      {
        KeyManagerFactory keyFactory = 
          KeyManagerFactory.getInstance ("SunX509");
        
        keyFactory.init (keystore, toPassphrase (keystorePassphrase));
        
        keyManagers = keyFactory.getKeyManagers ();
      }
      
      CustomTrustManager trustManager = 
        new CustomTrustManager (keystore, requireTrustedServer, false);
      
      SSLContext sslContext = SSLContext.getInstance ("TLS");
 
      sslContext.init (keyManagers, new TrustManager [] {trustManager}, null);

      return sslContext;
    } catch (Exception ex)
    {
      throw new IOException ("Error initialising TLS: " + ex);
    }
  }
  
  /**
   * An X.509 trust manager that allows optional client/server trust
   * requirements. When trust is required, it falls back on the
   * platform's default trust manager.
   * 
   * See also
   * http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html
   * #X509TrustManager
   * 
   * @author Matthew Phillips
   */
  private static class CustomTrustManager implements X509TrustManager
  {
    /*
     * The default X509TrustManager returned by SunX509. We'll delegate
     * decisions to it, and fall back to the logic in this class if the
     * default X509TrustManager doesn't trust it.
     */
    private X509TrustManager sunX509TrustManager;
    private boolean requireTrustedServer;
    private boolean requireTrustedClient;

    public CustomTrustManager (KeyStore keystore, 
                               boolean requireTrustedServer,
                               boolean requireTrustedClient) 
      throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException
    {
      this.requireTrustedServer = requireTrustedServer;
      this.requireTrustedClient = requireTrustedClient;
      this.sunX509TrustManager = initTrustManager (keystore);
    }

    private X509TrustManager initTrustManager (KeyStore keystore)
      throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException
    {
      TrustManagerFactory sunManagerFactories = 
        TrustManagerFactory.getInstance ("SunX509", "SunJSSE");
      
      sunManagerFactories.init (keystore);

      /*
       * Iterate over the returned trustmanagers, look for an instance
       * of X509TrustManager. If found, use that as our "default" trust
       * manager.
       */
      for (TrustManager trustManager : sunManagerFactories.getTrustManagers ())
      {
        if (trustManager instanceof X509TrustManager)
          return (X509TrustManager)trustManager;
      }
      
      throw new NoSuchProviderException ("No default X509 trust manager");
    }

    /*
     * Delegate to the default trust manager.
     */
    public void checkClientTrusted (X509Certificate [] chain,
                                    String authType)
      throws CertificateException
    {
      if (requireTrustedClient)
        sunX509TrustManager.checkClientTrusted (chain, authType);
    }

    /*
     * Delegate to the default trust manager.
     */
    public void checkServerTrusted (X509Certificate [] chain,
                                    String authType)
      throws CertificateException
    {
      if (requireTrustedServer)
        sunX509TrustManager.checkServerTrusted (chain, authType);
    }

    /*
     * Merely pass this through.
     */
    public X509Certificate [] getAcceptedIssuers ()
    {
      return sunX509TrustManager.getAcceptedIssuers ();
    }
  }
}
