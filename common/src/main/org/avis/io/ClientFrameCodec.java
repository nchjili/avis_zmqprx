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
package org.avis.io;

import org.apache.mina.common.IoFilter;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

import org.avis.io.messages.ConfConn;
import org.avis.io.messages.ConnRply;
import org.avis.io.messages.ConnRqst;
import org.avis.io.messages.Disconn;
import org.avis.io.messages.DisconnRply;
import org.avis.io.messages.DisconnRqst;
import org.avis.io.messages.DropWarn;
import org.avis.io.messages.Message;
import org.avis.io.messages.Nack;
import org.avis.io.messages.NotifyDeliver;
import org.avis.io.messages.NotifyEmit;
import org.avis.io.messages.QuenchPlaceHolder;
import org.avis.io.messages.SecRply;
import org.avis.io.messages.SecRqst;
import org.avis.io.messages.SubAddRqst;
import org.avis.io.messages.SubDelRqst;
import org.avis.io.messages.SubModRqst;
import org.avis.io.messages.SubRply;
import org.avis.io.messages.TestConn;
import org.avis.io.messages.UNotify;

/**
 * Codec for Elvin client protocol message frames.
 * 
 * @author Matthew Phillips
 */
public class ClientFrameCodec
  extends FrameCodec implements ProtocolCodecFactory
{
  public static final ClientFrameCodec INSTANCE = new ClientFrameCodec ();

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
      case ConnRqst.ID:
        return new ConnRqst ();
      case ConnRply.ID:
        return new ConnRply ();
      case DisconnRqst.ID:
        return new DisconnRqst ();
      case DisconnRply.ID:
        return new DisconnRply ();
      case Disconn.ID:
        return new Disconn ();
      case SubAddRqst.ID:
        return new SubAddRqst ();
      case SubRply.ID:
        return new SubRply ();
      case SubModRqst.ID:
        return new SubModRqst ();
      case SubDelRqst.ID:
        return new SubDelRqst ();
      case Nack.ID:
        return new Nack ();
      case NotifyDeliver.ID:
        return new NotifyDeliver ();
      case NotifyEmit.ID:
        return new NotifyEmit ();
      case TestConn.ID:
        return TestConn.INSTANCE;
      case ConfConn.ID:
        return ConfConn.INSTANCE;
      case SecRqst.ID:
        return new SecRqst ();
      case SecRply.ID:
        return new SecRply ();
      case UNotify.ID:
        return new UNotify ();
      case DropWarn.ID:
        return new DropWarn ();
      case QuenchPlaceHolder.ADD:
      case QuenchPlaceHolder.MODIFY:
      case QuenchPlaceHolder.DELETE:
        return new QuenchPlaceHolder (messageType, frameSize - 4);
      default:
        throw new ProtocolCodecException
          ("Unknown message type: ID = " + messageType);
    }
  }
}
