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
package org.avis.io.messages;

import static org.avis.util.Text.shortException;

/**
 * Synthetic message used to signal protocol errors.
 * 
 * @author Matthew Phillips
 */
public class ErrorMessage extends SyntheticMessage
{
  public static final int ID = -1;
  
  public Throwable error;
  public Message cause;
  
  public ErrorMessage (Throwable error, Message cause)
  {
    this.error = error;
    this.cause = cause;
  }

  /**
   * Generate an error message suitable for presentation as a
   * debugging aid.
   */
  public String formattedMessage ()
  {
    StringBuilder message = new StringBuilder ();
    
    if (cause == null)
      message.append ("Error decoding XDR frame");
    else
      message.append ("Error decoding ").append (cause.name ());
    
    if (error != null)
      message.append (": ").append (shortException (error));
    
    return message.toString (); 
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }  
}
