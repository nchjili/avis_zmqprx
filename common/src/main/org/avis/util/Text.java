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

import java.util.List;
import java.util.Map;

import java.nio.charset.CharacterCodingException;

import static java.lang.Integer.toHexString;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.lang.System.arraycopy;
import static java.lang.System.identityHashCode;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;

import static org.avis.io.XdrCoding.fromUTF8;
import static org.avis.io.XdrCoding.toUTF8;


/**
 * General text formatting utilities.
 * 
 * @author Matthew Phillips
 */
public final class Text
{
  private static final char [] HEX_TABLE = 
    {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
     'a', 'b', 'c', 'd', 'e', 'f'};
  
  private static final String [] EMPTY_STRING_ARRAY = new String [0];

  private Text ()
  {
    // cannot be instantiated
  }
  
  /**
   * Return just the name (minus the package) of an object's class.
   */
  public static String className (Object object)
  { 
    return className (object.getClass ());
  }
  
  /**
   * Return just the name (minus the package) of a class.
   */
  public static String className (Class<?> type)
  {
    String name = type.getName ();
    
    return name.substring (name.lastIndexOf ('.') + 1);
  }

  /**
   * Generate a short exception message without package name and
   * message (if null).
   */
  public static String shortException (Throwable ex)
  {
    if (ex.getMessage () == null)
      return className (ex.getClass ());
    else
      return className (ex.getClass ()) + ": " + ex.getMessage ();
  }

  /**
   * Generate a hex ID for an object.
   */
  public static String idFor (Object instance)
  {
    return toHexString (identityHashCode (instance));
  }
  
  /**
   * Generate a string value of the notification.
   * 
   * @param attributes The attribute name/value pairs.
   * 
   * @return The string formatted version of the notification attributes.
   */
  public static String formatNotification (Map<String, Object> attributes)
  {
    String [] names = new String [attributes.size ()];
    
    attributes.keySet ().toArray (names);
    
    sort (names, CASE_INSENSITIVE_ORDER);
    
    StringBuilder str = new StringBuilder (names.length * 16);
    boolean first = true;
    
    for (String name : names)
    {
      if (!first)
        str.append ('\n');
      
      first = false;
      
      appendEscaped (str, name, " :");
      
      str.append (": ");
      
      appendValue (str, attributes.get (name));
    }
    
    return str.toString ();
  }
  
  private static void appendValue (StringBuilder str, Object value)
  {
    if (value instanceof String)
    {
      str.append ('"');
      appendEscaped (str, (String)value, '"');
      str.append ('"');
    } else if (value instanceof Number)
    {
      str.append (value);
      
      if (value instanceof Long)
        str.append ('L');   
    } else
    {
      str.append ('[');
      appendHexBytes (str, (byte [])value);
      str.append (']');
    }
  }
  
  /**
   * Append a string to a builder, escaping (with '\') any instances
   * of a special character.
   */
  public static void appendEscaped (StringBuilder builder,
                                    String string, char charToEscape)
  {
    for (int i = 0; i < string.length (); i++)
    {
      char c = string.charAt (i);
      
      if (c == charToEscape)
        builder.append ('\\');
      
      builder.append (c);
    }
  }
  
  /**
   * Append a string to a builder, escaping (with '\') any instances
   * of a set of special characters.
   */
  public static void appendEscaped (StringBuilder builder,
                                    String string, String charsToEscape)
  {
    for (int i = 0; i < string.length (); i++)
    {
      char c = string.charAt (i);
      
      if (charsToEscape.indexOf (c) != -1)
        builder.append ('\\');
      
      builder.append (c);
    }
  }

  /**
   * Append a byte array to a builder in form: 01 e2 fe ff ...
   */
  public static void appendHexBytes (StringBuilder str, byte [] bytes)
  {
    boolean first = true;
    
    for (byte b : bytes)
    {
      if (!first)
        str.append (' ');
      
      first = false;
      
      appendHex (str, b);
    }
  }

