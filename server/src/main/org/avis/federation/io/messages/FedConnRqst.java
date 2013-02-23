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
package org.avis.federation.io.messages;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.RequestMessage;

import static org.avis.io.XdrCoding.getString;
import static org.avis.io.XdrCoding.putString;

public class FedConnRqst extends RequestMessage<FedConnRply>
{
  public static final int ID = 192;
  
  public int versionMajor;
  public int versionMinor;
  public String serverDomain;
  
  public FedConnRqst ()
  {
    // zip
  }

  public FedConnRqst (int versionMajor, int versionMinor, String serverDomain)
  {
    super (nextXid ());
    
    this.versionMajor = versionMajor;
    this.versionMinor = versionMinor;
    this.serverDomain = serverDomain;
  }

  @Override
  public Class<FedConnRply> replyType ()
  {
    return FedConnRply.class;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    out.putInt (versionMajor);
    out.putInt (versionMinor);
    putString (out, serverDomain);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    versionMajor = in.getInt ();
    versionMinor = in.getInt ();
    serverDomain = getString (in);
  }
}
