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

import java.util.HashSet;
import java.util.Set;

import org.avis.util.IllegalConfigOptionException;

import static java.util.Arrays.asList;

public class OptionTypeString extends OptionType
{
  public static final OptionTypeString ANY_STRING_OPTION = new OptionTypeString ();
  
  protected Set<String> validValues;

  public OptionTypeString ()
  {
    this (null);
  }

  public OptionTypeString (String defaultValue, String... validValues)
  {
    this.validValues = new HashSet<String> (asList (validValues));
    
    this.validValues.add (defaultValue);
  }
  
  public OptionTypeString (Set<String> validValues)
  {
    this.validValues = validValues;
  }
  
  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    return value.toString ();
  }
  
  @Override
  public String validate (String option, Object value)
  {
    if (value instanceof String)
    {
      if (validValues != null && !validValues.contains (value))
        return "Value must be one of: " + validValues.toString ();
      else
        return null;
    } else
    {
      return "Value must be a string";
    }
  }
}