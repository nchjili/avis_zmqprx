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

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.Keys;

import static org.avis.security.Keys.EMPTY_KEYS;

public class UNotify extends Notify
{
  public static final int ID = 32;
  
  public int clientMajorVersion;
  public int clientMinorVersion;
  
  public UNotify ()
  {
    // zip
  }
  
  public UNotify (int clientMajorVersion, 
                  int clientMinorVersion,
                  Map<String, Object> attributes)
  {
    this (clientMajorVersion, clientMinorVersion, attributes,
          true, EMPTY_KEYS);
  }
  
  public UNotify (int clientMajorVersion, 
                  int clientMinorVersion,
                  Map<String, Object> attributes,
                  boolean deliverInsecure,
                  Keys keys)
  {
    super (attributes, deliverInsecure, keys);
    
    this.clientMajorVersion = clientMajorVersion;
    this.clientMinorVersion = clientMinorVersion;
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
    clientMajorVersion = in.getInt ();
    clientMinorVersion = in.getInt ();
    
    super.decode (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    out.putInt (clientMajorVersion);
    out.putInt (clientMinorVersion);
    
    super.encode (out);
  }
}
