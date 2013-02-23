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

import org.avis.subscription.ast.nodes.Field;

import static java.util.Collections.singleton;

/**
 * Base class for nodes that have a field name as their primary child
 * parameter and whose result is derived from that.
 * 
 * @author Matthew Phillips
 */
public abstract class NameParentNode extends Node
{
  public String name;
  
  public NameParentNode (Field field)
  {
    this (field.fieldName ());
  }
  
  public NameParentNode (String name)
  {
    this.name = name;
  }

  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public Node inlineConstants ()
  {
    // name-based nodes will not be inline-able
    return this;
  }

  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return singleton ((Node)new Field (name));
  }
}
