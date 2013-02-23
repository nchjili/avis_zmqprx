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
 * Like InterruptedException, but less annoying. Classes that use
 * Thread.wait () etc and want to avoid everyhing up the chain
 * declaring InterruptedException, should catch it and re-throw this.
 * 
 * @author Matthew Phillips
 */
public class RuntimeInterruptedException extends RuntimeException
{
  public RuntimeInterruptedException (InterruptedException cause)
  {
    super (cause);
  }
}
