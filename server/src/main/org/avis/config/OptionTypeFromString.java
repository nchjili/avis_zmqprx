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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.avis.util.IllegalConfigOptionException;

/**
 * An option type that uses the string constructor of a value type to
 * convert strings to values of that type. This can be used for any
 * option whose values can be converted from strings.
 * 
 * @author Matthew Phillips
 */
public class OptionTypeFromString extends OptionType
{
  private Class<?> valueType;
  private Constructor<?> constructor;

  public OptionTypeFromString (Class<?> valueType)
  {
    this.valueType = valueType;
    
    try
    {
      this.constructor = valueType.getConstructor (String.class);
    } catch (Exception ex)
    {
      throw new IllegalArgumentException ("No constructor taking a string");
    }
  }
    
  @Override
  public String validate (String option, Object value)
  {
    return validateType (value, valueType);
  }

  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    if (valueType.isAssignableFrom (value.getClass ()))
      return value;
    
    try
    {
      return constructor.newInstance (value.toString ());
    } catch (InvocationTargetException ex)
    {
      throw new IllegalConfigOptionException (option, 
                                              ex.getCause ().getMessage ());
    } catch (Exception ex)
    {
      throw new IllegalConfigOptionException (option, ex.toString ());
    }
  }
}
