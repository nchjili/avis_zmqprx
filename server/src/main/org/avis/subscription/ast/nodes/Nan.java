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
package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.NameParentNode;

public class Nan extends NameParentNode
{
  public Nan (Field field)
  {
    this (field.fieldName ());
  }

  public Nan (String field)
  {
    super (field);
  }

  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
  
  @Override
  public String expr ()
  {
    return "nan";
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object value = attrs.get (name);
    
    if (!(value instanceof Double))
      return BOTTOM;
    else
      return ((Double)value).isNaN ();
  }
}
