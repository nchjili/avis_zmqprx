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

public class StrFoldCase extends Node
{
  private Node stringExpr;
  
  public StrFoldCase (Node stringExpr)
  {
    this.stringExpr = stringExpr;
  }
  
  @Override
  public Class<?> evalType ()
  {
    return String.class;
  }

  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    String result = (String)stringExpr.evaluate (attrs);
    
    return result == null ? null : result.toLowerCase ();
  }

  @Override
  public String expr ()
  {
    return "fold-case";
  }
  
  @Override
  public String presentation ()
  {
    return name (); 
  }

  @Override
  public Node inlineConstants ()
  {
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    return result == null ? this : new Const (result);
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return singleton (stringExpr);
  }
}
