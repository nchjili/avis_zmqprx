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
package org.avis.router;

import java.util.Properties;

import java.io.File;
import java.io.IOException;

import java.net.InetSocketAddress;

import org.avis.common.ElvinURI;
import org.avis.federation.EwafURI;
import org.avis.federation.FederationManager;
import org.avis.federation.FederationOptionSet;
import org.avis.logging.Log;
import org.avis.util.IllegalCommandLineOption;
import org.avis.util.IllegalConfigOptionException;

import static org.avis.federation.FederationManager.federationManagerFor;
import static org.avis.io.Net.addressesFor;
import static org.avis.logging.Log.DIAGNOSTIC;
import static org.avis.logging.Log.TRACE;
import static org.avis.logging.Log.alarm;
import static org.avis.logging.Log.diagnostic;
import static org.avis.logging.Log.enableLogging;
import static org.avis.logging.Log.info;
import static org.avis.logging.Log.shouldLog;
import static org.avis.logging.Log.warn;
import static org.avis.util.CommandLine.intArg;
import static org.avis.util.CommandLine.stringArg;
import static org.avis.util.Streams.fileStream;
import static org.avis.util.Streams.propertiesFrom;
import static org.avis.util.Streams.resourceStream;

/**
 * Invokes the Avis router from the command line.
 * 
 * @author Matthew Phillips
 */
public class Main
{
  private static final String USAGE =
    "Usage: avisd [-h] [-v] [-vv] [-p port] [-c file]\n\n" +
    " -h         : This text\n" +
    " -v and -vv : Increase verbosity\n" +
    " -p port    : Set port to listen on\n" +
    " -c file    : Load config from file\n";
  
  public static void main (String [] args)
    throws Exception
  {
    Log.setApplicationName ("Avis");
    
    enableLogging (TRACE, false);
    enableLogging (DIAGNOSTIC, false);
    
    Properties avisProperties = readAvisProperties ();
    System.getProperties ().putAll (avisProperties);
    
    info ("Avis event router version " +
          avisProperties.getProperty ("avis.router.version"), Main.class);
    
    try
    {
      // TODO: load zmq_address and zmq_bind from etc/avisd.conf
      final Router router = start (args);
      final ZmqRouter zmqRouter = new ZmqRouter(router);
      zmqRouter.start();

      Runtime.getRuntime ().addShutdownHook (new Thread ()
      {
        public void run ()
        {
          info ("Shutting down...", Main.class);
          
          router.close ();
          zmqRouter.close();
        }
      });
      
      for (ElvinURI uri : router.listenURIs ())
      {
        for (InetSocketAddress address : addressesFor (uri))
          info ("Router listening on " + address + " (" + uri + ")", Main.class);
      }
      
      if (router.options ().getBoolean ("Federation.Activated"))
      {
        for (EwafURI uri : federationManagerFor (router).listenURIs ())
        {
          for (InetSocketAddress address : addressesFor (uri))
          {
            info ("Federator listening on " + address + " (" + uri + ")", 
                  Main.class);
          }
        }
      }
    } catch (Throwable ex)
    {
      if (ex instanceof IllegalArgumentException)
      {
        if (ex instanceof IllegalCommandLineOption)
        {
          alarm (ex.getMessage (), Main.class);
        
          System.err.println ();
          System.err.println (USAGE);
        } else
        {
          alarm ("Error in router configuration: " + ex.getMessage (), 
                 Main.class);
        }
        
        exit (1);
      } else
      {
        alarm ("Error starting router: " + ex.getMessage (), Main.class);
        
        if (shouldLog (DIAGNOSTIC))
          ex.printStackTrace ();
        
        exit (2);
      }
    }
  }

  /**
   * Create and start a router with a given set of command line
   * arguments.
   * 
   * @param args The command line.
   * 
   * @return The new router instance.
   * 
   * @throws IllegalConfigOptionException
   * @throws IOException
   */
  public static Router start (String... args) 
    throws IllegalConfigOptionException, IOException
  {
    RouterOptionSet routerOptionSet = new RouterOptionSet ();
    
    // add federation options to router's option set
    routerOptionSet.inheritFrom (FederationOptionSet.OPTION_SET);
    
    RouterOptions config = new RouterOptions (routerOptionSet);
    
    parseCommandLine (args, config);
    
    Router router = new Router (config);
    
    if (config.getBoolean ("Federation.Activated"))
      new FederationManager (router, config);

    return router;
  }
  
  private static void parseCommandLine (String [] args,
                                        RouterOptions config) 
    throws IllegalConfigOptionException, IllegalCommandLineOption
  {
    for (int i = 0; i < args.length; i++)
    {
      String arg = args [i];
    
      try
      {
        if (arg.equals ("-v"))
        {
          enableLogging (DIAGNOSTIC, true);
        } else if (arg.equals ("-vv"))
        {
          enableLogging (DIAGNOSTIC, true);
          enableLogging (TRACE, true);
        } else if (arg.equals ("-p"))
        {
          config.set ("Port", intArg (args, ++i));
        } else if (arg.equals ("-c"))
        {
          File configFile = 
            new File (stringArg (args, ++i)).getAbsoluteFile ();
          
          config.setAll (propertiesFrom (fileStream (configFile)));
          config.setRelativeDirectory (configFile.getParentFile ());
          
          diagnostic ("Read configuration from " + configFile, Main.class);
        } else
        {
          throw new IllegalCommandLineOption 
            ("Unknown command line option: \"" + arg + "\"");
        }
      } catch (IOException ex)
      {
        throw new IllegalCommandLineOption 
          ("Error in command line option: \"" + arg + "\": " + 
           ex.getMessage ());
      }
    }
  }

  private static void exit (int errorCode)
  {
    info ("Exiting on error", Main.class);
    
    System.exit (errorCode);
  }

  private static Properties readAvisProperties ()
    throws IOException
  {
    Properties properties;
    
    try
    {
      properties = propertiesFrom (resourceStream ("/avis.properties"));
    } catch (IOException ex)
    {
      warn ("Failed to load Avis property file: " + ex.getMessage (),
            Main.class);
      
      properties = new Properties ();
    }
    
    if (!properties.containsKey ("avis.router.version"))
      properties.put ("avis.router.version", "<unknown>");
    
    return properties;
  }
}
