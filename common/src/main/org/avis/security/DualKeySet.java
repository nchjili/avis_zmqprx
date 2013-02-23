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

import static java.util.Collections.emptySet;

import static org.avis.util.Collections.difference;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;

/**
 * A pair of key sets (producer/consumer) used for dual key schemes.
 * 
 * @see Keys
 * @see DualKeyScheme
 * 
 * @author Matthew Phillips
 */
class DualKeySet implements KeySet
{
  public final Set<Key> producerKeys;
  public final Set<Key> consumerKeys;
  
  DualKeySet ()
  {
    this.producerKeys = new HashSet<Key> ();
    this.consumerKeys = new HashSet<Key> ();
  }
 
  /**
   * Create an immutable empty instance.
   * 
   * @param immutable Used as a flag.
   */
  DualKeySet (boolean immutable)
  {
    this.producerKeys = emptySet ();
    this.consumerKeys = emptySet ();
  }

  DualKeySet (Set<Key> producerKeys, Set<Key> consumerKeys)
  {
    this.producerKeys = producerKeys;
    this.consumerKeys = consumerKeys;
  }

  /**
   * Get the keys for a producer or consumer.
   * 
   * @param subset One of PRODUCER or CONSUMER.
   */           
  public Set<Key> keysFor (DualKeyScheme.Subset subset)
  {
    if (subset == PRODUCER)
      return producerKeys;
    else
      return consumerKeys;
  }
  
  public boolean isEmpty ()
  {
    return producerKeys.isEmpty () && consumerKeys.isEmpty ();
  }
  
  public int size ()
  {
    return producerKeys.size () + consumerKeys.size ();
  }
  
  public void add (KeySet theKeys)
    throws IllegalArgumentException
  {
    DualKeySet keys = (DualKeySet)theKeys;
    
    producerKeys.addAll (keys.producerKeys);
    consumerKeys.addAll (keys.consumerKeys);
  }
  
  public void remove (KeySet theKeys)
    throws IllegalArgumentException
  {
    DualKeySet keys = (DualKeySet)theKeys;
    
    producerKeys.removeAll (keys.producerKeys);
    consumerKeys.removeAll (keys.consumerKeys);
  }
  
  public boolean add (Key key)
    throws IllegalArgumentException, UnsupportedOperationException
  {
    throw new UnsupportedOperationException ("Cannot add to a dual key set");
  }
  
  public boolean remove (Key key)
    throws IllegalArgumentException, UnsupportedOperationException
  {
    throw new UnsupportedOperationException ("Cannot remove from a dual key set");
  }
  
  public KeySet subtract (KeySet theKeys)
  {
    DualKeySet keys = (DualKeySet)theKeys;
    
    return new DualKeySet (difference (producerKeys, keys.producerKeys),
                           difference (consumerKeys, keys.consumerKeys));
  }
  
  @Override
  public boolean equals (Object object)
  {
    return object instanceof DualKeySet && equals ((DualKeySet)object);
  }
  
  public boolean equals (DualKeySet keyset)
  {
    return producerKeys.equals (keyset.producerKeys) &&
           consumerKeys.equals (keyset.consumerKeys);
  }
  
  @Override
  public int hashCode ()
  {
    return producerKeys.hashCode () ^ consumerKeys.hashCode ();
  }
}
