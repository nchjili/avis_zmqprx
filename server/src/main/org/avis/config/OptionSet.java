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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.avis.util.IllegalConfigOptionException;
import org.avis.util.Pair;

import static org.avis.config.OptionTypeParam.splitOptionParam;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/**
 * Defines the set of valid options for an {@link Options} instance.
 * An option set can inherit from one or more subsets. An option set
 * includes the valid option names, value type, valid value ranges and
 * default values. Option names are stored in a case-preserving
 * manner, but are matched case-insensitively.
 * 
 * @author Matthew Phillips
 */
public class OptionSet
{
  public static final OptionSet EMPTY_OPTION_SET = new OptionSet ();

  /** The default values for each option. */
  public final Options defaults;
  
  /** The inherited sets. */
  protected List<OptionSet> inherited;
  /** Maps option names to validation info. */
  protected Map<String, OptionType> optionTypes;
  
  public OptionSet ()
  {
    this.defaults = new Options (this);
    this.inherited = new ArrayList<OptionSet> ();
    this.optionTypes = new TreeMap<String, OptionType> (CASE_INSENSITIVE_ORDER);
  }
  
  public OptionSet (OptionSet inheritedOptions)
  {
    this ();
    
    inherited.add (inheritedOptions);
  }
  
  /**
   * Inherit from a given option set.
   */
  public void inheritFrom (OptionSet optionSet)
  {
    inherited.add (optionSet);
  }
  
  /**
   * Look for the default value specified by this option set or any of
   * its inherited sets.
   * 
   * @param option The option to find a value for.
   * 
   * @return The value, or null if none found.
   */
  public Object peekDefaultValue (String option)
  {
    Object value = defaults.values.get (option);
    
    if (value == null)
    {
      for (OptionSet superSet : inherited)
      {
        value = superSet.defaults.values.get (option);
        
        if (value != null)
          break;
      }
    }
    
    return value;
  }
  
  /**
   * Define an int-valued option.
   * 
   * @param option The option name.
   * @param min The minimum value
   * @param defaultValue The default value
   * @param max The maximum value
   */
  public void add (String option, int min, int defaultValue, int max)
  {
    add (option, new OptionTypeInt (min, max), defaultValue);
  }
  
  public void add (String option, boolean defaultValue)
  {
    add (option, OptionTypeBoolean.INSTANCE, defaultValue);
  }
  
  /**
   * Define a string-valued option that can take any value.
   * 
   * @param option The option name.
   * @param defaultValue The default value.
   */
  public void add (String option, String defaultValue)
  {
    add (option, new OptionTypeString (), defaultValue);
  }
  
  /**
   * Define a string-valued option.
   * 
   * @param option The option name.
   * @param defaultValue The default value.
   * @param values Valid values (other than default).
   */
  public void add (String option, String defaultValue,
                   String... values)
  {
    add (option, new OptionTypeString (defaultValue, values), defaultValue);
  }
  
  /**
   * Add an option with a default value.
   */
  protected void add (String option, OptionType type, Object defaultValue)
  {
    optionTypes.put (option, type);
    set (defaults, option, defaultValue);
  }

  /**
   * Test if value is valid for a given option (does not set the value).
   * 
   * @see #validate(String, Object)
   */
  public final boolean isValid (String option, Object value)
  { 
    return validate (option, value) == null; 
  }
  
  /**
   * Test if a given option name is defined by this set or a subset.
   */
  public boolean isDefined (String option)
  {
    return findOptionType (option) != null;
  }
  
  /**
   * Get the maximum value for an int option.
   */
  public int getMaxValue (String name)
    throws IllegalConfigOptionException
  {
    return intOption (name).max;
  }
  
  /**
   * Get the minimum value for an int option.
   */
  public int getMinValue (String name)
    throws IllegalConfigOptionException
  {
    return intOption (name).min;
  }
  
  private OptionTypeInt intOption (String name)
    throws IllegalConfigOptionException
  {
    OptionType info = findOptionType (name);
    
    if (info instanceof OptionTypeInt)
      return (OptionTypeInt)info;
    else
      throw new IllegalConfigOptionException (name, "Not an integer value");  
  }
  
