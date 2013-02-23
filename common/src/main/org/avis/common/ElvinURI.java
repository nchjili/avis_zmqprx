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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;

import static org.avis.common.Common.CLIENT_VERSION_MAJOR;
import static org.avis.common.Common.CLIENT_VERSION_MINOR;
import static org.avis.common.Common.DEFAULT_PORT;
import static org.avis.util.Collections.join;
import static org.avis.util.Collections.list;

/**
 * An Elvin URI identifying an Elvin router as described in the "Elvin
 * URI Scheme" specification at
 * http://elvin.org/specs/draft-elvin-uri-prelim-02.txt. The most
 * common Elvin URI for a TCP endpoint is of the form (sections in in []
 * are optional):
 * 
 * <pre>
 *   elvin:[version]/[protocol]/hostname[:port][;options]
 *   
 *   version:  protocol version major.minor form e.g. "4.0"
 *   protocol: protocol stack in transport,security,marshalling order
 *             e.g. "tcp,none,xdr". Alternatively the alias "secure"
 *             can be used to denote the default secure stack
 *             ("ssl,none,xdr").
 *   options:  name1=value1[;name2=value2]* e.g. foo=bar;black=white
 * </pre>
 * 
 * <p>
 * 
 * Example URI 1: <code>elvin://localhost:2917</code>
 * <p>
 * Example URI 2: <code>elvin://192.168.0.2:2917;foo=true</code>
 * <p>
 * Example URI 3: <code>elvin:4.0/ssl,none,xdr/localhost:443</code>
 * 
 * @author Matthew Phillips
 */
public class ElvinURI
{
  private static final List<String> DEFAULT_PROTOCOL =
    list ("tcp", "none", "xdr");
  
  /*
   * Note: you might expect "tcp,ssl,xdr" as the logical secure stack,
   * but Mantara Elvin uses "ssl,none,xdr" and spec also indicates
   * this is correct, so we comply.
   */
  private static final List<String> SECURE_PROTOCOL =
    list ("ssl", "none", "xdr");
  
  /**
   * Basic matcher for URI's. Key sections pulled out here: detail
   * parsing is done as a separate pass.
   */
  private static final Pattern URL_PATTERN =
    // NB: key sections of regexp are marked with | below
    //                |scheme|ver     |protocol|host:port  |options 
    Pattern.compile ("(\\w+):([^/]+)?/([^/]+)?/([^;/][^;]*)(;.*)?");

  /**
   * The original URI string as passed into the constructor.
   */
  public String uriString;
  
  /**
   * The URI scheme (i.e the part before the ":"). This must be 
   * "elvin" for URI's referring to Elvin routers.
   */
  public String scheme;
  
  /**
   * Major protocol version. Default is {@link Common#CLIENT_VERSION_MAJOR}.
   */
  public int versionMajor;
  
  /**
   * Minor protocol version. Default is {@link Common#CLIENT_VERSION_MINOR}.
   */
  public int versionMinor;
  
  /**
   * The stack of protocol modules in (transport,security,marshalling)
   * order. e.g. "tcp", "none", "xdr". See also
   * {@link #defaultProtocol()}
   */
  public List<String> protocol;
  
  /**
   * The host name.
   */
  public String host;
  
  /**
   * The port. Default is {@link Common#DEFAULT_PORT}.
   */
  public int port;

  /**
   * The URI options. e.g. elvin://host:port;option1=value1;option2=value2
   */
  public Map<String, String> options;

  private int hash;

  /**
   * Create a new instance.
   * 
   * @param uriString The URI.
   * 
   * @throws InvalidURIException if the URI is not valid.
   */
  public ElvinURI (String uriString)
    throws InvalidURIException
  {
    init ();

    this.uriString = uriString;
    
    parseUri ();
    
    validate ();
    
    this.hash = computeHash ();
  }

  /**
   * Create a new instance from a host and port using defaults for others.
   * 
   * @param host Host name or IP address
   * @param port Port number.
   */
  public ElvinURI (String host, int port)
  {
    init ();
    
    this.uriString = "elvin://" + host + ':' + port;
    this.scheme = "elvin";
    this.host = host;
    this.port = port;
    this.hash = computeHash ();
  }

  /**
   * Create a new instance using an existing URI for defaults.
   * 
   * @param uriString The URI string.
   * @param defaultUri The URI to use for any values that are not
   *          specified by uriString.
   * @throws InvalidURIException if the URI is not valid.
   */
  public ElvinURI (String uriString, ElvinURI defaultUri)
    throws InvalidURIException
  {
    init (defaultUri);
    
    this.uriString = uriString;
    
    parseUri ();
    
    validate ();
    
    this.hash = computeHash ();
  }
  
  /**
   * Create a copy of a URI.
   * 
   * @param defaultUri The URI to copy.
   */
  public ElvinURI (ElvinURI defaultUri)
  {
    init (defaultUri);
    
    validate ();
  }

  protected void init (ElvinURI defaultUri)
  {
    this.uriString = defaultUri.uriString;
    this.scheme = defaultUri.scheme;
    this.versionMajor = defaultUri.versionMajor;
    this.versionMinor = defaultUri.versionMinor;
    this.protocol = defaultUri.protocol;
    this.host = defaultUri.host;
    this.port = defaultUri.port;
    this.options = defaultUri.options;
    this.hash = defaultUri.hash;
  }

  protected void init ()
  {
    this.scheme = null;
    this.versionMajor = CLIENT_VERSION_MAJOR;
    this.versionMinor = CLIENT_VERSION_MINOR;
    this.protocol = DEFAULT_PROTOCOL;
    this.host = null;
    this.port = DEFAULT_PORT;
    this.options = emptyMap ();
  }
  
