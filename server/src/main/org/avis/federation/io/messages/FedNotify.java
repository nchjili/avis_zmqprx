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

import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.messages.Notify;
import org.avis.security.Keys;

import static org.avis.io.XdrCoding.getStringArray;
import static org.avis.io.XdrCoding.putStringArray;

public class FedNotify extends Notify
{
  public static final int ID = 195;
  
  public String [] routing;
  
  public FedNotify ()
  {
    // zip
  }

  public FedNotify (Object... attributes)
  {
    super (attributes);
  }

  public FedNotify (Map<String, Object> attributes,
                    boolean deliverInsecure,
                    Keys keys,
                    String [] routing)
  {
    super (attributes, deliverInsecure, keys);
    
    this.routing = routing;
  }
  
  public FedNotify (Notify original, String [] routing)
  {
    this (original.attributes, original.deliverInsecure, 
          original.keys, routing);
  }
  
  public FedNotify (Notify original, Map<String, Object> attributes, 
                    Keys keys, String [] routing)
  {
    this (attributes, original.deliverInsecure, keys, routing);
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
    
    routing = getStringArray (in);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    super.encode (out);
    
    putStringArray (out, routing);    
  }
}
