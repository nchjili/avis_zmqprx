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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.DualKeyScheme.Subset;

import static org.avis.io.XdrCoding.getBytes;
import static org.avis.io.XdrCoding.putBytes;
import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;
import static org.avis.security.KeyScheme.schemeFor;
import static org.avis.security.Keys.Delta.EMPTY_DELTA;

/**
 * A key collection used to secure notifications. A key collection
 * contains zero or more mappings from a {@linkplain KeyScheme key
 * scheme} to the {@linkplain Key keys} registered for that scheme.
 * <p>
 * Once in use, key collections should be treated as immutable
 * i.e. never be modified directly after construction.
 * <p>
 * See also section 7.4 of the client protocol spec.
 * 
 * @author Matthew Phillips
 */
public class Keys
{
  /** An empty, immutable key collection. */
  public static final Keys EMPTY_KEYS = new EmptyKeys ();

  private static final DualKeySet EMPTY_DUAL_KEYSET =  new DualKeySet (true);
  private static final SingleKeySet EMPTY_SINGLE_KEYSET = new EmptySingleKeys ();
  
  /**
   * NB: this set must be kept normalised i.e. if there is a key
   * scheme in the map, then there must be a non-empty key set
   * associated with it.
   */
  private Map<KeyScheme, KeySet> keySets;

  public Keys ()
  {
    // todo opt: since schemes are static, could use a more optimized map here
    keySets = new HashMap<KeyScheme, KeySet> (4);
  }
  
   public Keys (Keys keys)
   {
     this ();
     
     add (keys);
   }
  
  /**
   * True if no keys are in the collection.
   */
  public boolean isEmpty ()
  {
    return keySets.isEmpty ();
  }
  
  /**
   * Return the total number of keys in this key collection.
   */
  public int size ()
  {
    if (isEmpty ())
      return 0;

    int size = 0;
    
    for (KeySet keyset : keySets.values ())
      size += keyset.size ();
    
    return size;
  }
  
  /**
   * Shortcut to efficiently generate a key collection that represents
   * this key collection's union with another.
   * 
   * @param keys The keys to add.
   * 
   * @return If keys is empty, this method will simply return this
   *         collection. If this collection is empty, keys will be
   *         returned. Otherwise a new collection instance is created
   *         as the union of both.
   */
  public Keys addedTo (Keys keys)
  {
    if (keys.isEmpty ())
    {
      return this;
    } else if (isEmpty ())
    {
      return keys;
    } else
    {
      Keys newKeys = new Keys (this);
      
      newKeys.add (keys);
      
      return newKeys;
    }
  }
  
  /**
   * Add a key for single key scheme.
   *  
   * @param scheme The key scheme.
   * @param key The key to add.
   * 
   * @see #remove(SingleKeyScheme, Key)
   */
  public void add (SingleKeyScheme scheme, Key key)
  {
    newKeysetFor (scheme).add (key);
  }
  
  /**
   * Remove a key for single key scheme.
   *  
   * @param scheme The key scheme.
   * @param key The key to remove.
   * 
   * @see #add(SingleKeyScheme, Key)
   */
  public void remove (SingleKeyScheme scheme, Key key)
    throws IllegalArgumentException
  {
    KeySet keys = keySets.get (scheme);

    if (keys != null)
    {
      keys.remove (key);
      
      if (keys.isEmpty ())
        keySets.remove (scheme);
    }
  }
  
  /**
   * Add a key for dual key scheme.
   *  
   * @param scheme The key scheme.
   * @param subset The key subset (PRODUCER or CONSUMER) to add the key to. 
   * @param key The key to add.
   * 
   * @see #remove(DualKeyScheme, org.avis.security.DualKeyScheme.Subset, Key)
   */
  public void add (DualKeyScheme scheme,
                   DualKeyScheme.Subset subset, Key key)
    throws IllegalArgumentException
  {
    newKeysetFor (scheme).keysFor (subset).add (key);
  }
  
  /**
   * Remove a key for dual key scheme.
   * 
   * @param scheme The key scheme.
   * @param subset The key subset (PRODUCER or CONSUMER) to remove the
   *          key from.
   * @param key The key to remove.
   * 
   * @see #add(DualKeyScheme, org.avis.security.DualKeyScheme.Subset,
   *      Key)
   */
  public void remove (DualKeyScheme scheme,
                      DualKeyScheme.Subset subset,
                      Key key)
  {
    DualKeySet keySet = (DualKeySet)keySets.get (scheme);
    
    if (keySet != null)
    {
      keySet.keysFor (subset).remove (key);
      
      if (keySet.isEmpty ())
        keySets.remove (scheme);
    }
  }

