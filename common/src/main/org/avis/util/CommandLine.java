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

/**
 * General command line utilities.
 * 
 * @author Matthew Phillips
 */
public final class CommandLine
{
  private CommandLine ()
  {
    // cannot instantiate
  }
  
  /**
   * Get a string argument from a given index, throwing a descriptive
   * exception if the argument is missing.
   * 
   * @param args The command line arguments.
   * @param arg The argument index to retrieve.
   * @return The argument at arg.
   * 
   * @throws IllegalCommandLineOption if arg does not exist.
   */
  public static String stringArg (String [] args, int arg)
  {
    try
    {
      return args [arg];
    } catch (ArrayIndexOutOfBoundsException ex)
    {
      throw new IllegalCommandLineOption 
        (args [arg - 1], "Missing parameter");
    }
  }

  /**
   * Get an integer argument from a given index, throwing a
   * descriptive exception if the argument is missing or not a number.
   * 
   * @param args The command line arguments.
   * @param arg The argument index to retrieve.
   * @return The argument at arg.
   * 
   * @throws IllegalCommandLineOption if arg does not exist or is not
   *           a number.
   */
  public static int intArg (String [] args, int arg)
  {
    try
    {
      return Integer.parseInt (stringArg (args, arg));
    } catch (NumberFormatException ex)
    {
      throw new IllegalCommandLineOption
        (args [arg - 1], "Not a valid number: " + args [arg]);
    }
  }
}
