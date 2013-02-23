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
import java.util.Map;

import org.avis.subscription.ast.Node;

import static java.util.Collections.singleton;

public class Not extends Node
{
  private Node child;
  
  public Not (Node child)
  {
    this.child = child;
  }

  @Override
  public String presentation ()
  {
    return "Not";
  }
  
  @Override
  public String expr ()
  {
    return "!";
  }
  
  @Override
  public Node inlineConstants ()
  {
    child = child.inlineConstants ();
    
    Boolean result = (Boolean)evaluate (EMPTY_NOTIFICATION);
    
    if (result != BOTTOM)
      return Const.bool (result);
    else
      return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Boolean result = (Boolean)child.evaluate (attrs);
    
    if (result == BOTTOM)
      return BOTTOM;
    else
      return !result;
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return singleton (child);
  }
  
  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
}
