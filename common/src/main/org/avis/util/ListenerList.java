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

import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * A generic event listener list. The list is thread safe, but does
 * not guarantee immediate visibility of changes: i.e. if thread1
 * executes a remove () and thread2 executes a fire () immediately
 * after, thread2 will not necessarily see the result of the remove ().
 * 
 * @author Matthew Phillips
 */
public class ListenerList<E>
{
  private List<E> listeners;
  private Method listenerMethod;
  
  /**
   * Create a new instance.
   * 
   * @param listenerType The type of the listener interface.
   * @param method The name of the method to call on the interface.
   * @param eventType The type of the single event parameter.
   * 
   * @throws IllegalArgumentException if the listener method could not
   *                 be found.
   */
  public ListenerList (Class<E> listenerType, String method, Class<?> eventType)
    throws IllegalArgumentException
  {
    this.listenerMethod = lookupMethod (listenerType, method, eventType);
    this.listeners = emptyList ();
  }
  
  /**
   * Create a new instance.
   * 
   * @param listenerType The type of the listener interface.
   * @param method The name of the method to call on the interface.
   * @param paramTypes The paramter types of the listener method.
   * 
   * @throws IllegalArgumentException if the listener method could not
   *                 be found.
   */
  public ListenerList (Class<E> listenerType, 
                       String method, 
                       Class<?>... paramTypes)
    throws IllegalArgumentException
  {
    this.listenerMethod = lookupMethod (listenerType, method, paramTypes);
    this.listeners = emptyList ();
  }
  
  public List<E> asList ()
  {
    return unmodifiableList (listeners);
  }
  
  public void add (E listener)
  {
    if (listener == null)
      throw new IllegalArgumentException ("Listener cannot be null");
    
    List<E> newListeners = new ArrayList<E> (listeners.size () + 4);
    
    newListeners.addAll (listeners);
    newListeners.add (listener);
    
    listeners = newListeners;
  }
  
  public void remove (E listener)
  {
    List<E> newListeners = new ArrayList<E> (listeners);
    
    newListeners.remove (listener);
    
    listeners = newListeners;
  }

  /**
   * Fire an event.
   * 
   * @param event The event parameter.
   */
  public void fire (Object event)
  {
    if (!listeners.isEmpty ())
      fire (new Object [] {event});
  }
  
  /**
   * Fire an event.
   * 
   * @param args The event parameters.
   */
  public void fire (Object... args)
  {
    List<E> fireList = listeners;
    
    for (int i = fireList.size () - 1; i >= 0; i--)
    {
      E listener = fireList.get (i);
      
      try
      {
        listenerMethod.invoke (listener, args); 
      } catch (InvocationTargetException ex)
      {
        throw new RuntimeException ("Error in listener method",
                                    ex.getCause ());
      } catch (Exception ex)
      {
        // should not be possible
        throw new RuntimeException (ex);
      }
    }
  }
  
  /**
   * Test if any listeners are in this list.
   */
  public boolean hasListeners ()
  {
    return !listeners.isEmpty ();
  }

  private static Method lookupMethod (Class<?> targetClass,
                                      String methodName,
                                      Class<?>... paramTypes)
  {
    try
    {
      Method method = targetClass.getMethod (methodName, paramTypes);
      
      method.setAccessible (true);
      
      return method;
    } catch (Exception ex)
    {
      throw new IllegalArgumentException ("No method named " + methodName);
    }
  }
}
