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

import org.avis.io.messages.Nack;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JUTestNack
{
  @Test
  public void formattedMessage ()
  {
    Nack nack = new Nack ();
    
    nack.args = new Object [] {"foo", "bar"};
    nack.message = "There was a %1 in the %2 (%1, %2) %3 %";
    
    String formattedMessage = nack.formattedMessage ();
    
    assertEquals ("There was a foo in the bar (foo, bar) %3 %",
                  formattedMessage);
  }
}
