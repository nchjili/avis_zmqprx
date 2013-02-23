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
import java.util.regex.PatternSyntaxException;

import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.StringCompareNode;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import static org.avis.subscription.ast.Nodes.createConjunction;

public class StrRegex extends StringCompareNode
{
  private Pattern regex;

  /**
   * Create from a list of arguments.
   */
  public static Node create (List<Node> args)
  {
    return createConjunction (StrRegex.class, Node.class, Const.class, args);
  }
  
  public StrRegex (Node stringExpr, Const stringConst)
    throws PatternSyntaxException
  {
    super (stringExpr, stringConst);
    
    this.regex = compile (string, DOTALL);
  }

  @Override
  public String expr ()
  {
    return "regex";
  }
  
  @Override
  protected boolean evaluate (String string1, String string2)
  {
    return regex.matcher (string1).find ();
  }
}
