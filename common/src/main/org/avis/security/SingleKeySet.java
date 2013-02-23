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
package org.avis.security;

import java.util.HashSet;
import java.util.Set;

import static org.avis.util.Collections.difference;

/**
 * A single set of keys. Can be used directly as a java.util.Set.
 *  
 * @author Matthew Phillips
 */
class SingleKeySet extends HashSet<Key> implements KeySet, Set<Key>
{
  SingleKeySet ()
  {
    super ();
  }
  
  SingleKeySet (Set<Key> keys)
  {
    super (keys);
  }

  public void add (KeySet theKeys)
    throws IllegalArgumentException
  {
    addAll ((SingleKeySet)theKeys);
  }

  public void remove (KeySet theKeys)
  {
    removeAll ((SingleKeySet)theKeys);
  }

  public boolean remove (Key key)
  {
    return remove ((Object)key);
  }
  
  public KeySet subtract (KeySet keys)
  {
    return new SingleKeySet (difference (this, (SingleKeySet)keys));
  }
}