  /**
   * Add all keys in a collection.
   * 
   * @param keys The keys to add.
   * 
   * @see #remove(Keys)
   */
  public void add (Keys keys)
  {
    if (keys == this)
      throw new IllegalArgumentException
        ("Cannot add key collection to itself");
    
    for (KeyScheme scheme: keys.keySets.keySet ())
      add (scheme, keys.keySets.get (scheme));
  }
  
  private void add (KeyScheme scheme, KeySet keys)
  {
    if (!keys.isEmpty ())
      newKeysetFor (scheme).add (keys);
  }

  /**
   * Remove all keys in a collection.
   * 
   * @param keys The keys to remove.
   * 
   * @see #add(Keys)
   */
  public void remove (Keys keys)
  {
    if (keys == this)
      throw new IllegalArgumentException
        ("Cannot remove key collection from itself");
    
    for (KeyScheme scheme: keys.keySets.keySet ())
    {
      KeySet myKeys = keySets.get (scheme);
      
      if (myKeys != null)
      {
        myKeys.remove (keys.keysetFor (scheme));
        
        if (myKeys.isEmpty ())
          keySets.remove (scheme);
      }
    }
  }

  /**
   * Create a new key collection with some keys added/removed. This
   * does not modify the current collection.
   * 
   * @param toAdd Keys to add.
   * @param toRemove Keys to remove
   * 
   * @return A new key set with keys added/removed. If both add/remove
   *         key sets are empty, this returns the current instance.
   *         
   * @see #deltaFrom(Keys)
   */
  public Keys delta (Keys toAdd, Keys toRemove)
  {
    if (toAdd.isEmpty () && toRemove.isEmpty ())
    {
      return this;
    } else
    {
      Keys keys = new Keys (this);
      
      keys.add (toAdd);
      keys.remove (toRemove);
      
      return keys;
    }
  }
  
  /**
   * Compute the changes between one key collection and another.
   * 
   * @param keys The target key collection.
   * @return The delta (i.e. key sets to be added and removed)
   *         required to change this collection into the target.
   * 
   * @see #delta(Keys, Keys)
   */
  public Delta deltaFrom (Keys keys)
  {
    if (keys == this)
      return EMPTY_DELTA;
    
    Keys addedKeys = new Keys ();
    Keys removedKeys = new Keys ();
    
    for (KeyScheme scheme : KeyScheme.schemes ())
    {
      KeySet existingKeyset = keysetFor (scheme);
      KeySet otherKeyset = keys.keysetFor (scheme);
      
      addedKeys.add (scheme, otherKeyset.subtract (existingKeyset));
      removedKeys.add (scheme, existingKeyset.subtract (otherKeyset));
    }
    
    return new Delta (addedKeys, removedKeys);
  }
  
  /**
   * Get the key set for a given scheme. This set should not be
   * modified.
   * 
   * @param scheme The scheme.
   * @return The key set for the scheme. Will be empty if no keys are
   *         defined for the scheme.
   * 
   * @see #keysetFor(DualKeyScheme)
   * @see #keysetFor(SingleKeyScheme)
   */
  private KeySet keysetFor (KeyScheme scheme)
  {
    KeySet keys = keySets.get (scheme);
    
    if (keys == null)
      return scheme.isDual () ? EMPTY_DUAL_KEYSET : EMPTY_SINGLE_KEYSET;
    else
      return keys;
  }
  
  /**
   * Get the key set for a dual scheme. This set should not be
   * modified.
   * 
   * @param scheme The scheme.
   * @return The key set for the scheme. Will be empty if no keys are
   *         defined for the scheme.
   * 
   * @see #keysetFor(KeyScheme)
   * @see #keysetFor(SingleKeyScheme)
   */
  DualKeySet keysetFor (DualKeyScheme scheme)
  {
    DualKeySet keys = (DualKeySet)keySets.get (scheme);
    
    if (keys == null)
      return EMPTY_DUAL_KEYSET;
    else
      return keys;
  }
  
