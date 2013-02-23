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

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.security.Keys;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import static org.avis.io.XdrCoding.getBool;
import static org.avis.io.XdrCoding.getNameValues;
import static org.avis.io.XdrCoding.putBool;
import static org.avis.io.XdrCoding.putNameValues;
import static org.avis.security.Keys.EMPTY_KEYS;

/**
 * Base class for notify messages.
 * 
 * @author Matthew Phillips
 */
public abstract class Notify extends Message
{
  public Map<String, Object> attributes;
  public boolean deliverInsecure;
  public Keys keys;
 
  protected Notify ()
  {
    this.attributes = emptyMap ();
    this.deliverInsecure = true;
    this.keys = EMPTY_KEYS;
  }
    
  protected Notify (Object... attributes)
  {
    this (asAttributes (attributes), true, EMPTY_KEYS);
  }
  
  protected Notify (Map<String, Object> attributes,
                    boolean deliverInsecure,
                    Keys keys)
  {
    this.attributes = attributes;
    this.deliverInsecure = deliverInsecure;
    this.keys = keys;
  }
  
  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    attributes = getNameValues (in);
    deliverInsecure = getBool (in);
    keys = Keys.decode (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    putNameValues (out, attributes);
    putBool (out, deliverInsecure);
    keys.encode (out);
  }
  
  public static Map<String, Object> asAttributes (Object... pairs)
  {
    if (pairs.length % 2 != 0)
      throw new IllegalArgumentException ("Items must be a set of pairs");
    
    HashMap<String, Object> map = new HashMap<String, Object> ();
    
    for (int i = 0; i < pairs.length; i += 2)
      map.put ((String)pairs [i], pairs [i + 1]);

    return unmodifiableMap (map);
  }
}
