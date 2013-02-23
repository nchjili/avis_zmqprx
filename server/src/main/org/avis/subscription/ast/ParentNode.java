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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Base class for nodes with an arbitrary number of children.
 * 
 * @author Matthew Phillips
 */
public abstract class ParentNode extends Node
{
  protected ArrayList<Node> children;
  
  public ParentNode ()
  {
    this.children = new ArrayList<Node> (2);
  }

  public ParentNode (Node node1)
  {
    this ();
    
    addChild (node1);
  }
  
  public ParentNode (Node node1, Node node2)
    throws IllegalChildException
  {
    this ();
    
    addChild (node1);
    addChild (node2);
  }

  /**
   * Add any number of children
   */
  public ParentNode (Node ...children)
  {
    this.children = new ArrayList<Node> (children.length);
    
    for (Node child : children)
      addChild (child);
  }
  
  /**
   * Add any number of children
   */
  public ParentNode (Collection<? extends Node> children)
  {
    this.children = new ArrayList<Node> (children.size ());
    
    for (Node child : children)
      addChild (child);
  }

  /**
   * Called by {@link #addChild(Node)} to validate the child is valid
   * as a child.
   * 
   * @param child
   * 
   * @return An error message if not valid, null if child is OK to be
   *         added.
   */
  protected abstract String validateChild (Node child);
  
  public void addChild (Node child)
    throws IllegalChildException
  {
    String error = validateChild (child);
    
    if (error == null)
      children.add (child);
    else
      throw new IllegalChildException (error, this, child);
  }
  
  @Override
  public boolean hasChildren ()
  {
    return !children.isEmpty ();
  }
  
  @Override
  public Collection<Node> children ()
  {
    return children;
  }
}
