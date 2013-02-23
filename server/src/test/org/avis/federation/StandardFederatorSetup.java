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
package org.avis.federation;

import org.avis.config.Options;
import org.avis.router.Router;
import org.avis.router.SimpleClient;
import org.avis.subscription.parser.ParseException;

import static java.net.InetAddress.getLocalHost;

import static org.avis.federation.JUTestFederation.PORT1;
import static org.avis.federation.JUTestFederation.PORT2;
import static org.avis.federation.TestUtils.waitForConnect;
import static org.avis.util.Collections.set;

public class StandardFederatorSetup
{
  public Router server1;
  public Router server2;
  
  public SimpleClient client1;
  public SimpleClient client2;
  
  public Connector connector;
  public Acceptor acceptor;
  
  public StandardFederatorSetup ()
    throws Exception
  {
    this (new Options (FederationOptionSet.OPTION_SET));
  }
  
  public StandardFederatorSetup (FederationClasses classes1,
                                 FederationClasses classes2)
    throws Exception
  {
    this (classes1, classes2, new Options (FederationOptionSet.OPTION_SET));
  }
  
  public StandardFederatorSetup (Options options)
    throws Exception
  {
    this (new FederationClasses (defaultClass ()),
          new FederationClasses (defaultClass ()),
          options);
  }

  public static FederationClass defaultClass ()
    throws ParseException
  {
    return new FederationClass ("require (federated)",
                                "require (federated)");
  }
  
  public StandardFederatorSetup (FederationClasses classes1,
                                 FederationClasses classes2,
                                 Options options)
    throws Exception
  {
    server1 = new Router (JUTestFederation.PORT1);
    server2 = new Router (JUTestFederation.PORT2);

    EwafURI ewafURI = new EwafURI ("ewaf://localhost:" + (PORT1 + 1));
    
    acceptor = 
      new Acceptor (server2, "server2", classes2, set (ewafURI), options);
    
    connector = 
      new Connector (server1, "server1", ewafURI, 
                     classes1.classFor (getLocalHost ()), options);
    
    waitForConnect (connector);
    
    client1 = new SimpleClient ("client1", "localhost", PORT1);
    client2 = new SimpleClient ("client2", "localhost", PORT2);
    
    client1.connect ();
    client2.connect ();
    
    client1.subscribe ("require (federated) && from == 'client2'");
    client2.subscribe ("require (federated) && from == 'client1'");
  }
  
  public void close ()
    throws Exception
  {
    client1.close ();
    client2.close ();
    
    connector.close ();
    acceptor.close ();
    
    server1.close ();
    server2.close ();
  }
}