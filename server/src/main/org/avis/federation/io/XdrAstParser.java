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
package org.avis.federation.io;

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import org.avis.subscription.ast.IllegalChildException;
import org.avis.subscription.ast.Node;
import org.avis.subscription.ast.nodes.And;
import org.avis.subscription.ast.nodes.Compare;
import org.avis.subscription.ast.nodes.Const;
import org.avis.subscription.ast.nodes.Field;
import org.avis.subscription.ast.nodes.MathBitAnd;
import org.avis.subscription.ast.nodes.MathBitInvert;
import org.avis.subscription.ast.nodes.MathBitLogShiftRight;
import org.avis.subscription.ast.nodes.MathBitOr;
import org.avis.subscription.ast.nodes.MathBitShiftLeft;
import org.avis.subscription.ast.nodes.MathBitShiftRight;
import org.avis.subscription.ast.nodes.MathBitXor;
import org.avis.subscription.ast.nodes.MathDiv;
import org.avis.subscription.ast.nodes.MathMinus;
import org.avis.subscription.ast.nodes.MathMod;
import org.avis.subscription.ast.nodes.MathMult;
import org.avis.subscription.ast.nodes.MathPlus;
import org.avis.subscription.ast.nodes.MathUnaryMinus;
import org.avis.subscription.ast.nodes.Nan;
import org.avis.subscription.ast.nodes.Not;
import org.avis.subscription.ast.nodes.Or;
import org.avis.subscription.ast.nodes.Require;
import org.avis.subscription.ast.nodes.Size;
import org.avis.subscription.ast.nodes.StrBeginsWith;
import org.avis.subscription.ast.nodes.StrContains;
import org.avis.subscription.ast.nodes.StrEndsWith;
import org.avis.subscription.ast.nodes.StrFoldCase;
import org.avis.subscription.ast.nodes.StrRegex;
import org.avis.subscription.ast.nodes.StrUnicodeDecompose;
import org.avis.subscription.ast.nodes.StrWildcard;
import org.avis.subscription.ast.nodes.Type;
import org.avis.subscription.ast.nodes.Xor;

import static org.avis.federation.io.XdrAstType.ADD;
import static org.avis.federation.io.XdrAstType.AND;
import static org.avis.federation.io.XdrAstType.BEGINS_WITH;
import static org.avis.federation.io.XdrAstType.BIT_AND;
import static org.avis.federation.io.XdrAstType.BIT_NEGATE;
import static org.avis.federation.io.XdrAstType.BIT_OR;
import static org.avis.federation.io.XdrAstType.BIT_XOR;
import static org.avis.federation.io.XdrAstType.CONST_INT32;
import static org.avis.federation.io.XdrAstType.CONST_INT64;
import static org.avis.federation.io.XdrAstType.CONST_REAL64;
import static org.avis.federation.io.XdrAstType.CONST_STRING;
import static org.avis.federation.io.XdrAstType.CONTAINS;
import static org.avis.federation.io.XdrAstType.DECOMPOSE;
import static org.avis.federation.io.XdrAstType.DECOMPOSE_COMPAT;
import static org.avis.federation.io.XdrAstType.DIVIDE;
import static org.avis.federation.io.XdrAstType.EMPTY;
import static org.avis.federation.io.XdrAstType.ENDS_WITH;
import static org.avis.federation.io.XdrAstType.EQUALS;
import static org.avis.federation.io.XdrAstType.FOLD_CASE;
import static org.avis.federation.io.XdrAstType.F_EQUALS;
import static org.avis.federation.io.XdrAstType.GREATER_THAN;
import static org.avis.federation.io.XdrAstType.GREATER_THAN_EQUALS;
import static org.avis.federation.io.XdrAstType.INT32;
import static org.avis.federation.io.XdrAstType.INT64;
import static org.avis.federation.io.XdrAstType.LESS_THAN;
import static org.avis.federation.io.XdrAstType.LESS_THAN_EQUALS;
import static org.avis.federation.io.XdrAstType.LOGICAL_SHIFT_RIGHT;
import static org.avis.federation.io.XdrAstType.MODULO;
import static org.avis.federation.io.XdrAstType.MULTIPLY;
import static org.avis.federation.io.XdrAstType.NAME;
import static org.avis.federation.io.XdrAstType.NAN;
import static org.avis.federation.io.XdrAstType.NOT;
import static org.avis.federation.io.XdrAstType.NOT_EQUALS;
import static org.avis.federation.io.XdrAstType.OPAQUE;
import static org.avis.federation.io.XdrAstType.OR;
import static org.avis.federation.io.XdrAstType.REAL64;
import static org.avis.federation.io.XdrAstType.REGEX;
import static org.avis.federation.io.XdrAstType.REGEXP;
import static org.avis.federation.io.XdrAstType.REQUIRE;
import static org.avis.federation.io.XdrAstType.SHIFT_LEFT;
import static org.avis.federation.io.XdrAstType.SHIFT_RIGHT;
import static org.avis.federation.io.XdrAstType.SIZE;
import static org.avis.federation.io.XdrAstType.STRING;
import static org.avis.federation.io.XdrAstType.SUBTRACT;
import static org.avis.federation.io.XdrAstType.UNARY_MINUS;
import static org.avis.federation.io.XdrAstType.UNARY_PLUS;
import static org.avis.federation.io.XdrAstType.WILDCARD;
import static org.avis.federation.io.XdrAstType.XOR;
import static org.avis.io.XdrCoding.TYPE_INT32;
import static org.avis.io.XdrCoding.TYPE_INT64;
import static org.avis.io.XdrCoding.TYPE_REAL64;
import static org.avis.io.XdrCoding.TYPE_STRING;
import static org.avis.io.XdrCoding.getString;
import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.util.Text.className;

