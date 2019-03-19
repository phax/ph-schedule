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
package com.helger.quartz.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.helger.quartz.ICalendar;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.spi.IOperableTrigger;
import com.helger.quartz.spi.TriggerFiredBundle;

public class JobExecutionContext implements IJobExecutionContext
{
  private transient IScheduler m_aScheduler;
  private final ITrigger m_aTrigger;
  private final IJobDetail m_aJobDetail;
  private final JobDataMap m_aJobDataMap;
  private transient IJob m_aJob;
  private final ICalendar m_aCalendar;
  private boolean m_bRecovering = false;
  private int m_nNumRefires = 0;
  private final Date m_aFireTime;
  private final Date m_aScheduledFireTime;
  private final Date m_aPrevFireTime;
  private final Date m_aNextFireTime;
  private long m_nJobRunTime = -1;
  private Object m_aResult;
  private final Map <Object, Object> m_aData = new HashMap <> ();

  /**
   * Create a JobExcecutionContext with the given context data.
   */
  public JobExecutionContext (final IScheduler scheduler, final TriggerFiredBundle firedBundle, final IJob job)
  {
    m_aScheduler = scheduler;
    m_aTrigger = firedBundle.getTrigger ();
    m_aCalendar = firedBundle.getCalendar ();
    m_aJobDetail = firedBundle.getJobDetail ();
    m_aJob = job;
    m_bRecovering = firedBundle.isRecovering ();
    m_aFireTime = firedBundle.getFireTime ();
    m_aScheduledFireTime = firedBundle.getScheduledFireTime ();
    m_aPrevFireTime = firedBundle.getPrevFireTime ();
    m_aNextFireTime = firedBundle.getNextFireTime ();

    m_aJobDataMap = new JobDataMap ();
    m_aJobDataMap.putAll (m_aJobDetail.getJobDataMap ());
    m_aJobDataMap.putAll (m_aTrigger.getJobDataMap ());
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * {@inheritDoc}
   */
  public IScheduler getScheduler ()
  {
    return m_aScheduler;
  }

  /**
   * {@inheritDoc}
   */
  public ITrigger getTrigger ()
  {
    return m_aTrigger;
  }

  /**
   * {@inheritDoc}
   */
  public ICalendar getCalendar ()
  {
    return m_aCalendar;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isRecovering ()
  {
    return m_bRecovering;
  }

  public TriggerKey getRecoveringTriggerKey ()
  {
    if (isRecovering ())
    {
      return new TriggerKey (m_aJobDataMap.getAsString (IScheduler.FAILED_JOB_ORIGINAL_TRIGGER_GROUP),
                             m_aJobDataMap.getAsString (IScheduler.FAILED_JOB_ORIGINAL_TRIGGER_NAME));
    }
    throw new IllegalStateException ("Not a recovering job");
  }

  public void incrementRefireCount ()
  {
    m_nNumRefires++;
  }

  /**
   * {@inheritDoc}
   */
  public int getRefireCount ()
  {
    return m_nNumRefires;
  }

  /**
   * {@inheritDoc}
   */
  public JobDataMap getMergedJobDataMap ()
  {
    return m_aJobDataMap;
  }

  /**
   * {@inheritDoc}
   */
  public IJobDetail getJobDetail ()
  {
    return m_aJobDetail;
  }

  /**
   * {@inheritDoc}
   */
  public IJob getJobInstance ()
  {
    return m_aJob;
  }

  /**
   * {@inheritDoc}
   */
  public Date getFireTime ()
  {
    return m_aFireTime;
  }

  /**
   * {@inheritDoc}
   */
  public Date getScheduledFireTime ()
  {
    return m_aScheduledFireTime;
  }

  /**
   * {@inheritDoc}
   */
  public Date getPreviousFireTime ()
  {
    return m_aPrevFireTime;
  }

  /**
   * {@inheritDoc}
   */
  public Date getNextFireTime ()
  {
    return m_aNextFireTime;
  }

  @Override
  public String toString ()
  {
    return "JobExecutionContext:" +
           " trigger: '" +
           getTrigger ().getKey () +
           " job: " +
           getJobDetail ().getKey () +
           " fireTime: '" +
           getFireTime () +
           " scheduledFireTime: " +
           getScheduledFireTime () +
           " previousFireTime: '" +
           getPreviousFireTime () +
           " nextFireTime: " +
           getNextFireTime () +
           " isRecovering: " +
           isRecovering () +
           " refireCount: " +
           getRefireCount ();
  }

  /**
   * {@inheritDoc}
   */
  public Object getResult ()
  {
    return m_aResult;
  }

  /**
   * {@inheritDoc}
   */
  public void setResult (final Object result)
  {
    m_aResult = result;
  }

  /**
   * {@inheritDoc}
   */
  public long getJobRunTime ()
  {
    return m_nJobRunTime;
  }

  /**
   * @param jobRunTime
   *        The jobRunTime to set.
   */
  public void setJobRunTime (final long jobRunTime)
  {
    m_nJobRunTime = jobRunTime;
  }

  /**
   * {@inheritDoc}
   */
  public void put (final Object key, final Object value)
  {
    m_aData.put (key, value);
  }

  /**
   * {@inheritDoc}
   */
  public Object get (final Object key)
  {
    return m_aData.get (key);
  }

  /**
   * {@inheritDoc}
   */
  public String getFireInstanceId ()
  {
    return ((IOperableTrigger) m_aTrigger).getFireInstanceId ();
  }
}
