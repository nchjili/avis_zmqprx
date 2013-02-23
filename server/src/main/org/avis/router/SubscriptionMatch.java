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
package org.avis.router;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

/**
 * Represents the result of matching a subscription against a message.
 * 
 * @author Matthew Phillips
 */
class SubscriptionMatch
{
  private static final long [] EMPTY = new long [0];
  
  /** Securely matched subscription ID's */
  public final LongArrayList secure;
  /** Insecurely matched subscription ID's */
  public final LongArrayList insecure;
  
  public SubscriptionMatch ()
  {
    this.secure = new LongArrayList ();
    this.insecure = new LongArrayList ();
  }
  
  public long [] secure ()
  {
    return toArray (secure);
  }
  
  public long [] insecure ()
  {
    return toArray (insecure);
  }
  
  public boolean matched ()
  {
    return !insecure.isEmpty () || !secure.isEmpty ();
  }

  private static long [] toArray (LongList ids)
  {
    if (ids.isEmpty ())
      return EMPTY;
    else
      return ids.toLongArray ();
  }
}
