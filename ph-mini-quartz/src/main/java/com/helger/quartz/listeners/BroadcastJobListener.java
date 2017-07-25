/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.quartz.listeners;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.JobExecutionException;

/**
 * Holds a List of references to JobListener instances and broadcasts all events
 * to them (in order).
 * <p>
 * The broadcasting behavior of this listener to delegate listeners may be more
 * convenient than registering all of the listeners directly with the Scheduler,
 * and provides the flexibility of easily changing which listeners get notified.
 * </p>
 *
 * @see #addListener(com.helger.quartz.IJobListener)
 * @see #removeListener(com.helger.quartz.IJobListener)
 * @see #removeListener(String)
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastJobListener implements IJobListener
{

  private final String name;
  private final List <IJobListener> listeners;

  /**
   * Construct an instance with the given name. (Remember to add some delegate
   * listeners!)
   *
   * @param name
   *        the name of this instance
   */
  public BroadcastJobListener (final String name)
  {
    if (name == null)
    {
      throw new IllegalArgumentException ("Listener name cannot be null!");
    }
    this.name = name;
    listeners = new LinkedList <> ();
  }

  /**
   * Construct an instance with the given name, and List of listeners.
   *
   * @param name
   *        the name of this instance
   * @param listeners
   *        the initial List of JobListeners to broadcast to.
   */
  public BroadcastJobListener (final String name, final List <IJobListener> listeners)
  {
    this (name);
    this.listeners.addAll (listeners);
  }

  public String getName ()
  {
    return name;
  }

  public void addListener (final IJobListener listener)
  {
    listeners.add (listener);
  }

  public boolean removeListener (final IJobListener listener)
  {
    return listeners.remove (listener);
  }

  public boolean removeListener (final String listenerName)
  {
    final Iterator <IJobListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final IJobListener jl = itr.next ();
      if (jl.getName ().equals (listenerName))
      {
        itr.remove ();
        return true;
      }
    }
    return false;
  }

  public List <IJobListener> getListeners ()
  {
    return java.util.Collections.unmodifiableList (listeners);
  }

  public void jobToBeExecuted (final IJobExecutionContext context)
  {

    final Iterator <IJobListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final IJobListener jl = itr.next ();
      jl.jobToBeExecuted (context);
    }
  }

  public void jobExecutionVetoed (final IJobExecutionContext context)
  {

    final Iterator <IJobListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final IJobListener jl = itr.next ();
      jl.jobExecutionVetoed (context);
    }
  }

  public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {

    final Iterator <IJobListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final IJobListener jl = itr.next ();
      jl.jobWasExecuted (context, jobException);
    }
  }

}