  /**
   * Get the key set for a single scheme. This set should not be
   * modified.
   * 
   * @param scheme The scheme.
   * @return The key set for the scheme. Will be empty if no keys are
   *         defined for the scheme.
   *         
   * @see #keysetFor(KeyScheme)
   * @see #keysetFor(DualKeyScheme)
   */
  SingleKeySet keysetFor (SingleKeyScheme scheme)
  {
    SingleKeySet keys = (SingleKeySet)keySets.get (scheme);
    
    if (keys == null)
      return EMPTY_SINGLE_KEYSET;
    else
      return keys;
  }
  
  /**
   * Lookup/create a key set for a scheme.
   */
  private KeySet newKeysetFor (KeyScheme scheme)
  {
    if (scheme.isDual ())
      return newKeysetFor ((DualKeyScheme)scheme);
    else
      return newKeysetFor ((SingleKeyScheme)scheme);
  }
  
  /**
   * Lookup/create a key set for a single key scheme.
   */
  private SingleKeySet newKeysetFor (SingleKeyScheme scheme)
  {
    SingleKeySet keys = (SingleKeySet)keySets.get (scheme);
    
    if (keys == null)
    {
      keys = new SingleKeySet ();
      
      keySets.put (scheme, keys);
    }
    
    return keys;
  }
  
  /**
   * Lookup/create a key set for a single key scheme.
   */
  private DualKeySet newKeysetFor (DualKeyScheme scheme)
  {
    DualKeySet keys = (DualKeySet)keySets.get (scheme);
    
    if (keys == null)
    {
      keys = new DualKeySet ();
      
      keySets.put (scheme, keys);
    }
    
    return keys;
  }
  
  /**
   * Turn all private keys for a given role into their public versions
   * by hashing them in place. Clients should never need to use this.
   * 
   * @param role PRODUCER (producer keys are hashed) or CONSUMER
   *          (consumer keys are hashed).
   */
  public void hashPrivateKeysForRole (Subset role)
  {
    if (isEmpty ())
      return;
    
    for (Map.Entry<KeyScheme, KeySet> entry : keySets.entrySet ())
    {
      KeyScheme scheme = entry.getKey ();
      
      if (scheme.isDual ())
      {
        hashKeys (scheme, 
                  ((DualKeySet)entry.getValue ()).keysFor (role));
      } else if (scheme.consumer && role == CONSUMER ||
                 scheme.producer && role == PRODUCER)
      {
        hashKeys (scheme, (SingleKeySet)entry.getValue ());
      }
    }
  }
  
  private void hashKeys (KeyScheme scheme, Set<Key> keys)
  {    
    if (!keys.isEmpty ())
    {
      Collection<Key> publicKeys = new ArrayList<Key> (keys.size ());
      
      for (Key key : keys)
        publicKeys.add (key.publicKeyFor (scheme));
      
      keys.clear ();
      keys.addAll (publicKeys);
    }
  }
  
  /**
   * Test whether a given key collection matches this one for the
   * purpose of notification delivery. Clients should never need to use
   * this. The keys are all assumed to be public (prime) keys.
   * 
   * @param producerKeys The producer keys to match against this
   *          (consumer) key collection.
   * @return True if a consumer using this key collection could
   *         receive a notification from a producer with the given
   *         producer key collection.
   */
  public boolean match (Keys producerKeys)
  {
    if (isEmpty () || producerKeys.isEmpty ())
      return false;
    
    for (Entry<KeyScheme, KeySet> entry : producerKeys.keySets.entrySet ())
    {
      KeyScheme scheme = entry.getKey ();
      KeySet keyset = entry.getValue ();
      
      if (keySets.containsKey (scheme) &&
          scheme.match (keyset, keySets.get (scheme)))
      {
        return true;
      }
    }
    
    return false;
  }
  
  public void encode (ByteBuffer out)
  {
    // number of key schemes in the list
    out.putInt (keySets.size ());
    
    for (Entry<KeyScheme, KeySet> entry : keySets.entrySet ())
    {
      KeyScheme scheme = entry.getKey ();
      KeySet keySet = entry.getValue ();

      // scheme ID
      out.putInt (scheme.id);
      
      if (scheme.isDual ())
      {
        DualKeySet dualKeySet = (DualKeySet)keySet;

        out.putInt (2);
        
        encodeKeys (out, dualKeySet.producerKeys);
        encodeKeys (out, dualKeySet.consumerKeys);
      } else
      {
        out.putInt (1);
        
        encodeKeys (out, (SingleKeySet)keySet);
      }
    }
  }

