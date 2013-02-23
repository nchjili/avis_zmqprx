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
package org.avis.federation;

import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static junit.framework.Assert.fail;

public class TestUtils
{
  public static final long MAX_WAIT = 8000;
  
  /**
   * Wait up to 8 seconds for a connector to be in connected state.
   */
  public static void waitForConnect (Connector connector)
    throws InterruptedException
  {
    long finish = currentTimeMillis () + MAX_WAIT;
    
    while (currentTimeMillis () < finish && !connector.isConnected ())
      sleep (200);
    
    if (!connector.isConnected ())
      fail ("Failed to connect");
  }

  /**
   * Wait up to 8 seconds for a connector to go to waiting for an
   * async connect.
   */
  public static void waitForAsyncConnect (Connector connector)
    throws InterruptedException
  {
    long finish = currentTimeMillis () + MAX_WAIT;
    
    while (currentTimeMillis () < finish && 
           !connector.isWaitingForAsyncConnection ())
    {
      sleep (200);
    }
    
    if (!connector.isWaitingForAsyncConnection ())
      fail ("Failed to go to async connect state");
  }

  public static void waitForSingleLink (final Acceptor acceptor) 
    throws InterruptedException
  {
    waitUpto (MAX_WAIT, MILLISECONDS, new Predicate ()
    {
      public boolean satisfied ()
      {
        return acceptor.links.size () == 1;
      }
    });
  }
  
  public static void waitUpto (long duration, TimeUnit unit,
                               Predicate predicate) 
    throws InterruptedException
  {
    long finishAt = currentTimeMillis () + unit.toMillis (duration);
    
    while (!predicate.satisfied () && currentTimeMillis () < finishAt)
      sleep (10);
    
    if (!predicate.satisfied ())
      fail ();
  }
}
