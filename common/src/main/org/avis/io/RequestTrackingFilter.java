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
package org.avis.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoSession;

import org.avis.io.messages.ErrorMessage;
import org.avis.io.messages.Message;
import org.avis.io.messages.RequestMessage;
import org.avis.io.messages.RequestTimeoutMessage;
import org.avis.io.messages.XidMessage;

import static java.lang.Math.min;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A MINA I/O filter that adds tracking for XID-based
 * RequestMessage's.
 * 
 * <ul>
 * <li>Automatically fills in the {@link XidMessage#request} field of
 * reply XidMessage's</li>
 * <li>Generates ErrorMessage's in place of XID-based replies that
 * have no associated request</li>
 * <li>Generates TimeoutMessage's for requests that do not receive a
 * reply within a given timeout.</li>
 * <ul>
 * 
 * @author Matthew Phillips
 */
public class RequestTrackingFilter 
  extends IoFilterAdapter implements IoFilter
{
  protected ScheduledExecutorService executor;
  protected long replyTimeout;
  protected String filterName;
  
  /**
   * Create a new instance. Uses a {@link SharedExecutor}.
   * 
   * @param replyTimeout The amount of time (in millis) to wait for
   *                a reply.
   */
  public RequestTrackingFilter (long replyTimeout)
  {
    this (null, replyTimeout);
  }
  
  /**
   * Create a new instance.
   * 
   * @param executor The executor to use for timed callbacks.
   * @param replyTimeout The amount of time (in millis) to wait for
   *                a reply.
   */
  public RequestTrackingFilter (ScheduledExecutorService executor,
                                long replyTimeout)
  {
    this.executor = executor;
    this.replyTimeout = replyTimeout;
  }
  
  @Override
  public void onPreAdd (IoFilterChain parent, String name,
                        NextFilter nextFilter) 
    throws Exception
  {
    if (executor == null)
      executor = SharedExecutor.acquire ();

    this.filterName = name;
  }
  
  @Override
  public void filterClose (NextFilter nextFilter, 
                           IoSession session)
    throws Exception
  {
    nextFilter.filterClose (session);
    
    if (SharedExecutor.release (executor))
      executor = null;
  }
  
  @Override
  public void sessionCreated (NextFilter nextFilter, 
                              IoSession session)
    throws Exception
  {
    nextFilter.sessionCreated (session);
  }
  
  @Override
  public void sessionOpened (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    session.setAttribute ("requestTracker", new Tracker (session));
    
    nextFilter.sessionOpened (session);
  }
  
  private static Tracker trackerFor (IoSession session)
  {
    return (Tracker)session.getAttribute ("requestTracker");
  }
  
  @Override
  public void sessionClosed (NextFilter nextFilter, IoSession session)
    throws Exception
  {
    Tracker tracker = trackerFor (session);
    
    if (tracker != null)
      tracker.dispose ();
    
    nextFilter.sessionClosed (session);
  }
  
  @Override
  public void filterWrite (NextFilter nextFilter, 
                           IoSession session,
                           WriteRequest writeRequest)
    throws Exception
  {
    Object message = writeRequest.getMessage ();
    
    if (message instanceof RequestMessage<?>)
      trackerFor (session).add ((RequestMessage<?>)message);
    
    nextFilter.filterWrite (session, writeRequest);
  }
  
  @Override
  public void messageReceived (NextFilter nextFilter,
                               IoSession session, Object message)
    throws Exception
  {
    if (message instanceof XidMessage && 
        !(message instanceof RequestMessage<?>))
    {
      XidMessage reply = (XidMessage)message;
      
      try
      {
        reply.request = trackerFor (session).remove (reply);
      } catch (IllegalArgumentException ex)
      {
        message = new ErrorMessage (ex, reply);
      }
    }
    
    nextFilter.messageReceived (session, message);
  }
  
  /**
   * An instance of this is attached to each session to track requests.
   */
  class Tracker implements Runnable
  {
    private IoSession session;
    private Map<Integer, Request> xidToRequest;
    private ScheduledFuture<?> replyFuture;
    
    public Tracker (IoSession session)
    {
      this.session = session;
      this.xidToRequest = new HashMap<Integer, Request> ();
    }
    
    public synchronized void dispose ()
    {
      cancelReplyCheck ();
    }
    
    public synchronized void add (RequestMessage<?> request)
    {
      xidToRequest.put (request.xid, new Request (request));
      
      scheduleReplyCheck (replyTimeout);
    }
    
    public synchronized RequestMessage<?> remove (XidMessage reply)
    {
      Request request = xidToRequest.remove (reply.xid);
      
      if (request == null)
        throw new IllegalArgumentException 
          ("Reply with unknown XID " + reply.xid);
      else
        return request.message;
    }
    
    private void cancelReplyCheck ()
    {
      if (replyFuture != null)
      {
        replyFuture.cancel (false);
        
        replyFuture = null;
      }
    }
    
    private void scheduleReplyCheck (long delay)
    {
      if (replyFuture == null)
        replyFuture = executor.schedule (this, delay, MILLISECONDS);
    }
    
    /**
     * Called periodically to check for timed-out requests.
     */
    public synchronized void run ()
    {
      replyFuture = null;
      
      long now = currentTimeMillis ();
      long earliestTimeout = now;
      
      for (Iterator<Map.Entry<Integer, Request>> i = 
             xidToRequest.entrySet ().iterator (); i.hasNext (); )
      {
        Request request = i.next ().getValue ();
        
        if (now - request.sentAt >= replyTimeout)
        {
          i.remove ();
          
          injectMessage (new RequestTimeoutMessage (request.message));
        } else
        {
          earliestTimeout = min (earliestTimeout, request.sentAt);
        }
      }
      
      if (!xidToRequest.isEmpty ())
        scheduleReplyCheck (replyTimeout - (now - earliestTimeout));
    }

    private void injectMessage (Message message)
    {
      NextFilter filter = 
        session.getFilterChain ().getNextFilter (filterName);
      
      filter.messageReceived (session, message);
    }
  }
  
  static class Request
  {
    public RequestMessage<?> message;
    public long sentAt;

    public Request (RequestMessage<?> request)
    {
      this.message = request;
      this.sentAt = currentTimeMillis ();
    }
  }
}