/**
 * Parser class for translating XDR-encoded AST's into Node-based AST's.
 * 
 * @see XdrAstCoding#decodeAST(ByteBuffer)
 *
 * @author Matthew Phillips
 */
class XdrAstParser
{
  private ByteBuffer in;

  public XdrAstParser (ByteBuffer in)
  {
    this.in = in;
  }

  /**
   * Read any AST expression.
   * 
   * @return The root node of the expression.
   * 
   * @throws ProtocolCodecException if an error occurs in decoding the
   *                 AST.
   * @throws IllegalChildException if a child in the AST is not a
   *                 valid type, i.e. AST is syntactically invalid.
   */
  public Node expr ()
    throws ProtocolCodecException, IllegalChildException
  {
    int type = in.getInt ();
    
    if (type == EMPTY)
      return CONST_FALSE;
    
    int leafType = in.getInt ();
    
    // sanity check leaf type for parent nodes
    if (type > CONST_STRING && leafType != 0)
    {
      throw new ProtocolCodecException
        ("Invalid leaf type for parent node: " + leafType);
    }
    
    switch (type) 
    {
      case NAME:
      case CONST_INT32:
      case CONST_INT64:
      case CONST_REAL64:
      case CONST_STRING:
        return leaf (type, leafType);
      case REGEXP:
        return StrRegex.create (children ());
      case EQUALS:
        return new Compare (binary ().expr (), expr (),  0, true);
      case NOT_EQUALS:
        return new Not (new Compare (binary ().expr (), expr (),  0, true));
      case LESS_THAN:
        return new Compare (binary ().expr (), expr (), -1, false);
      case LESS_THAN_EQUALS:
        return new Compare (binary ().expr (), expr (), -1, true);
      case GREATER_THAN:
        return new Compare (binary ().expr (), expr (),  1, false);
      case GREATER_THAN_EQUALS:
        return new Compare (binary ().expr (), expr (),  1, true);
      case OR:
        return new Or (children ());
      case XOR:
        return new Xor (children ());
      case AND:
        return new And (children ());
      case NOT:
        return new Not (single ().expr ());
      case UNARY_PLUS:
        return single ().expr ();
      case UNARY_MINUS:
        return new MathUnaryMinus (single ().expr ());
      case MULTIPLY:
        return new MathMult (binary ().expr (), expr ());
      case DIVIDE:
        return new MathDiv (binary ().expr (), expr ());
      case MODULO:
        return new MathMod (binary ().expr (), expr ());
      case ADD:
        return new MathPlus (binary ().expr (), expr ());
      case SUBTRACT:
        return new MathMinus (binary ().expr (), expr ());
      case SHIFT_LEFT:
        return new MathBitShiftLeft (binary ().expr (), expr ());
      case SHIFT_RIGHT:
        return new MathBitShiftRight (binary ().expr (), expr ());
      case LOGICAL_SHIFT_RIGHT:
        return new MathBitLogShiftRight (binary ().expr (), expr ());
      case BIT_AND:
        return new MathBitAnd (binary ().expr (), expr ());
      case BIT_XOR:
        return new MathBitXor (binary ().expr (), expr ());
      case BIT_OR:
        return new MathBitOr (binary ().expr (), expr ());
      case BIT_NEGATE:
        return new MathBitInvert (single ().expr ());
      case INT32:
        return new Type (single ().field (), Integer.class);
      case INT64:
        return new Type (single ().field (), Long.class);
      case REAL64:
        return new Type (single ().field (), Double.class);
      case STRING:
        return new Type (single ().field (), String.class);
      case OPAQUE:
        return new Type (single ().field (), byte [].class);
      case NAN:
        return new Nan (single ().field ());
      case BEGINS_WITH:
        return StrBeginsWith.create (children ());
      case CONTAINS:
        return StrContains.create (children ());
      case ENDS_WITH:
        return StrEndsWith.create (children ());
      case WILDCARD:
        return StrWildcard.create (children ());
      case REGEX:
        return StrRegex.create (children ());
      case FOLD_CASE:
        return new StrFoldCase (single ().expr ());
      case DECOMPOSE:
        return new StrUnicodeDecompose
          (single ().expr (), StrUnicodeDecompose.Mode.DECOMPOSE);
      case DECOMPOSE_COMPAT:
        return new StrUnicodeDecompose
          (single ().expr (), StrUnicodeDecompose.Mode.DECOMPOSE_COMPAT);
      case REQUIRE:
        return new Require (single ().field ());
      case F_EQUALS:
        return Compare.createEquals (children ());
      case SIZE:
        return new Size (single ().field ());
      default:
        throw new ProtocolCodecException 
          ("Unknown AST node type: " + type);
    }
  }

