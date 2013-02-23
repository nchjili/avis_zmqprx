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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.StringReader;

import java.lang.reflect.Constructor;

import org.junit.Test;

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
import org.avis.subscription.parser.ParseException;
import org.avis.subscription.parser.SubscriptionParser;

import static java.lang.Double.NaN;

import static org.avis.subscription.ast.Node.BOTTOM;
import static org.avis.subscription.ast.Node.EMPTY_NOTIFICATION;
import static org.avis.subscription.ast.Node.FALSE;
import static org.avis.subscription.ast.Node.TRUE;
import static org.avis.subscription.ast.nodes.StrUnicodeDecompose.Mode.DECOMPOSE;
import static org.avis.subscription.ast.nodes.StrUnicodeDecompose.Mode.DECOMPOSE_COMPAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the evaluation of AST's.
 * 
 * @author Matthew Phillips
 */
public class JUTestEvaluation
{
  /** All tri-state logic states. */
  private static final Boolean [] LOGIC_STATES =
    new Boolean [] {TRUE, BOTTOM, FALSE};
  
  private static final Boolean [] NOT_TRUTH_TABLE =
    new Boolean [] {FALSE, BOTTOM, TRUE};
  
  private static final Boolean [] OR_TRUTH_TABLE =
    new Boolean [] {TRUE, TRUE, TRUE, TRUE, BOTTOM, 
                   BOTTOM, TRUE, BOTTOM, FALSE};
  
  private static final Boolean [] AND_TRUTH_TABLE =
    new Boolean [] {TRUE, BOTTOM, FALSE, BOTTOM, BOTTOM,
                    FALSE, FALSE, FALSE, FALSE};

  private static final Boolean [] XOR_TRUTH_TABLE =
    new Boolean [] {FALSE, BOTTOM, TRUE, BOTTOM, BOTTOM, 
                    BOTTOM, TRUE, BOTTOM, FALSE};
  
  /**
   * Test NOT, AND, OR and XOR logic operators against their tri-state
   * truth tables.
   */
  @Test
  public void logicOps ()
    throws Exception
  {
    // NOT
    for (Boolean state : LOGIC_STATES)
    {
      assertEquals (NOT_TRUTH_TABLE [toIndex (state)],
                    new Not (new Const (state)).evaluate (EMPTY_NOTIFICATION));
    }
    
    // OR, AND, XOR
    for (Boolean a : LOGIC_STATES)
    {
      for (Boolean b : LOGIC_STATES)
      {
        checkBooleanOp (And.class, a, b, and (a, b));
        checkBooleanOp (Or.class, a, b, or (a, b));
        checkBooleanOp (Xor.class, a, b, xor (a, b));
      }
    }

    // check that triple children work also
    for (Boolean a : LOGIC_STATES)
    {
      for (Boolean b : LOGIC_STATES)
      {
        for (Boolean c : LOGIC_STATES)
        {
          checkBooleanOp (And.class, a, b, c, and (and (a, b), c));
          checkBooleanOp (Or.class, a, b, c, or (or (a, b), c));
          checkBooleanOp (Xor.class, a, b, c, xor (xor (a, b), c));
        }
      }
    }

  }
  
  /**
   * Lookup result for AND in truth table.
   */
  private static Boolean and (Boolean a, Boolean b)
  {
    return AND_TRUTH_TABLE [toIndex (a) * 3 + toIndex (b)];
  }

  /**
   * Lookup result for OR in truth table.
   */
  private static Boolean or (Boolean a, Boolean b)
  {
    return OR_TRUTH_TABLE [toIndex (a) * 3 + toIndex (b)];
  }
  
  /**
   * Lookup result for XOR in truth table.
   */
  private static Boolean xor (Boolean a, Boolean b)
  {
    return XOR_TRUTH_TABLE [toIndex (a) * 3 + toIndex (b)];
  }

  /**
   * Truth table index for tri-state value.
   */
  private static int toIndex (Boolean b)
  {
    if (b == TRUE)
      return 0;
    else if (b == BOTTOM)
      return 1;
    else
      return 2;
  }

