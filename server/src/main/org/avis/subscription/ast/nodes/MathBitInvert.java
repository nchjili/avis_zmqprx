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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.avis.subscription.ast.IllegalChildException;
import org.avis.subscription.ast.Node;

public class MathBitInvert extends Node
{
  private Node child;

  public MathBitInvert (Node child)
    throws IllegalChildException
  {
    Class<?> childType = child.evalType ();
    
    if (childType != Object.class &&
        !(childType == Integer.class || child.evalType () == Long.class))
    {
      throw new IllegalChildException
        ("~ requires an integer argument", this, child);
    }
    
    this.child = child;
  }
  
  @Override
  public String expr ()
  {
    return "~";
  }
  
  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public Class<?> evalType ()
  {
    return child.evalType ();
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return Collections.singleton (child);
  }
  
  @Override
  public Node inlineConstants ()
  {
    child = child.inlineConstants ();
    
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != null)
      return new Const (result);
    else
      return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Object result = child.evaluate (attrs);
    
    if (result instanceof Integer)
      return (Integer)result ^ 0xFFFFFFFF;
    else if (result instanceof Long)
      return (Long)result ^ 0xFFFFFFFFFFFFFFFFL;
    else
      return BOTTOM;
  }
}
