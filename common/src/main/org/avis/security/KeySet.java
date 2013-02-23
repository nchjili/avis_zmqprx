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
 * A polymorphic key set stored as part of a {@link Keys} key
 * collection: may be either a single set of Key items or a dual set
 * for the dual key schemes. Clients should not generally need to
 * access key sets directly: use the {@link Keys} class instead.
 * 
 * @author Matthew Phillips
 */
interface KeySet
{
  public int size ();
  
  public boolean isEmpty ();
  
  public void add (KeySet keys)
    throws IllegalArgumentException;
  
  public void remove (KeySet keys)
    throws IllegalArgumentException;
  
  public boolean add (Key key)
     throws IllegalArgumentException, UnsupportedOperationException;
  
  public boolean remove (Key key)
    throws IllegalArgumentException, UnsupportedOperationException;

  /**
   * Return this key with the given set removed.
   */
  public KeySet subtract (KeySet keys);
}
