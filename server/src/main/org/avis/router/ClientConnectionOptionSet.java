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

import java.util.HashMap;
import java.util.Map;

import org.avis.config.Options;
import org.avis.util.IllegalConfigOptionException;

import static org.avis.io.LegacyConnectionOptions.legacyToNew;

/**
 * Extends the Avis connection option set to be used against the
 * options sent by router clients. Adds code to support legacy
 * compatibility and make value checking stricter.
 * 
 * @author Matthew Phillips
 */
public class ClientConnectionOptionSet extends ConnectionOptionSet
{
  public static final ClientConnectionOptionSet CLIENT_CONNECTION_OPTION_SET =
    new ClientConnectionOptionSet ();
  
  /**
   * Generate the set of accepted connection options for reporting to
   * a client. Handles removal of invalid options, echoing of actual
   * defaults for values out of range and legacy backward
   * compatibility.
   * 
   * @param connectionOptions The connection option set.
   * @param requestedOptions The original requested set of options.
   * 
   * @return The accepted set suitable for reporting to client as per
   *         the client connection option spec.
   */
  public Map<String, Object> accepted (Options connectionOptions,
                                       Map<String, Object> requestedOptions)
  {
    HashMap<String, Object> accepted = new HashMap<String, Object> ();
    
    for (String requestedOption : requestedOptions.keySet ())
    {
      String option = legacyToNew (requestedOption);
      
      if (isDefined (option))
      {
        Object value = connectionOptions.peek (option);

        if (value == null)
          value = defaults.get (option);

        /*
         * Special handling for old router.coalesce-delay, which has the
         * opposite meaning to its replacement, TCP.Send-Immediately.
         */
        if (requestedOption.equals ("router.coalesce-delay"))
        {
          if (value.equals (0))
            value = 1;
          else if (value.equals (1))
            value = 0;
        }
        
        accepted.put (requestedOption, value);
      }
    }
    
    return accepted;
  }

  /**
   * Override validation to add legacy support and to simply not
   * include invalid options rather than explode violently. Also
   * removes auto value conversion.
   */
  @Override
  protected void validateAndSet (Options options,
                                 String option, Object value)
    throws IllegalConfigOptionException
  {
    /*
     * Special handling for old router.coalesce-delay, which has the
     * opposite meaning to its replacement, TCP.Send-Immediately.
     */
    if (option.equals ("router.coalesce-delay"))
    {
      if (value.equals (0))
        value = 1;
      else if (value.equals (1))
        value = 0;
    }
    
    option = legacyToNew (option);
    
    // validate and set, or simply discard value
    if (validate (option, value) == null)
      set (options, option, value);
  }
}
