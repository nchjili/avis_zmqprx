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

import static org.avis.util.Text.className;

import org.avis.util.IllegalConfigOptionException;


/**
 * Defines a type of option in an OptionSet.
 * 
 * @author Matthew Phillips
 */
public abstract class OptionType
{
  /**
   * Attempt to convert a value to be compatible with this option type.
   *  
   * @param option The option.
   * @param value The value.
   * @return The converted value or just value if none needed.
   * 
   * @throws IllegalConfigOptionException if value is not convertible.
   */
  public abstract Object convert (String option, Object value)
    throws IllegalConfigOptionException;
  
  /**
   * Check that a value is valid for this option.
   * 
   * @param option The option.
   * @param value The value.
   * 
   * @return Error text if not valid, null if OK.
   */
  public abstract String validate (String option, Object value);

  /**
   * Utility for validate () to call if it just needs a given type.
   * 
   * @param value The value.
   * @param type The required type.
   * 
   * @return The validation response.
   */
  protected String validateType (Object value, Class<?> type)
  {
    return validateType (value, type, className (type));
  }
  
  /**
   * Utility for validate () to call if it just needs a given type.
   * 
   * @param value The value.
   * @param type The required type.
   * @param typeName A readable name for the type.
   * 
   * @return The validation response.
   */
  protected String validateType (Object value, Class<?> type,
                                  String typeName)
  {
    return type.isAssignableFrom (value.getClass ()) ? null : 
             "Value must be a " + typeName + ": " + className (value);
  }
}
