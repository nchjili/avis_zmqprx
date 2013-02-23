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

import java.util.Timer;

import org.apache.mina.common.IoSession;

public abstract class AgentClient extends SimpleClient
{
  public volatile int sentMessageCount;
  public volatile int receivedMessageCount;
  
  protected Timer timer;

  public AgentClient (String name, String host, int port)
    throws Exception
  {
    super (name, host, port);
    
    timer = new Timer ();

    sentMessageCount = 0;
    receivedMessageCount = 0;

    connect ();
  }
  
  public String report ()
  {
    return clientName +
           " sent " + sentMessageCount + 
           ", received " + receivedMessageCount;
  }
  
  @Override
  public void messageReceived (IoSession session, Object message)
    throws Exception
  {
    receivedMessageCount++;

    super.messageReceived (session, message);
  }
}
