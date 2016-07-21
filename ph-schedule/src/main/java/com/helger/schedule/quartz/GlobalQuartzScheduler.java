/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.schedule.quartz;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.commons.state.EChange;
import com.helger.quartz.IJob;
import com.helger.quartz.JobBuilder;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobListener;
import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.ITrigger;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.EverythingMatcher;
import com.helger.schedule.quartz.listener.StatisticsJobListener;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;

/**
 * Global scheduler instance.
 *
 * @author Philip Helger
 */
public final class GlobalQuartzScheduler extends AbstractGlobalSingleton
{
  public static final String GROUP_NAME = "com.helger";

  private static final Logger s_aLogger = LoggerFactory.getLogger (GlobalQuartzScheduler.class);

  private final IScheduler m_aScheduler;
  private String m_sGroupName = GROUP_NAME;

  @Deprecated
  @UsedViaReflection
  public GlobalQuartzScheduler ()
  {
    // main scheduler - start directly
    m_aScheduler = QuartzSchedulerHelper.getScheduler (true);

    // Always add the statistics listener
    addJobListener (new StatisticsJobListener ());
  }

  @Nonnull
  public static GlobalQuartzScheduler getInstance ()
  {
    return getGlobalSingleton (GlobalQuartzScheduler.class);
  }

  /**
   * @return The Quartz internal group name to be used. Defaults to
   *         {@link #GROUP_NAME}.
   */
  @Nonnull
  @Nonempty
  public String getGroupName ()
  {
    return m_sGroupName;
  }

  /**
   * Set the Quartz internal group name.
   *
   * @param sGroupName
   *        The new group name to be used. May neither be <code>null</code> nor
   *        empty.
   */
  public void setGroupName (@Nonnull @Nonempty final String sGroupName)
  {
    ValueEnforcer.notEmpty (sGroupName, "GroupName");
    m_sGroupName = sGroupName;
  }

  /**
   * Add a job listener for all jobs.
   *
   * @param aJobListener
   *        The job listener to be added. May not be <code>null</code>.
   */
  public void addJobListener (@Nonnull final IJobListener aJobListener)
  {
    ValueEnforcer.notNull (aJobListener, "JobListener");

    try
    {
      m_aScheduler.getListenerManager ().addJobListener (aJobListener, EverythingMatcher.allJobs ());
    }
    catch (final SchedulerException ex)
    {
      throw new IllegalStateException ("Failed to add job listener " + aJobListener.toString (), ex);
    }
  }

  /**
   * @return The underlying Quartz scheduler object. Never <code>null</code>.
   */
  @Nonnull
  public IScheduler getScheduler ()
  {
    return m_aScheduler;
  }

  /**
   * This method is only for testing purposes.
   *
   * @param sJobName
   *        Name of the job. Needs to be unique since no two job details with
   *        the same name may exist.
   * @param aTriggerBuilder
   *        The trigger builder instance to schedule the job
   * @param aJobClass
   *        Class to execute
   * @param aJobData
   *        Additional parameters. May be <code>null</code>.
   * @return The created trigger key for further usage. Never <code>null</code>.
   */
  @Nonnull
  public TriggerKey scheduleJob (@Nonnull final String sJobName,
                                 @Nonnull final JDK8TriggerBuilder <? extends ITrigger> aTriggerBuilder,
                                 @Nonnull final Class <? extends IJob> aJobClass,
                                 @Nullable final Map <String, ? extends Object> aJobData)
  {
    ValueEnforcer.notNull (sJobName, "JobName");
    ValueEnforcer.notNull (aTriggerBuilder, "TriggerBuilder");
    ValueEnforcer.notNull (aJobClass, "JobClass");

    // what to do
    final IJobDetail aJobDetail = JobBuilder.newJob (aJobClass).withIdentity (sJobName, m_sGroupName).build ();

    // add custom parameters
    final JobDataMap aJobDataMap = aJobDetail.getJobDataMap ();
    if (aJobData != null)
      for (final Map.Entry <String, ? extends Object> aEntry : aJobData.entrySet ())
        aJobDataMap.put (aEntry.getKey (), aEntry.getValue ());

    try
    {
      // Schedule now
      final ITrigger aTrigger = aTriggerBuilder.build ();
      m_aScheduler.scheduleJob (aJobDetail, aTrigger);

      final TriggerKey ret = aTrigger.getKey ();
      s_aLogger.info ("Succesfully scheduled job '" +
                      sJobName +
                      "' with TriggerKey " +
                      ret.toString () +
                      " - starting at " +
                      PDTFactory.createLocalDateTime (aTrigger.getStartTime ()));
      return ret;
    }
    catch (final SchedulerException ex)
    {
      throw new RuntimeException (ex);
    }
  }