  /**
   * Test if value is valid for a given option in this set or any
   * inherited sets. (does not set the value).
   * 
   * @return Null if valid, a message describing why the value is
   *         invalid otherwise.
   */
  public final String validate (String option, Object value)
  {
    String message = null;
    
    if (optionTypes.containsKey (option))
    {
      message = testValid (option, value);
    } else
    {
      for (OptionSet inheritedSet : inherited)
      {
        message = inheritedSet.testValid (option, value);
        
        // if one inherited set accepts the option, we're done
        if (message == null)
          break;
      }
    }
    
    return message;
  }
  
  /**
   * Called by {@link Options#set(String, Object)} to validate and set
   * the value. If the value is valid, it should be set with a call to
   * {@link #set(Options, String, Object)}, otherwise an
   * IllegalOptionException should be thrown. This method is also
   * responsible for any automatic value conversion (see
   * {@link OptionType#convert(String, Object)}).
   * <p>
   * Subclasses may override to customise validation behaviour.
   * 
   * @param options The options to update.
   * @param option The option.
   * @param value The value to validate and set.
   * 
   * @throws IllegalConfigOptionException if the value or option are not
   *           valid.
   * 
   * @see #validate(String, Object)
   */
  protected void validateAndSet (Options options, String option, Object value)
  {
    Pair<String, List<String>> optionItems = splitOptionParam (option);
    
    OptionType type = optionTypeFor (optionItems.item1);
    
    if (type instanceof OptionTypeParam)
    {
      // allow param option to create/update param values
      value = 
        ((OptionTypeParam)type).updateValue (options, option, 
                                             optionItems.item1,
                                             optionItems.item2, value);
    } else
    {
      if (!optionItems.item2.isEmpty ())
      {
        throw new IllegalConfigOptionException
          (option, "Cannot specify parameters for option");
      }
    }
    
    value = type.convert (option, value);
    
    String message = type.validate (option, value);
    
    if (message == null)
      set (options, optionItems.item1, value);
    else
      throw new IllegalConfigOptionException (option, message);
  }
  
  /**
   * Set a value in the options with no validation. This is usually
   * called to set validated values from
   * {@link #validateAndSet(Options, String, Object)}.
   */
  protected final void set (Options options, String option, Object value)
  {
    options.values.put (option, value);
  }

  /**
   * Check the validity of an option/value pair against this set only
   * (no inherited checks).
   * 
   * @see #validate(String, Object)
   */
  private String testValid (String option, Object value)
  {
    return optionTypeFor (option).validate (option, value);
  }
  
  /**
   * Shortcut to {@link OptionType#convert(String, Object)}.
   * 
   * @throws IllegalConfigOptionException if option not defined or value is
   *                 invalid.
   */
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    return optionTypeFor (option).convert (option, value);
  }
  
  /**
   * Recursively look for the first option set that has a mapping for
   * a given option.
   */
  protected OptionSet findOptionSetFor (String option)
  {
    if (peekOptionTypeFor (option) != null)
    {
      return this;
    } else
    {
      for (OptionSet superset : inherited)
      {
        OptionSet set = superset.findOptionSetFor (option);
        
        if (set != null)
          return set;
      }
      
      return null;
    }
  }

  /**
   * Lookup the option type for a given option in this set only (no
   * recursion). This defaults to optionTypes.get (option), but can be
   * overridden to extend how options map to types. 
   */
  protected OptionType peekOptionTypeFor (String option)
  {
    Pair<String, List<String>> optionItems = splitOptionParam (option);
    
    return optionTypes.get (optionItems.item1);
  }
  
  /**
   * Get the option type for a given option.
   * 
   * @throws IllegalConfigOptionException if option is not defined.
   */
  public OptionType optionTypeFor (String option)
    throws IllegalConfigOptionException
  {
    OptionType type = findOptionType (option);
    
    if (type == null)
      throw new IllegalConfigOptionException (option, "Undefined option");
    
    return type;
  }
  
  /**
   * Recursively search this set and subsets for an option's type.
   * 
   * @param option Name of option. Must be lower case.
   */
  private OptionType findOptionType (String option)
  {
    OptionType optionType = optionTypes.get (option);
    
    if (optionType == null)
    {
      for (OptionSet inheritedSet : inherited)
      {
        optionType = inheritedSet.findOptionType (option);
        
        if (optionType != null)
          break;
      }
    }
    
    return optionType;
  }
}
