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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.io.RequestTrackingFilter;

/**
 * Base class for messages that use a transaction id to identify replies.
 * 
 * @author Matthew Phillips
 */
public abstract class XidMessage extends Message
{
  private static final AtomicInteger xidCounter = new AtomicInteger ();
  
  public int xid;
  
  /**
   * The request message that triggered this reply. This is for the
   * convenience of message processing, not part of the serialized
   * format: you need to add a {@link RequestTrackingFilter} to the
   * filter chain if you want this automatically filled in.
   */
  public transient RequestMessage<?> request;
  
  public XidMessage ()
  {
    xid = -1;
  }
  
  public XidMessage (XidMessage inReplyTo)
  {
    this (inReplyTo.xid);
  }

  public XidMessage (int xid)
  {
    if (xid <= 0)
      throw new IllegalArgumentException ("Invalid XID: " + xid);
    
    this.xid = xid;
  }

  public boolean hasValidXid ()
  {
    return xid > 0;
  }

  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    xid = in.getInt ();
    
    if (xid <= 0)
      throw new ProtocolCodecException ("XID must be >= 0: " + xid);
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    if (xid == -1)
      throw new ProtocolCodecException ("No XID");
    
    out.putInt (xid);
  }
  
  protected static int nextXid ()
  {
    // NOTE: XID must not be zero (sec 7.4)
    return xidCounter.incrementAndGet ();
  }
}
