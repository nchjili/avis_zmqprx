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
package org.avis.tools;

import java.util.Enumeration;

import java.io.IOException;

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * Utility to dump network addresses to console for debugging.
 * 
 * @author Matthew Phillips
 */
public class DumpHostAddresses
{
  public static void main (String [] args)
    throws IOException
  {
    System.out.println ("local host name: " + InetAddress.getLocalHost ());
    
    for (Enumeration<NetworkInterface> i = 
        NetworkInterface.getNetworkInterfaces (); i.hasMoreElements (); )
    {
      NetworkInterface ni = i.nextElement ();
      
      for (Enumeration<InetAddress> j = ni.getInetAddresses ();
           j.hasMoreElements (); )
      {
        InetAddress address = j.nextElement ();
        
        System.out.println ("-------");
        System.out.println ("host name: " + address.getCanonicalHostName ());
        System.out.println ("loopback: " + address.isLoopbackAddress ());
        System.out.println ("link local: " + address.isLinkLocalAddress ());
        System.out.println ("multicast: " + address.isMulticastAddress ());
        System.out.println ("site local: " + address.isSiteLocalAddress ());
      }
    }
  }
}
