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

/**
 * Placeholder for QnchAddRqst, QnchModRqst and QnchDelRqst that
 * allows them to be decoded and sent to server. Server will currently
 * NACK.
 * 
 * @author Matthew Phillips
 */
public class QuenchPlaceHolder extends XidMessage
{
  public static final int ID = -2;
  
  public static final int ADD = 80;
  public static final int MODIFY = 81;
  public static final int DELETE = 82;
  
  public int messageType;
  public int length;

  public QuenchPlaceHolder (int messageType, int length)
  {
    this.messageType = messageType;
    this.length = length;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    in.skip (length - 4);
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    throw new UnsupportedOperationException
      ("This is just a quench placeholder for now");
  }
}
