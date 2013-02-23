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

import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.avis.util.ListenerList;

/**
 * A simple logging facility.
 * 
 * @author Matthew Phillips
 */
public final class Log
{
  public static final int TRACE = 0;
  public static final int DIAGNOSTIC = 1;
  public static final int INFO = 2;
  public static final int WARNING = 3;
  public static final int ALARM = 4;
  public static final int INTERNAL_ERROR = 5;

  private static final String [] TYPE_NAMES =
    new String [] {"Trace", "Diagnostic", "Info",
                   "Warning", "Alarm", "Internal Error"};
  
  private static final ThreadLocal<DateFormat> dateFormat =
    new ThreadLocal<DateFormat> ()
  {
    @Override
    protected DateFormat initialValue ()
    {
      return new SimpleDateFormat ("MMM dd HH:mm:ss");
    }
  };
  
  private static String applicationName;
  private static PrintWriter stdout;
  private static PrintWriter stderr;
  private static boolean [] enabledTypes;
  private static ListenerList<LogListener> listeners;
  
  static
  {
    stdout = new PrintWriter (System.out);
    stderr = new PrintWriter (System.err);
    listeners = 
      new ListenerList<LogListener> (LogListener.class, 
                                     "messageLogged", LogEvent.class);
    enabledTypes = new boolean [6];
    
    logAll ();
    
    enableLogging (TRACE, false);
    enableLogging (DIAGNOSTIC, false);
  }
  
  private Log ()
  {
    // cannot instantiate
  }
  
  /**
   * Enable/disable logging for a given type (INFO, WARNING, etc).
   */
  public static void enableLogging (int type, boolean isEnabled)
  {
    enabledTypes [type] = isEnabled;
  }
  
  /**
   * Enable logging for all types.
   */
  public static void logAll ()
  {
    for (int i = 0; i < enabledTypes.length; i++)
      enabledTypes [i] = true;
  }

  /**
   * Disable logging for all types.
   */
  public static void logNone ()
  {
    for (int i = 0; i < enabledTypes.length; i++)
      enabledTypes [i] = false;
  }

  /**
   * Test if we should be logging a given type.
   */
  public static boolean shouldLog (int type)
  {
    return enabledTypes [type];
  }

  /**
   * Set the global application name to be used in log messages.
   */
  public static void setApplicationName (String name)
  {
    applicationName = name;
  }
  
  public static String applicationName ()
  {
    return applicationName;
  }
  
  public static void trace (String message, Object source)
  {
    log (TRACE, message, source);
  }
  
  public static void info (String message, Object source)
  {
    log (INFO, message, source);
  }
  
  public static void diagnostic (String message, Object source)
  {
    log (DIAGNOSTIC, message, source);
  }
  
  public static void diagnostic (String message, Object source, Throwable ex)
  {
    log (DIAGNOSTIC, message, source, ex);
  }
  
  public static void alarm (String message, Object source)
  {
    log (ALARM, message, source);
  }
  
  public static void alarm (String message, Object source, Throwable ex)
  {
    log (ALARM, message, source, ex);
  }

  public static void warn (String message, Object source)
  {
    log (WARNING, message, source);
  }
  
  public static void warn (String message, Object source, Throwable ex)
  {
    log (WARNING, message, source, ex);
  }
  
  public static void internalError (String message, Object source)
  {
    log (INTERNAL_ERROR, message, source);
  }
  
  public static void internalError (String message, Object source, Throwable ex)
  {
    log (INTERNAL_ERROR, message, source, ex);
  }
  
  private static void log (int type, String message, Object source)
  {
    log (type, message, source, null);
  }
  
  private static void log (int type, String message, Object source,
                           Throwable exception)
  {
    if (shouldLog (type))
    {
      Date time = new Date ();
      StringBuilder str = new StringBuilder ();
      
      printMessage (str, type, time, message, exception);

      PrintWriter output = (type >= WARNING) ? stderr : stdout;

      output.println (str);
      
      if (exception != null)
      {
        output.println ("Exception trace:");
        
        printExceptionTrace (output, exception);
      }
      
      output.flush ();
    }
    
    synchronized (listeners)
    {
      if (listeners.hasListeners ())
      {
        listeners.fire (new LogEvent (source, new Date (),
                                      type, message, exception));
      }      
    }
  }
  
  private static void printMessage (StringBuilder str,
                                    int type,
                                    Date time,
                                    String messageStr,
                                    Throwable exception)
  {
    str.append (dateFormat.get ().format (time));

    if (applicationName != null)
      str.append (": ").append (applicationName);

    str.append (": ").append (TYPE_NAMES [type]);
    str.append (": ").append (messageStr);
    
    if (exception != null && exception.getMessage () != null)
      str.append (": " + exception.getMessage ());
  }

  private static void printExceptionTrace (PrintWriter str,
                                           Throwable exception)
  {
    exception.printStackTrace (str);
    
    Throwable legacyNestedException = legacyNestedException (exception);
    
    if (legacyNestedException != null)
    {
      str.println ("--------------------");
      str.println ("Nested exception:");
      
      printExceptionTrace (str, legacyNestedException);
    }
  }

  private static Throwable legacyNestedException (Throwable exception)
  {
    if (exception instanceof SQLException)
      return ((SQLException)exception).getNextException ();
    else if (exception instanceof InvocationTargetException)
      return ((InvocationTargetException)exception).getTargetException ();
    else
      return null;
  }
  
  /**
   * Add a listener to all log messages.
   */
  public static void addLogListener (LogListener listener)
  {
    synchronized (listeners)
    {
      listeners.add (listener);
    }
  }

  /**
   * Remove a
   * {@linkplain #addLogListener(LogListener) previously added}
   * listener.
   */
  public static void removeLogListener (LogListener listener)
  {
    synchronized (listeners)
    {
      listeners.remove (listener);
    }
  }
  
  /**
   * Generate the same string that would be sent to the console for a
   * given log event.
   */
  public static String toLogString (LogEvent e)
  {
    StringBuilder str = new StringBuilder ();
    
    printMessage (str, e.type, e.time, e.message, e.exception);
    
    return str.toString ();
  }
}
