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

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;

import org.avis.io.messages.LivenessFailureMessage;
import org.avis.io.messages.TestConn;

import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUTestLivenessFilter
{
  @Test
  public void livenessTimeout () 
    throws Exception
  {
    // Log.enableLogging (Log.TRACE, true);
    // Log.enableLogging (Log.DIAGNOSTIC, true);
    
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    LivenessFilter filter = new LivenessFilter (1000, 1000);
    
    testSetup.connectorConfig.getFilterChain ().addLast 
      ("liveness", filter);
    
    TestingIoHandler acceptorListener = new TestingIoHandler ();
    TestingIoHandler connectListener = new TestingIoHandler ();
    
    testSetup.connect (acceptorListener, connectListener);
    
    // wait for TestConn message
    acceptorListener.waitForMessage ();

    assertEquals (TestConn.ID, acceptorListener.message.typeId ());
    
    sleep (1000);
    
    // wait for LivenessTimeout
    connectListener.waitForMessage ();
    assertEquals (LivenessFailureMessage.ID, connectListener.message.typeId ());
    
    LivenessFilter.Tracker tracker = 
      LivenessFilter.trackerFor (testSetup.session);
    
    testSetup.close ();
    
    assertTrue (SharedExecutor.sharedExecutorDisposed ());
    
    // wait for tracker to be disposed on session close
    long start = currentTimeMillis ();
    
    while (currentTimeMillis () - start < 5000 && !tracker.isDisposed ())
      sleep (200);
    
    assertTrue (tracker.isDisposed ());
  }
  
  static class NullAcceptorListener 
    extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
}
