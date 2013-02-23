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

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

import org.avis.util.Filter;

import static org.avis.logging.Log.diagnostic;

/**
 * An IO filter that blocks hosts matching a given selection filter.
 * 
 * @author Matthew Phillips
 */
public class BlacklistFilter extends IoFilterAdapter implements IoFilter
{
  private Filter<InetAddress> blacklist;

  public BlacklistFilter (Filter<InetAddress> blacklist)
  {
    this.blacklist = blacklist;
  }

  @Override
  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    InetAddress address = 
      ((InetSocketAddress)session.getRemoteAddress ()).getAddress ();
    
    if (blacklist.matches (address))
    {
      diagnostic 
        ("Refusing non-TLS connection from host " + address + 
         " due to it matching the hosts requiring authentication", this);
      
      session.close ();
    } else
    {
      nextFilter.sessionOpened (session);
    }
  }
}
