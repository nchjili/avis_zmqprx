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

import junit.framework.AssertionFailedError;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

import org.avis.io.messages.Message;

import static org.junit.Assert.fail;

import static org.avis.logging.Log.alarm;
import static org.avis.util.Text.className;

/**
 * IO handler that allows a test client to wait for an incoming
 * message.
 * 
 * @author Matthew Phillips
 */
public class TestingIoHandler
  extends IoHandlerAdapter implements IoHandler
{
  public Message message;

  @Override
  public synchronized void messageReceived (IoSession session, 
                                            Object theMessage)
    throws Exception
  {
    message = (Message)theMessage;
    
    notifyAll ();
  }

  public synchronized Message waitForMessage ()
    throws InterruptedException
  {
    if (message == null)
      wait (5000);
    
    if (message == null)
      fail ("No message received");
    
    return message;
  }

  @SuppressWarnings("unchecked")
  public synchronized <T extends Message> T waitForMessage (Class<T> type)
    throws InterruptedException
  {
    waitForMessage ();
    
    if (type.isAssignableFrom (message.getClass ()))
      return (T)message;
    else
      throw new AssertionFailedError ("Expected " + className (type) + ", was " + className (message));
  }

  public synchronized void waitForClose (IoSession session)
  {
    if (session.isClosing ())
      return;
    
    try
    {
      wait (5000);
    } catch (InterruptedException ex)
    {
      throw new Error (ex);
    }
    
    if (!session.isClosing ())
      throw new AssertionFailedError ("Session not closed");
  }
  
  @Override
  public synchronized void sessionClosed (IoSession session)
    throws Exception
  {
    notifyAll ();
  }
  
  @Override
  public void exceptionCaught (IoSession session, Throwable cause)
    throws Exception
  {
    alarm ("MINA IO exception", this, cause);
  }
}