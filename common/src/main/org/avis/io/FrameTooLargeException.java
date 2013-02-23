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

import java.io.IOException;

/**
 * Throws by the frame codec when a frame is too big to be decoded.
 * 
 * @author Matthew Phillips
 */
public class FrameTooLargeException extends IOException
{
  public FrameTooLargeException (int maxLength, int actualLength)
  {
    super ("Frame size of " + actualLength + 
           " bytes is larger than maximum " + maxLength);
  }
}
