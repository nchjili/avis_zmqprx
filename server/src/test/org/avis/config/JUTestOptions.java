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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import java.net.URI;

import org.junit.Test;

import org.avis.util.IllegalConfigOptionException;

import static java.util.Collections.emptySet;

import static org.avis.common.Common.K;
import static org.avis.common.Common.MB;
import static org.avis.util.Collections.set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JUTestOptions
{
  @Test
  public void basic ()
  {
    ConnectionOptionSet connectionOptionSet = new ConnectionOptionSet ();
    OptionSet routerOptionSet = new OptionSet (connectionOptionSet);
    
    routerOptionSet.add ("Port", 1, 2917, 65535);
    routerOptionSet.add ("Interfaces", "*");
    routerOptionSet.add ("Vendor-Identification", "Foobar");
    routerOptionSet.add ("Federation.Activated", false);
    
    Options routerOptions = new Options (routerOptionSet);
    
    Properties loadedProperties = new Properties ();
    loadedProperties.setProperty ("Port", "29170");
    loadedProperties.setProperty ("Keys.Max-Count", "42");
    loadedProperties.setProperty ("Interfaces", "en1");
    loadedProperties.setProperty ("Federation.Activated", "yes");
    
    routerOptions.setAll (loadedProperties);
    
    Map<String, Object> requestedOptions = new HashMap<String, Object> ();
    requestedOptions.put ("Packet.Max-Length", 20*K);
    requestedOptions.put ("router.subscription.max-count", 1234);
    
    Options connectionOptions = new Options (connectionOptionSet);
    connectionOptions.addDefaults (routerOptions);
    connectionOptions.setAll (requestedOptions);
    
    Map<String, Object> accepted =
      connectionOptionSet.accepted (connectionOptions, requestedOptions);
    
    assertEquals (29170, routerOptions.getInt ("Port"));
    assertEquals ("en1", routerOptions.getString ("Interfaces"));
    assertEquals (true, routerOptions.getBoolean ("Federation.Activated"));
    assertEquals (20*K, connectionOptions.getInt ("Packet.Max-Length"));
    assertEquals (42, connectionOptions.getInt ("Keys.Max-Count"));
    assertEquals (1234, connectionOptions.getInt ("Subscription.Max-Count"));
    assertEquals (1234, accepted.get ("router.subscription.max-count"));
  }
  
  /**
   * Test that options are case-preserving but case independent for lookup.
   */
  @Test
  public void caseIndependence ()
  {
    OptionSet optionSet = new OptionSet ();
    optionSet.add ("Port", 0, 2917, 65536);
    optionSet.add ("foobar", "frob", "wibble");
    
    Options options = new Options (optionSet);
    
    options.set ("port", 1234);
    options.set ("FooBar", "wibble");
    
    assertEquals (1234, options.get ("Port"));
    assertEquals (1234, options.get ("port"));
    assertEquals ("wibble", options.get ("foobar"));
    assertEquals ("wibble", options.get ("FOOBAR"));
    
    assertTrue (options.options ().contains ("FooBar"));
  }
  
  /**
   * Test unit conversion.
   */
  @Test
  public void units ()
  {
    OptionSet optionSet = new OptionSet ();
    optionSet.add ("Packet.Max-Length", 0, 100, 10000);
    optionSet.add ("Attribute.Max-Count", 0, 100, 5000000);
    
    Options options = new Options (optionSet);
    
    options.set ("Packet.Max-Length", "5K");
    options.set ("Attribute.Max-Count", "1M");
    
    assertEquals (5*1024, options.get ("Packet.Max-Length"));
    assertEquals (1*1024*1024, options.get ("Attribute.Max-Count"));
  }
  
  @Test
  public void multipleInherit () 
    throws Exception
  {
    OptionSet optionSet1 = new OptionSet ();
    optionSet1.add ("Packet.Max-Length", 0, 100, 10000);
    optionSet1.add ("Attribute.Max-Count", 0, 100, 5000000);
    
    OptionSet optionSet2 = new OptionSet ();
    optionSet2.add ("Federation.Active", true);
    optionSet2.add ("Federation.Name", "");
    
    OptionSet rootOptionSet = new OptionSet ();
    rootOptionSet.add ("Root1", true);
    
    Options options = new Options (rootOptionSet);

    // add inherited afterwards to be tricky
    rootOptionSet.inheritFrom (optionSet1);
    rootOptionSet.inheritFrom (optionSet2);
    
    assertEquals (true, options.get ("Federation.Active"));
    assertEquals (100, options.get ("Packet.Max-Length"));
    assertEquals (true, options.get ("Root1"));
    
    options.set ("Packet.Max-Length", 200);
    options.set ("Federation.Active", false);
    options.set ("Root1", false);
    
    assertEquals (false, options.get ("Federation.Active"));
    assertEquals (200, options.get ("Packet.Max-Length"));
    assertEquals (false, options.get ("Root1"));
  }
  
  @Test
  public void uri () 
    throws Exception
  {
    OptionSet optionSet = new OptionSet ();
    optionSet.add ("FileUri", new OptionTypeURI (), new URI (""));
    optionSet.add ("HttpUri", new OptionTypeURI (), new URI (""));
    
    Options options = new Options (optionSet);
    
    options.set ("FileUri", "file.txt");
    options.set ("HttpUri", "http://host/file.txt");
    
    URI fileUri = options.getAbsoluteURI ("FileUri");
    URI httpUri = options.getAbsoluteURI ("HttpUri");
    
    String dir = System.getProperty ("user.dir").replaceAll ("\\\\", "/");
    
    assertEquals ("file:" + dir + "/file.txt", fileUri.toString ());
    assertTrue (fileUri.isAbsolute ());
    assertEquals ("http://host/file.txt", httpUri.toString ());
  }
  
  @Test
  public void optionTypeSet () 
    throws Exception
  {
    OptionSet optionSet = new OptionSet ();
    optionSet.add ("Values", new OptionTypeSet (String.class), emptySet ());
    
    Options options = new Options (optionSet);
    
    options.set ("Values", "1");
    assertEquals (set ("1"), options.get ("Values"));
    
    options.set ("Values", "1 2");
    assertEquals (set ("1", "2"), options.get ("Values"));
    
    options.set ("Values", " 1\\ 2 \n 3\n ");
    assertEquals (set ("1 2", "3"), options.get ("Values"));
  }
  
  static class ConnectionOptionSet extends OptionSet
  {
    private Map<String, String> legacyToNew;
    private Map<String, String> newToLegacy;

    public ConnectionOptionSet ()
    {
      this.legacyToNew = new HashMap<String, String> ();
      this.newToLegacy = new HashMap<String, String> ();
      
      add ("Packet.Max-Length", 1*K, 1*MB, 1*MB);
      add ("Subscription.Max-Count", 16, 2*K, 2*K);
      add ("Keys.Max-Count", 16, 2*K, 2*K);
      add ("Send-Queue.Drop-Policy",
           "oldest", "newest", "largest", "fail");
      
      addLegacy ("router.packet.max-length", "Packet.Max-Length");
      addLegacy ("router.subscription.max-count",
                 "Subscription.Max-Count");
    }

    public Map<String, Object> accepted (Options connectionOptions,
                                         Map<String, Object> requestedOptions)
    {
      HashMap<String, Object> accepted = new HashMap<String, Object> ();
      
      for (String option : connectionOptions.options ())
      {
        Object value = connectionOptions.get (option);
        
        if (!requestedOptions.containsKey (option))
          option = newToLegacy.get (option);
        
        accepted.put (option, value);
      }
      
      return accepted;
    }
    
    private void addLegacy (String oldOption, String newOption)
    {
      legacyToNew.put (oldOption, newOption);
      newToLegacy.put (newOption, oldOption);
    }
    
    @Override
    protected void validateAndSet (Options options,
                                   String option, Object value)
      throws IllegalConfigOptionException
    {
      if (legacyToNew.containsKey (option))
        option = legacyToNew.get (option);
      
      super.validateAndSet (options, option, value);
    }
  }
}
