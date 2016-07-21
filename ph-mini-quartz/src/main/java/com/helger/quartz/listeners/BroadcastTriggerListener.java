/*
 * Copyright 2001-2009 Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.helger.quartz.listeners;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.helger.quartz.JobExecutionContext;
import com.helger.quartz.Trigger;
import com.helger.quartz.TriggerListener;
import com.helger.quartz.Trigger.CompletedExecutionInstruction;

/**
 * Holds a List of references to TriggerListener instances and broadcasts all
 * events to them (in order).
 * <p>
 * The broadcasting behavior of this listener to delegate listeners may be more
 * convenient than registering all of the listeners directly with the Scheduler,
 * and provides the flexibility of easily changing which listeners get notified.
 * </p>
 *
 * @see #addListener(com.helger.quartz.TriggerListener)
 * @see #removeListener(com.helger.quartz.TriggerListener)
 * @see #removeListener(String)
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastTriggerListener implements TriggerListener
{

  private final String name;
  private final List <TriggerListener> listeners;

  /**
   * Construct an instance with the given name. (Remember to add some delegate
   * listeners!)
   *
   * @param name
   *        the name of this instance
   */
  public BroadcastTriggerListener (final String name)
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
   *        the initial List of TriggerListeners to broadcast to.
   */
  public BroadcastTriggerListener (final String name, final List <TriggerListener> listeners)
  {
    this (name);
    this.listeners.addAll (listeners);
  }

  public String getName ()
  {
    return name;
  }

  public void addListener (final TriggerListener listener)
  {
    listeners.add (listener);
  }

  public boolean removeListener (final TriggerListener listener)
  {
    return listeners.remove (listener);
  }

  public boolean removeListener (final String listenerName)
  {
    final Iterator <TriggerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final TriggerListener l = itr.next ();
      if (l.getName ().equals (listenerName))
      {
        itr.remove ();
        return true;
      }
    }
    return false;
  }

  public List <TriggerListener> getListeners ()
  {
    return java.util.Collections.unmodifiableList (listeners);
  }

  public void triggerFired (final Trigger trigger, final JobExecutionContext context)
  {

    final Iterator <TriggerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final TriggerListener l = itr.next ();
      l.triggerFired (trigger, context);
    }
  }

  public boolean vetoJobExecution (final Trigger trigger, final JobExecutionContext context)
  {

    final Iterator <TriggerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final TriggerListener l = itr.next ();
      if (l.vetoJobExecution (trigger, context))
      {
        return true;
      }
    }
    return false;
  }

  public void triggerMisfired (final Trigger trigger)
  {

    final Iterator <TriggerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final TriggerListener l = itr.next ();
      l.triggerMisfired (trigger);
    }
  }

  public void triggerComplete (final Trigger trigger,
                               final JobExecutionContext context,
                               final CompletedExecutionInstruction triggerInstructionCode)
  {

    final Iterator <TriggerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final TriggerListener l = itr.next ();
      l.triggerComplete (trigger, context, triggerInstructionCode);
    }
  }

}
