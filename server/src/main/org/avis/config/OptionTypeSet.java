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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.avis.util.IllegalConfigOptionException;

import static org.avis.util.Text.split;
import static org.avis.util.Text.stripBackslashes;

/**
 * An option that turns space-separated items in string values into a
 * set of values by using a string constructor of a type. Backslash
 * escape can be used to quote spaces.
 */
public class OptionTypeSet extends OptionType
{
  private Constructor<?> constructor;

  public OptionTypeSet (Class<?> setValueType)
  {
    try
    {
      this.constructor = setValueType.getConstructor (String.class);
    } catch (Exception ex)
    {
      throw new IllegalArgumentException ("No constructor taking a string");
    }
  }
  
  @Override
  public String validate (String option, Object value)
  {
    return validateType (value, Set.class);
  }
  
  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    try
    {
      Set<Object> values = new HashSet<Object> ();
      
      // split using regex that allows any space not preceeded by \
      for (String item : split (value.toString ().trim (), "((?<!\\\\)\\s)+"))
        values.add (constructor.newInstance (stripBackslashes (item)));
      
      return values;
    } catch (InvocationTargetException ex)
    {
      throw new IllegalConfigOptionException (option, ex.getCause ().getMessage ());
    } catch (Exception ex)
    {
      throw new IllegalConfigOptionException (option, ex.toString ());
    }
  }
}