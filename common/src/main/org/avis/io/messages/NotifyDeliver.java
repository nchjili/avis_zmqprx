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

import static org.avis.io.XdrCoding.getLongArray;
import static org.avis.io.XdrCoding.getNameValues;
import static org.avis.io.XdrCoding.putLongArray;
import static org.avis.io.XdrCoding.putNameValues;

public class NotifyDeliver extends Message
{
  public static final int ID = 57;

  public Map<String, Object> attributes;
  public long [] secureMatches;
  public long [] insecureMatches;
  
  public NotifyDeliver ()
  {
    // zip
  }
  
  public NotifyDeliver (Map<String, Object> attributes,
                        long [] secureMatches, long [] insecureMatches)
  {
    this.attributes = attributes;
    this.secureMatches = secureMatches;
    this.insecureMatches = insecureMatches;
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
    putNameValues (out, attributes);
    putLongArray (out, secureMatches);
    putLongArray (out, insecureMatches);
  }

  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    attributes = getNameValues (in);
    secureMatches = getLongArray (in);
    insecureMatches = getLongArray (in);
  }
}
