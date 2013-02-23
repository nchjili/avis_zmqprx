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

/**
 * An enumeration of supported secure hash algorithms.
 * 
 * @author Matthew Phillips
 */
public enum SecureHash
{
  SHA1
  {
    @Override
    public byte [] hash (byte [] input)
    {
      SHA1 sha1 = new SHA1 ();
      
      sha1.engineUpdate (input, 0, input.length);
      
      return sha1.engineDigest ();
    }
  };
  
  /**
   * Perform the hash scheme on an input byte array.
   * 
   * @param input The data to hash.
   * @return The hashed result. Length depends on the hash scheme.
   */
  public abstract byte [] hash (byte [] input);
}
