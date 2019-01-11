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
package com.helger.quartz;

import java.util.Date;

/**
 * Describes the settings and capabilities of a given
 * <code>{@link IScheduler}</code> instance.
 *
 * @author James House
 */
public class SchedulerMetaData implements java.io.Serializable
{
  private final String m_sSchedName;
  private final String m_sSchedInst;
  private final Class <?> m_sSchedClass;
  private final boolean m_bStarted;
  private final boolean m_bIsInStandbyMode;
  private final boolean m_bShutdown;
  private final Date m_aStartTime;
  private final int m_nNumJobsExec;
  private final Class <?> m_aJsClass;
  private final boolean m_bJsPersistent;
  private final boolean m_bJsClustered;
  private final Class <?> m_aTpClass;
  private final int m_nTpSize;
  private final String m_sVersion;

  public SchedulerMetaData (final String schedName,
                            final String schedInst,
                            final Class <?> schedClass,
                            final boolean started,
                            final boolean isInStandbyMode,
                            final boolean shutdown,
                            final Date startTime,
                            final int numJobsExec,
                            final Class <?> jsClass,
                            final boolean jsPersistent,
                            final boolean jsClustered,
                            final Class <?> tpClass,
                            final int tpSize,
                            final String version)
  {
    this.m_sSchedName = schedName;
    this.m_sSchedInst = schedInst;
    this.m_sSchedClass = schedClass;
    this.m_bStarted = started;
    this.m_bIsInStandbyMode = isInStandbyMode;
    this.m_bShutdown = shutdown;
    this.m_aStartTime = startTime;
    this.m_nNumJobsExec = numJobsExec;
    this.m_aJsClass = jsClass;
    this.m_bJsPersistent = jsPersistent;
    this.m_bJsClustered = jsClustered;
    this.m_aTpClass = tpClass;
    this.m_nTpSize = tpSize;
    this.m_sVersion = version;
  }

  /**
   * <p>
   * Returns the name of the <code>Scheduler</code>.
   * </p>
   */
  public String getSchedulerName ()
  {
    return m_sSchedName;
  }

  /**
   * <p>
   * Returns the instance Id of the <code>Scheduler</code>.
   * </p>
   */
  public String getSchedulerInstanceId ()
  {
    return m_sSchedInst;
  }

  /**
   * <p>
   * Returns the class-name of the <code>Scheduler</code> instance.
   * </p>
   */
  public Class <?> getSchedulerClass ()
  {
    return m_sSchedClass;
  }

  /**
   * <p>
   * Returns the <code>Date</code> at which the Scheduler started running.
   * </p>
   *
   * @return null if the scheduler has not been started.
   */
  public Date getRunningSince ()
  {
    return m_aStartTime;
  }

  /**
   * <p>
   * Returns the number of jobs executed since the <code>Scheduler</code>
   * started..
   * </p>
   */
  public int getNumberOfJobsExecuted ()
  {
    return m_nNumJobsExec;
  }

  /**
   * <p>
   * Returns whether the scheduler has been started.
   * </p>
   * <p>
   * Note: <code>isStarted()</code> may return <code>true</code> even if
   * <code>isInStandbyMode()</code> returns <code>true</code>.
   * </p>
   */
  public boolean isStarted ()
  {
    return m_bStarted;
  }

