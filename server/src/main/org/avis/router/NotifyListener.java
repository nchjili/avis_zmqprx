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

import org.avis.io.messages.Notify;
import org.avis.security.Keys;

/**
 * Interface for listeners to router Notify messages.
 * 
 * @author Matthew Phillips
 */
public interface NotifyListener
{
  /**
   * Invoked when the router has received a Notify message for
   * delivery.
   * 
   * @param message The message.
   * @param keys The global notification keys that apply to the message.
   */
  public void notifyReceived (Notify message, Keys keys);
}
