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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.nio.BufferUnderflowException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import static java.nio.CharBuffer.wrap;
import static java.util.Collections.emptyMap;

/**
 * Encoding/decoding helpers for the Elvin XDR wire format.
 * 
 * @author Matthew Phillips
 */
public final class XdrCoding
{
  /**
   * Type codes from client protocol spec.
   *  enum {
   *       int32_tc  = 1,
   *       int64_tc  = 2,
   *       real64_tc = 3,
   *       string_tc = 4,
   *       opaque_tc = 5
   *   } value_typecode;
   */
  public static final int TYPE_INT32  = 1;
  public static final int TYPE_INT64  = 2;
  public static final int TYPE_REAL64 = 3;
  public static final int TYPE_STRING = 4;
  public static final int TYPE_OPAQUE = 5;

  private static final byte [] EMPTY_BYTES = new byte [0];
  
  /**
   * Per thread UTF-8 decoder.
   */
  private static final ThreadLocal<CharsetDecoder> UTF8_DECODER =
    new ThreadLocal<CharsetDecoder> ()
  {
    @Override
    protected CharsetDecoder initialValue ()
    {
      return Charset.forName ("UTF-8").newDecoder ();
    }
  };
  
  /**
   * Per thread UTF-8 encoder.
   */
  private static final ThreadLocal<CharsetEncoder> UTF8_ENCODER =
    new ThreadLocal<CharsetEncoder> ()
  {
    @Override
    protected CharsetEncoder initialValue ()
    {
      return Charset.forName ("UTF-8").newEncoder ();
    }
  };
 
  private XdrCoding ()
  {
    // zip
  }
  
  public static byte [] toUTF8 (String string)
  { 
    try
    {
      if (string.length () == 0)
        return EMPTY_BYTES;
      else
        return UTF8_ENCODER.get ().encode (wrap (string)).array ();
    } catch (CharacterCodingException ex)
    {
      // shouldn't be possible to get an error encoding from UTF-16 to UTF-8.
      throw new Error ("Internal error", ex);
    }
  }
  
  /**
   * Turn a UTF-8 byte array into a string.
   * 
   * @param utf8Bytes The bytes.
   * @param offset The offset into bytes.
   * @param length The number of bytes to use.
   * @return The string.
   * 
   * @throws CharacterCodingException if the bytes do not represent a
   *           UTF-8 string.
   */
  public static String fromUTF8 (byte [] utf8Bytes, int offset, int length)
    throws CharacterCodingException
  { 
    if (utf8Bytes.length == 0)
      return "";
    else
      return UTF8_DECODER.get ().decode
        (java.nio.ByteBuffer.wrap (utf8Bytes, offset, length)).toString ();
  }
  
  /**
   * Read a length-delimited 4-byte-aligned UTF-8 string.
   */
  public static String getString (ByteBuffer in)
    throws BufferUnderflowException, ProtocolCodecException
  {
    try
    {
      int length = getPositiveInt (in);

      if (length == 0)
      {
        return "";
      } else
      {
        String string = in.getString (length, UTF8_DECODER.get ());
        
        in.skip (paddingFor (length));
        
        return string;
      }
    } catch (CharacterCodingException ex)
    {
      throw new ProtocolCodecException ("Invalid UTF-8 string", ex);
    }
  }

  /**
   * Write a length-delimited 4-byte-aligned UTF-8 string.
   */
  public static void putString (ByteBuffer out, String string)
  {
    try
    {
      if (string.length () == 0)
      {
        out.putInt (0);
      } else
      {
        int start = out.position ();
        
        out.skip (4);
        
        out.putString (string, UTF8_ENCODER.get ());
        
        // write length
        
        int byteCount = out.position () - start - 4;
        
        out.putInt (start, byteCount);
        
        putPadding (out, byteCount);
      }
    } catch (CharacterCodingException ex)
    {
      // shouldn't be possible to get an error encoding from UTF-16 to UTF-8.
      throw new Error ("Internal error", ex);
    }
  }

  /**
   * Generate null padding to 4-byte pad out a block of a given length
   */
  public static void putPadding (ByteBuffer out, int length)
  {
    for (int count = paddingFor (length); count > 0; count--)
      out.put ((byte)0);
  }

  /**
   * Calculate the padding needed for the size of a block of bytes to
   * be a multiple of 4.
   */
  public static int paddingFor (int length)
  {
    // tricky eh? this is equivalent to (4 - length % 4) % 4
    return (4 - (length & 3)) & 3;
  }

  /**
   * Write a name/value set.
   */
  public static void putNameValues (ByteBuffer out,
                                    Map<String, Object> nameValues)
    throws ProtocolCodecException
  {
    out.putInt (nameValues.size ());
    
    for (Entry<String, Object> entry : nameValues.entrySet ())
    {
      putString (out, entry.getKey ());
      putObject (out, entry.getValue ());
    }
  }

