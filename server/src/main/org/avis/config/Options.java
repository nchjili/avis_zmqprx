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
package org.avis.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import java.io.File;

import java.net.URI;

import org.avis.util.IllegalConfigOptionException;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import static org.avis.config.OptionSet.EMPTY_OPTION_SET;

/**
 * Defines a set of configuration options. The options are validated
 * against an {@link OptionSet}. Default values are taken from the
 * option set, but may be overridden/added in this instance using
 * {@link #addDefaults(Options)}.
 * 
 * @author Matthew Phillips
 */
public class Options implements Iterable<Map.Entry<String, Object>>
{
  protected Map<String, Object> values;
  protected List<Options> defaults;
  protected OptionSet optionSet;
  protected File relativeDirectory;
  
  public Options ()
  {
    this (EMPTY_OPTION_SET);
  }
  
  public Options (OptionSet optionSet)
  {
    this.values = new TreeMap<String, Object> (CASE_INSENSITIVE_ORDER);
    this.defaults = new ArrayList<Options> ();
    this.optionSet = optionSet;
    this.relativeDirectory = new File (System.getProperty ("user.dir"));
  }
  
  public OptionSet optionSet ()
  {
    return optionSet;
  }
  
  public void setRelativeDirectory (String directory)
  {
    setRelativeDirectory (new File (directory));
  }
  
  /**
   * Set the directory used to resolve relative file paths and URI's
   * given in the config. This can be used so that relative paths
   * specified in the config resolve relative to the config itself
   * rather than the current working directory (which is the default).
   */
  public void setRelativeDirectory (File directory)
  {
    this.relativeDirectory = directory;
  }

  public Iterator<Entry<String, Object>> iterator ()
  {
    return values.entrySet ().iterator ();
  }
  
  /**
   * Add a set of options as defaults. Any overlapping values override
   * existing option defaults in this set and the validating option
   * set.
   */
  public void addDefaults (Options newDefaults)
  {
    defaults.add (0, newDefaults);
  }

  /**
   * Set a bulk lot of options. This is equivalent to calling
   * {@link #set(String, Object)} for each entry.
   * 
   * @throws IllegalConfigOptionException
   */
  public void setAll (Map<String, Object> options)
    throws IllegalConfigOptionException
  {
    for (Entry<String, Object> entry : options.entrySet ())
      set (entry.getKey (), entry.getValue ());
  }

  /**
   * Set a bulk lot of options from java.util.Properties source.
   * 
   * @throws IllegalConfigOptionException
   */
  public void setAll (Properties properties)
    throws IllegalConfigOptionException
  {
    for (Entry<Object, Object> entry : properties.entrySet ())
      set ((String)entry.getKey (), entry.getValue ());
  }

  /**
   * Get an integer option.
   * 
   * @param option The option name.
   * @return The value.
   * 
   * @throws IllegalConfigOptionException if the option is not defined or is
   *           not an integer.
   * 
   * @see #get(String)
   */
  public int getInt (String option)
    throws IllegalConfigOptionException
  {
    Object value = get (option);
    
    if (value instanceof Integer)
      return (Integer)value;
    else
      throw new IllegalConfigOptionException (option, "Not an integer");
  }
  
  /**
   * Get a string option.
   * 
   * @param option The option name.
   * @return The value.
   * 
   * @throws IllegalConfigOptionException if the option is not defined or is
   *           not a string.
   * 
   * @see #get(String)
   */
  public String getString (String option)
    throws IllegalConfigOptionException
  {
    Object value = get (option);
    
    if (value instanceof String)
      return (String)value;
    else
      throw new IllegalConfigOptionException (option, "Not a string");
  }
  
  /**
   * Get a boolean option.
   * 
   * @param option The option name.
   * @return The value.
   * 
   * @throws IllegalConfigOptionException if the option is not defined or is
   *           not a boolean.
   * 
   * @see #get(String)
   */
  public boolean getBoolean (String option)
    throws IllegalConfigOptionException
  {
    Object value = get (option);
    
    if (value instanceof Boolean)
      return (Boolean)value;
    else
      throw new IllegalConfigOptionException (option, "Not a boolean");
  }
  
  /**
   * Get a value for a parameterised option. e.g.
   * "Federation.Subscribe[Internal]".
   * 
   * @param option The option, minus the parameters.
   * 
   * @return The value of the option, mapping parameters to values.
   */
  public Map<String, ?> getParamOption (String option)
  {
    return OptionTypeParam.getParamOption (this, option);
  }
  
