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
package org.avis.subscription.ast;

import static org.avis.util.Text.className;

/**
 * Base class for math nodes that require integer arguments.
 * 
 * @author Matthew Phillips
 */
public abstract class MathIntParentNode extends MathParentNode
{
  public MathIntParentNode (Node child1,
                            Node child2)
  {
    super (child1, child2);
  }
  
  @Override
  public Class<?> evalType ()
  {
    return Integer.class;
  }
  
  @Override
  protected String validateChild (Node child)
  {
    Class<?> childType = child.evalType ();
    
    if (childType == Object.class)
    {
      // allow generic nodes such as fields
      return null; 
    } else if (!(Integer.class.isAssignableFrom (childType) ||
                 Long.class.isAssignableFrom (childType)))
    {
      return "\"" + expr () + "\" needs an integer as an argument (was " +
             className (child.evalType ()).toLowerCase () + ")";
    } else 
    {
      return null;
    }
  }
  
  @Override
  protected boolean validOperand (Object value)
  {
    return value instanceof Integer || value instanceof Long;
  }
  
  @Override
  protected double evaluateReal64 (double number1, double number2)
  {
    throw new UnsupportedOperationException ("Not applicable for Real64 values");
  }
}
