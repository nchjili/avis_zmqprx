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
import org.avis.util.InvalidFormatException;
import org.avis.util.Text;

import static org.avis.util.Text.stringToValue;

/**
 * An option that uses {@link Text#stringToValue(String)} to convert
 * its value.
 * 
 * @author Matthew Phillips
 */
public class OptionTypeValueExpr extends OptionType
{
  @Override
  public String validate (String option, Object value)
  {
    return null;
  }

  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    try
    {
      return stringToValue (value.toString ());
    } catch (InvalidFormatException ex)
    {
      throw new IllegalConfigOptionException (option, ex.getMessage ());
    }
  }
}
