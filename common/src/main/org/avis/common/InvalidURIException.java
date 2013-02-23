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

import java.net.URISyntaxException;

/**
 * Unchecked invalid URI exception. This is used in places that it
 * would be irritating to use the checked Java {@link URISyntaxException}.
 * 
 * @author Matthew Phillips
 */
public class InvalidURIException extends RuntimeException
{
  public InvalidURIException (String uri, String message)
  {
    super ("Invalid URI \"" + uri + "\": " + message);
  }

  public InvalidURIException (URISyntaxException ex)
  {
    super (ex);
  }
}
