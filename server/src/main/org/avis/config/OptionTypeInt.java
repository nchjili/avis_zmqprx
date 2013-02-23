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

import org.avis.util.IllegalConfigOptionException;


public class OptionTypeInt extends OptionType
{
  protected int min;
  protected int max;

  public OptionTypeInt (int min, int max)
  {
    this.min = min;
    this.max = max;
  }
  
  @Override
  public Object convert (String option, Object value)
  {
    if (value instanceof Integer)
      return value;
    
    try
    {
      String text = value.toString ().toLowerCase ();
      int unit = 1;
      
      if (text.endsWith ("m"))
      {
        unit = 1024*1024;
        text = text.substring (0, text.length () - 1);
      } else if (text.endsWith ("k"))
      {
        unit = 1024;
        text = text.substring (0, text.length () - 1);
      }
      
      return Integer.parseInt (text) * unit;
    } catch (NumberFormatException ex)
    {
      throw new IllegalConfigOptionException
        (option, "\"" + value + "\" is not a valid integer");
    }
  }
  
  @Override
  public String validate (String option, Object value)
  {
    if (value instanceof Integer)
    {
      int intValue = (Integer)value;
      
      if (intValue >= min && intValue <= max)
        return null;
      else
        return "Value must be in range " + min + ".." + max;
    } else
    {
      return "Value is not an integer";
    }
  }
}