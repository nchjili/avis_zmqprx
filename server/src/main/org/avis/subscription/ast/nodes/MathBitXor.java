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

import org.avis.subscription.ast.MathIntParentNode;
import org.avis.subscription.ast.Node;

public class MathBitXor extends MathIntParentNode
{
  public MathBitXor (Node child1,
                     Node child2)
  {
    super (child1, child2);
  }
  
  @Override
  public String expr ()
  {
    return "^";
  }
  
  @Override
  protected int evaluateInt32 (int number1, int number2)
  {
    return number1 ^ number2;
  }

  @Override
  protected long evaluateInt64 (long number1, long number2)
  {
    return number1 ^ number2;
  }
}
