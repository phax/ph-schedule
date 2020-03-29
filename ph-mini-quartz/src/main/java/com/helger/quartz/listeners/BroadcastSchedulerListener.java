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

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.TriggerKey;

/**
 * Holds a List of references to SchedulerListener instances and broadcasts all
 * events to them (in order).
 * <p>
 * This may be more convenient than registering all of the listeners directly
 * with the Scheduler, and provides the flexibility of easily changing which
 * listeners get notified.
 * </p>
 *
 * @see #addListener(com.helger.quartz.ISchedulerListener)
 * @see #removeListener(com.helger.quartz.ISchedulerListener)
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastSchedulerListener implements ISchedulerListener
{
  private final ICommonsList <ISchedulerListener> m_aListeners = new CommonsLinkedList <> ();

  public BroadcastSchedulerListener ()
  {}

  /**
   * Construct an instance with the given List of listeners.
   *
   * @param listeners
   *        the initial List of SchedulerListeners to broadcast to.
   */
  public BroadcastSchedulerListener (final Iterable <? extends ISchedulerListener> listeners)
  {
    m_aListeners.addAll (listeners);
  }

  public void addListener (@Nonnull final ISchedulerListener listener)
  {
    ValueEnforcer.notNull (listener, "Listener");
    m_aListeners.add (listener);
  }

  public boolean removeListener (final ISchedulerListener listener)
  {
    return m_aListeners.remove (listener);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISchedulerListener> getListeners ()
  {
    return m_aListeners.getClone ();
  }

  @Override
  public void jobAdded (final IJobDetail jobDetail)
  {
    m_aListeners.forEach (x -> x.jobAdded (jobDetail));
  }

  @Override
  public void jobDeleted (final JobKey jobKey)
  {
    m_aListeners.forEach (x -> x.jobDeleted (jobKey));
  }

  @Override
  public void jobScheduled (final ITrigger trigger)
  {
    m_aListeners.forEach (x -> x.jobScheduled (trigger));
  }

  @Override
  public void jobUnscheduled (final TriggerKey triggerKey)
  {
    m_aListeners.forEach (x -> x.jobUnscheduled (triggerKey));
  }

  @Override
  public void triggerFinalized (final ITrigger trigger)
  {
    m_aListeners.forEach (x -> x.triggerFinalized (trigger));
  }

  @Override
  public void triggerPaused (final TriggerKey key)
  {
    m_aListeners.forEach (x -> x.triggerPaused (key));
  }

  @Override
  public void triggersPaused (final String triggerGroup)
  {
    m_aListeners.forEach (x -> x.triggersPaused (triggerGroup));
  }

  @Override
  public void triggerResumed (final TriggerKey key)
  {
    m_aListeners.forEach (x -> x.triggerResumed (key));
  }

  @Override
  public void triggersResumed (final String triggerGroup)
  {
    m_aListeners.forEach (x -> x.triggersResumed (triggerGroup));
  }

  @Override
  public void schedulingDataCleared ()
  {
    m_aListeners.forEach (ISchedulerListener::schedulingDataCleared);
  }

  @Override
  public void jobPaused (final JobKey key)
  {
    m_aListeners.forEach (x -> x.jobPaused (key));
  }

  @Override
  public void jobsPaused (final String jobGroup)
  {
    m_aListeners.forEach (x -> x.jobsPaused (jobGroup));
  }

  @Override
  public void jobResumed (final JobKey key)
  {
    m_aListeners.forEach (x -> x.jobResumed (key));
  }

  @Override
  public void jobsResumed (final String jobGroup)
  {
    m_aListeners.forEach (x -> x.jobsResumed (jobGroup));
  }

  @Override
  public void schedulerError (final String msg, final SchedulerException cause)
  {
    m_aListeners.forEach (x -> x.schedulerError (msg, cause));
  }

  @Override
  public void schedulerStarted ()
  {
    m_aListeners.forEach (ISchedulerListener::schedulerStarted);
  }

  @Override
  public void schedulerStarting ()
  {
    m_aListeners.forEach (ISchedulerListener::schedulerStarting);
  }

  @Override
  public void schedulerInStandbyMode ()
  {
    m_aListeners.forEach (ISchedulerListener::schedulerInStandbyMode);
  }

  @Override
  public void schedulerShutdown ()
  {
    m_aListeners.forEach (ISchedulerListener::schedulerShutdown);
  }

  @Override
  public void schedulerShuttingdown ()
  {
    m_aListeners.forEach (ISchedulerListener::schedulerShuttingdown);
  }
}