  /**
   * Basic test for the Compare operator minus numeric conversion.
   */
  @Test
  public void compare ()
  {
    // less than: 10 < 20
    assertTrue ("10 < 20", compare (10, 20, -1, false));
    assertFalse ("10 < 10", compare (10, 10, -1, false));
        
    // less than or equal
    assertTrue ("10 <= 20", compare (10, 20, -1, true));
    assertTrue ("10 <= 10", compare (10, 10, -1, true));
    assertFalse ("20 <= 10", compare (20, 10, -1, true));
    
    // greater than
    assertTrue ("20 > 10", compare (20, 10, 1, false));
    assertFalse ("10 > 10", compare (10, 10, 1, false));
    
    //  greater than or equal
    assertTrue ("20 >= 10", compare (20, 10, 1, true));
    assertTrue ("10 >= 10", compare (10, 10, 1, true));
    assertFalse ("10 > 20", compare (10, 20, 1, true));
    
    // equal
    assertTrue ("10 == 10", compare (10, 10, 0, true));
    assertFalse ("10 == 20", compare (10, 20, 0, true));
  }

  /**
   * Test logic operator expressions using operators in LOGIC_OP_EXPR1
   * and LOGIC_OP_EXPR2.
   */
  @Test
  public void logicOpExprs ()
  {
    Node expr1 = logicOpExpr1 ();
    
    Map<String, Object> n1 = new HashMap<String, Object> ();
    n1.put ("name", "Matt");
    n1.put ("age", 19);
    n1.put ("blah", "blah");
    
    assertTrue (expr1.evaluate (n1) == TRUE);
    
    Map<String, Object> n2 = new HashMap<String, Object> ();
    n2.put ("name", "Matt");
    n2.put ("age", 30);
    n2.put ("blah", "blah");
    
    assertFalse (expr1.evaluate (n2) == TRUE);
    
    Map<String, Object> n3 = new HashMap<String, Object> ();
    n3.put ("name", "Matt");
    n3.put ("blah", "blah");
    
    assertEquals (BOTTOM, expr1.evaluate (n3));
    
    Map<String, Object> n4 = new HashMap<String, Object> ();
    n4.put ("name", "Matt");
    n4.put ("age", 19);
    n4.put ("blah", "frob");
    
    assertFalse (expr1.evaluate (n4) == TRUE);
    
    // XOR tests
    
    Node expr2 = logicOpExpr2 ();
    
    Map<String, Object> n5 = new HashMap<String, Object> ();
    n5.put ("name", "Matt");
    n5.put ("age", 5);
    
    assertTrue (expr2.evaluate (n5) == TRUE);
    
    Map<String, Object> n6 = new HashMap<String, Object> ();
    n6.put ("name", "Matt");
    n6.put ("age", 30);
    
    assertFalse (expr2.evaluate (n6) == TRUE);
    
    Map<String, Object> n7 = new HashMap<String, Object> ();
    n7.put ("hello", "there");
    
    assertEquals (BOTTOM, expr2.evaluate (n7));
    
    // opaque data
    Map<String, Object> n8 = new HashMap<String, Object> ();
    n8.put ("name", new byte [] {1, 2, 3});
    n8.put ("age", 30);
    
    assertEquals (BOTTOM, expr2.evaluate (n8));
  }
  
