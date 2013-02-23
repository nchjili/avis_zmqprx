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
package org.avis.util;

/**
 * General Avis utility functions.
 * 
 * @author Matthew Phillips
 */
public final class Util
{
  private Util ()
  {
    // zip
  }

  /**
   * Test if two objects are equal, handling null values and type differences. 
   */
  public static boolean valuesEqual (Object value1, Object value2)
  {
    if (value1 == value2)
      return true;
    else if (value1 == null || value2 == null)
      return false;
    else if (value1.getClass () == value2.getClass ())
      return value1.equals (value2);
    else
      return false;
  }

  /**
   * Check a value is non-null or throw an IllegalArgumentException.
   * 
   * @param value The value to test.
   * @param name The name of the value to be used in the exception.
   */
  public static void checkNotNull (Object value, String name)
    throws IllegalArgumentException
  {
    if (value == null)
      throw new IllegalArgumentException (name + " cannot be null");
  }
}
