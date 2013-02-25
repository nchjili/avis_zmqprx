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

import org.avis.config.OptionSet;
import org.avis.config.OptionTypeFromString;
import org.avis.config.OptionTypeURI;
import org.avis.io.InetAddressFilter;
import org.avis.util.Filter;

import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.io.Net.uri;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;

/**
 * The configuration options accepted by the router.
 * 
 * @author Matthew Phillips
 */
public class RouterOptionSet extends OptionSet
{
  public RouterOptionSet ()
  {
    add ("Port", 1, DEFAULT_PORT, 65535);
    add ("Listen", "elvin://0.0.0.0");
    add ("IO.Idle-Connection-Timeout", 1, 15, Integer.MAX_VALUE);
    add ("IO.Use-Direct-Buffers", false);
    add ("TLS.Keystore", new OptionTypeURI (), uri (""));
    add ("TLS.Keystore-Passphrase", "");
    add ("Require-Authenticated", 
         new OptionTypeFromString (InetAddressFilter.class), Filter.MATCH_NONE);
    add("Zmq-pub-address", "tcp://127.0.0.1:5555");
    add("Zmq-sub-address", "tcp://127.0.0.1:5556");
    add("Zmq-pub-bind",true);
    add("Zmq-sub-bind",false);
    add("Zmq-send-zmq", true);
    
    inheritFrom (CONNECTION_OPTION_SET);
  }
}