  @Test
  public void functions ()
    throws Exception
  {
    Map<String, Object> ntfn;
    
    // basic string comparison predicates
    testPred (StrBeginsWith.class, "foobar", "foo", TRUE);
    testPred (StrBeginsWith.class, "foobar", "frob", FALSE);
    testPred (StrBeginsWith.class, null, "frob", BOTTOM);
    
    testPred (StrEndsWith.class, "foobar", "bar", TRUE);
    testPred (StrEndsWith.class, "foobar", "frob", FALSE);
    testPred (StrEndsWith.class, null, "frob", BOTTOM);
    
    testPred (StrContains.class, "foobar", "oob", TRUE);
    testPred (StrContains.class, "foobar", "frob", FALSE);
    testPred (StrContains.class, null, "frob", BOTTOM);
    
    testPred (StrRegex.class, "foobar", "o+", TRUE);
    testPred (StrRegex.class, "foobar", "o+x", FALSE);
    testPred (StrRegex.class, null, "o+", BOTTOM);
    
    testPred (StrWildcard.class, "foobar", "fo*a?", TRUE);
    testPred (StrWildcard.class, "foobar", "fo*a", FALSE);
    testPred (StrWildcard.class, null, "fo*a?", BOTTOM);
    testPred (StrWildcard.class, "fo*a", "fo\\*a", TRUE);
    testPred (StrWildcard.class, "foxa", "fo\\*a", FALSE);
    testPred (StrWildcard.class, "fo\\", "fo\\\\", TRUE);
    testPred (StrWildcard.class, "fo\\", "fo\\", TRUE);
    
    // require
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("exists", "true");
    assertEquals (TRUE, new Require ("exists").evaluate (ntfn));
    assertEquals (BOTTOM, new Require ("not_exists").evaluate (ntfn));
    
    // size
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("opaque", new byte [10]);
    ntfn.put ("string", "1234");
    ntfn.put ("int32", 1234);
    assertEquals (10, new Size ("opaque").evaluate (ntfn));
    assertEquals (4, new Size ("string").evaluate (ntfn));
    assertEquals (BOTTOM, new Size ("int32").evaluate (ntfn));
    assertEquals (BOTTOM, new Size ("not_exists").evaluate (ntfn));
    
    // fold-case
    assertEquals ("hello",
                  new StrFoldCase (new Const ("HellO")).evaluate (EMPTY_NOTIFICATION));
    assertEquals (BOTTOM,
                  new StrFoldCase (new Const (null)).evaluate (EMPTY_NOTIFICATION));
    
    // decompose
    // see examples at http://unicode.org/reports/tr15/#Canonical_Composition_Examples
    assertEquals ("\u0041\u0301",
                  new StrUnicodeDecompose
                    (new Const ("\u00C1"),
                     DECOMPOSE).evaluate (EMPTY_NOTIFICATION));
    assertEquals ("A\u0308\uFB03n",
                  new StrUnicodeDecompose
                    (new Const ("\u00C4\uFB03n"),
                     DECOMPOSE).evaluate (EMPTY_NOTIFICATION));
    assertEquals ("A\u0308ffin",
                  new StrUnicodeDecompose
                    (new Const ("\u00C4\uFB03n"),
                     DECOMPOSE_COMPAT).evaluate (EMPTY_NOTIFICATION));
    
    // nan
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("nan", NaN);
    ntfn.put ("notnan", 42.0);
    ntfn.put ("notnan_int", 42);
    
    assertEquals (TRUE,
                  new Nan (new Field ("nan")).evaluate (ntfn));
    assertEquals (FALSE,
                  new Nan (new Field ("notnan")).evaluate (ntfn));
    assertEquals (BOTTOM,
                  new Nan (new Field ("notnan_int")).evaluate (ntfn));
    assertEquals (BOTTOM,
                  new Nan (new Field ("nonexistent")).evaluate (ntfn));
    
    // type checks
    ntfn = new HashMap<String, Object> ();
    ntfn.put ("int32", 1);
    ntfn.put ("int64", 2L);
    ntfn.put ("real64", 42.0);
    ntfn.put ("string", "hello");
    ntfn.put ("opaque", new byte [] {1, 2, 3});
    
    assertEquals (TRUE,
                  new Type ("int32", Integer.class).evaluate (ntfn));
    assertEquals (TRUE,
                 new Type ("int64", Long.class).evaluate (ntfn));
    assertEquals (TRUE,
                 new Type ("real64", Double.class).evaluate (ntfn));
    assertEquals (TRUE,
                 new Type ("string", String.class).evaluate (ntfn));
    assertEquals (TRUE,
                  new Type ("opaque", byte [].class).evaluate (ntfn));
    assertEquals (FALSE,
                  new Type ("string", Integer.class).evaluate (ntfn));
    assertEquals (BOTTOM,
                  new Type ("nonexistent", String.class).evaluate (ntfn));
  }
  
  /**
   * Check that variable-type expressions like equals (name, "foobar",
   * 42) work.
   */
  @Test
  public void compareMultitype ()
  {
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("name", "foobar");
    
    Node node = Compare.createEquals (argsList (field ("name"),
                                                   Const.string ("foobar"),
                                                   Const.int32 (42)));
    assertEquals (TRUE, node.evaluate (ntfn));
    
    ntfn.put ("name", 42);
    assertEquals (TRUE, node.evaluate (ntfn));
    
    ntfn.put ("name", new byte [] {1, 2, 3});
    assertEquals (BOTTOM, node.evaluate (ntfn));
  }
  
