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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

import static org.avis.util.Wildcard.toPattern;

/**
 * A filter that matches strings against wildcard patterns.
 * 
 * @author Matthew Phillips
 */
public class WildcardFilter implements Filter<String>
{
  private List<Pattern> patterns;
  
  public WildcardFilter (String wildcardPattern)
  {
    this (singleton (wildcardPattern));
  }
  
  public WildcardFilter (String... wildcardPatterns)
  {
    this (asList (wildcardPatterns));
  }
  
  public WildcardFilter (Collection<String> wildcardPatterns)
  {
    this.patterns = new ArrayList<Pattern> (wildcardPatterns.size ());
    
    for (String wildcardExpr : wildcardPatterns)
      patterns.add (toPattern (wildcardExpr, CASE_INSENSITIVE));
  }
  
  public boolean isNull ()
  {
    return patterns.isEmpty ();
  }
  
  public boolean matches (String string)
  {
    for (Pattern pattern : patterns)
    {
      if (pattern.matcher (string).matches ())
        return true;
    }
    
    return false;
  }
}
