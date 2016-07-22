/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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
package com.helger.quartz.spi;

import java.io.Serializable;
import java.util.Date;

import com.helger.quartz.ICalendar;
import com.helger.quartz.IJobDetail;

/**
 * <p>
 * A simple class (structure) used for returning execution-time data from the
 * JobStore to the <code>QuartzSchedulerThread</code>.
 * </p>
 *
 * @see com.helger.quartz.core.QuartzSchedulerThread
 * @author James House
 */
public class TriggerFiredBundle implements Serializable
{
  private final IJobDetail job;
  private final IOperableTrigger trigger;
  private final ICalendar cal;
  private final boolean jobIsRecovering;
  private final Date fireTime;
  private final Date scheduledFireTime;
  private final Date prevFireTime;
  private final Date nextFireTime;

  public TriggerFiredBundle (final IJobDetail job,
                             final IOperableTrigger trigger,
                             final ICalendar cal,
                             final boolean jobIsRecovering,
                             final Date fireTime,
                             final Date scheduledFireTime,
                             final Date prevFireTime,
                             final Date nextFireTime)
  {
    this.job = job;
    this.trigger = trigger;
    this.cal = cal;
    this.jobIsRecovering = jobIsRecovering;
    this.fireTime = fireTime;
    this.scheduledFireTime = scheduledFireTime;
    this.prevFireTime = prevFireTime;
    this.nextFireTime = nextFireTime;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public IJobDetail getJobDetail ()
  {
    return job;
  }

  public IOperableTrigger getTrigger ()
  {
    return trigger;
  }

  public ICalendar getCalendar ()
  {
    return cal;
  }

  public boolean isRecovering ()
  {
    return jobIsRecovering;
  }

  /**
   * @return Returns the fireTime.
   */
  public Date getFireTime ()
  {
    return fireTime;
  }

  /**
   * @return Returns the nextFireTime.
   */
  public Date getNextFireTime ()
  {
    return nextFireTime;
  }

  /**
   * @return Returns the prevFireTime.
   */
  public Date getPrevFireTime ()
  {
    return prevFireTime;
  }

  /**
   * @return Returns the scheduledFireTime.
   */
  public Date getScheduledFireTime ()
  {
    return scheduledFireTime;
  }

}
