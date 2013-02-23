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
package org.avis.common;

/**
 * Common Avis definitions.
 * 
 * @author Matthew Phillips
 */
public final class Common
{
  public static final int K = 1024;
  public static final int MB = 1024 * 1024;
  public static final int MAX = Integer.MAX_VALUE;
  
  public static final int DEFAULT_PORT = 2917;
  
  public static final int CLIENT_VERSION_MAJOR = 4;
  public static final int CLIENT_VERSION_MINOR = 0;
  
  private Common ()
  {
    // cannot be instantiated
  } 
}
