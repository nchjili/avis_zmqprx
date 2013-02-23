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
package org.avis.federation;

import org.avis.config.OptionSet;
import org.avis.config.OptionType;
import org.avis.config.OptionTypeFromString;
import org.avis.config.OptionTypeParam;
import org.avis.config.OptionTypeSet;
import org.avis.config.OptionTypeValueExpr;
import org.avis.io.InetAddressFilter;
import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.util.Filter;
import org.avis.util.IllegalConfigOptionException;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import static org.avis.federation.FederationClass.parse;
import static org.avis.router.ConnectionOptionSet.CONNECTION_OPTION_SET;

/**
 * Configuration option set for Avis federation.
 * 
 * @author Matthew Phillips
 */
public class FederationOptionSet extends OptionSet
{
  public static final OptionSet OPTION_SET = new FederationOptionSet ();
  
  protected FederationOptionSet ()
  {
    OptionTypeParam fedClassOption = new OptionTypeParam (new SubExpOption ());
    OptionTypeParam attrOption = 
      new OptionTypeParam (new OptionTypeValueExpr (), 2);
    OptionTypeSet setOfURI = new OptionTypeSet (EwafURI.class);
    
    add ("Federation.Activated", false);
    add ("Federation.Router-Name", "");
    add ("Federation.Subscribe", fedClassOption, emptyMap ());
    add ("Federation.Provide", fedClassOption, emptyMap ());
    add ("Federation.Apply-Class", 
         new OptionTypeParam (new OptionTypeSet (String.class)), emptyMap ());
    add ("Federation.Default-Class", "");
    add ("Federation.Connect", new OptionTypeParam (setOfURI), emptyMap ());
    add ("Federation.Listen", setOfURI, emptySet ());
    add ("Federation.Router-Name", "");
    add ("Federation.Add-Incoming-Attribute", attrOption, emptyMap ());
    add ("Federation.Add-Outgoing-Attribute", attrOption, emptyMap ());
    add ("Federation.Request-Timeout", 1, 20, Integer.MAX_VALUE);
    add ("Federation.Keepalive-Interval", 1, 60, Integer.MAX_VALUE);
    add ("Federation.Require-Authenticated", 
         new OptionTypeFromString (InetAddressFilter.class), Filter.MATCH_NONE);
    
    // allow connection options such as Packet.Max-Length
    inheritFrom (CONNECTION_OPTION_SET);
  }
  
  /**
   * A subscription expression option.
   */
  static class SubExpOption extends OptionType
  {
    @Override
    public Object convert (String option, Object value)
      throws IllegalConfigOptionException
    {
      try
      {
        if (value instanceof Node)
          return value;
        else
          return parse (value.toString ());
      } catch (ParseException ex)
      {
        throw new IllegalConfigOptionException 
          (option, "Invalid subscription: " + ex.getMessage ());
      }
    }

    @Override
    public String validate (String option, Object value)
    {
      return validateType (value, Node.class);
    }
  }
}