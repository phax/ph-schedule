/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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
package com.helger.quartz.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.ITrigger;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.spi.ISchedulerSignaler;

/**
 * An interface to be used by <code>JobStore</code> instances in order to
 * communicate signals back to the <code>QuartzScheduler</code>.
 *
 * @author jhouse
 */
public class SchedulerSignaler implements ISchedulerSignaler
{
  private static final Logger log = LoggerFactory.getLogger (SchedulerSignaler.class);

  protected QuartzScheduler m_aScheduler;
  protected QuartzSchedulerThread m_aSchedulerThread;

  public SchedulerSignaler (final QuartzScheduler sched, final QuartzSchedulerThread schedThread)
  {
    this.m_aScheduler = sched;
    this.m_aSchedulerThread = schedThread;

    log.info ("Initialized Scheduler Signaller of type: " + getClass ());
  }

  public void notifyTriggerListenersMisfired (final ITrigger trigger)
  {
    try
    {
      m_aScheduler.notifyTriggerListenersMisfired (trigger);
    }
    catch (final SchedulerException se)
    {
      m_aScheduler.getLog ().error ("Error notifying listeners of trigger misfire.", se);
      m_aScheduler.notifySchedulerListenersError ("Error notifying listeners of trigger misfire.", se);
    }
  }

  public void notifySchedulerListenersFinalized (final ITrigger trigger)
  {
    m_aScheduler.notifySchedulerListenersFinalized (trigger);
  }

  public void signalSchedulingChange (final long candidateNewNextFireTime)
  {
    m_aSchedulerThread.signalSchedulingChange (candidateNewNextFireTime);
  }

  public void notifySchedulerListenersJobDeleted (final JobKey jobKey)
  {
    m_aScheduler.notifySchedulerListenersJobDeleted (jobKey);
  }

  public void notifySchedulerListenersError (final String string, final SchedulerException jpe)
  {
    m_aScheduler.notifySchedulerListenersError (string, jpe);
  }
}