  /**
   * Get an option value that is a set.
   * 
   * @param <T> The type of item in the set.
   * 
   * @param option The option name.
   * @param type The type of option. This is somewhat bogus, but
   *                needed by the compiler to check generic types.
   * @return The set value.
   * 
   * @throws IllegalConfigOptionException if the value is not a set.
   */
  @SuppressWarnings({"unchecked"})
  public <T> Set<T> getSet (String option, Class<T> type)
  {
    Object value = get (option);
    
    if (value instanceof Set<?>)
      return (Set<T>)value;
    else
       throw new IllegalConfigOptionException (option, "Not a set");
  }
  
  /**
   * Get the absolute value of a URI option, resolved against the
   * {@linkplain #setRelativeDirectory(File) current directory} if
   * needed. e.g. "file.txt" resolves to something like
   * "file:/home/user/file.txt", whereas "http://host/file.txt" is
   * untouched.
   * 
   * @param option The option name.
   * 
   * @return An absolute URI.
   * 
   * @see #toAbsoluteURI(URI)
   */
  public URI getAbsoluteURI (String option)
  {
    Object value = get (option);
    
    if (value instanceof URI)
      return toAbsoluteURI ((URI)value);
    else
      throw new IllegalConfigOptionException (option, "Not a URI");
  }
  
  /**
   * Resolve a URI against the
   * {@linkplain #setRelativeDirectory(File) current directory} if
   * needed. e.g. "file.txt" resolves to something like
   * "file:/home/user/file.txt", whereas "http://host/file.txt" is
   * untouched.
   * 
   * @param uri The URI to resolve.
   * 
   * @return An absolute URI.
   */
  public URI toAbsoluteURI (URI uri)
  {
    if (uri.isAbsolute ())
      return uri;
    else
      return relativeDirectory.toURI ().resolve (uri);
  }
  
  /**
   * Get the value of an option, searching defaults if needed.
   * 
   * @param option The option name
   * @return The value.
   * 
   * @throws IllegalConfigOptionException if the option is not defined.
   * 
   * @see #peek(String)
   * @see #set(String, Object)
   * @see #isDefined(String)
   */
  public Object get (String option)
    throws IllegalConfigOptionException
  {
    Object value = peek (option);
    
    if (value != null)
      return value;
    else
      throw new IllegalConfigOptionException (option, "Undefined option");
  }
  
  /**
   * Same as get(), but returns null if no value found rather than
   * throwing an exception.
   * 
   * @see #get(String)
   */
  public Object peek (String option)
  {
    Object value = values.get (option);
    
    // look in defaults on this option set
    if (value == null)
    {
      for (Options options : defaults)
      {
        value = options.peek (option);
        
        if (value != null)
          break;
      }
    }
    
    // look in option set for defaults
    if (value == null)
      value = optionSet.peekDefaultValue (option);
    
    return value;
  }

  /**
   * Set the value of an option.
   * 
   * @param option The option name.
   * @param value The option value.
   * 
   * @throws IllegalConfigOptionException if the option is not defined or
   *           the value is invalid.
   *           
   * @see #get(String)
   * @see #remove(String)
   * @see OptionSet#validateAndSet(Options, String, Object)
   */
  public void set (String option, Object value)
    throws IllegalConfigOptionException
  {
    if (value == null)
      throw new IllegalConfigOptionException (option, "Value cannot be null");
    
    OptionSet set = optionSet.findOptionSetFor (option);
    
    /*
     * If no option set found, fall back on this one in case
     * validateAndSet () can do something clever
     */
    if (set == null)
      set = optionSet;
    
    set.validateAndSet (this, option, value);
  }
  
  /**
   * Undo the effect of set ().
   * 
   * @param option The option to remove.
   * 
   * @see #set(String, Object)
   */
  public void remove (String option)
  {
    values.remove (option);
  }
  
  /**
   * Test if an option is defined.
   */
  public boolean isDefined (String option)
  {
    return peek (option) != null;
  }

  /**
   * Return an unmodifiable, live set of the options just for this
   * instance (not including defaults).
   */
  public Set<String> options ()
  {
    return unmodifiableSet (values.keySet ());
  }
  
  /**
   * Return an unmodifiable, live map containing all the options and
   * values in this instance (not including defaults).
   */
  public Map<String, Object> asMap ()
  {
    return unmodifiableMap (values);
  }
}
