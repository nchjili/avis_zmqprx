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
 * General utility functions for messing with numbers.
 * 
 * @author Matthew Phillips
 */
public final class Numbers
{
  private Numbers ()
  {
    // cannot instantiate
  }
  
  /**
   * Convert a numeric value upwards to a given type.
   * 
   * @param value A numeric value.
   * @param type The target type: either Long or Double.
   * 
   * @return value upconverted to the target type.
   * 
   * @throws IllegalArgumentException if type is not valid.
   */
  public static Number upconvert (Number value, Class<? extends Number> type)
  {
    if (type == Long.class)
      return value.longValue ();
    else if (type == Double.class)
      return value.doubleValue ();
    else
      throw new IllegalArgumentException ("Cannot upconvert to " + type);
  }

  /**
   * Return the highest precision (class with the largest range) of
   * two classes.
   * 
   * @throws IllegalArgumentException if class1 or class2 is not a number.
   */
  public static Class<? extends Number>
    highestPrecision (Class<? extends Number> class1,
                      Class<? extends Number> class2)
  {
    if (precision (class1) >= precision (class2))
      return class1;
    else
      return class2;
  }

  private static int precision (Class<? extends Number> type)
  {
    if (type == Integer.class)
      return 0;
    else if (type == Long.class)
      return 1;
    else if (type == Double.class)
      return 2;
    else
      throw new IllegalArgumentException ("Unknown number type " + type);
  }
}