  /**
   * Append the hex form of a byte to a builder.
   */
  public static void appendHex (StringBuilder str, byte b)
  {
    str.append (HEX_TABLE [(b >>> 4) & 0x0F]);
    str.append (HEX_TABLE [(b >>> 0) & 0x0F]);
  }

  /**
   * Parse a string expression as a hex-coded unsigned byte.
   * 
   * @return A byte in the range 0 - 255 if sign is ignored.
   */
  public static byte hexToByte (String byteExpr)
    throws InvalidFormatException
  {
    if (byteExpr.length () == 0)
    {
      throw new InvalidFormatException ("Byte value cannot be empty");
    } else if (byteExpr.length () > 2)
    {
      throw new InvalidFormatException
        ("Byte value too long: \"" + byteExpr + "\"");
    }
    
    int value = 0;
    
    for (int i = 0; i < byteExpr.length (); i++)
      value = (value << 4) | hexValue (byteExpr.charAt (i));
  
    return (byte)value;
  }
  
  /**
   * Parse a string expression as a value. Values may be quoted
   * strings ("string"), numbers (0.1, 3, 123456789L), or byte arrays
   * ([0a ff de ad]).
   * 
   * @param expr The string expression.
   * 
   * @return The value.
   * 
   * @throws InvalidFormatException if expr is not parseable.
   * 
   * @see #stringToNumber(String)
   * @see #stringToOpaque(String)
   * @see #quotedStringToString(String)
   */
  public static Object stringToValue (String expr)
    throws InvalidFormatException
  {
    char firstChar = expr.charAt (0);
    
    if (firstChar == '"' || firstChar == '\'')
      return quotedStringToString (expr);
    else if (firstChar >= '0' && firstChar <= '9')
      return stringToNumber (expr);
    else if (firstChar == '[')
      return stringToOpaque (expr);
    else
      throw new InvalidFormatException
        ("Unrecognised value expression: \"" + expr + "\"");
  }

  /**
   * Parse a numeric int, long or double value. e.g. 32L, 3.14, 42.
   */
  public static Number stringToNumber (String valueExpr)
    throws InvalidFormatException
  {
    try
    {
      if (valueExpr.indexOf ('.') != -1)
        return Double.valueOf (valueExpr);
      else if (valueExpr.endsWith ("L") || valueExpr.endsWith ("l"))
        return Long.decode (valueExpr.substring (0, valueExpr.length () - 1));
      else
        return Integer.decode (valueExpr);
    } catch (NumberFormatException ex)
    {
      throw new InvalidFormatException ("Invalid number: " + valueExpr);
    }
  }

  /**
   * Parse a string value in the format "string", allowing escaped "'s
   * inside the string.
   */
  public static String quotedStringToString (String valueExpr)
    throws InvalidFormatException
  {
    if (valueExpr.length () == 0)
      throw new InvalidFormatException ("Empty string");
    
    char quote = valueExpr.charAt (0);
    
    if (quote != '\'' && quote != '"')
      throw new InvalidFormatException ("String must start with a quote");
    
    int last = findFirstNonEscaped (valueExpr, 1, quote);
    
    if (last == -1)
      throw new InvalidFormatException ("Missing terminating quote in string");
    else if (last != valueExpr.length () - 1)
      throw new InvalidFormatException ("Extra characters following string");
    
    return stripBackslashes (valueExpr.substring (1, last));
  }

  /**
   * Parse an opaque value expression e.g. [00 0f 01]. 
   */
  public static byte [] stringToOpaque (String valueExpr)
    throws InvalidFormatException
  {
    if (valueExpr.length () < 2)
      throw new InvalidFormatException ("Opaque value too short");
    else if (valueExpr.charAt (0) != '[')
      throw new InvalidFormatException ("Missing '[' at start of opaque");
    
    int closingBrace = valueExpr.indexOf (']');
    
    if (closingBrace == -1)
      throw new InvalidFormatException ("Missing closing \"]\"");
    else if (closingBrace != valueExpr.length () - 1)
      throw new InvalidFormatException ("Junk at end of opaque value");
  
    return hexToBytes (valueExpr.substring (1, closingBrace));
  }
  
