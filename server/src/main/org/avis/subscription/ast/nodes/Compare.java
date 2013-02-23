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

import java.util.List;
import java.util.Map;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.ParentBiNode;

import static org.avis.util.Text.className;
import static org.avis.util.Numbers.highestPrecision;
import static org.avis.util.Numbers.upconvert;

/**
 * Comparison operator that can implement equals, greater than, less
 * than and any combination of those.
 * <pre>
 *              inequality      equality
 *  -------------------------------------
 *   ==          0              true
 *   <          -1              false
 *   <=         -1              true
 *   >           1              false
 *   >=          1              true
 * </pre>
 * 
 * @author Matthew Phillips
 */
public class Compare extends ParentBiNode
{
  public int inequality;
  public boolean equality;
  
  /**
   * Create an "==" compare node from a list of comparable children
   * (size >= 2). If more than two children, generates an OR wrapper.
   */
  public static Node createEquals (List<Node> args)
  {
    if (args.size () < 2)
    {
      throw new IllegalArgumentException ("Two or more children required");
    } else if (args.size () == 2)
    {
      return new Compare (args.get (0), args.get (1), 0, true);
    } else
    {
      Node arg0 = args.get (0);
      
      Or or = new Or ();
      
      for (int i = 1; i < args.size (); i++)
        or.addChild (new Compare (arg0, args.get (i), 0, true));
      
      return or;
    }
  }
  
  /**
   * Create a new instance. e.g. Compare (n1, n2, 1, true) => n1 >=
   * n2. Compare (n1, n2, -1, false) => n1 &lt; n2. Compare (n1, n2,
   * 0, true) => n1 == n2.
   * 
   * @param child1 Left operand.
   * @param child2 Right operand.
   * @param inequality > 0 => true if left > right, &lt; 0 => true if left
   *          &lt; right.
   * @param equality True => true if equal.
   */
  public Compare (Node child1,
                  Node child2,
                  int inequality, boolean equality)
  {
    this.inequality = inequality;
    this.equality = equality;
  
    init (child1, child2);
  }

  @Override
  protected String validateChild (Node child)
  {
    Class<?> childType = child.evalType ();
    
    if (childType == Object.class)
    {
      // allow generic nodes such as fields
      return null;
    } else if (childType != Number.class &&
               (childType == Boolean.class ||
                !Comparable.class.isAssignableFrom (childType)))
    {
      return expr () + " cannot have expression of type " +
             className (childType) + " as an argument";
    } else if (child1 != null)
    {
      Class<?> evalType = child1.evalType ();
    
      if (evalType != Object.class &&
          evalType != childType)
      {
        if (!(Number.class.isAssignableFrom (evalType) &&
              Number.class.isAssignableFrom (childType)))
        {
          return expr () + ": argument ("  +
                 child.expr () + ") cannot be compared to " +
                 "(" + child1.expr () + ")";
        }
      }
    }   
    
    return null;
  }
  
  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }

  @Override
  public String expr ()
  {
    StringBuilder str = new StringBuilder ();
    
    if (inequality < 0)
      str.append ('<');
    else if (inequality > 0)
      str.append ('>');
    
    if (equality)
    {
      if (str.length () == 0)
        str.append ("==");
      else
        str.append ('=');
    }
    
    return str.toString ();
  }
  
  @Override
  public String presentation ()
  {
    return "Compare: " + expr ();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object evaluate (Map<String, Object> attrs)
  {
    Object result1 = child1.evaluate (attrs);
    
    if (!(result1 instanceof Comparable))
      return BOTTOM;

    Object result2 = child2.evaluate (attrs);
    
    if (!(result2 instanceof Comparable))
      return BOTTOM;
    
    Class class1 = result1.getClass ();
    Class class2 = result2.getClass ();
    
    // check for compatible types
    if (class1 != class2)
    {
      // if numeric, can upconvert
      if (class1.getSuperclass () == Number.class &&
          class2.getSuperclass () == Number.class)
      {
        Class newType = highestPrecision (class1, class2);
        
        if (class1 != newType)
          result1 = upconvert ((Number)result1, class2);
        else
          result2 = upconvert ((Number)result2, class1);
      } else
      {
        // incompatible types
        return BOTTOM;
      }
    }
    
    int compare = ((Comparable)result1).compareTo (result2);
    
    if (compare == 0)
      return equality;
    else if (compare < 0)
      return inequality < 0;
    else
      return inequality > 0;
  }
}
