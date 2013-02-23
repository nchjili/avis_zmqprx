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

import org.avis.subscription.ast.Node;

import static org.avis.util.Util.valuesEqual;

public class Const extends Node
{
  public static final Const CONST_FALSE = new Const (FALSE);
  public static final Const CONST_TRUE = new Const (TRUE);
  public static final Const CONST_BOTTOM = new Const (BOTTOM);

  public static final Const CONST_ZERO = new Const (0);
  
  private Object value;

  public static Const bool (Boolean value)
  {
    if (value == TRUE)
      return CONST_TRUE;
    else if (value == FALSE)
      return CONST_FALSE;
    else
      throw new IllegalArgumentException ("Invalid value: " + value);
  }
  
  public static Const string (String string)
  {
    return new Const (string);
  }
  
  public static Const int32 (int value)
  {
    if (value == 0)
      return CONST_ZERO;
    else
      return new Const (value);
  }
  
  public static Node int64 (long value)
  {
    return new Const (value);
  }
  
  public static Node real64 (double value)
  {
    return new Const (value);
  }
   
  public Const (Object value)
  {
    this.value = value;
  }
  
  @Override
  public boolean equals (Object obj)
  {
    return obj.getClass () == Const.class && 
           valuesEqual (((Const)obj).value, value);
  }
  
  @Override
  public int hashCode ()
  {
    return value == null ? 0 : value.hashCode ();
  }
  
  public Object value ()
  {
    return value;
  }

  @Override
  public Class<?> evalType ()
  {
    if (value == BOTTOM)
      return Boolean.class;
    else
      return value.getClass ();
  }

  @Override
  public Node inlineConstants ()
  {
    return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    return value;
  }

  @Override
  public String expr ()
  {
    if (value instanceof String)
      return "'" + value + "'";
    else if (value instanceof Number)
      return numExpr ();
    else
      return value.toString ();
  }

  @Override
  public String presentation ()
  {
    if (value instanceof String)
      return "Str: \"" + value + '\"';
    else if (value instanceof Number)
      return numPresentation ();
    else
      return "Const: " + value;
  }

  private String numPresentation ()
  {
    StringBuilder str = new StringBuilder ("Num: ");
    
    str.append (value);
    
    if (value instanceof Long)
      str.append ('L');
    else if (value instanceof Double)
      str.append (" (double)");
      
    return str.toString ();
  }
  
  private String numExpr ()
  {
    StringBuilder str = new StringBuilder ();
    
    str.append (value);

    if (value instanceof Long)
      str.append ('L');
    
    return str.toString ();
  }
}
