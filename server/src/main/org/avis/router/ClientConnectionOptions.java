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

import java.util.Map;

import org.avis.config.Options;

import static java.util.Collections.emptyMap;

import static org.avis.io.LegacyConnectionOptions.newToLegacy;
import static org.avis.router.ClientConnectionOptionSet.CLIENT_CONNECTION_OPTION_SET;

/**
 * Avis router connection options.
 * 
 * @author Matthew Phillips
 * 
 * @see ConnectionOptionSet
 */
public class ClientConnectionOptions extends Options
{
  private static final Map<String, Object> EMPTY_OPTIONS = emptyMap ();
  
  private Map<String, Object> requested;

  public ClientConnectionOptions ()
  {
    this (null, EMPTY_OPTIONS);
  }
  
  public ClientConnectionOptions (Map<String, Object> requested)
  {
    this (null, requested);
  }
  
  /**
   * Create a new instance from a set of requested values.
   * 
   * @param defaultOptions The default option values for options that
   *                are not set, usually set by the router. May be
   *                null for standard defaults.
   * @param requested The requested set of values, usually from the
   *                client creating the connection.
   */
  public ClientConnectionOptions (Options defaultOptions, 
                                  Map<String, Object> requested)
  {
    super (CLIENT_CONNECTION_OPTION_SET);
    
    if (defaultOptions != null)
      addDefaults (defaultOptions);
    
    this.requested = requested;
    
    setAll (requested);
  }
  
  /**
   * Generate the set of accepted client options.
   *
   * @see ClientConnectionOptionSet#accepted(Options, Map)
   */
  public Map<String, Object> accepted ()
  {
    return CLIENT_CONNECTION_OPTION_SET.accepted (this, requested);
  }

  /**
   * Set an option and its legacy option (if any).
   */
  public void setWithLegacy (String option, Object value)
  {
    set (option, value);
    set (newToLegacy (option), value);
  }
}