  /**
   * Parse a series of hex pairs as a sequence of unsigned bytes.
   * Pairs may be separated by optional whitespace. e.g. "0A FF 00 01"
   * or "deadbeef".
   */
  public static byte [] hexToBytes (String string)
    throws InvalidFormatException
  {
    string = string.replaceAll ("\\s+", "");
    
    if (string.length () % 2 != 0)
      throw new InvalidFormatException ("Hex bytes must be a set of hex pairs");
    
    byte [] bytes = new byte [string.length () / 2];
    
    for (int i = 0; i < string.length (); i += 2)
      bytes [i / 2] = hexToByte (string.substring (i, i + 2));
    
    return bytes;
  }
  
  /**
   * Turn an array of bytes into a hex-encoded string e.g. "00 01 aa de".
   */
  public static String bytesToHex (byte [] bytes)
  {
    StringBuilder str = new StringBuilder (bytes.length * 3);
    
    appendHexBytes (str, bytes);
    
    return str.toString ();
  }
  
  /**
   * Turn a data block expression into a block of bytes.
   * 
   * Formats:
   * <pre>
   *   Hex pairs: [0a 02 ff 31]
   *   String:    "hello"
   *   Raw data:  #data
   * </pre>
   * 
   * @param expr The data block expression
   * @return The data.
   * 
   * @throws InvalidFormatException if the expression was not valid. 
   */
  public static byte [] dataToBytes (byte [] expr) 
    throws InvalidFormatException
  {
    if (expr.length == 0)
      throw new InvalidFormatException ("Expression cannot be empty");
    
    try
    {
      switch (expr [0])
      {
        case '[':
          return stringToOpaque (fromUTF8 (expr, 0, expr.length).trim ());
        case '"':
          return toUTF8 (quotedStringToString (fromUTF8 (expr, 0, expr.length).trim ()));
        case '#':
          return slice (expr, 1, expr.length);
        default:
          throw new InvalidFormatException ("Unknown data block format");
      }
    } catch (CharacterCodingException ex)
    {
      throw new InvalidFormatException ("Invalid UTF-8 string");
    }
  }
  
  public static byte [] slice (byte [] bytes, int start, int end)
  {
    byte [] slice = new byte [end - start];
    
    arraycopy (bytes, start, slice, 0, slice.length);
    
    return slice;
  }

  /**
   * Find the first index of the given character, skipping instances
   * that are escaped by '\'.
   */
  public static int findFirstNonEscaped (String str, char toFind)
  {
    return findFirstNonEscaped (str, 0, toFind);
  }
  
  /**
   * Find the first index of the given character, skipping instances
   * that are escaped by '\'.
   */
  public static int findFirstNonEscaped (String str, int start, char toFind)
  {
    boolean escaped = false;
    
    for (int i = start; i < str.length (); i++)
    {
      char c = str.charAt (i);
      
      if (c == '\\')
      {
        escaped = true;
      } else
      {
        if (!escaped && c == toFind)
          return i;
        
        escaped = false;
      }
    }
    
    return -1;
  }
  
  /**
   * Remove any \'s from a string.
   */
  public static String stripBackslashes (String text)
    throws InvalidFormatException
  {
    if (text.indexOf ('\\') != -1)
    {
      StringBuilder buff = new StringBuilder (text.length ());
      
      for (int i = 0; i < text.length (); i++)
      {
        char c = text.charAt (i);
        
        if (c != '\\')
        {
          buff.append (c);
        } else
        {
          i++;
          
          if (i < text.length ())
            buff.append (text.charAt (i));
          else
            throw new InvalidFormatException ("Invalid trailing \\");
        }
      }
      
      text = buff.toString ();
    }
    
    return text;
  }
  
  /**
   * Shortcut to execute split on any whitespace character.
   */
  public static String [] split (String text)
  {
    return split (text, "\\s+");
  }
  
