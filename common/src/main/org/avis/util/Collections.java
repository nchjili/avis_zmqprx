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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

/**
 * General collection utilities.
 * 
 * @author Matthew Phillips
 */
public final class Collections
{
  private Collections ()
  {
    // cannot instantiate
  }
  
  /**
   * Create an immutable set from a number of items.
   */
  public static <E> Set<E> set (E... items)
  {
    return unmodifiableSet (new HashSet<E> (asList (items)));
  }
  
  /**
   * Create an immutable list from a number of items.
   */
  public static <E> List<E> list (E... items)
  {
    return unmodifiableList (asList (items));
  }
  
  /**
   * Create an immutable map from a number of item pairs: even items
   * are keys, their adjacent items are values.
   */
  public static <E> Map<E, E> map (E... pairs)
  {
    if (pairs.length % 2 != 0)
      throw new IllegalArgumentException ("Items must be a set of pairs");
    
    HashMap<E, E> map = new HashMap<E, E> ();
    
    for (int i = 0; i < pairs.length; i += 2)
      map.put (pairs [i], pairs [i + 1]);

    return unmodifiableMap (map);
  }

  /**
   * Join a collection of items with a separator and append to a
   * string builder.
   */
  public static void join (StringBuilder str,
                           Iterable<?> items,
                           char separator)
  {
    boolean first = true;

    for (Object item : items)
    {
      if (!first)
        str.append (separator);
      
      first = false;
      
      str.append (item);
    }
  }

  /**
   * Compute the difference between set1 and set2. This is not
   * guaranteed to generate a new set instance.
   */
  public static <T> Set<T> difference (Set<T> set1, Set<T> set2)
  {
    if (set1.isEmpty () || set2.isEmpty ())
      return set1;

    HashSet<T> diff = new HashSet<T> ();
    
    for (T item : set1)
    {
      if (!set2.contains (item))
        diff.add (item);
    }
    
    return diff;
  }

  /**
   * Compute the union of set1 and set2. This is not guaranteed to
   * generate a new set instance: it will return set1 or set2 directly
   * if the other set is empty.
   */
  public static <T> Set<T> union (Set<T> set1, Set<T> set2)
  {
    if (set1.isEmpty ())
      return set2;
    else if (set2.isEmpty ())
      return set1;

    HashSet<T> union = new HashSet<T> ();
    
    union.addAll (set1);
    union.addAll (set2);
    
    return union;
  }

  /**
   * Compute the union of map1 and map2. This is not guaranteed to
   * generate a new map instance: it will return map1 or map2 directly
   * if the other map is empty. If there is a key/value overlap, map2
   * wins.
   */
  public static <K, V> Map<K, V> union (Map<K, V> map1, Map<K, V> map2)
  {
    if (map1.isEmpty ())
    {
      return map2;
    } else if (map2.isEmpty ())
    {
      return map1;
    } else
    {
      HashMap<K, V> union = new HashMap<K, V> ();
      
      union.putAll (map1);
      union.putAll (map2);
      
      return union;
    }
  }
}