  /**
   * Schedule a new job that should be executed now and only once.
   *
   * @param sJobName
   *        Name of the job - must be unique within the whole system!
   * @param aJobClass
   *        The Job class to be executed.
   * @param aJobData
   *        Optional job data map.
   * @return The created trigger key for further usage. Never <code>null</code>.
   */
  @Nonnull
  public TriggerKey scheduleJobNowOnce (@Nonnull final String sJobName,
                                        @Nonnull final Class <? extends IJob> aJobClass,
                                        @Nullable final Map <String, ? extends Object> aJobData)
  {
    return scheduleJob (sJobName,
                        JDK8TriggerBuilder.newTrigger ()
                                          .startNow ()
                                          .withSchedule (SimpleScheduleBuilder.simpleSchedule ()
                                                                              .withIntervalInMinutes (1)
                                                                              .withRepeatCount (0)),
                        aJobClass,
                        aJobData);
  }

  /**
   * Unschedule the job with the specified trigger key as returned from
   * {@link #scheduleJob(String, JDK8TriggerBuilder, Class, Map)}.
   *
   * @param aTriggerKey
   *        Trigger key to use. May not be <code>null</code>.
   * @return {@link EChange}.
   */
  @Nonnull
  public EChange unscheduleJob (@Nonnull final TriggerKey aTriggerKey)
  {
    ValueEnforcer.notNull (aTriggerKey, "TriggerKey");
    try
    {
      if (m_aScheduler.unscheduleJob (aTriggerKey))
      {
        s_aLogger.info ("Succesfully unscheduled job with TriggerKey " + aTriggerKey.toString ());
        return EChange.CHANGED;
      }
    }
    catch (final SchedulerException ex)
    {
      s_aLogger.error ("Failed to unschedule job with TriggerKey " + aTriggerKey.toString (), ex);
    }
    return EChange.UNCHANGED;
  }

  /**
   * Pause the job with the specified trigger key as returned from
   * {@link #scheduleJob(String, JDK8TriggerBuilder, Class, Map)}.
   *
   * @param aTriggerKey
   *        Trigger key to use. May not be <code>null</code>.
   * @see #resumeJob(TriggerKey)
   */
  public void pauseJob (@Nonnull final TriggerKey aTriggerKey)
  {
    ValueEnforcer.notNull (aTriggerKey, "TriggerKey");
    try
    {
      m_aScheduler.pauseTrigger (aTriggerKey);
      s_aLogger.info ("Succesfully paused job with TriggerKey " + aTriggerKey.toString ());
    }
    catch (final SchedulerException ex)
    {
      s_aLogger.error ("Failed to pause job with TriggerKey " + aTriggerKey.toString (), ex);
    }
  }

  /**
   * Resume the job with the specified trigger key as returned from
   * {@link #scheduleJob(String, JDK8TriggerBuilder, Class, Map)}.
   *
   * @param aTriggerKey
   *        Trigger key to use. May not be <code>null</code>.
   */
  public void resumeJob (@Nonnull final TriggerKey aTriggerKey)
  {
    ValueEnforcer.notNull (aTriggerKey, "TriggerKey");
    try
    {
      m_aScheduler.resumeTrigger (aTriggerKey);
      s_aLogger.info ("Succesfully resumed job with TriggerKey " + aTriggerKey.toString ());
    }
    catch (final SchedulerException ex)
    {
      s_aLogger.error ("Failed to resume job with TriggerKey " + aTriggerKey.toString (), ex);
    }
  }

  /**
   * Shutdown the scheduler and wait for all jobs to complete.
   *
   * @throws SchedulerException
   *         If something goes wrong
   */
  public void shutdown () throws SchedulerException
  {
    try
    {
      // Shutdown but wait for jobs to complete
      m_aScheduler.shutdown (true);
      s_aLogger.info ("Successfully shutdown GlobalQuartzScheduler");
    }
    catch (final SchedulerException ex)
    {
      s_aLogger.error ("Failed to shutdown GlobalQuartzScheduler", ex);
      throw ex;
    }
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction) throws Exception
  {
    shutdown ();
  }
}
