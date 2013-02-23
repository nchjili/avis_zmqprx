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
package org.avis.io;

import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * Manages a single shared ScheduledExecutorService.
 * 
 * @author Matthew Phillips
 */
public final class SharedExecutor
{
  protected static int shareCount = 0;
  protected static ScheduledExecutorService sharedExecutor = null;
  
  private SharedExecutor ()
  {
    // zip
  }
  
  /**
   * Acquire a reference to the shared executor.
   */
  public static ScheduledExecutorService acquire ()
  {
    synchronized (SharedExecutor.class)
    {
      if (shareCount++ == 0)
        sharedExecutor = newScheduledThreadPool (1);

      return sharedExecutor;
    }
  }

  /**
   * Release the shared exectutor.
   * 
   * @param executor The executor. If this is not the shared instance,
   *                nothing is done.
   */
  public static boolean release (ScheduledExecutorService executor)
  {
    synchronized (SharedExecutor.class)
    {
      if (executor == sharedExecutor)
      {
        if (shareCount == 0)
          throw new IllegalStateException ("Too many release calls");

        if (--shareCount == 0)
        {
          sharedExecutor.shutdown ();
          sharedExecutor = null;
        }
     
        return true;
      }
    }
    
    return false;
  }

  public static boolean sharedExecutorDisposed ()
  {
    synchronized (SharedExecutor.class)
    {
      return shareCount == 0 && sharedExecutor == null;
    }
  }
}