  /**
   * Reports whether the <code>Scheduler</code> is in standby mode.
   */
  public boolean isInStandbyMode ()
  {
    return m_bIsInStandbyMode;
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> has been shutdown.
   * </p>
   */
  public boolean isShutdown ()
  {
    return m_bShutdown;
  }

  /**
   * <p>
   * Returns the class-name of the <code>JobStore</code> instance that is being
   * used by the <code>Scheduler</code>.
   * </p>
   */
  public Class <?> getJobStoreClass ()
  {
    return m_aJsClass;
  }

  /**
   * <p>
   * Returns whether or not the <code>Scheduler</code>'s<code>JobStore</code>
   * instance supports persistence.
   * </p>
   */
  public boolean isJobStoreSupportsPersistence ()
  {
    return m_bJsPersistent;
  }

  /**
   * <p>
   * Returns whether or not the <code>Scheduler</code>'s<code>JobStore</code> is
   * clustered.
   * </p>
   */
  public boolean isJobStoreClustered ()
  {
    return m_bJsClustered;
  }

  /**
   * <p>
   * Returns the class-name of the <code>ThreadPool</code> instance that is being
   * used by the <code>Scheduler</code>.
   * </p>
   */
  public Class <?> getThreadPoolClass ()
  {
    return m_aTpClass;
  }

  /**
   * <p>
   * Returns the number of threads currently in the <code>Scheduler</code>'s
   * <code>ThreadPool</code>.
   * </p>
   */
  public int getThreadPoolSize ()
  {
    return m_nTpSize;
  }

  /**
   * <p>
   * Returns the version of Quartz that is running.
   * </p>
   */
  public String getVersion ()
  {
    return m_sVersion;
  }

  /**
   * <p>
   * Return a simple string representation of this object.
   * </p>
   */
  @Override
  public String toString ()
  {
    try
    {
      return getSummary ();
    }
    catch (final SchedulerException se)
    {
      return "SchedulerMetaData: undeterminable.";
    }
  }

  /**
   * <p>
   * Returns a formatted (human readable) String describing all the
   * <code>Scheduler</code>'s meta-data values.
   * </p>
   * <p>
   * The format of the String looks something like this:
   * </p>
   *
   * <pre>
   *  Mini Quartz Scheduler 'SchedulerName' with instanceId 'SchedulerInstanceId' Scheduler class: 'com.helger.quartz.impl.StdScheduler' - running locally. Running since: '11:33am on Jul 19, 2002' Not currently paused. Number of Triggers fired: '123' Using thread pool 'com.helger.quartz.simpl.SimpleThreadPool' - with '8' threads Using job-store 'com.helger.quartz.impl.JDBCJobStore' - which supports persistence.
   * </pre>
   *
   * @throws SchedulerException
   *         On error
   */
  public String getSummary () throws SchedulerException
  {
    final StringBuilder aSB = new StringBuilder ("Mini Quartz Scheduler (v");
    aSB.append (getVersion ());
    aSB.append (") '");

    aSB.append (getSchedulerName ());
    aSB.append ("' with instanceId '");
    aSB.append (getSchedulerInstanceId ());
    aSB.append ("'\n");

    aSB.append ("  Scheduler class: '");
    aSB.append (getSchedulerClass ().getName ());
    aSB.append ("'");
    aSB.append (" - running locally.");
    aSB.append ("\n");

    if (!isShutdown ())
    {
      if (getRunningSince () != null)
      {
        aSB.append ("  Running since: ");
        aSB.append (getRunningSince ());
      }
      else
      {
        aSB.append ("  NOT STARTED.");
      }
      aSB.append ("\n");

      if (isInStandbyMode ())
      {
        aSB.append ("  Currently in standby mode.");
      }
      else
      {
        aSB.append ("  Not currently in standby mode.");
      }
    }
    else
    {
      aSB.append ("  Scheduler has been SHUTDOWN.");
    }
    aSB.append ("\n");

    aSB.append ("  Number of jobs executed: ");
    aSB.append (getNumberOfJobsExecuted ());
    aSB.append ("\n");

    aSB.append ("  Using thread pool '");
    aSB.append (getThreadPoolClass ().getName ());
    aSB.append ("' - with ");
    aSB.append (getThreadPoolSize ());
    aSB.append (" threads.");
    aSB.append ("\n");

    aSB.append ("  Using job-store '");
    aSB.append (getJobStoreClass ().getName ());
    aSB.append ("' - which ");
    if (isJobStoreSupportsPersistence ())
    {
      aSB.append ("supports persistence.");
    }
    else
    {
      aSB.append ("does not support persistence.");
    }
    if (isJobStoreClustered ())
    {
      aSB.append (" and is clustered.");
    }
    else
    {
      aSB.append (" and is not clustered.");
    }
    aSB.append ("\n");

    return aSB.toString ();
  }

}
