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
package org.avis.common;

import java.net.URISyntaxException;

import org.avis.common.ElvinURI;

import org.junit.Test;

import static org.avis.common.Common.CLIENT_VERSION_MAJOR;
import static org.avis.common.Common.CLIENT_VERSION_MINOR;
import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.util.Collections.list;
import static org.avis.util.Collections.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class JUTestElvinURI
{
  @Test
  public void version ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host");
    
    assertEquals (CLIENT_VERSION_MAJOR, uri.versionMajor);
    assertEquals (CLIENT_VERSION_MINOR, uri.versionMinor);
    assertEquals ("elvin_host", uri.host);
    
    uri = new ElvinURI ("elvin:5.1//elvin_host");
    
    assertEquals (5, uri.versionMajor);
    assertEquals (1, uri.versionMinor);  
    assertEquals ("elvin_host", uri.host);
    
    uri = new ElvinURI ("elvin:5//elvin_host");
    
    assertEquals (5, uri.versionMajor);
    assertEquals (CLIENT_VERSION_MINOR, uri.versionMinor);
    assertEquals ("elvin_host", uri.host);
    
    assertInvalid ("http:hello//elvin_host");
    assertInvalid ("elvin:hello//elvin_host");
    assertInvalid ("elvin:4.0.0//elvin_host");
    assertInvalid ("elvin:4.//elvin_host");
    assertInvalid ("elvin: //elvin_host");
    assertInvalid ("elvin:111111111111111.2222222222222222222//elvin_host");
  }

  @Test
  public void protocol ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host");
    
    assertEquals (ElvinURI.defaultProtocol (), uri.protocol);
    
    uri = new ElvinURI ("elvin:/tcp,xdr,ssl/elvin_host");
    
    assertEquals (list ("tcp", "xdr", "ssl"), uri.protocol);
    assertEquals ("elvin_host", uri.host);
    
    uri = new ElvinURI ("elvin:/secure/elvin_host");
    
    assertEquals (ElvinURI.secureProtocol (), uri.protocol);
    
    assertInvalid ("elvin:/abc,xyz/elvin_host");
    assertInvalid ("elvin:/abc,xyz,dfg,qwe/elvin_host");
    assertInvalid ("elvin:/abc,/elvin_host");
    assertInvalid ("elvin:/,abc/elvin_host");
    assertInvalid ("elvin:/abc,,xyz/elvin_host");
    assertInvalid ("elvin:///elvin_host");
  }
  
  @Test
  public void endpoint ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host");
    assertEquals ("elvin_host", uri.host);
    assertEquals (DEFAULT_PORT, uri.port);
    
    uri = new ElvinURI ("elvin://elvin_host:12345");
    assertEquals ("elvin_host", uri.host);
    assertEquals (12345, uri.port);
    
    assertInvalid ("elvin://");
    assertInvalid ("elvin://hello:there");
  }
  
  @Test
  public void options ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host;name1=value1");
    
    assertEquals (map ("name1", "value1"), uri.options);
    
    uri = new ElvinURI ("elvin://elvin_host;name1=value1;name2=value2");
    
    assertEquals (map ("name1", "value1", "name2", "value2"), uri.options);
    
    assertInvalid ("elvin://elvin_host;name1;name2=value2");
    assertInvalid ("elvin://elvin_host;=name1;name2=value2");
    assertInvalid ("elvin://elvin_host;");
    assertInvalid ("elvin://;x=y");
    assertInvalid ("elvin://;x=");
  }
  
  @Test
  public void equality ()
    throws URISyntaxException
  {
    assertSameUri ("elvin://elvin_host", "elvin://elvin_host:2917");
    assertSameUri ("elvin://elvin_host", "elvin:/tcp,none,xdr/elvin_host");
    
    assertNotSameUri ("elvin://elvin_host", "elvin:/tcp,ssl,xdr/elvin_host");
    assertNotSameUri ("elvin://elvin_host", "elvin://elvin_host:29170");
    assertNotSameUri ("elvin://elvin_host", "elvin://elvin_host;name=value");
  }
  
  @Test
  public void canonicalize ()
    throws URISyntaxException
  {
    ElvinURI uri = new ElvinURI ("elvin://elvin_host");
    assertEquals ("elvin://elvin_host", uri.toString ());
    
    assertEquals ("elvin:4.0/tcp,none,xdr/elvin_host:2917",
                  uri.toCanonicalString ());

    uri = new ElvinURI ("elvin://elvin_host;name1=value1");
    assertEquals ("elvin:4.0/tcp,none,xdr/elvin_host:2917;name1=value1",
                  uri.toCanonicalString ());
    
    uri = new ElvinURI ("elvin:/secure/elvin_host:29170;b=2;a=1");
    assertEquals ("elvin:4.0/ssl,none,xdr/elvin_host:29170;a=1;b=2",
                  uri.toCanonicalString ());
    
    uri = new ElvinURI ("elvin:5.1/secure/elvin_host:29170;b=2;a=1");
    assertEquals ("elvin:5.1/ssl,none,xdr/elvin_host:29170;a=1;b=2",
                  uri.toCanonicalString ());
  }
  
  @Test
  public void constructors ()
    throws Exception
  {
    ElvinURI defaultUri = new ElvinURI ("elvin:5.6/a,b,c/default_host:1234");
    
    ElvinURI uri = new ElvinURI ("elvin://host", defaultUri);
    
    assertEquals (defaultUri.scheme, "elvin");
    assertEquals (defaultUri.versionMajor, uri.versionMajor);
    assertEquals (defaultUri.versionMinor, uri.versionMinor);
    assertEquals (defaultUri.protocol, uri.protocol);
    assertEquals ("host", uri.host);
    assertEquals (defaultUri.port, uri.port);
    
    uri = new ElvinURI ("elvin:7.0/x,y,z/host:5678", defaultUri);
    
    assertEquals (7, uri.versionMajor);
    assertEquals (0, uri.versionMinor);
    assertEquals (list ("x", "y", "z"), uri.protocol);
    assertEquals ("host", uri.host);
    assertEquals (5678, uri.port);
  }
  
  @Test
  public void ipv6 ()
    throws URISyntaxException
  {
    ElvinURI uri =
      new ElvinURI ("elvin://[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]:1234");
    
    assertEquals ("[2001:0db8:85a3:08d3:1319:8a2e:0370:7344]", uri.host);
    assertEquals (1234, uri.port);
    
    uri = new ElvinURI ("elvin:/tcp,xdr,ssl/[::1/128]:4567");
    
    assertEquals (list ("tcp", "xdr", "ssl"), uri.protocol);
    assertEquals ("[::1/128]", uri.host);
    assertEquals (4567, uri.port);
    
    assertInvalid ("elvin://[::1/128");
    assertInvalid ("elvin://[[::1/128");
    assertInvalid ("elvin://[::1/128]]");
    assertInvalid ("elvin://[]");
    assertInvalid ("elvin://[");
    assertInvalid ("elvin://[::1/128];hello");
    assertInvalid ("elvin://[::1/128]:xyz");
    assertInvalid ("elvin://[::1/128];");
    assertInvalid ("elvin:///[::1/128]");
  }
  
  private static void assertSameUri (String uri1, String uri2)
    throws URISyntaxException
  {
    assertEquals (new ElvinURI (uri1), new ElvinURI (uri2));
    assertEquals (new ElvinURI (uri1).hashCode (),
                  new ElvinURI (uri2).hashCode ());
  }
  
  private static void assertNotSameUri (String uri1, String uri2)
    throws URISyntaxException
  {
    assertFalse (new ElvinURI (uri1).equals (new ElvinURI (uri2)));
  }

  private static void assertInvalid (String uriString)
  {
    try
    {
      new ElvinURI (uriString);
      
      fail ("Invalid URI \"" + uriString + "\" not detected");
    } catch (InvalidURIException ex)
    {
      // ok
      // System.out.println ("error = " + ex.getMessage ());
    }
  }
}