  private void validate ()
  {
    if (!validScheme (scheme))
      throw new InvalidURIException (uriString, "Invalid scheme: " + scheme);
  }

  /**
   * Check if scheme is valid. May be extended.
   */
  protected boolean validScheme (String schemeToCheck)
  {
    return schemeToCheck.equals ("elvin");
  }

  @Override
  public String toString ()
  {
    return uriString;
  }
  
  /**
   * Generate a canonical text version of this URI.
   */
  public String toCanonicalString ()
  {
    StringBuilder str = new StringBuilder ();
    
    str.append (scheme).append (':');
    
    str.append (versionMajor).append ('.').append (versionMinor);
    
    str.append ('/');
    
    join (str, protocol, ',');
    
    str.append ('/').append (host).append (':').append (port);

    // NB: options is a sorted map, canonical order is automatic
    for (Entry<String, String> option : options.entrySet ())
    {
      str.append (';');
      str.append (option.getKey ()).append ('=').append (option.getValue ());
    }
    
    return str.toString ();
  }
  
  @Override
  public int hashCode ()
  {
    return hash;
  }
  
  @Override
  public boolean equals (Object obj)
  {
    return obj instanceof ElvinURI && equals ((ElvinURI)obj);
  }

  public boolean equals (ElvinURI uri)
  {
    return hash == uri.hash &&
           scheme.equals (uri.scheme) &&
           host.equals (uri.host) &&
           port == uri.port &&
           versionMajor == uri.versionMajor &&
           versionMinor == uri.versionMinor && 
           options.equals (uri.options) &&
           protocol.equals (uri.protocol);
  }
  
  private int computeHash ()
  {
    return scheme.hashCode () ^ host.hashCode () ^ port ^ protocol.hashCode ();
  }
  
  private void parseUri ()
    throws InvalidURIException
  { 
    Matcher matcher = URL_PATTERN.matcher (uriString);
    
    if (!matcher.matches ())
      throw new InvalidURIException (uriString, "Not a valid Elvin URI");
    
    scheme = matcher.group (1);
    
    // version
    if (matcher.group (2) != null)
      parseVersion (matcher.group (2));
    
    // protocol
    if (matcher.group (3) != null)
      parseProtocol (matcher.group (3));
    
    // endpoint (host/port)
    parseEndpoint (matcher.group (4));
    
    // options
    if (matcher.group (5) != null)
      parseOptions (matcher.group (5));
  }

  private void parseVersion (String versionExpr)
    throws InvalidURIException
  {
    Matcher versionMatch =
      Pattern.compile ("(\\d+)(?:\\.(\\d+))?").matcher (versionExpr);
    
    if (versionMatch.matches ())
    {
      try
      {
        versionMajor = parseInt (versionMatch.group (1));
        
        if (versionMatch.group (2) != null)
          versionMinor = parseInt (versionMatch.group (2));
      } catch (NumberFormatException ex)
      {
        throw new InvalidURIException (uriString,
                                       "Number too large in version string: \"" +
                                       versionExpr + "\"");
      }
    } else
    {
      throw new InvalidURIException (uriString,
                                     "Invalid version string: \"" +
                                     versionExpr + "\"");
    }
  }
  
  private void parseProtocol (String protocolExpr)
    throws InvalidURIException
  {
    Matcher protocolMatch =
      Pattern.compile ("(?:(\\w+),(\\w+),(\\w+))|secure").matcher (protocolExpr);
    
    if (protocolMatch.matches ())
    {
      if (protocolMatch.group (1) != null)
        protocol = asList (protocolExpr.split (","));
      else
        protocol = SECURE_PROTOCOL;
    } else
    {
      throw new InvalidURIException (uriString,
                                     "Invalid protocol: \"" +
                                     protocolExpr + "\"");
    }
  }
  
  private void parseEndpoint (String endpoint)
    throws InvalidURIException
  {
    Pattern pattern;
    
    // choose between IPv6 and IPv4 address scheme
    if (endpoint.charAt (0) == '[')
      pattern = Pattern.compile ("(\\[[^\\]]+\\])(?::(\\d+))?");
    else
      pattern = Pattern.compile ("([^:]+)(?::(\\d+))?");
    
    Matcher endpointMatch = pattern.matcher (endpoint);
    
    if (endpointMatch.matches ())
    {
      host = endpointMatch.group (1);
      
      if (endpointMatch.group (2) != null)
        port = parseInt (endpointMatch.group (2));
    } else
    {
      throw new InvalidURIException (uriString, "Invalid port number");
    }
  }
  
  private void parseOptions (String optionsExpr)
    throws InvalidURIException
  {
    Matcher optionMatch =
      Pattern.compile (";([^=;]+)=([^=;]*)").matcher (optionsExpr);
    
    options = new TreeMap<String, String> ();
    
    int index = 0;
    
    while (optionMatch.lookingAt ())
    {
      options.put (optionMatch.group (1), optionMatch.group (2));
      
      index = optionMatch.end ();
      optionMatch.region (index, optionsExpr.length ());
    }
    
    if (index != optionsExpr.length ())
      throw new InvalidURIException
        (uriString, "Invalid options: \"" + optionsExpr + "\"");
  }

  /**
   * The default URI protocol stack: "tcp", "none", "xdr"
   */
  public static List<String> defaultProtocol ()
  {
    return DEFAULT_PROTOCOL;
  }

  /**
   * The secure URI protocol stack: "ssl", "none", "xdr"
   */
  public static List<String> secureProtocol ()
  {
    return SECURE_PROTOCOL;
  }

  /**
   * True if this URI specifies secure TLS transport (protocol.equals
   * (secureProtocol ())).
   */
  public boolean isSecure ()
  {
    return protocol.equals (secureProtocol ());
  }
}
