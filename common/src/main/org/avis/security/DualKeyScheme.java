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
 * A key scheme that requires a pair of keys. e.g. SHA-1 Dual.
 * 
 * @author Matthew Phillips
 */
public final class DualKeyScheme extends KeyScheme
{
  /**
   * Specifies which of the two subsets of a dual scheme a key is part
   * of: the producer subset (for sending notifications) or consumer
   * subset (for receiving notifications).
   */
  public enum Subset {PRODUCER, CONSUMER}
  
  DualKeyScheme (int id, SecureHash keyHash)
  {
    super (id, keyHash, true, true);
  }
}