  /**
   * String.split ("") returns {""} rather than {} like you might
   * expect: this returns empty array on "".
   */
  public static String [] split (String text, String regex)
  {
    if (text.length () == 0)
      return EMPTY_STRING_ARRAY;
    else
      return text.split (regex);
  }
  
  /**
   * Join a list of objects into a string.
   * 
   * @param items The items to stringify.
   * 
   * @return The stringified list.
   */
  public static String join (Object [] items)
  {
    return join (items, ", ");
  }
  
  /**
   * Join a list of objects into a string.
   * 
   * @param items The items to stringify.
   * @param separator The separator between items.
   * 
   * @return The stringified list.
   */
  public static String join (Object [] items, String separator)
  {
    return join (asList (items), separator);
  }

  /**
   * Join a list of objects into a string.
   * 
   * @param items The items to stringify.
   * @param separator The separator between items.
   * 
   * @return The stringified list.
   */
  public static String join (List<?> items, String separator)
  {
    StringBuilder str = new StringBuilder ();
    
    boolean first = true;
    
    for (Object item : items)
    {
      if (!first)
        str.append (separator);
      
      first = false;

      str.append (item);
    }
    
    return str.toString ();
  }

  /**
   * Generate human friendly string dump of a Map.
   */
  public static String mapToString (Map<?, ?> map)
  {
    StringBuilder str = new StringBuilder ();
    boolean first = true;
    
    for (Map.Entry<?, ?> entry : map.entrySet ())
    {
      if (!first)
        str.append (", ");
      
      first = false;
      
      str.append ('{');
      str.append (entry.getKey ()).append (" = ").append (entry.getValue ());
      str.append ('}');
    }
    
    return str.toString ();
  }
  
  /**
   * Expand C-like backslash codes such as \n \x90 etc into their
   * literal values.
   * @throws InvalidFormatException 
   */
  public static String expandBackslashes (String text)
    throws InvalidFormatException
  {
    if (text.indexOf ('\\') != -1)
    {
      StringBuilder buff = new StringBuilder (text.length ());
      
      for (int i = 0; i < text.length (); i++)
      {
        char c = text.charAt (i);
        
        if (c == '\\')
        {
          c = text.charAt (++i);
          
          switch (c)
          {
            case 'n':
              c = '\n'; break;
            case 't':
              c = '\t'; break;
            case 'b':
              c = '\b'; break;
            case 'r':
              c = '\r'; break;
            case 'f':
              c = '\f'; break;
            case 'a':
              c = 7; break;
            case 'v':
              c = 11; break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
              int value = c - '0';
              int end = Math.min (text.length (), i + 3);
              
              while (i + 1 < end && octDigit (text.charAt (i + 1)))
              {
                c = text.charAt (++i);
                value = value * 8 + (c - '0');                
              }
              
              c = (char)value;
              break;
            case 'x':
              value = 0;
              end = Math.min (text.length (), i + 3);
              
              do
              {
                c = text.charAt (++i);
                value = value * 16 + hexValue (c);
              } while (i + 1 < end && hexDigit (text.charAt (i + 1)));
              
              c = (char)value;
              break;
          }
        }

        buff.append (c);
      }
      
      text = buff.toString ();
    }
    
    return text;
  }
  
  private static boolean octDigit (char c)
  {
    return c >= '0' && c <= '7';
  }
  
  private static boolean hexDigit (char c)
  {
    return (c >= '0' && c <= '9') ||
           (c >= 'a' && c <= 'f') ||
           (c >= 'A' && c <= 'F');
  }

  private static int hexValue (char c)
    throws InvalidFormatException
  {
    if (c >= '0' && c <= '9')
      return c - '0';
    else if (c >= 'a' && c <= 'f')
      return c - 'a' + 10;
    else if (c >= 'A' && c <= 'F')
      return c - 'A' + 10;
    else
      throw new InvalidFormatException ("Not a valid hex character: " + c);
  }
}
