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
package org.avis.util;

import java.util.Properties;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * General utilities for messing with I/O streams.
 * 
 * @author Matthew Phillips
 */
public final class Streams
{
  private Streams ()
  {
    // zio
  }
  
  /**
   * Generate a buffered reader wrapper for a reader, if it is not
   * already one.
   */
  public static BufferedReader bufferedReaderFor (Reader reader)
  {
    return reader instanceof BufferedReader ? (BufferedReader)reader :
                                              new BufferedReader (reader);
  }

  /**
   * Read all the bytes from a stream and then close it.
   * 
   * @param in The input stream to read.
   * @return The bytes read.
   * 
   * @throws IOException if an error occurs reading stream.
   * 
   * @see #stringFrom(InputStream)
   */
  public static byte [] bytesFrom (InputStream in)
    throws IOException
  {
    try
    {
      ByteArrayOutputStream str = new ByteArrayOutputStream (4096);
      
      byte [] buffer = new byte [4096];
      int length;
      
      while ((length = in.read (buffer)) != -1)
        str.write (buffer, 0, length);
      
      return str.toByteArray ();
    } finally
    {
      close (in);
    }
  }

  /**
   * Reader's and InputStream's (almost unbelievably) do not have a
   * way to tell when the stream is at eof without modifying it. This
   * uses mark () and read () to non-destructively test for eof. The
   * stream must support mark ().
   */
  public static boolean eof (Reader in)
    throws IOException
  {
    in.mark (10);
    
    if (in.read () == -1)
    {
      return true;
    } else
    {
      in.reset ();
      
      return false;
    }
  }

  /**
   * Open an input stream on a file.
   */
  public static InputStream fileStream (File file)
    throws FileNotFoundException
  {
    return fileStream (file.getPath ());
  }
  
  /**
   * Open an input stream on a file.
   */
  public static InputStream fileStream (String filename)
    throws FileNotFoundException
  {
    return new BufferedInputStream (new FileInputStream (filename));
  }

  /**
   * Load a set of java.util.Properties from an input stream and close it.
   */
  public static Properties propertiesFrom (InputStream in)
    throws IOException
  {
    try
    {
      Properties properties = new Properties ();
      properties.load (in);
      
      return properties;
    } finally
    {
      close (in);
    } 
  }

  /**
   * Read a line from a Reader. The reader must support mark () and
   * reset (). Lines may be terminated by NL, CRLF/NL or NL/CRLF.
   * 
   * @param in The reader.
   * @return The line read, or null if at EOF.
   * 
   * @throws IOException if an error occurs reading data.
   */
  public static String readLine (Reader in)
    throws IOException
  {
    int state = 0; // 0 = init, 1 = in line, 2 = in CR, 3 = in NL
    StringBuilder str = new StringBuilder ();
    
    for (;;)
    {
      in.mark (1);
      int c = in.read ();
      
      switch (c)
      {
        case -1:
          switch (state)
          {
            case 0:
              return null;
            default:
              return str.toString ();
          }
        case '\r':
          switch (state)
          {
            case 0:
            case 1:
              state = 2;
              break;
            case 2:
              in.reset (); // second \r: push back and return
            default:
              return str.toString ();
          }
          break;
        case '\n':
          switch (state)
          {
            case 0:
            case 1:
              state = 3;
              break;
            case 3:
              in.reset (); // second \n: push back and return
            default:
              return str.toString ();
          }
          break;
        default:
          switch (state)
          {
            case 2:
            case 3:
              in.reset ();
              
              return str.toString ();
            default:
              state = 1;
              str.append ((char)c);
          }
      }
    }
  }
  
  /**
   * Open an input stream on a resource.
   */
  public static InputStream resourceStream (String resource)
    throws FileNotFoundException
  {
    InputStream in = Streams.class.getResourceAsStream (resource);
    
    if (in == null)
      throw new FileNotFoundException ("Missing resource: " + resource);
    else
      return in;
  }

  /**
   * Read the entirety of a UTF-8 encoded input stream into a string.
   */
  public static String stringFrom (InputStream stream)
    throws IOException
  {
    return stringFrom (new InputStreamReader (stream, "UTF-8"));
  }

  /**
   * Read the entirety of stream into a string.
   * 
   * @see #bytesFrom(InputStream)
   */
  public static String stringFrom (Reader reader)
    throws IOException
  {
    try
    {
      StringBuilder str = new StringBuilder ();
      
      char [] buffer = new char [4096];
      int length;

      while ((length = reader.read (buffer)) != -1)
        str.append (buffer, 0, length);
      
      return str.toString ();
    } finally
    {
      close (reader);
    }
  }
  
  /**
   * Close an input stream. Eats the (pointless, because there's
   * nothing we can do) IO exception and returns true/false instead.
   */
  public static boolean close (Closeable in)
  {
    try
    {
      in.close ();
      
      return true;
    } catch (IOException ex)
    {
      return false;
    }
  }
}
