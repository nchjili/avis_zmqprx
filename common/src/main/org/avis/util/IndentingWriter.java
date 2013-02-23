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
package org.avis.util;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * A PrintWriter that provides auto indented output.
 */
public class IndentingWriter extends PrintWriter
{
  private int indent;
  private int indentIncr;
  
  public IndentingWriter (OutputStream str)
  {
    this (new OutputStreamWriter (str));
  }
  
  public IndentingWriter (Writer writer)
  {
    super (writer);
    
    this.indent = 0;
    this.indentIncr = 2;
  }
  
  /**
   * Make it easier for StringWriter users.
   */
  @Override
  public String toString ()
  {
    return out.toString ();
  }
  
  /**
   * Set the amount of spaces to increment the indent level by when using
   * {@link #indent()}.
   */
  public void setIndentIncr (int newValue)
  {
    this.indentIncr = newValue;
  }

  /**
   * Get the amount of spaces to increment the indent level by when using
   * {@link #indent()}.
   */
  public int getIndentIncr ()
  {
    return indentIncr;
  }
  
  /**
   * Set the current indent level. You probably want to use {@link #indent()}
   * and {@link #unindent()} instead.
   */
  public void setIndent (int indent)
  {
    this.indent = indent;
  }
  
  /**
   * Get the current indent level. 
   */
  public int getIndent ()
  {
    return indent;
  }
   
  /**
   * Increment indent and start a new line.
   */
  public void indentln ()
  {
    indent ();
    println ();
  }
  
  /**
   * Increment the indent level by {@link #getIndentIncr()} spaces.
   * 
   * @see #unindent()
   * @see #setIndentIncr(int)
   */
  public void indent ()
  {
    indent += indentIncr;
  }
  
  /**
   * Reverse the effect of an {@link #indent()}.
   */
  public void unindent ()
  {
    indent -= indentIncr;
  }
  
  public void indent (int spaces)
  {
    indent += spaces;
  }
  
  public void unindent (int spaces)
  {
    indent -= spaces;
  } 
  
  @Override
  public void write (int c)
  {
    super.write (c);
    
    if (c == '\n')
      writeIndent ();
  }

  @Override
  public void write (char buf [], int off, int len)
  {
    int index = off;
    
    for (int i = 0; i < len; i++)
      write (buf [index++]);
  }
  
  @Override
  public void write (String s, int off, int len)
  {
    write (s.toCharArray (), off, len);
  }
  
  @Override
  public void println ()
  {
    super.println ();
    
    writeIndent ();
    
    flush ();
  }  
  
  private void writeIndent ()
  {
    for (int i = 0; i < indent; i++)
      write (' ');
  }
}
