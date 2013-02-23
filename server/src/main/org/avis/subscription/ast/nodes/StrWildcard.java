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

import java.util.List;
import java.util.regex.Pattern;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.StringCompareNode;

import static org.avis.subscription.ast.Nodes.createConjunction;
import static org.avis.util.Wildcard.toPattern;

/**
 * NOTE: no real spec for wildcard exists. This implements ? and *
 * only, which is what is supported by Mantara elvind 4.4.0.
 * 
 * @author Matthew Phillips
 */
public class StrWildcard extends StringCompareNode
{
  private Pattern wildcard;

  /**
   * Create from a list of arguments.
   */
  public static Node create (List<Node> args)
  {
    return createConjunction (StrWildcard.class, Node.class, Const.class, args);
  }
  
  public StrWildcard (Node stringExpr, Const stringConst)
  {
    super (stringExpr, stringConst);
    
    this.wildcard = toPattern (string);
  }

  @Override
  public String expr ()
  {
    return "wildcard";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return wildcard.matcher (string1).matches ();
  }
}
