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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JUTestWildcardFilter
{
  @Test
  public void wildcards () 
    throws Exception
  {
    assertFalse (new WildcardFilter ().matches (""));
    assertTrue  (new WildcardFilter ("").matches (""));
    assertFalse (new WildcardFilter ("").matches ("a"));
    assertTrue  (new WildcardFilter ("abc").matches ("abc"));
    assertFalse (new WildcardFilter ("abc").matches ("abcd"));
    assertTrue  (new WildcardFilter ("abc*").matches ("abcd"));
    assertTrue  (new WildcardFilter ("abc*z").matches ("abcdefgz"));
    assertTrue  (new WildcardFilter ("def", "abc*").matches ("abcd"));
    assertTrue  (new WildcardFilter ("abc?").matches ("abcd"));
    assertFalse (new WildcardFilter ("abc?").matches ("abcde"));
  }
}
