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

import org.avis.security.Keys;

import static org.avis.security.Keys.EMPTY_KEYS;

public class SecRqst extends RequestMessage<SecRply>
{
  public static final int ID = 54;
  
  public Keys addNtfnKeys;
  public Keys delNtfnKeys;
  public Keys addSubKeys;
  public Keys delSubKeys;
  
  public SecRqst ()
  {
    // make it easier for client to create and assign keys later
    this (EMPTY_KEYS, EMPTY_KEYS, EMPTY_KEYS, EMPTY_KEYS);
  }
  
  public SecRqst (Keys addNtfnKeys, Keys delNtfnKeys,
                  Keys addSubKeys, Keys delSubKeys)
  {
    super (nextXid ());
    
    this.addNtfnKeys = addNtfnKeys;
    this.delNtfnKeys = delNtfnKeys;
    this.addSubKeys = addSubKeys;
    this.delSubKeys = delSubKeys;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public Class<SecRply> replyType ()
  {
    return SecRply.class;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    addNtfnKeys.encode (out);
    delNtfnKeys.encode (out);
    addSubKeys.encode (out);
    delSubKeys.encode (out);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    addNtfnKeys = Keys.decode (in);
    delNtfnKeys = Keys.decode (in);
    addSubKeys = Keys.decode (in);
    delSubKeys = Keys.decode (in);
  }
}
