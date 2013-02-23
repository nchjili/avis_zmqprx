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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

import org.avis.common.ElvinURI;
import org.avis.federation.FederationOptionSet;

import org.junit.Test;

import static java.net.NetworkInterface.getNetworkInterfaces;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.io.Net.addressesFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUTestRouterOptions
{
  @Test
  public void bindAll ()
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Port", "29170");
    options.set ("Listen", "elvin://0.0.0.0");
    
    Set<ElvinURI> uris = options.listenURIs ();
    Set<InetSocketAddress> addresses = addressesFor (uris);
    
    assertEquals (1, uris.size ());
    
    assertTrue
      (addresses.contains (new InetSocketAddress (options.getInt ("Port"))));
  }
  
  /**
   * Test binding to an interface name.
   */
  @Test
  public void bindInterface ()
    throws Exception
  {
    NetworkInterface netInterface = getNetworkInterfaces ().nextElement ();
    
    Set<InetAddress> interfaceAddresses = new HashSet<InetAddress> ();
    
    for (Enumeration<InetAddress> i = netInterface.getInetAddresses ();
        i.hasMoreElements (); )
    {
      InetAddress address = i.nextElement ();

      // System.out.println ("Interface address = " + address);
      if (!address.isLinkLocalAddress ())
        interfaceAddresses.add (address);        
    }
    
    testInterfaces (interfaceAddresses, netInterface.getName (), DEFAULT_PORT);
    testInterfaces (interfaceAddresses,
                    netInterface.getName () + ":29170", 29170);
  }
  
  /**
   * Test binding to a host name.
   */
  @Test
  public void bindHost ()
    throws Exception
  {
    InetAddress localhost = InetAddress.getLocalHost ();
    Set<InetAddress> localhostAddresses = new HashSet<InetAddress> ();
    
    for (InetAddress address : InetAddress.getAllByName (localhost.getHostName ()))
    {
      // System.out.println ("Host address = " + address);
      if (!address.isLinkLocalAddress ())
        localhostAddresses.add (address);
    }

    testHost (localhostAddresses, localhost.getHostName (), DEFAULT_PORT);
    testHost (localhostAddresses, localhost.getHostName () + ":29170", 29170);
  }

  /**
   * Test multiple URI's in Listen field with whitespace.
   * @throws Exception
   */
  @Test
  public void multipleURIs ()
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen",
                 "elvin:/tcp,none,xdr/localhost:1234 \t elvin://localhost");
    
    Set<InetSocketAddress> addresses = addressesFor (options.listenURIs ());
    
    boolean found1234 = false;
    
    for (InetSocketAddress address : addresses)
    {
      if (address.getPort () == 1234)
        found1234 = true;
    }
    
    assertTrue (found1234);
  }
  
  /**
   * Test that adding federator options (as router's Main does) to
   * router's works
   */
  @Test
  public void federationOptions ()
    throws Exception
  {
    RouterOptionSet routerOptionSet = new RouterOptionSet ();
    
    routerOptionSet.inheritFrom (FederationOptionSet.OPTION_SET);
    
    RouterOptions config = new RouterOptions (routerOptionSet);
    
    config.set ("Federation.Subscribe[Test]", "require (federated)");
  }

  private void testHost (Set<InetAddress> hostAddresses, String hostOption, int port)
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen", "elvin://" + hostOption);
    
    Set<InetSocketAddress> addresses = addressesFor (options.listenURIs ());
    
    assertEquals (hostAddresses.size (), addresses.size ());
    
    for (InetAddress address : hostAddresses)
      assertTrue (addresses.contains (new InetSocketAddress (address, port)));
  }

  private void testInterfaces (Set<InetAddress> interfaceAddresses,
                               String interfaceOption,
                               int port)
    throws Exception
  {
    RouterOptions options = new RouterOptions ();
    options.set ("Listen", "elvin://!" + interfaceOption);
    
    Set<InetSocketAddress> addresses = addressesFor (options.listenURIs ());
    
    assertEquals (interfaceAddresses.size (), addresses.size ());
    
    for (InetAddress address : interfaceAddresses)
      assertTrue (addresses.contains (new InetSocketAddress (address, port)));
  }
}
