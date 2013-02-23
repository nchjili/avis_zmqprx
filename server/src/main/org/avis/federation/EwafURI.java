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
package org.avis.federation;

import org.avis.common.ElvinURI;
import org.avis.common.InvalidURIException;

import static org.avis.federation.Federation.DEFAULT_EWAF_PORT;
import static org.avis.federation.Federation.VERSION_MAJOR;
import static org.avis.federation.Federation.VERSION_MINOR;

/**
 * A URI specifying an Elvin wide-area federation endpoint.
 * 
 * @author Matthew Phillips
 */
public class EwafURI extends ElvinURI
{
  public EwafURI (String uri)
    throws InvalidURIException
  {
    super (uri);
  }
  
  @Override
  protected boolean validScheme (String schemeToCheck)
  {
    return schemeToCheck.equals ("ewaf");
  }
  
  @Override
  protected void init ()
  {
    super.init ();
    
    this.versionMajor = VERSION_MAJOR;
    this.versionMinor = VERSION_MINOR;
    this.port = DEFAULT_EWAF_PORT;
  }
}
