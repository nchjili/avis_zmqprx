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
 * A key scheme that requires a single key set. e.g. SHA-1 Consumer.
 * 
 * @author Matthew Phillips
 */
public final class SingleKeyScheme extends KeyScheme
{
  SingleKeyScheme (int id, SecureHash keyHash, 
                   boolean producer, boolean consumer)
  {
    super (id, keyHash, producer, consumer);
  }
}
