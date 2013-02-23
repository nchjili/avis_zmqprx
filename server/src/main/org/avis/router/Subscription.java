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
package org.avis.router;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import java.io.StringReader;

import org.avis.security.Keys;
import org.avis.subscription.ast.Node;
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParser;

import static org.avis.security.DualKeyScheme.Subset.CONSUMER;
import static org.avis.subscription.ast.Node.TRUE;

/**
 * Represents a client's subscription.
 *  
 * @author Matthew Phillips
 */
class Subscription
{
  private static final AtomicLong idCounter = new AtomicLong ();
  
  public long id;
  public String expr;
  public boolean acceptInsecure;
  public Keys keys;

  private Node ast;

  public Subscription (String expr, Keys keys, boolean acceptInsecure)
    throws ParseException
  {
    this.expr = expr;
    this.keys = keys;
    this.acceptInsecure = acceptInsecure;
    this.ast = parse (expr);
    this.id = nextId ();
    
    keys.hashPrivateKeysForRole (CONSUMER);
  }

  public void updateExpression (String subscriptionExpr)
    throws ParseException
  {
    ast = parse (subscriptionExpr);
    expr = subscriptionExpr;
  }
  
  public boolean matches (Map<String, Object> attributes)
  {
    return ast.evaluate (attributes) == TRUE;
  }
  
  private static Node parse (String expr)
    throws ParseException
  {
    return new SubscriptionParser (new StringReader (expr)).parseAndValidate ();
  }

  private static long nextId ()
  {
    return idCounter.incrementAndGet ();
  }
}