  public static Keys decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    int length = in.getInt ();
    
    if (length == 0)
      return EMPTY_KEYS;
    
    try
    {
      Keys keys = new Keys ();
      
      for ( ; length > 0; length--)
      {
        KeyScheme scheme = schemeFor (in.getInt ());
        int keySetCount = in.getInt ();
        
        if (scheme.isDual ())
        {
          if (keySetCount != 2)
            throw new ProtocolCodecException
              ("Dual key scheme with " + keySetCount + " key sets");
          
          DualKeySet keyset = keys.newKeysetFor ((DualKeyScheme)scheme);
          
          decodeKeys (in, keyset.producerKeys);
          decodeKeys (in, keyset.consumerKeys);
        } else
        {
          if (keySetCount != 1)
            throw new ProtocolCodecException
              ("Single key scheme with " + keySetCount + " key sets");
          
          decodeKeys (in, keys.newKeysetFor ((SingleKeyScheme)scheme));
        }
      }
      
      return keys;
    } catch (IllegalArgumentException ex)
    {
      // most likely an invalid KeyScheme ID
      throw new ProtocolCodecException (ex);
    }
  }
  
  private static void encodeKeys (ByteBuffer out, Set<Key> keys)
  {
    out.putInt (keys.size ());
    
    for (Key key : keys)
      putBytes (out, key.data);
  }
  
  private static void decodeKeys (ByteBuffer in, Set<Key> keys) 
    throws ProtocolCodecException
  {
    for (int keysetCount = in.getInt (); keysetCount > 0; keysetCount--)
      keys.add (new Key (getBytes (in)));
  }
  
  @Override
  public boolean equals (Object object)
  {
    return object instanceof Keys && equals ((Keys)object);
  }
  
  public boolean equals (Keys keys)
  {
    if (keySets.size () != keys.keySets.size ())
      return false;
    
    for (KeyScheme scheme: keys.keySets.keySet ())
    {
      if (!keysetFor (scheme).equals (keys.keysetFor (scheme)))
        return false;
    }
    
    return true;
  }
  
  @Override
  public int hashCode ()
  {
    // todo opt get a better hash code?
    int hash = 0;
    
    for (KeyScheme scheme : keySets.keySet ())
      hash ^= 1 << scheme.id;
    
    return hash;
  }

  /**
   * Represents a delta (diff) between two key sets.
   */
  public static class Delta
  {
    public static final Delta EMPTY_DELTA = new Delta (EMPTY_KEYS, EMPTY_KEYS);

    public final Keys added;
    public final Keys removed;

    Delta (Keys added, Keys removed)
    {
      this.added = added;
      this.removed = removed;
    }

    public boolean isEmpty ()
    {
      return added.isEmpty () && removed.isEmpty ();
    }
  }
  
  static class EmptySingleKeys extends SingleKeySet
  {
    @Override
    public boolean add (Key key) throws IllegalArgumentException
    {
      throw new UnsupportedOperationException ();
    }

    @Override
    public void add (KeySet keys)
      throws IllegalArgumentException, UnsupportedOperationException
    {
      throw new UnsupportedOperationException ();
    }

    @Override
    public boolean remove (Key key)
      throws IllegalArgumentException, UnsupportedOperationException
    {
      return false;
    }

    @Override
    public void remove (KeySet keys)
      throws IllegalArgumentException
    {
      // zip
    }
  }

  static class EmptyKeys extends Keys
  {
    @Override
    public void add (Keys keys)
    {
      throw new UnsupportedOperationException ();
    }

    @Override
    public void remove (Keys keys)
    {
      throw new UnsupportedOperationException ();
    }

    @Override
    public void add (SingleKeyScheme scheme, Key key)
    {
      throw new UnsupportedOperationException ();
    }

    @Override
    public void remove (SingleKeyScheme scheme, Key key)
    {
      throw new UnsupportedOperationException ();
    }

    @Override
    public void add (DualKeyScheme scheme, DualKeyScheme.Subset subset, Key key)
    {
      throw new UnsupportedOperationException ();
    }

    @Override
    public void remove (DualKeyScheme scheme, DualKeyScheme.Subset subset, Key key)
    {
      throw new UnsupportedOperationException ();
    }
  }
}
