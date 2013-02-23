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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import static org.avis.logging.Log.alarm;

public class GoodClient extends AgentClient
{
  public GoodClient (String name, String host, int port)
    throws Exception
  {
    super (name, host, port);

    subscribe ("Channel == 'goodclient'");
  }
  
  public void startSending ()
  {
    timer.schedule (new SendTask (), 0, 20);
  }

  public void stopSending ()
  {
    timer.cancel ();
  }
  
  class SendTask extends TimerTask
  {
    @Override
    public void run ()
    {
      try
      {
        Map<String, Object> ntfn = new HashMap<String, Object> ();
        
        ntfn.put ("From", getClass ().getName ());
        ntfn.put ("Time", new Date ().toString ());
        ntfn.put ("Channel", "goodclient");
        
        sendNotify (ntfn);
        
        sentMessageCount++;
      } catch (Exception ex)
      {
        alarm ("Failed to send notification", this, ex);
      }
    }
  }
}
