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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import static org.avis.io.XdrCoding.getString;
import static org.avis.io.XdrCoding.putString;

public class Disconn extends Message
{
  public static final int ID = 53;

  public static final int REASON_SHUTDOWN = 1;
  public static final int REASON_SHUTDOWN_REDIRECT = 2;
  public static final int REASON_PROTOCOL_VIOLATION = 4;
  
  public int reason;
  public String args;
  
  public Disconn ()
  {
    this (-1, "");
  }
  
  public Disconn (int reason)
  {
    this (reason, "");
  }
  
  public Disconn (int reason, String args)
  {
    this.reason = reason;
    this.args = args;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  public boolean hasArgs ()
  {
    return args.length () > 0;
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    reason = in.getInt ();
    args = getString (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    if (reason == -1)
      throw new ProtocolCodecException ("Reason not set");
    
    out.putInt (reason);
    putString (out, args);
  }
}
