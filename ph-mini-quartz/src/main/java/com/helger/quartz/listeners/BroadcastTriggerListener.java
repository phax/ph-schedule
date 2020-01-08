/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.ITriggerListener;

/**
 * Holds a List of references to TriggerListener instances and broadcasts all
 * events to them (in order).
 * <p>
 * The broadcasting behavior of this listener to delegate listeners may be more
 * convenient than registering all of the listeners directly with the Scheduler,
 * and provides the flexibility of easily changing which listeners get notified.
 * </p>
 *
 * @see #addListener(com.helger.quartz.ITriggerListener)
 * @see #removeListener(com.helger.quartz.ITriggerListener)
 * @see #removeListener(String)
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastTriggerListener implements ITriggerListener
{
  private final String m_sName;
  private final ICommonsList <ITriggerListener> m_aListeners = new CommonsArrayList <> ();

  /**
   * Construct an instance with the given name. (Remember to add some delegate
   * listeners!)
   *
   * @param name
   *        the name of this instance
   */
  public BroadcastTriggerListener (@Nonnull final String name)
  {
    ValueEnforcer.notNull (name, "Name");
    m_sName = name;
  }

  /**
   * Construct an instance with the given name, and List of listeners.
   *
   * @param name
   *        the name of this instance
   * @param listeners
   *        the initial List of TriggerListeners to broadcast to.
   */
  public BroadcastTriggerListener (@Nonnull final String name, final Iterable <? extends ITriggerListener> listeners)
  {
    this (name);
    m_aListeners.addAll (listeners);
  }

  @Nonnull
  public String getName ()
  {
    return m_sName;
  }

  public void addListener (@Nonnull final ITriggerListener listener)
  {
    ValueEnforcer.notNull (listener, "Listener");
    m_aListeners.add (listener);
  }

  public boolean removeListener (final ITriggerListener listener)
  {
    return m_aListeners.remove (listener);
  }

  public boolean removeListener (final String listenerName)
  {
    final Iterator <ITriggerListener> itr = m_aListeners.iterator ();
    while (itr.hasNext ())
    {
      final ITriggerListener l = itr.next ();
      if (l.getName ().equals (listenerName))
      {
        itr.remove ();
        return true;
      }
    }
    return false;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ITriggerListener> getListeners ()
  {
    return m_aListeners.getClone ();
  }

  public void triggerFired (final ITrigger trigger, final IJobExecutionContext context)
  {
    m_aListeners.forEach (x -> x.triggerFired (trigger, context));
  }

  public boolean vetoJobExecution (final ITrigger trigger, final IJobExecutionContext context)
  {
    for (final ITriggerListener l : m_aListeners)
      if (l.vetoJobExecution (trigger, context))
        return true;
    return false;
  }

  public void triggerMisfired (final ITrigger trigger)
  {
    m_aListeners.forEach (x -> x.triggerMisfired (trigger));
  }

  public void triggerComplete (final ITrigger trigger,
                               final IJobExecutionContext context,
                               final ECompletedExecutionInstruction triggerInstructionCode)
  {
    m_aListeners.forEach (x -> x.triggerComplete (trigger, context, triggerInstructionCode));
  }
}
