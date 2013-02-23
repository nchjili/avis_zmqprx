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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import static org.avis.federation.io.XdrAstType.OPAQUE;
import static org.avis.federation.io.XdrAstType.OR;
import static org.avis.federation.io.XdrAstType.REAL64;
import static org.avis.federation.io.XdrAstType.REGEX;
import static org.avis.federation.io.XdrAstType.REQUIRE;
import static org.avis.federation.io.XdrAstType.SHIFT_LEFT;
import static org.avis.federation.io.XdrAstType.SHIFT_RIGHT;
import static org.avis.federation.io.XdrAstType.SIZE;
import static org.avis.federation.io.XdrAstType.STRING;
import static org.avis.federation.io.XdrAstType.SUBTRACT;
import static org.avis.federation.io.XdrAstType.UNARY_MINUS;
import static org.avis.federation.io.XdrAstType.WILDCARD;
import static org.avis.federation.io.XdrAstType.XOR;
import static org.avis.io.XdrCoding.TYPE_INT32;
import static org.avis.io.XdrCoding.TYPE_INT64;
import static org.avis.io.XdrCoding.TYPE_REAL64;
import static org.avis.io.XdrCoding.TYPE_STRING;
import static org.avis.io.XdrCoding.putString;
import static org.avis.util.Text.className;

/**
 * Functions for encoding/decoding Node-based AST's into the Elvin XDR
 * wire format.
 * 
 * @author Matthew Phillips
 */
public final class XdrAstCoding
{
  private static Map<Class<? extends Node>, Integer> typeCodes;
  
  static
  {
    typeCodes = new HashMap<Class<? extends Node>, Integer> ();
    
    typeCodes.put (And.class, AND);
    typeCodes.put (MathBitAnd.class, BIT_AND);
    typeCodes.put (MathBitInvert.class, BIT_NEGATE);
    typeCodes.put (MathBitLogShiftRight.class, LOGICAL_SHIFT_RIGHT);
    typeCodes.put (MathBitOr.class, BIT_OR);
    typeCodes.put (MathBitShiftLeft.class, SHIFT_LEFT);
    typeCodes.put (MathBitShiftRight.class, SHIFT_RIGHT);
    typeCodes.put (MathBitXor.class, BIT_XOR);
    typeCodes.put (MathDiv.class, DIVIDE);
    typeCodes.put (MathMinus.class, SUBTRACT);
    typeCodes.put (MathMod.class, MODULO);
    typeCodes.put (MathMult.class, MULTIPLY);
    typeCodes.put (MathPlus.class, ADD);
    typeCodes.put (MathUnaryMinus.class, UNARY_MINUS);
    typeCodes.put (Nan.class, NAN);
    typeCodes.put (Not.class, NOT);
    typeCodes.put (Or.class, OR);
    typeCodes.put (Require.class, REQUIRE);
    typeCodes.put (Size.class, SIZE);
    typeCodes.put (StrBeginsWith.class, BEGINS_WITH);
    typeCodes.put (StrContains.class, CONTAINS);
    typeCodes.put (StrEndsWith.class, ENDS_WITH);
    typeCodes.put (StrFoldCase.class, FOLD_CASE);
    typeCodes.put (StrRegex.class, REGEX);
    typeCodes.put (StrWildcard.class, WILDCARD);
    typeCodes.put (Xor.class, XOR);
  }
  
  private XdrAstCoding ()
  {
    // zip
  }
  
  /**
   * Encode an AST in Elvin XDR format.
   * 
   * @param out The buffer to encode to.
   * @param node The root of the AST.
   * 
   * @see #decodeAST(ByteBuffer)
   */
  public static void encodeAST (ByteBuffer out, Node node)
    throws ProtocolCodecException
  {
    if (node instanceof Const)
    {
      encodeConst (out, (Const)node);
    } else if (node instanceof Field)
    {
      out.putInt (NAME);
      out.putInt (TYPE_STRING);
      putString (out, ((Field)node).fieldName ());
    } else
    {
      out.putInt (typeCodeFor (node));
      out.putInt (0); // composite node base type is 0
      
      Collection<Node> children = node.children ();
      
      out.putInt (children.size ());
      
      for (Node child : children)
        encodeAST (out, child);
    }
  }
  
  /**
   * Generate the AST type code for a node, taking into account cases
   * where there is not a 1-1 mapping from Node -> Elvin AST node type.
   */
  private static int typeCodeFor (Node node)
  {
    if (node instanceof Compare)
    {
      Compare compare = (Compare)node;
      
      switch (compare.inequality)
      {
        case 0:
          return F_EQUALS;
        case -1:
          return compare.equality ? LESS_THAN_EQUALS : LESS_THAN;
        case 1:
          return compare.equality ? GREATER_THAN_EQUALS : GREATER_THAN;
        default:
          throw new Error ();
      }
    } else if (node instanceof Type)
    {
      Class<?> type = ((Type)node).type;
      
      if (type == String.class)
        return STRING;
      else if (type == Integer.class)
        return INT32;
      else if (type == Long.class)
        return INT64;
      else if (type == Double.class)
        return REAL64;
      else if (type == byte [].class)
        return OPAQUE;
      else
        throw new Error ();
    } else if (node instanceof StrUnicodeDecompose)
    {
      StrUnicodeDecompose decompose = (StrUnicodeDecompose)node;
      
      return decompose.mode == StrUnicodeDecompose.Mode.DECOMPOSE ?
               DECOMPOSE : DECOMPOSE_COMPAT;
    } else
    {
      // this will NPE if we hit an unmapped type
      return typeCodes.get (node.getClass ());
    }
  }

  /**
   * Encode a constant value (leaf) node.
   */
  private static void encodeConst (ByteBuffer out, Const node)
    throws ProtocolCodecException
  {
    Object value = node.value ();
    Class<?> type = value.getClass ();

    if (type == String.class)
    {
      out.putInt (CONST_STRING);
      out.putInt (TYPE_STRING);
      putString (out, (String)value);
    } else if (type == Integer.class)
    {
      out.putInt (CONST_INT32);
      out.putInt (TYPE_INT32);
      out.putInt ((Integer)value);
    } else if (type == Long.class)
    {
      out.putInt (CONST_INT64);
      out.putInt (TYPE_INT64);
      out.putLong ((Long)value);
    } else if (type == Double.class)
    {
      out.putInt (CONST_REAL64);
      out.putInt (TYPE_REAL64);
      out.putDouble ((Double)value);
    } else if (type == Boolean.class)
    {
      if ((Boolean)node.value () == false)
        out.putInt (EMPTY);
      else
        throw new ProtocolCodecException ("Cannot encode TRUE in AST");
    } else
    {
      throw new ProtocolCodecException ("Cannot encode constant type " + 
                                        className (type));
    }
  }

  /**
   * Decode an XDR-encoded AST into a Node-based AST.
   * 
   * @param in The buffer to read from.
   * 
   * @return The root of the AST.
   * 
   * @throws ProtocolCodecException if an error occurred reading the tree.
   * 
   * @see #encodeAST(ByteBuffer, Node)
   */
  public static Node decodeAST (ByteBuffer in)
    throws ProtocolCodecException
  {
    try
    {
      return new XdrAstParser (in).expr ();
    } catch (IllegalChildException ex)
    {
      throw new ProtocolCodecException ("Invalid AST: " + ex.getMessage ());
    }
  }
}