  /**
   * Assert that a single child is found and return this, otherwise
   * throw an exception. Used as a predicate for single-child nodes.
   */
  private XdrAstParser single ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    if (count == 1)
      return this;
    else
      throw new ProtocolCodecException 
        ("Expected single child, found " + count);
  }
  
  /**
   * Assert that two children are found and return this, otherwise
   * throw an exception. Used as a predicate for binary nodes.
   */
  private XdrAstParser binary ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    if (count == 2)
      return this;
    else
      throw new ProtocolCodecException 
        ("Expected two children, found " + count);
  }
  
  /**
   * Read a list of child nodes of any length.
   */
  private List<Node> children ()
    throws ProtocolCodecException
  {
    int count = in.getInt ();
    
    if (count < 0)
      throw new ProtocolCodecException 
        ("Child count cannot be negative: " + count);
    
    List<Node> children = new ArrayList<Node> (count);
    
    for ( ; count > 0; count--)
      children.add (expr ());
    
    return children;
  }
  
  private Field field ()
    throws ProtocolCodecException
  {
    Node node = expr ();
    
    if (node instanceof Field)
      return (Field)node;
    else
      throw new ProtocolCodecException ("Field node required, found " + 
                                        className (node));
  }
  
  /**
   * Read a leaf node of a given type and leaf type.
   * 
   * @param type The node type.
   * @param leafType The type of value contained in the leaf: must be
   *                one of the XdrCoding.TYPE_* values.
   * @return The node.
   * 
   * @throws ProtocolCodecException if the node was not valid constant
   *                 node of the specified type.
   */
  private Node leaf (int type, int leafType)
    throws ProtocolCodecException
  {
    switch (type)
    {
      case NAME:
        assertLeafType (leafType, TYPE_STRING);
        return new Field (getString (in));
      case CONST_STRING:
        assertLeafType (leafType, TYPE_STRING);
        return new Const (getString (in));
      case CONST_INT32:
        assertLeafType (leafType, TYPE_INT32);
        return new Const (in.getInt ());
      case CONST_INT64:
        assertLeafType (leafType, TYPE_INT64);
        return new Const (in.getLong ());
      case CONST_REAL64:
        assertLeafType (leafType, TYPE_REAL64);
        return new Const (in.getDouble ());
      default:
        throw new Error ();
    }
  }

  /**
   * Check that a node type matches an actual required value.
   */
  private static void assertLeafType (int required, int actual)
    throws ProtocolCodecException
  {
    if (required != actual)
    {
      throw new ProtocolCodecException 
        ("Leaf node has incorrect value type: " + actual);
    }
  }
}
