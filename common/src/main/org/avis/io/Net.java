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

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;

import static java.util.Arrays.asList;

/**
 * General networking utilities.
 * 
 * @author Matthew Phillips
 */
public final class Net
{
  private Net ()
  {
    // zip
  }
  
  /**
   * Get local host name.
   * 
   * @return The canonical host name.
   * 
   * @throws IOException if no host name can be found.
   */
  public static String localHostName () 
    throws IOException
  {
    return InetAddress.getLocalHost ().getCanonicalHostName ();
    /*
     * todo select "best" host name here if InetAddress.localHostName ()
     * doesn't produce anything useful. maybe choose address with most
     * "false" values from output from DumpHostAddresses.
     * 
     * host name: hex.dsto.defence.gov.au
     * loopback: false
     * link local: false
     * multicast: false
     * site local: false
     * -------
     * host name: hex.local
     * loopback: falselink local: true
     * multicast: false
     * site local: false
     * -------
     */
//    for (Enumeration<NetworkInterface> i = 
//        NetworkInterface.getNetworkInterfaces (); i.hasMoreElements (); )
//    {
//      NetworkInterface ni = i.nextElement ();
//      
//      for (Enumeration<InetAddress> j = ni.getInetAddresses ();
//           j.hasMoreElements (); )
//      {
//        InetAddress address = j.nextElement ();
//        
//        if (!address.isLoopbackAddress () && !address.isSiteLocalAddress ())
//          return address.getCanonicalHostName ();
//      }
//    }
//    
//    throw new IOException ("Cannot determine a valid local host name");
  }
  
  /**
   * Generate a set of socket addresses for a given set of URI's. This
   * method allows interface names to be used rather than host names
   * by prefixing the host name with "!".
   * 
   * @param uris The URI's to turn into addresses.
   * 
   * @return The corresponding set of InetSocketAddress's for the URI's.
   * 
   * @throws IOException
   * @throws SocketException
   * @throws UnknownHostException
   */
  public static Set<InetSocketAddress>
    addressesFor (Set<? extends ElvinURI> uris)
      throws IOException, SocketException, UnknownHostException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    for (ElvinURI uri : uris)
      addAddressFor (addresses, uri);
    
    return addresses;
  }

  /**
   * Generate the addresses for a given URI. This method allows
   * interface names to be used rather than host names by prefixing
   * the host name with "!".
   * 
   * @param uri The URI.
   * @return The set of network addresses that correspond to the URI.
   * 
   * @throws IOException
   * @throws SocketException
   * @throws UnknownHostException
   */
  public static Set<InetSocketAddress> addressesFor (ElvinURI uri)
    throws IOException, SocketException, UnknownHostException
  {
    Set<InetSocketAddress> addresses = new HashSet<InetSocketAddress> ();
    
    addAddressFor (addresses, uri);
    
    return addresses;
  }

  private static void addAddressFor (Set<InetSocketAddress> addresses,
                                     ElvinURI uri)
    throws SocketException, IOException, UnknownHostException
  {
    Collection<InetAddress> inetAddresses;
    
    if (uri.host.startsWith ("!"))
      inetAddresses = addressesForInterface (uri.host.substring (1));
    else
      inetAddresses = addressesForHost (uri.host);
    
    for (InetAddress address : inetAddresses)
    {
      if (address.isAnyLocalAddress ())
        addresses.add (new InetSocketAddress (uri.port));
      else if (!address.isLinkLocalAddress ())
        addresses.add (new InetSocketAddress (address, uri.port));
    }
  }
  
  private static Collection<InetAddress> addressesForHost (String host)
    throws UnknownHostException
  {
    return asList (InetAddress.getAllByName (host));
  }

  private static Collection<InetAddress> addressesForInterface (String name)
    throws SocketException, IOException
  {
    NetworkInterface netInterface = NetworkInterface.getByName (name);
    
    if (netInterface == null)
    {
      throw new IOException
        ("Unknown interface name \"" + name + "\"");
    }
    
    HashSet<InetAddress> addresses = new HashSet<InetAddress> ();

    for (Enumeration<InetAddress> i = netInterface.getInetAddresses ();
         i.hasMoreElements (); )
    {
      addresses.add (i.nextElement ());
    }
    
    return addresses;
  }

  /**
   * Find the host address for an InetSocketAddress session.
   */
  public static InetAddress remoteHostAddressFor (IoSession session)
  {
    if (session.getRemoteAddress () instanceof InetSocketAddress)
    {
      return ((InetSocketAddress)session.getRemoteAddress ()).getAddress ();
    } else
    {
      throw new Error ("Can't get host name for address type " + 
                       session.getRemoteAddress ().getClass ());
    }
  }
  
  /**
   * Generate a standard host ID for a session using host name and IP.
   */
  public static String hostIdFor (IoSession session)
  {
    InetAddress address = remoteHostAddressFor (session);
    
    return address.getCanonicalHostName () + '/' + address.getHostAddress ();
  }

  /**
   * Set TCP no-delay flag for socket connections.
   * 
   * @param session The IO session.
   * @param noDelay The new setting for TCP_NODELAY.
   * 
   * @return true if the setting could be changed.
   */
  public static boolean enableTcpNoDelay (IoSession session, boolean noDelay)
  {
    if (session.getConfig () instanceof SocketSessionConfig)
    {
      ((SocketSessionConfig)session.getConfig ()).setTcpNoDelay (noDelay);
      
      return true;
    } else
    {
      return false; 
    }
  }

  /**
   * Create a new URI without the annoying checked exception.
   * 
   * @param uriString The URI string.
   * @return A new URI.
   * @throws InvalidURIException if uriString is invalid.
   */
  public static URI uri (String uriString)
    throws InvalidURIException
  {
    try
    {
      return new URI (uriString);
    } catch (URISyntaxException ex)
    {
      throw new InvalidURIException (ex);
    }
  }
}