  @Test
  public void mathOps ()
    throws Exception
  {
    testMathOp (MathMinus.class, 20 - 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathMinus.class, 20L - 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathMinus.class, 10.5 - 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathMinus.class, 10 - 20.25, Const.int32 (10), Const.real64 (20.25));
    testMathOp (MathMinus.class, null, new Field ("string"), Const.real64 (20.25));
    testMathOp (MathMinus.class, null, Const.int32 (10), new Field (""));
    
    testMathOp (MathPlus.class, 20 + 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathPlus.class, 20L + 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathPlus.class, 10.5 + 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathPlus.class, 10 + 20.25, Const.int32 (10), Const.real64 (20.25));
    
    testMathOp (MathMult.class, 20 * 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathMult.class, 20L * 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathMult.class, 10.5 * 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathMult.class, 10 * 20.25, Const.int32 (10), Const.real64 (20.25));
    
    testMathOp (MathDiv.class, 20 / 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathDiv.class, 20L / 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathDiv.class, 10.5 / 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathDiv.class, 10 / 20.25, Const.int32 (10), Const.real64 (20.25));
    
    // div by 0
    testMathOp (MathDiv.class, BOTTOM, Const.int32 (10), Const.int32 (0));
    testMathOp (MathDiv.class, BOTTOM, Const.int64 (10), Const.int64 (0));
    testMathOp (MathDiv.class, Double.POSITIVE_INFINITY, Const.real64 (10), Const.real64 (0));
    
    testMathOp (MathMod.class, BOTTOM, Const.int32 (20), Const.int32 (0));
    testMathOp (MathMod.class, Double.NaN, Const.real64 (20), Const.real64 (0));

    // modulo div
    testMathOp (MathMod.class, 20 % 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathMod.class, 20L % 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathMod.class, 10.5 % 20.25, Const.real64 (10.5), Const.real64 (20.25));
    testMathOp (MathMod.class, 10 % 20.25, Const.int32 (10), Const.real64 (20.25));
    
    // bitwise ops
    testMathOp (MathBitAnd.class, 20 & 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitAnd.class, 20L & 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitAnd.class, 20L & 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitOr.class, 20 | 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitOr.class, 20L | 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitOr.class, 20L | 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitXor.class, 20 ^ 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitXor.class, 20L ^ 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitXor.class, 20L ^ 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitShiftLeft.class, 20 << 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitShiftLeft.class, 20L << 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitShiftLeft.class, 20L << 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitShiftRight.class, 20 >> 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitShiftRight.class, 20L >> 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitShiftRight.class, 20L >> 30, Const.int64 (20), Const.int32 (30));
    
    testMathOp (MathBitLogShiftRight.class, 20 >>> 30, Const.int32 (20), Const.int32 (30));
    testMathOp (MathBitLogShiftRight.class, 20L >>> 30L, Const.int64 (20), Const.int64 (30));
    testMathOp (MathBitLogShiftRight.class, 20L >>> 30, Const.int64 (20), Const.int32 (30));
    
    // bitwise complement ~
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("string", "string");
    
    MathBitInvert invert;
    
    invert = new MathBitInvert (Const.int32 (10));
    assertEquals (~10, invert.evaluate (ntfn));
    
    invert = new MathBitInvert (Const.int64 (1234567890L));
    assertEquals (~1234567890L, invert.evaluate (ntfn));
    
    invert = new MathBitInvert (new Field ("string"));
    assertEquals (null, invert.evaluate (ntfn));
    
    // unary minus
    MathUnaryMinus unaryMinus = new MathUnaryMinus (Const.int32 (42));
    assertEquals (-42, unaryMinus.evaluate (ntfn));
    
    unaryMinus = new MathUnaryMinus (Const.int32 (-42));
    assertEquals (42, unaryMinus.evaluate (ntfn));
    
    unaryMinus = new MathUnaryMinus (Const.int32 (0));
    assertEquals (0, unaryMinus.evaluate (ntfn));
    
    unaryMinus = new MathUnaryMinus (Const.int64 (123));
    assertEquals (-123l, unaryMinus.evaluate (ntfn));
    
    unaryMinus = new MathUnaryMinus (Const.real64 (3.14));
    assertEquals (-3.14, unaryMinus.evaluate (ntfn));
    
    unaryMinus = new MathUnaryMinus (new Field ("string"));
    assertEquals (null, unaryMinus.evaluate (ntfn));
  }
  
