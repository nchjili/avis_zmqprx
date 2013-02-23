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

import java.util.regex.Pattern;

import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isWhitespace;
import static java.util.regex.Pattern.compile;

/**
 * Generates regex patterns that match Unix-style wildcards * and ?.
 * Currently handles * and ? by generating regex equivalents.
 */
public final class Wildcard
{
  private Wildcard ()
  {
    // zip
  }
  
  /**
   * Generate a patten matcher for a wildcard expression.
   * 
   * @param wildcard The wildcard expression.
   * 
   * @return A pattern that matches the wildcard.
   */
  public static Pattern toPattern (String wildcard)
  {
    return toPattern (wildcard, 0);
  }
  
  /**
   * Generate a patten matcher for a wildcard expression.
   * 
   * @param wildcard The wildcard expression.
   * @param flags The patten flags (e.g Pattern.CASE_INSENSITIVE).
   * 
   * @return A pattern that matches the wildcard.
   */
  public static Pattern toPattern (String wildcard, int flags)
  {
    StringBuilder regex = new StringBuilder (wildcard.length () * 2);
    
    for (int i = 0; i < wildcard.length (); i++)
    {
      char c = wildcard.charAt (i);
      
      switch (c)
      {
        case '*':
          regex.append (".*");
          break;
        case '?':
          regex.append ('.');
          break;
        case '\\':
          if (++i < wildcard.length ())
          {
            regex.append ('\\');
            regex.append (wildcard.charAt (i));
          } else
          {
            regex.append ("\\\\");
          }
          break;
        default:
          if (isLetterOrDigit (c) || isWhitespace (c))
            regex.append (c);
          else
            regex.append ('\\').append (c);
      }
    }
    
    return compile (regex.toString (), flags);
  }
}
