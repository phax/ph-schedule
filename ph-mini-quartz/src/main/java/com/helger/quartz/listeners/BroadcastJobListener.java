/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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

import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
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
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastJobListener implements IJobListener
{
  private final String m_sName;
  private final ICommonsList <IJobListener> m_aListeners = new CommonsArrayList <> ();

  /**
   * Construct an instance with the given name. (Remember to add some delegate
   * listeners!)
   *
   * @param name
   *        the name of this instance
   */
  public BroadcastJobListener (@Nonnull final String name)
  {
    ValueEnforcer.notNull (name, "Listener Name");
    m_sName = name;
  }

  /**
   * Construct an instance with the given name, and List of listeners.
   *
   * @param name
   *        the name of this instance
   * @param listeners
   *        the initial List of JobListeners to broadcast to.
   */
  public BroadcastJobListener (@Nonnull final String name, final List <IJobListener> listeners)
  {
    this (name);
    m_aListeners.addAll (listeners);
  }

  @Nonnull
  public String getName ()
  {
    return m_sName;
  }

  public void addListener (final IJobListener listener)
  {
    m_aListeners.add (listener);
  }

  public boolean removeListener (final IJobListener listener)
  {
    return m_aListeners.remove (listener);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IJobListener> getListeners ()
  {
    return m_aListeners.getClone ();
  }

  @Override
  public void jobToBeExecuted (final IJobExecutionContext context)
  {
    m_aListeners.forEach (x -> x.jobToBeExecuted (context));
  }

  @Override
  public void jobExecutionVetoed (final IJobExecutionContext context)
  {
    m_aListeners.forEach (x -> x.jobExecutionVetoed (context));
  }

  @Override
  public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {
    m_aListeners.forEach (x -> x.jobWasExecuted (context, jobException));
  }
}
