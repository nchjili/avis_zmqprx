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


/**
 * Synthetic message generated when a request timeout has elapsed.
 * 
 * @author Matthew Phillips
 */
public class RequestTimeoutMessage extends SyntheticMessage
{
  public static final int ID = -2;
  
  /**
   * The request that timed out.
   */
  public final RequestMessage<?> request;

  public RequestTimeoutMessage (RequestMessage<?> request)
  {
    this.request = request;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
}
