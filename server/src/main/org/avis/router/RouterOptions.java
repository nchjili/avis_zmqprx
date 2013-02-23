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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;
import org.avis.config.Options;
import org.avis.util.IllegalConfigOptionException;

import static org.avis.common.ElvinURI.defaultProtocol;
import static org.avis.common.ElvinURI.secureProtocol;
import static org.avis.util.Text.split;

/**
 * Options used to configure the Avis router.
 * 
 * @author Matthew Phillips
 */
public class RouterOptions extends Options
{
  public RouterOptions ()
  {
    this (new RouterOptionSet ());
  }
  
  public RouterOptions (RouterOptionSet optionSet)
  {
    super (optionSet);
  }

  /**
   * Shortcut to create an option set with a given "Port" setting.
   */
  public RouterOptions (int port)
  {
    this ();
    
    set ("Port", port);
  }
  
  /**
   * Shortcut to create an option set from an initial set of
   * properties.
   * 
   * @param properties The initial settings.
   * 
   * @see #setAll(Properties)
   */
  public RouterOptions (Properties properties)
  {
    this ();
    
    setAll (properties);
  }
  
  /**
   * Generate the set of URI's the server should bind to as specified
   * by the Listen setting.
   */
  public Set<ElvinURI> listenURIs ()
  {
    Set<ElvinURI> uris = new HashSet<ElvinURI> ();
    ElvinURI defaultUri = new ElvinURI ("0.0.0.0", getInt ("Port"));
    
    for (String listenItem : split (getString ("Listen"), "\\s+"))
    {
      try
      {
        ElvinURI uri = new ElvinURI (listenItem, defaultUri);
        
        if (uri.protocol.equals (defaultProtocol ()) ||
            uri.protocol.equals (secureProtocol ()))
        {
          uris.add (uri);
        } else
        {
          throw new IllegalConfigOptionException
            ("Listen",
             "Avis only supports protocols: " + 
             defaultProtocol () + " and " + secureProtocol () + 
             ": " + listenItem);
        }
      } catch (InvalidURIException ex)
      {
        throw new IllegalConfigOptionException ("Listen", ex.getMessage ());
      }
    }
    
    return uris;
  }
}