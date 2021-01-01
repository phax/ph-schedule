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
  private final IJobDetail m_aJob;
  private final IOperableTrigger m_aTrigger;
  private final ICalendar m_aCal;
  private final boolean m_bJobIsRecovering;
  private final Date m_aFireTime;
  private final Date m_aScheduledFireTime;
  private final Date m_aPrevFireTime;
  private final Date m_aNextFireTime;

  public TriggerFiredBundle (final IJobDetail job,
                             final IOperableTrigger trigger,
                             final ICalendar cal,
                             final boolean jobIsRecovering,
                             final Date fireTime,
                             final Date scheduledFireTime,
                             final Date prevFireTime,
                             final Date nextFireTime)
  {
    m_aJob = job;
    m_aTrigger = trigger;
    m_aCal = cal;
    m_bJobIsRecovering = jobIsRecovering;
    m_aFireTime = fireTime;
    m_aScheduledFireTime = scheduledFireTime;
    m_aPrevFireTime = prevFireTime;
    m_aNextFireTime = nextFireTime;
  }

  public IJobDetail getJobDetail ()
  {
    return m_aJob;
  }

  public IOperableTrigger getTrigger ()
  {
    return m_aTrigger;
  }

  public ICalendar getCalendar ()
  {
    return m_aCal;
  }

  public boolean isRecovering ()
  {
    return m_bJobIsRecovering;
  }

  /**
   * @return Returns the fireTime.
   */
  public Date getFireTime ()
  {
    return m_aFireTime;
  }

  /**
   * @return Returns the nextFireTime.
   */
  public Date getNextFireTime ()
  {
    return m_aNextFireTime;
  }

  /**
   * @return Returns the prevFireTime.
   */
  public Date getPrevFireTime ()
  {
    return m_aPrevFireTime;
  }

  /**
   * @return Returns the scheduledFireTime.
   */
  public Date getScheduledFireTime ()
  {
    return m_aScheduledFireTime;
  }
}
