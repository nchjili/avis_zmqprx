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

import java.util.Collection;

/**
 * Base class for parent nodes that evaluate to boolean from boolean
 * children.
 * 
 * @author Matthew Phillips
 */
public abstract class BoolParentNode
  extends ParentNode
{
  public BoolParentNode ()
  {
    // zip
  }

  public BoolParentNode (Node node1)
  {
    super (node1);
  }

  public BoolParentNode (Node node1,
                         Node node2)
  {
    super (node1, node2);
  }

  public BoolParentNode (Node... children)
  {
    super (children);
  }
  
  public BoolParentNode (Collection<? extends Node> children)
  {
    super (children);
  }

  @Override
  public String presentation ()
  {
    return name (); 
  }

  @Override
  public Class<?> evalType ()
  {
    return Boolean.class;
  }
  
  @Override
  public String validateChild (Node child)
  {
    if (child.evalType () != Boolean.class)
      return expr () + " requires boolean arguments (" + child.expr () + ")";
    else
      return null;
  }
}
