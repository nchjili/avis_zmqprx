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
package org.avis.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.avis.util.IllegalConfigOptionException;

/**
 * A URI-valued option.
 * 
 * @author Matthew Phillips
 */
public class OptionTypeURI extends OptionType
{
  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    try
    {
      if (value instanceof URI)
        return value;
      else if (value instanceof URL)
        return ((URL)value).toURI ();
      else
        return new URI (value.toString ());
    } catch (URISyntaxException ex)
    {
      throw new IllegalConfigOptionException
        (option, "\"" + value + "\" is not a valid URI");
    }
  }

  @Override
  public String validate (String option, Object value)
  {
    return validateType (value, URI.class);
  }
}
