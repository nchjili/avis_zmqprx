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

/**
 * Test whether a field is a given type. Can be used for int32(),
 * int64(), string(), etc functions.
 * 
 * @author Matthew Phillips
 */
public class Type extends NameParentNode
{
  public Class<?> type;

  public Type (Field field, Class<?> type)
  {
    this (field.fieldName (), type);
  }

  public Type (String field, Class<?> type)
  {
    super (field);
    
    this.type = type;
  }

  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object value = attrs.get (name);
    
    if (value == null)
      return BOTTOM;
    else
      return type == value.getClass ();
  }

  @Override
  public String expr ()
  {
    if (type == Integer.class)
      return "int32";
    else if (type == Long.class)
      return "int64";
    else if (type == Double.class)
      return "real64";
    else if (type == String.class)
      return "string";
    else if (type == byte [].class)
      return "opaque";
    else
      return type.getClass ().getName ();
  }
}
