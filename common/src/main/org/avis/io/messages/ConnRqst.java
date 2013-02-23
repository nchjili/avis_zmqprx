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

import static java.util.Collections.emptyMap;

import static org.avis.io.XdrCoding.getNameValues;
import static org.avis.io.XdrCoding.putNameValues;
import static org.avis.security.Keys.EMPTY_KEYS;

/**
 * 
 * @author Matthew Phillips
 */
public class ConnRqst extends RequestMessage<ConnRply>
{
  public static final Map<String, Object> EMPTY_OPTIONS = emptyMap ();

  public static final int ID = 49;
  
  public int versionMajor;
  public int versionMinor;
  public Map<String, Object> options;
  public Keys notificationKeys;
  public Keys subscriptionKeys;

  public ConnRqst ()
  {
    // zip
  }
  
  public ConnRqst (int major, int minor)
  {
    this (major, minor, EMPTY_OPTIONS, EMPTY_KEYS, EMPTY_KEYS);
  }

  public ConnRqst (int major, int minor, Map<String, Object> options,
                   Keys notificationKeys, Keys subscriptionKeys)
  {
    super (nextXid ());
    
    this.versionMajor = major;
    this.versionMinor = minor;
    this.options = options;
    this.notificationKeys = notificationKeys;
    this.subscriptionKeys = subscriptionKeys;
  }
  
  @Override
  public int typeId ()
  {
    return ID;
  }
  
  @Override
  public Class<ConnRply> replyType ()
  {
    return ConnRply.class;
  }
  
  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    out.putInt (versionMajor);
    out.putInt (versionMinor);
    
    putNameValues (out, options);

    notificationKeys.encode (out);
    subscriptionKeys.encode (out);
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    super.decode (in);
    
    versionMajor = in.getInt ();
    versionMinor = in.getInt ();
    
    options = getNameValues (in);
    
    notificationKeys = Keys.decode (in);
    subscriptionKeys = Keys.decode (in);
  }
}
