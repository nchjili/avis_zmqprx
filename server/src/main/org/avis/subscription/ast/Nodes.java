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

import java.util.List;

import java.io.StringWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.avis.subscription.ast.nodes.Or;
import org.avis.util.IndentingWriter;

/**
 * General utilities for node trees.
 */
public final class Nodes
{
  private Nodes ()
  {
    // zip
  }

  /**
   * Generate a pretty-printed version of a node tree suitable for
   * human consumption. Uses {@link Node#presentation()} to generate
   * text for each node.
   * 
   * @see #unparse(Node)
   */
  public static String toString (Node node)
  {
    IndentingWriter out = new IndentingWriter (new StringWriter ());
    
    print (out, node);
    
    return out.toString ();
  }

  private static void print (IndentingWriter out, Node node)
  {
    out.print (node.presentation ());
    
    if (node.hasChildren ())
    {
      out.indent ();
      
      for (Node child : node.children ())
      {
        out.println ();
        
        print (out, child);
      }
      
      out.unindent ();
    }
  }

  /**
   * "Unparse" an AST to a canonical S-expression-like string
   * expression useful for testing purposes. Uses the
   * {@link Node#expr()} method to unparse each node.
   * 
   * e.g. <code>field1 > 2 && field2 == 'hello there'</code> becomes
   * <code>(&& (> (field 'field1') (int32 2))
   * (== (field 'field2') (string 'hello there')))</code>
   * 
   * @see Node#expr()
   * @see #toString(Node)
   */
  public static String unparse (Node node)
  {
    StringBuilder str = new StringBuilder ();
    
    unparse (str, node);
    
    return str.toString ();
  }

  private static void unparse (StringBuilder str, Node node)
  {
    if (node.hasChildren ())
    {
      str.append ('(');
      
      str.append (node.expr ());

      for (Node child : node.children ())
      {
        str.append (' ');
       
        unparse (str, child);
      }

      str.append (')');
    } else
    {
      str.append (node.expr ());
    }
  }

  /**
   * Allow a node that usually operates on two arguments to optionally
   * operate on any number with an OR conjunction. For example, could
   * be used to turn <tt>begins-with (name, 'value1', 'value2')</tt>
   * into
   * <tt>begins-with (name, 'value1') || begins-with (name, 'value2')</tt>.
   * 
   * @param nodeType The node type to generate.
   * @param param1 The constructor's first parameter type.
   * @param param2 The constructor's second parameter type.
   * @param args A list of arguments for the node (must be >= 2). If
   *                these are longer than 2, then arg0 is paired with
   *                arg1..argN as children to an OR node.
   * @return Either an instance of T or an Or with T's as children.
   */
  public static <T extends Node> 
    Node createConjunction (Class<T> nodeType,
                            Class<?> param1,
                            Class<?> param2,
                            List<Node> args)
  {
    try
    {
      Constructor<T> nodeConstructor = 
        nodeType.getConstructor (param1, param2);
      
      if (args.size () == 2)
      {
        return nodeConstructor.newInstance (args.get (0), args.get (1));
      } else
      {
        Node arg0 = args.get (0);
        
        Or or = new Or ();
        
        for (int i = 1; i < args.size (); i++)
          or.addChild (nodeConstructor.newInstance (arg0, args.get (i)));
        
        return or;
      }
    } catch (InvocationTargetException ex)
    {
      if (ex.getCause () instanceof RuntimeException)
        throw (RuntimeException)ex.getCause ();
      else
        throw new Error (ex);
    } catch (Exception ex)
    {
      throw new Error (ex);
    }
  }
}

