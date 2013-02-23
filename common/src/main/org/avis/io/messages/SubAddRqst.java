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

import static org.avis.io.XdrCoding.getBool;
import static org.avis.io.XdrCoding.getString;
import static org.avis.io.XdrCoding.putBool;
import static org.avis.io.XdrCoding.putString;
import static org.avis.security.Keys.EMPTY_KEYS;

public class SubAddRqst extends RequestMessage<SubRply>
{
  public static final int ID = 58;
  
  public String subscriptionExpr;
  public boolean acceptInsecure;
  public Keys keys;

  public SubAddRqst ()
  {
    // zip
  }
  
  public SubAddRqst (String subExpr)
  {
    this (subExpr, EMPTY_KEYS, true);
  }
  
  public SubAddRqst (String subExpr, Keys keys, boolean acceptInsecure)
  {
    super (nextXid ());
    
    this.subscriptionExpr = subExpr;
    this.acceptInsecure = acceptInsecure;
    this.keys = keys;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }

  @Override
  public Class<SubRply> replyType ()
  {
    return SubRply.class;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    putString (out, subscriptionExpr);
    putBool (out, acceptInsecure);
    keys.encode (out);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    subscriptionExpr = getString (in);
    acceptInsecure = getBool (in);
    keys = Keys.decode (in);
  }
}
