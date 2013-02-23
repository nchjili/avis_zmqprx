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
package org.avis.logging;

import java.util.Date;

/**
 * An event sent by the log to
 * {@linkplain Log#addLogListener(LogListener) registered listeners}
 * when a message is logged.
 * 
 * @see Log#addLogListener(LogListener)
 * 
 * @author Matthew Phillips
 */
public class LogEvent
{
  public final Date time;
  public final Object source;
  public final int type;
  public final String message;
  public final Throwable exception;
  
  public LogEvent (Object source, Date time, int type,
                   String message, Throwable exception)
  {
    this.source = source;
    this.time = time;
    this.type = type;
    this.message = message;
    this.exception = exception;
  }
}
