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
package org.avis.io.messages;

import java.util.Map;

import org.avis.security.Keys;

import static org.avis.security.Keys.EMPTY_KEYS;

public class NotifyEmit extends Notify
{
  public static final int ID = 56;

  public NotifyEmit ()
  {
    super ();
  }
  
  public NotifyEmit (Object... attributes)
  {
    super (attributes);
  }

  public NotifyEmit (Map<String, Object> attributes)
  {
    this (attributes, true, EMPTY_KEYS);
  }
  
  public NotifyEmit (Map<String, Object> attributes,
                     boolean deliverInsecure,
                     Keys keys)
  {
    super (attributes, deliverInsecure, keys);
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
}