  /**
   * Test that the Compare node handles upconverting numeric children.
   */
  @Test
  public void numericPromotion ()
  {
    Const thirtyTwoLong = new Const (32L);
    Const thirtyTwoInt = new Const (32);
    Const tenInt = new Const (10);
    Const piDouble = new Const (3.1415);
    
    Compare compare;
   
    // 32L < 10
    compare = new Compare (thirtyTwoLong, tenInt, -1, false);
    
    assertEquals (FALSE, compare.evaluate (new HashMap<String, Object> ()));
    
    // 10 > 32L
    compare = new Compare (tenInt, thirtyTwoLong, -1, false);
    
    assertEquals (TRUE, compare.evaluate (new HashMap<String, Object> ()));
    
    // 10 > pi
    compare = new Compare (tenInt, piDouble, 1, false);
    
    assertEquals (TRUE, compare.evaluate (new HashMap<String, Object> ()));
    
    // 32 > pi
    compare = new Compare (thirtyTwoLong, piDouble, 1, false);
    
    assertEquals (TRUE, compare.evaluate (new HashMap<String, Object> ()));
    
    //  32 == 32L
    compare = new Compare (thirtyTwoInt, thirtyTwoLong, 0, true);
    
    assertEquals (TRUE, compare.evaluate (new HashMap<String, Object> ()));
  }
  
  /**
   * Test inlining of constant sub-expressions. NOTE: this depends on
   * the parser to generate AST's.
   */
  @Test
  public void constantExpressions ()
    throws ParseException
  {
    // test reduction to constant
    assertReducesTo ("1 == 1", "true");
    assertReducesTo ("1 != 1", "false");
    assertReducesTo ("1 != 1", "false");
    assertReducesTo ("10 > 9", "true");
    assertReducesTo ("!(10 > 9)", "false");
    assertReducesTo ("1 == 1 ^^ 10 > 9", "false");
    assertReducesTo ("1 != 1 || 2 != 2 || 3 != 3", "false");
    assertReducesTo ("1 == 1 && 2 == 2 && 3 == 3", "true");
    assertReducesTo ("! ! (1 == 1)", "true");
    
    // test AND/OR erasure of constant non-contributing subtree
    assertReducesTo ("field == 5 && 10 > 9", "(== (field 'field') 5)");
    assertReducesTo ("field == 5 || 10 < 9", "(== (field 'field') 5)");
    assertReducesTo ("field == 5 || !(10 > 9)", "(== (field 'field') 5)");

    // test redundant constants are removed from AND/OR
    assertReducesTo ("field == 5 || 9 > 10 || field == 10",
                     "(|| (== (field 'field') 5) (== (field 'field') 10))");
    assertReducesTo ("field == 5 && 10 > 9 && field == 10",
                     "(&& (== (field 'field') 5) (== (field 'field') 10))");
    
    // predicate functions
    assertReducesTo ("fold-case ('HellO')", "'hello'");
    assertReducesTo ("decompose ('\u00C4\uFB03n')", "'A\u0308\uFB03n'");
    assertReducesTo ("decompose-compat ('\u00C4\uFB03n')", "'A\u0308ffin'");
    
    // some math ops
    assertReducesTo ("-10", "-10");
    assertReducesTo ("1 + 1", "2");
    assertReducesTo ("2 * 4.5", "9.0");
    assertReducesTo ("0xFF & 0xF0", "240");
    assertReducesTo ("0x0F << 4", "240");
  }
  
  /**
   * Test a math operator node.
   * 
   * @param opType The operator type.
   * @param correct The correct answer.
   * @param number1 Number parameter 1
   * @param number2 Number parameter 1
   */
  private static void testMathOp (Class<? extends Node> opType,
                                  Object correct,
                                  Node number1, Node number2)
    throws Exception
  {
    Node op =
      opType.getConstructor (Node.class, Node.class).newInstance (number1,
                                                                  number2);
    
    Map<String, Object> ntfn = new HashMap<String, Object> ();
    ntfn.put ("string", "string");
    
    assertEquals (correct, op.evaluate (ntfn));
  }
  
  /**
   * Create a list from an array of nodes.
   */
  private static List<Node> argsList (Node... args)
  {
    ArrayList<Node> argArray =
      new ArrayList<Node> (args.length);
    
    for (Node node : args)
      argArray.add (node);

    return argArray;
  }

