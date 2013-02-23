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

import java.net.InetAddress;

import org.avis.util.Filter;
import org.avis.util.WildcardFilter;

import static java.util.Arrays.asList;

import static org.avis.util.Text.split;

/**
 * A filter for internet addresses, matching against either the host
 * name or IP address.
 * 
 * @author Matthew Phillips
 */
public class InetAddressFilter implements Filter<InetAddress>
{
  private WildcardFilter filter;

  public InetAddressFilter (String patterns)
  {
    this.filter = new WildcardFilter (asList (split (patterns)));
  }
  
  public boolean matches (InetAddress address)
  {
    return filter.matches (address.getHostName ()) ||
           filter.matches (address.getHostAddress ()) ||
           filter.matches (address.getCanonicalHostName ());
  }
}
