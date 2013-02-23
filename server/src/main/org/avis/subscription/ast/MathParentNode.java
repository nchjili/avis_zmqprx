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

import java.util.Map;

import static org.avis.util.Text.className;
import static org.avis.util.Numbers.highestPrecision;
import static org.avis.util.Numbers.upconvert;

/**
 * Base class for mathematical operator nodes. Subclasses implement
 * the evaluateXXX () methods to add math operation implementations.
 * 
 * @author Matthew Phillips
 */
public abstract class MathParentNode extends ParentBiNode
{
  public MathParentNode (Node child1,
                         Node child2)
  {
    super (child1, child2);
  }

  @Override
  protected String validateChild (Node child)
  {
    Class<?> childType = child.evalType ();
    
    if (childType == Object.class)
    {
      // allow generic nodes such as fields    
      return null; 
    } else if (!Number.class.isAssignableFrom (childType))
    {
      return "\"" + expr () + "\" needs a number as an argument (was " +
             className (child.evalType ()).toLowerCase () + ")";
    } else
    {
      return null;
    }
  }
  
  @Override
  public Class<?> evalType ()
  {
    return Number.class;
  }
 
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Number number1 = evaluate (child1, attrs);
    
    if (number1 == null)
      return null;
    
    Number number2 = evaluate (child2, attrs);
    
    if (number2 == null)
      return null;
    
    Class<? extends Number> type1 = number1.getClass ();
    Class<? extends Number> type2 = number2.getClass ();
    
    // check if upconvert needed
    if (type1 != type2)
    {
      Class<? extends Number> newType = highestPrecision (type1, type2);
      
      if (type1 != newType)
        number1 = upconvert (number1, type2);
      else
        number2 = upconvert (number2, type1);
    }
    
    try
    {
      if (number1 instanceof Integer)
        return evaluateInt32 ((Integer)number1, (Integer)number2);
      else if (number1 instanceof Long)
        return evaluateInt64 ((Long)number1, (Long)number2);
      else
        return evaluateReal64 ((Double)number1, (Double)number2);
    } catch (ArithmeticException ex)
    {
      // e.g. div by zero. treat this as a bottom'ing condition
      return null;
    }
  }

  private Number evaluate (Node child,
                           Map<String, Object> attrs)
  {
    Object result = child.evaluate (attrs);
    
    if (result == null || !validOperand (result))
      return null;
    else
      return (Number)result;
  }

  /**
   * Test whether value is a valid operand for this operation.
   */
  protected boolean validOperand (Object value)
  {
    return value instanceof Number;
  }

  protected abstract int evaluateInt32 (int number1, int number2);
  
  protected abstract long evaluateInt64 (long number1, long number2);
  
  protected abstract double evaluateReal64 (double number1, double number2);
}
