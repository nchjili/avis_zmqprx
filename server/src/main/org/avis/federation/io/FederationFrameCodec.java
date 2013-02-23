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
package org.avis.federation.io;

import org.apache.mina.common.IoFilter;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import org.avis.federation.io.messages.Ack;
import org.avis.federation.io.messages.FedConnRply;
import org.avis.federation.io.messages.FedConnRqst;
import org.avis.federation.io.messages.FedNotify;
import org.avis.federation.io.messages.FedSubReplace;
import org.avis.io.FrameCodec;
import org.avis.io.messages.ConfConn;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DropWarn;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.TestConn;

public class FederationFrameCodec
  extends FrameCodec implements ProtocolCodecFactory
{
  private static final FederationFrameCodec INSTANCE =
    new FederationFrameCodec ();
  
  public static final IoFilter FILTER = new ProtocolCodecFilter (INSTANCE);

  public ProtocolEncoder getEncoder ()
    throws Exception
  {
    return INSTANCE;
  }
  
  public ProtocolDecoder getDecoder ()
    throws Exception
  {
    return INSTANCE;
  }
  
  @Override
  protected Message newMessage (int messageType, int frameSize)
    throws ProtocolCodecException
  {
    switch (messageType)
    {
      case Nack.ID:
        return new Nack ();
      case Disconn.ID:
        return new Disconn ();
      case Ack.ID:
        return new Ack ();
      case FedConnRply.ID:
        return new FedConnRply ();
      case FedConnRqst.ID:
        return new FedConnRqst ();
      case FedSubReplace.ID:
        return new FedSubReplace ();
      case FedNotify.ID:
        return new FedNotify ();
      case TestConn.ID:
        return TestConn.INSTANCE;
      case ConfConn.ID:
        return ConfConn.INSTANCE;
      case DropWarn.ID:
        return new DropWarn ();
      default:
        throw new ProtocolCodecException
          ("Unknown message type: ID = " + messageType);
    }
  }
}
