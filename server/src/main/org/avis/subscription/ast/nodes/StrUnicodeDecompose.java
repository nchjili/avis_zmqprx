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

import java.util.Collection;
import java.util.Map;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.avis.subscription.ast.Node;

import static java.util.Collections.singleton;

/**
 * @author Matthew Phillips
 */
public class StrUnicodeDecompose extends Node
{
  /*
   * Java 5 does not have a public Unicode normalisation API. Rather
   * than ship a huge Unicode library, we use reflection to access the
   * non-public sun.text.Normalizer on 1.5, or the public
   * java.text.Normalizer API in 1.6 onwards.
   */
  private static Method normalizeMethod;
  private static boolean java5;
  private static Object modeDecompose;
  private static Object modeDecomposeCompat;
  
  static
  {
    try
    {
      java5 = false;

      Class<?> java6Normalizer = Class.forName ("java.text.Normalizer");
      Class<?> formClass = Class.forName ("java.text.Normalizer$Form");
      
      normalizeMethod = 
        java6Normalizer.getMethod ("normalize", CharSequence.class, formClass);
      
      modeDecompose = formClass.getEnumConstants () [0]; // NFD
      modeDecomposeCompat = formClass.getEnumConstants () [2]; // NFKD
      
    } catch (ClassNotFoundException ex)
    {
      // no Normalizer API, fall back on Java 5 workaround
      java5 = true;
      
      try
      {
        Class<?> java5Normalizer = Class.forName ("sun.text.Normalizer");
        Class<?> modeClass = Class.forName ("sun.text.Normalizer$Mode");
        
        normalizeMethod = 
          java5Normalizer.getMethod ("normalize", 
                                     String.class, modeClass, Integer.TYPE);
        
        modeDecompose = 
          java5Normalizer.getField ("DECOMP").get (null);
        modeDecomposeCompat = 
          java5Normalizer.getField ("DECOMP_COMPAT").get (null);
      } catch (Exception ex2)
      {
        throw new ExceptionInInitializerError (ex2);
      }
    } catch (Exception ex)
    {
      throw new ExceptionInInitializerError (ex);
    }
  }
  
  public static enum Mode
  {
    DECOMPOSE, DECOMPOSE_COMPAT;
  }
  
  public final Node stringExpr;
  public final Mode mode;
  
  private final Object normMode;

  public StrUnicodeDecompose (Node stringExpr, Mode mode)
  {
    this.stringExpr = stringExpr;
    this.mode = mode;
    
    this.normMode = 
      mode == Mode.DECOMPOSE ? modeDecompose : modeDecomposeCompat;
  }
  
  @Override
  public Class<?> evalType ()
  {
    return String.class;
  }

  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    String result = (String)stringExpr.evaluate (attrs);
    
    if (result == null)
      return BOTTOM;
    
    try
    {
      if (java5)
        return normalizeMethod.invoke (null, result, normMode, 0);
      else
        return normalizeMethod.invoke (null, result, normMode);
    } catch (InvocationTargetException ex)
    {
      throw new Error (ex.getCause ());
    } catch (Exception ex)
    {
      throw new Error (ex);
    }
  }

  @Override
  public String expr ()
  {
    return mode == Mode.DECOMPOSE ? "decompose" : "decompose-compat";
  }

  @Override
  public Node inlineConstants ()
  {
    Object result = evaluate (EMPTY_NOTIFICATION);
    
    return result == null ? this : new Const (result);
  }

  @Override
  public String presentation ()
  {
    return name ();
  }
  
  @Override
  public boolean hasChildren ()
  {
    return true;
  }
  
  @Override
  public Collection<Node> children ()
  {
    return singleton (stringExpr);
  }
}
