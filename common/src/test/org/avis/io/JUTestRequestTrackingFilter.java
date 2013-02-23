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

import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.RequestTimeoutMessage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class JUTestRequestTrackingFilter
{
  /**
   * Send a message with reply, check that RequestTracker generates a
   * timeout message.
   */
  @Test
  public void requestTimeout () 
    throws Exception
  {
    AcceptorConnectorSetup testSetup = new AcceptorConnectorSetup ();
    
    RequestTrackingFilter filter = new RequestTrackingFilter (2000);
    
    testSetup.connectorConfig.getFilterChain ().addLast 
      ("tracker", filter);
    
    TestingIoHandler connectListener = new TestingIoHandler ();
    
    testSetup.connect (new NullAcceptorListener (), connectListener);
    
    // send message, wait for timeout message
    connectListener.message = null;
    
    ConnRqst fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.session.write (fedConnRqst);
    
    connectListener.waitForMessage ();

    assertNotNull (connectListener.message);
    assertEquals (RequestTimeoutMessage.ID, connectListener.message.typeId ());
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    // try again, with two in the pipe
    connectListener.message = null;
    
    fedConnRqst = new ConnRqst (1, 0);
    
    testSetup.session.write (fedConnRqst);
    
    Thread.sleep (1000);
    
    testSetup.session.write (new ConnRqst (1, 0));
    
    connectListener.waitForMessage ();

    assertNotNull (connectListener.message);
    assertSame (fedConnRqst, ((RequestTimeoutMessage)connectListener.message).request);
    
    testSetup.close ();
    
    assertTrue (SharedExecutor.sharedExecutorDisposed ());
  }
  
  static class NullAcceptorListener 
    extends IoHandlerAdapter implements IoHandler
  {
    // zip
  }
}
