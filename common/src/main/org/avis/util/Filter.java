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
 * A filter that selects values of the generic type T.
 * 
 * @author Matthew Phillips
 */
public interface Filter<T>
{
  /**
   * Matches nothing.
   */
  public static final Filter<?> MATCH_NONE = new Filter<Object> ()
  {
    public boolean matches (Object value)
    {
      return false;
    }
  };
  
  /**
   * Matches everything.
   */
  public static final Filter<?> MATCH_ALL = new Filter<Object> ()
  {
    public boolean matches (Object value)
    {
      return true;
    }
  };

  /**
   * Test if the filter matches.
   * 
   * @param value The value to test.
   * 
   * @return True if the fiLter matches.
   */
  public boolean matches (T value);
}