  /**
   * Read a name/value set.
   */
  public static Map<String, Object> getNameValues (ByteBuffer in)
    throws ProtocolCodecException
  {
    int pairs = getPositiveInt (in);
    
    if (pairs == 0)
      return emptyMap ();
    
    HashMap<String, Object> nameValues = new HashMap<String, Object> ();
    
    for ( ; pairs > 0; pairs--)
      nameValues.put (getString (in), getObject (in));

    return nameValues;
  }

  public static void putObjects (ByteBuffer out, Object [] objects)
    throws ProtocolCodecException
  {
    out.putInt (objects.length);
    
    for (Object object : objects)
      putObject (out, object);
  }
  
  public static Object [] getObjects (ByteBuffer in)
    throws ProtocolCodecException
  {
    Object [] objects = new Object [getPositiveInt (in)];
    
    for (int i = 0; i < objects.length; i++)
      objects [i] = getObject (in);
    
    return objects;
  }
  
  /**
   * Put an object value in type_id/value format.
   */
  public static void putObject (ByteBuffer out, Object value)
    throws ProtocolCodecException
  {
    if (value instanceof String)
    {
      out.putInt (TYPE_STRING);
      putString (out, (String)value);
    } else if (value instanceof Integer)
    {
      out.putInt (TYPE_INT32);
      out.putInt ((Integer)value);
    } else if (value instanceof Long)
    {
      out.putInt (TYPE_INT64);
      out.putLong ((Long)value);
    } else if (value instanceof Double)
    {
      out.putInt (TYPE_REAL64);
      out.putDouble ((Double)value);
    } else if (value instanceof byte [])
    {
      out.putInt (TYPE_OPAQUE);
      putBytes (out, (byte [])value);
    } else if (value == null)
    {
      throw new IllegalArgumentException ("Value cannot be null");
    } else
    {
      throw new IllegalArgumentException
        ("Don't know how to encode " + value.getClass ());
    }
  }
  
  /**
   * Read an object in type_id/value format.
   */
  public static Object getObject (ByteBuffer in)
    throws ProtocolCodecException
  {
    int type = in.getInt ();
    
    switch (type)
    {
      case TYPE_INT32:
        return in.getInt ();
      case TYPE_INT64:
        return in.getLong ();
      case TYPE_REAL64:
        return in.getDouble ();
      case TYPE_STRING:
        return getString (in);
      case TYPE_OPAQUE:
        return getBytes (in);
      default:
        throw new ProtocolCodecException ("Unknown type code: " + type);
    }
  }
  
  /**
   * Write a length-delimited, 4-byte-aligned byte array.
   */
  public static void putBytes (ByteBuffer out, byte [] bytes)
  {
    out.putInt (bytes.length);
    out.put (bytes);
    putPadding (out, bytes.length);
  }

  /**
   * Read a length-delimited, 4-byte-aligned byte array.
   * 
   * @throws ProtocolCodecException 
   */
  public static byte [] getBytes (ByteBuffer in) 
    throws ProtocolCodecException
  {
    return getBytes (in, getPositiveInt (in));
  }
  
  /**
   * Read a length-delimited, 4-byte-aligned byte array with a given length.
   */
  public static byte [] getBytes (ByteBuffer in, int length)
  {
    byte [] bytes = new byte [length];
    
    in.get (bytes);
    in.skip (paddingFor (length));
    
    return bytes;
  }

  public static void putBool (ByteBuffer out, boolean value)
  {
    out.putInt (value ? 1 : 0);
  }
  
  public static boolean getBool (ByteBuffer in)
    throws ProtocolCodecException
  {
    int value = in.getInt ();
    
    if (value == 0)
      return false;
    else if (value == 1)
      return true;
    else
      throw new ProtocolCodecException
        ("Cannot interpret " + value + " as boolean");
  }
  
  /**
   * Read a length-demlimited array of longs.
   */
  public static long [] getLongArray (ByteBuffer in)
    throws ProtocolCodecException
  {
    long [] longs = new long [getPositiveInt (in)];
    
    for (int i = 0; i < longs.length; i++)
      longs [i] = in.getLong ();
    
    return longs;
  }

  /**
   * Write a length-delimted array of longs.
   */
  public static void putLongArray (ByteBuffer out, long [] longs)
  {
    out.putInt (longs.length);
    
    for (long l : longs)
      out.putLong (l);
  }

  /**
   * Read a length-demlimited array of strings.
   */
  public static String [] getStringArray (ByteBuffer in)
    throws BufferUnderflowException, ProtocolCodecException
  {
    String [] strings = new String [getPositiveInt (in)];
    
    for (int i = 0; i < strings.length; i++)
      strings [i] = getString (in);
    
    return strings;
  }

  /**
   * Write a length-delimted array of strings.
   */
  public static void putStringArray (ByteBuffer out, String [] strings)
  {
    out.putInt (strings.length);
    
    for (String s : strings)
      putString (out, s);
  }
  
  /**
   * Read an int >= 0 or generate an exception.
   */
  private static int getPositiveInt (ByteBuffer in) 
    throws ProtocolCodecException
  {
    int value = in.getInt ();
    
    if (value >= 0)
      return value;
    else
      throw new ProtocolCodecException ("Length cannot be negative: " + value);
  }
}
