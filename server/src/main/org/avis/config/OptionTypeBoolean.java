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


public class OptionTypeBoolean extends OptionType
{
  public static final OptionTypeBoolean INSTANCE = new OptionTypeBoolean ();

  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    if (value instanceof Boolean)
      return value;
    
    String v = value.toString ().trim ().toLowerCase ();
    
    if (v.equals ("1") || v.equals ("true") || v.equals ("yes"))
      return true;
    else if (v.equals ("0") || v.equals ("false") || v.equals ("no"))
      return false;
    else
      throw new IllegalConfigOptionException
        (option, "\"" + value + "\" is not a valid true/false boolean");
  }
  
  @Override
  public String validate (String option, Object value)
  {
    return value instanceof Boolean ? null : "Value must be true/false";
  }
}