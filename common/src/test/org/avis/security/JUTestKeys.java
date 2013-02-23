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

import org.apache.mina.common.ByteBuffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.security.DualKeyScheme.Subset.PRODUCER;
import static org.avis.security.KeyScheme.SHA1_CONSUMER;
import static org.avis.security.KeyScheme.SHA1_DUAL;
import static org.avis.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.security.Keys.EMPTY_KEYS;

public class JUTestKeys
{
  @Test
  public void IO ()
    throws Exception
  {
    ByteBuffer buff = ByteBuffer.allocate (1024);
    
    Keys keys = new Keys ();
    
    keys.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    keys.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    keys.add (SHA1_CONSUMER, new Key ("consumer key 2"));
    
    // test roundtrip encode/decode
    keys.encode (buff);
    
    buff.flip ();
    
    Keys newKeys = Keys.decode (buff);
    
    assertEquals (keys, newKeys);
  }
  
  @Test
  public void equality ()
  {
    assertTrue
      (new Key ("a test key number 1").equals (new Key ("a test key number 1")));
    assertFalse
      (new Key ("a test key number 1").equals (new Key ("a test key number 2")));
    
    assertEquals (new Key ("a test key number 1").hashCode (),
                  new Key ("a test key number 1").hashCode ());
    
    // test Keys.equals ()
    Keys keys1 = new Keys ();
    keys1.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys1.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    
    keys1.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys1.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    Keys keys2 = new Keys ();
    keys2.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys2.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    
    keys2.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys2.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    assertEquals (keys1.hashCode (), keys2.hashCode ());
    assertEquals (keys1, keys2);
    
    keys2.remove (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    assertFalse (keys1.equals (keys2));
  }
  
  /**
   * Test add/remove of single keys.
   */
  @Test
  public void addRemove ()
    throws Exception
  {
    Keys keys = new Keys ();
    
    keys.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    
    keys.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    DualKeySet dualKeys = keys.keysetFor (SHA1_DUAL);
    
    assertEquals (1, dualKeys.consumerKeys.size ());
    assertEquals (1, dualKeys.producerKeys.size ());
    
    keys.add (SHA1_DUAL, PRODUCER, new Key ("dual key 3"));
    assertEquals (2, keys.keysetFor (SHA1_DUAL).producerKeys.size ());
    
    keys.remove (SHA1_DUAL, PRODUCER, new Key ("dual key 3"));
    assertEquals (1, keys.keysetFor (SHA1_DUAL).producerKeys.size ());
    
    keys.remove (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    assertEquals (0, keys.keysetFor (SHA1_DUAL).producerKeys.size ());
    
    // remove a non-existent key, check nothing Bad happens
    keys.remove (SHA1_CONSUMER, new Key ("blah"));
    
    keys.remove (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    assertEquals (0, keys.keysetFor (SHA1_DUAL).consumerKeys.size ());
    
    SingleKeySet consumerKeys = keys.keysetFor (SHA1_CONSUMER);
    assertEquals (1, consumerKeys.size ());
    keys.remove (SHA1_CONSUMER, new Key ("consumer key 1"));
    assertEquals (0, consumerKeys.size ());
    
    SingleKeySet producerKeys = keys.keysetFor (SHA1_PRODUCER);
    assertEquals (1, producerKeys.size ());
    keys.remove (SHA1_PRODUCER, new Key ("producer key 1"));
    assertEquals (0, producerKeys.size ());
    
    assertTrue (keys.isEmpty ());
    
    // remove a non-existent key, check nothing Bad happens
    keys.remove (SHA1_CONSUMER, new Key ("blah"));
  }
  
  /**
   * Test add/remove of entire keysets.
   */
  @Test
  public void addRemoveSets ()
    throws Exception
  {
    Keys keys1 = new Keys ();
    Keys keys2 = new Keys ();
    Keys keys3 = new Keys ();
    
    keys1.add (SHA1_DUAL, PRODUCER, new Key ("dual key prod 1"));
    keys1.add (SHA1_DUAL, CONSUMER, new Key ("dual key cons 1"));
    keys1.add (SHA1_PRODUCER, new Key ("producer key 1.1"));
    keys1.add (SHA1_PRODUCER, new Key ("producer key 1.2"));
    
    keys2.add (SHA1_DUAL, PRODUCER, new Key ("dual key prod 2"));
    keys2.add (SHA1_DUAL, CONSUMER, new Key ("dual key cons 2"));
    keys2.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    // add keys in bulk to keys3
    keys3.add (keys1);
    
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).consumerKeys.size ());
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).producerKeys.size ());
    assertEquals (2, keys3.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (0, keys3.keysetFor (SHA1_CONSUMER).size ());
    
    keys3.add (keys2);
    assertEquals (2, keys3.keysetFor (SHA1_DUAL).consumerKeys.size ());
    assertEquals (2, keys3.keysetFor (SHA1_DUAL).producerKeys.size ());
    assertEquals (2, keys3.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (1, keys3.keysetFor (SHA1_CONSUMER).size ());
    
    keys3.remove (keys1);
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).consumerKeys.size ());
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).producerKeys.size ());
    assertEquals (0, keys3.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (1, keys3.keysetFor (SHA1_CONSUMER).size ());
    
    keys3.remove (keys2);
    
    assertTrue (keys3.isEmpty ());
  }
  
  @Test
  public void delta ()
    throws Exception
  {
    Keys addedKeys = new Keys ();
    Keys removedKeys = new Keys ();
    Keys baseKeys = new Keys ();
    
    addedKeys.add (SHA1_DUAL, PRODUCER, new Key ("added/removed key"));
    addedKeys.add (SHA1_DUAL, CONSUMER, new Key ("added key 1"));
    addedKeys.add (SHA1_PRODUCER, new Key ("added key 2"));
    
    removedKeys.add (SHA1_DUAL, PRODUCER, new Key ("added/removed key"));
    removedKeys.add (SHA1_DUAL, CONSUMER, new Key ("non existent key"));
    removedKeys.add (SHA1_DUAL, CONSUMER, new Key ("removed key"));
    
    baseKeys.add (SHA1_DUAL, PRODUCER, new Key ("kept key"));
    baseKeys.add (SHA1_DUAL, CONSUMER, new Key ("removed key"));
    
    Keys delta = baseKeys.delta (addedKeys, removedKeys);
    
    Keys correctKeys = new Keys ();
    correctKeys.add (SHA1_DUAL, CONSUMER, new Key ("added key 1"));
    correctKeys.add (SHA1_PRODUCER, new Key ("added key 2"));
    correctKeys.add (SHA1_DUAL, PRODUCER, new Key ("kept key"));
    
    assertEquals (correctKeys, delta);
    
    // check delta works with empty keys
    Keys keys4 = EMPTY_KEYS.delta (addedKeys, removedKeys);
    
    correctKeys = new Keys ();
    correctKeys.add (SHA1_DUAL, CONSUMER, new Key ("added key 1"));
    correctKeys.add (SHA1_PRODUCER, new Key ("added key 2"));
    
    assertEquals (correctKeys, keys4);
  }
  
  /**
   * Basic test of computeDelta ()
   */
  @Test
  public void computeDeltaSingleScheme ()
  {
    Keys keys1 = new Keys ();
    Keys keys2 = new Keys ();
    
    Key key1 = new Key ("key 1");
    Key key2 = new Key ("key 2");
    
    Keys.Delta delta = keys1.deltaFrom (keys2);
    
    assertEquals (0, delta.added.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (0, delta.added.keysetFor (SHA1_CONSUMER).size ());
    assertEquals (0, delta.added.keysetFor (SHA1_DUAL).size ());
    
    assertEquals (0, delta.removed.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (0, delta.removed.keysetFor (SHA1_CONSUMER).size ());
    assertEquals (0, delta.removed.keysetFor (SHA1_DUAL).size ());
    
    // add a single producer key
    keys2.add (SHA1_PRODUCER, key1);
    
    delta = keys1.deltaFrom (keys2);
    assertEquals (1, delta.added.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (0, delta.removed.keysetFor (SHA1_PRODUCER).size ());
    
    checkApplyDelta (delta, keys1, keys2);
    
    // remove a single producer key
    keys1.add (SHA1_PRODUCER, key2);
    
    delta = keys1.deltaFrom (keys2);
    assertEquals (1, delta.added.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (1, delta.removed.keysetFor (SHA1_PRODUCER).size ());
    
    checkApplyDelta (delta, keys1, keys2);
    
    // key1 is now in both sets
    keys1.add (SHA1_PRODUCER, key1);
    
    delta = keys1.deltaFrom (keys2);
    assertEquals (0, delta.added.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (1, delta.removed.keysetFor (SHA1_PRODUCER).size ());
    
    // key2 is not in both
    keys2.add (SHA1_PRODUCER, key2);
    
    delta = keys1.deltaFrom (keys2);
    assertEquals (0, delta.added.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (0, delta.removed.keysetFor (SHA1_PRODUCER).size ());
    
    checkApplyDelta (delta, keys1, keys2);
  }
  
  /**
   * Test computeDelta () with a multiple key schemes in use.
   */
  @Test
  public void computeDeltaMultiScheme ()
  {
    Keys keys1 = new Keys ();
    Keys keys2 = new Keys ();
    
    Key key1 = new Key ("key 1");
    Key key2 = new Key ("key 2");
    Key key3 = new Key ("key 3");
    
    keys1.add (SHA1_PRODUCER, key1);
    keys1.add (SHA1_CONSUMER, key2);
    keys1.add (SHA1_CONSUMER, key3);
    
    keys2.add (SHA1_PRODUCER, key3);
    keys2.add (SHA1_CONSUMER, key3);
    
    Keys.Delta delta = keys1.deltaFrom (keys2);
    assertEquals (1, delta.added.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (1, delta.removed.keysetFor (SHA1_PRODUCER).size ());
    
    assertEquals (0, delta.added.keysetFor (SHA1_CONSUMER).size ());
    assertEquals (1, delta.removed.keysetFor (SHA1_CONSUMER).size ());
    
    checkApplyDelta (delta, keys1, keys2);
  }
  
  /**
   * Test computeDelta () with a dual key set.
   */
  @Test
  public void computeDeltaDual ()
  {
    Keys keys1 = new Keys ();
    Keys keys2 = new Keys ();
    
    Key key1 = new Key ("key 1");
    Key key2 = new Key ("key 2");
    Key key3 = new Key ("key 3");
    
    keys1.add (SHA1_DUAL, PRODUCER, key1);
    keys1.add (SHA1_DUAL, CONSUMER, key2);
    keys1.add (SHA1_DUAL, CONSUMER, key3);
    
    keys2.add (SHA1_DUAL, PRODUCER, key3);
    keys2.add (SHA1_DUAL, CONSUMER, key3);
    
    Keys.Delta delta = keys1.deltaFrom (keys2);
    assertEquals (1, delta.added.keysetFor (SHA1_DUAL).producerKeys.size ());
    assertEquals (1, delta.removed.keysetFor (SHA1_DUAL).producerKeys.size ());
    
    assertEquals (0, delta.added.keysetFor (SHA1_DUAL).consumerKeys.size ());
    assertEquals (1, delta.removed.keysetFor (SHA1_DUAL).consumerKeys.size ());
    
    checkApplyDelta (delta, keys1, keys2);
  }
  
  /**
   * Check applying delta to keys1 gives keys2
   */
  private static void checkApplyDelta (Keys.Delta delta,
                                       Keys keys1, Keys keys2)
  {
    assertEquals (keys1.delta (delta.added, delta.removed), keys2);
  }
  
  @Test
  public void producer ()
  {
    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_PRODUCER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePublic);
    
    Keys eveSubKeys = new Keys ();
    eveSubKeys.add (SHA1_PRODUCER,
                    new Key ("Not alice's key").publicKeyFor (SHA1_PRODUCER));
    
    // turn alice's private keys into public (prime)
    aliceNtfnKeys.hashPrivateKeysForRole (PRODUCER);
    
    assertTrue (bobSubKeys.match (aliceNtfnKeys));
    assertFalse (eveSubKeys.match (aliceNtfnKeys));
  }
  
  @Test
  public void consumer ()
  {
    Key bobPrivate = new Key ("bob private");
    Key bobPublic = bobPrivate.publicKeyFor (SHA1_CONSUMER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_CONSUMER, bobPublic);

    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_CONSUMER, bobPrivate);
    
    Keys eveSubKeys = new Keys ();
    eveSubKeys.add (SHA1_CONSUMER, new Key ("Not bob's key"));
    
   // turn bob and eve's private keys into public (prime)
    bobSubKeys.hashPrivateKeysForRole (CONSUMER);
    eveSubKeys.hashPrivateKeysForRole (CONSUMER);
    
    assertTrue (bobSubKeys.match (aliceNtfnKeys));
    assertFalse (eveSubKeys.match (aliceNtfnKeys));
  }
  
  @Test
  public void dual ()
  {
    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_DUAL);
    Key bobPrivate = new Key ("bob private");
    Key bobPublic = bobPrivate.publicKeyFor (SHA1_DUAL);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_DUAL, CONSUMER, bobPublic);
    aliceNtfnKeys.add (SHA1_DUAL, PRODUCER, alicePrivate);

    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_DUAL, CONSUMER, bobPrivate);
    bobSubKeys.add (SHA1_DUAL, PRODUCER, alicePublic);
    
    Keys eveSubKeys = new Keys ();
    Key randomPrivate = new Key ("Not bob's key");
    eveSubKeys.add (SHA1_DUAL, CONSUMER, randomPrivate);
    eveSubKeys.add (SHA1_DUAL, PRODUCER, randomPrivate.publicKeyFor (SHA1_DUAL));
    
    // turn private keys into public (prime)
    bobSubKeys.hashPrivateKeysForRole (CONSUMER);
    eveSubKeys.hashPrivateKeysForRole (CONSUMER);
    
    aliceNtfnKeys.hashPrivateKeysForRole (PRODUCER);
    
    assertTrue (bobSubKeys.match (aliceNtfnKeys));
    assertTrue (aliceNtfnKeys.match (bobSubKeys));
    assertFalse (eveSubKeys.match (aliceNtfnKeys));
  }
}
