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

import org.apache.mina.common.ExceptionMonitor;

import static org.avis.logging.Log.internalError;

/**
 * MINA exception monitor that routes exceptions to the log.
 * 
 * @author Matthew Phillips
 */
public class ExceptionMonitorLogger extends ExceptionMonitor
{
  public static final ExceptionMonitorLogger INSTANCE = 
    new ExceptionMonitorLogger ();

  private ExceptionMonitorLogger ()
  {
    // zip
  }
  
  @Override
  public void exceptionCaught (Throwable cause)
  {
    internalError ("Unexpected exception during IO", this, cause);
  }
}
