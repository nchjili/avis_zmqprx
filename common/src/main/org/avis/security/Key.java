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

import static org.avis.io.XdrCoding.toUTF8;

import java.util.Arrays;

/**
 * A key value used to secure notifications. A key is simply an
 * immutable block of bytes.
 * <p>
 * Elvin defines two types of key, <em>private</em> (or <em>raw</em>)
 * keys, and <em>public</em> (or <em>prime</em>) keys. A public
 * key is a one-way hash (e.g. using SHA-1) of a private key. A
 * private key may be any random data, or simply a password. A private
 * key is defined to match a public key if the corresponding hash of
 * its data matches the public key's data, e.g. if
 * <code>sha1 (privateKey.data) == publicKey.data</code>.
 * <p>
 * Note that this is not a public key system in the RSA sense but
 * that, like RSA, public keys can be shared in the open without loss
 * of security.
 * <p>
 * This class precomputes a hash code for the key data to accelerate
 * equals () and hashCode ().
 * 
 * @author Matthew Phillips
 */
public final class Key
{
  /**
   * The key's data block.
   */
  public final byte [] data;
  
  private int hash;
  
  /**
   * Create a key from a password by using the password's UTF-8
   * representation as the data block.
   */
  public Key (String password)
  {
    this (toUTF8 (password));
  }
  
  /**
   * Create a key from a block of data.
   */
  public Key (byte [] data)
  {
    if (data == null || data.length == 0)
      throw new IllegalArgumentException ("Key data cannot be empty");
    
    this.data = data;
    this.hash = Arrays.hashCode (data);
  }

  /**
   * Shortcut to generate the public (prime) key for a given scheme.
   * 
   * @see KeyScheme#publicKeyFor(Key)
   */
  public Key publicKeyFor (KeyScheme scheme)
  {
    return scheme.publicKeyFor (this);
  }

  @Override
  public boolean equals (Object object)
  {
    return object instanceof Key && equals ((Key)object);
  }
  
  public boolean equals (Key key)
  {
    return hash == key.hash && Arrays.equals (data, key.data);
  }
  
  @Override
  public int hashCode ()
  {
    return hash;
  }
}