  /**
   * Test a predicate operator node.
   * 
   * @param type The node type.
   * @param arg1 Argument 1.
   * @param arg2 Argument 2.
   * @param answer The correct answer.
   */
  private static void testPred (Class<? extends StringCompareNode> type,
                                String arg1, String arg2, Boolean answer)
    throws Exception
  {
    Node node =
      type.getConstructor (Node.class, Const.class).newInstance
        (new Const (arg1), new Const (arg2));
    
    assertEquals (answer, node.evaluate (EMPTY_NOTIFICATION));
  }

  private static void assertReducesTo (String subExpr, String treeExpr)
    throws ParseException
  {
    // System.out.println ("tree: " +
    //                     Nodes.unparse (parse (subExpr).expandConstants ()));
    assertEquals (treeExpr,
                  Nodes.unparse (parse (subExpr).inlineConstants ()));
  }
  
  private static Node parse (String expr)
    throws org.avis.subscription.parser.ParseException
  {
    return new SubscriptionParser (new StringReader (expr)).parse ();
  }
  
  /**
   * Check that an operator matches its truth table entry.
   * 
   * @param opType Operator type.
   * @param a Left value
   * @param b Right value
   * @param correct Correct result
   */
  private static <NODE extends Node>
    void checkBooleanOp (Class<NODE> opType,
                         Boolean a, Boolean b,
                         Boolean correct)
    throws Exception
  {
    NODE node = newLogicNodeInstance (opType, a, b);
    Object result = node.evaluate (EMPTY_NOTIFICATION);
    
    assertEquals ("Truth table check failed:" + node.name () + 
                  " (" + a + ", " + b + ") == " + correct + 
                  ": was " + result,
                  correct, result);
  }
  
  private static <NODE extends Node>
    void checkBooleanOp (Class<NODE> opType,
                         Boolean a, 
                         Boolean b,
                         Boolean c,
                         Boolean correct)
    throws Exception
  {
    NODE node = newLogicNodeInstance (opType, a, b, c);
    Object result = node.evaluate (EMPTY_NOTIFICATION);
    
    assertEquals ("Truth table check failed:" + node.name () + 
                  " (" + a + ", " + b + ", " + c + ") == " + correct + 
                  ": was " + result,
                  correct, result);
  }
  
  /**
   * Create a new node instance for nodes taking two fixed value
   * boolean children.
   */
  private static <NODE extends Node>
    NODE newLogicNodeInstance (Class<NODE> nodeType, Boolean a, Boolean b)
    throws Exception
  {
    Constructor<NODE> c =
      nodeType.getConstructor (Node.class, Node.class);
    
    return c.newInstance (new Const (a), new Const (b));
  }
  
  private static <NODE extends Node>
    NODE newLogicNodeInstance (Class<NODE> nodeType,
                               Boolean a, Boolean b, Boolean c)
    throws Exception
  {
    Constructor<NODE> constructor =
      nodeType.getConstructor (Collection.class);
    
    ArrayList<Node> children = new ArrayList<Node> (3);
    children.add (new Const (a));
    children.add (new Const (b));
    children.add (new Const (c));
    
    return constructor.newInstance (children);
  }

  /**
   * Use the Compare node to compare two numbers.
   */
  private Boolean compare (int n1, int n2, int inequality, boolean equality)
  {
    return (Boolean)new Compare
      (new Const (n1),
       new Const (n2),
       inequality, equality).evaluate (EMPTY_NOTIFICATION);
  }
  
 /**
  * Generate an AST that tests AND, OR, NOT, plus various comparisons
  * 
  * name == "Matt" && (age < 20 || age >= 50) && ! blah == "frob"
  */
  private static Node logicOpExpr1 ()
  {
    return new And
      (new Compare
        (field ("name"), new Const ("Matt"), 0, true),
       new Or
         (new Compare
           (field ("age"), new Const (20), -1, false),
          new Compare
           (field ("age"), new Const (50), 1, true)),
       new Not
         (new Compare (field ("blah"), new Const ("frob"), 0, true)));
  }

 /**
  * Generate an AST that tests XOR.
  *
  * name == "Matt" ^^ age == 30";
  */
  private static Node logicOpExpr2 ()
  {
    return new Xor
      (new Compare
        (field ("name"), new Const ("Matt"), 0, true),
       new Compare
        (field ("age"), new Const (30), 0, true));
  }
  
  private static Node field (String name)
  {
    return new Field (name);
  }
}
