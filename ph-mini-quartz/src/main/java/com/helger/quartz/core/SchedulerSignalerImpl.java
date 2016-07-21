
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
 *
 */

package com.helger.quartz.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.ITrigger;
import com.helger.quartz.spi.ISchedulerSignaler;

/**
 * An interface to be used by <code>JobStore</code> instances in order to
 * communicate signals back to the <code>QuartzScheduler</code>.
 *
 * @author jhouse
 */
public class SchedulerSignalerImpl implements ISchedulerSignaler
{

  Logger log = LoggerFactory.getLogger (SchedulerSignalerImpl.class);

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  protected QuartzScheduler sched;
  protected QuartzSchedulerThread schedThread;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public SchedulerSignalerImpl (final QuartzScheduler sched, final QuartzSchedulerThread schedThread)
  {
    this.sched = sched;
    this.schedThread = schedThread;

    log.info ("Initialized Scheduler Signaller of type: " + getClass ());
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public void notifyTriggerListenersMisfired (final ITrigger trigger)
  {
    try
    {
      sched.notifyTriggerListenersMisfired (trigger);
    }
    catch (final SchedulerException se)
    {
      sched.getLog ().error ("Error notifying listeners of trigger misfire.", se);
      sched.notifySchedulerListenersError ("Error notifying listeners of trigger misfire.", se);
    }
  }

  public void notifySchedulerListenersFinalized (final ITrigger trigger)
  {
    sched.notifySchedulerListenersFinalized (trigger);
  }

  public void signalSchedulingChange (final long candidateNewNextFireTime)
  {
    schedThread.signalSchedulingChange (candidateNewNextFireTime);
  }

  public void notifySchedulerListenersJobDeleted (final JobKey jobKey)
  {
    sched.notifySchedulerListenersJobDeleted (jobKey);
  }

  public void notifySchedulerListenersError (final String string, final SchedulerException jpe)
  {
    sched.notifySchedulerListenersError (string, jpe);
  }
}
