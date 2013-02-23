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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.avis.util.IllegalConfigOptionException;
import org.avis.util.Pair;

import static java.util.Collections.emptyList;

/**
 * A parameterised option. These options have a Map as a value,
 * mapping parameters to their values (which may themselves be maps
 * for multi-dimensional options).
 */
public class OptionTypeParam extends OptionType
{
  private static final List<String> EMPTY_STRING_LIST = emptyList ();
  
  private OptionType subOption;
  private int paramCount;

  public OptionTypeParam (OptionType option)
  {
    this (option, 1);
  }

  public OptionTypeParam (OptionType subOption, int paramCount)
  {
    this.subOption = subOption;
    this.paramCount = paramCount;
  }

  @Override
  public Object convert (String option, Object value)
    throws IllegalConfigOptionException
  {
    return value;
  }

  @Override
  public String validate (String option, Object value)
  {
    return validateType (value, Map.class);
  }

  @SuppressWarnings("unchecked")
  public Object updateValue (Options options, 
                             String option, 
                             String baseOption,
                             List<String> params, Object value)
  {
    if (params.size () != paramCount)
    {
      throw new IllegalConfigOptionException 
        (option, "Parameters required: " + paramCount);
    }
    
    value = subOption.convert (option, value);

    String valid = subOption.validate (option, value);
    
    if (valid != null)
      throw new IllegalConfigOptionException (option, valid);
    
    // create/lookup base Map value
    Map<String, Object> baseValue = 
      (Map<String, Object>)options.get (baseOption);
    
    if (baseValue.isEmpty ())
      baseValue = new HashMap<String, Object> ();
    
    // create/lookup any sub-map's
    Map<String, Object> map = baseValue;
    
    for (int i = 0; i < paramCount - 1; i++)
      map = createEntry (map, params.get (i));
    
    // store value
    map.put (params.get (params.size () - 1), value);
    
    return baseValue;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> getParamOption (Options options, 
                                                    String option)
  {
    OptionType type = options.optionSet ().optionTypeFor (option);
    
    if (type instanceof OptionTypeParam)
      return (Map<String, Object>)options.get (option);
    else
      throw new IllegalConfigOptionException (option, "Not a parameterised option");
  }

  /**
   * Split a parameterised option into a (base option, params) pair.
   */
  public static Pair<String,List<String>> splitOptionParam (String option)
  {
    int index = option.indexOf ('[');
    
    if (index == -1)
    {
      if (option.indexOf (']') == -1)
        return new Pair<String, List<String>> (option, EMPTY_STRING_LIST);
      else
        throw new IllegalConfigOptionException (option, "Orphan ]");
    }
    
    String base = option.substring (0, index);
    List<String> params = new ArrayList<String> ();
  
    while (index < option.length ())
    {
      int end = option.indexOf (']', index);
      
      if (end == -1)
        throw new IllegalConfigOptionException (option, "Missing ]");
      
      params.add (option.substring (index + 1, end));
      
      index = end + 1;
      
      if (index < option.length () && option.charAt (index) != '[')
          throw new IllegalConfigOptionException (option, "Junk in parameter list");
    }
    
    return new Pair<String, List<String>> (base, params);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> createEntry (Map<String, Object> map,
                                                  String key)
  {
    Map<String, Object> entry = (Map<String, Object>)map.get (key);
    
    if (entry == null)
    {
      entry = new HashMap<String, Object> ();
      
      map.put (key, entry);
    }
    
    return entry;
  }
}