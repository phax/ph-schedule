/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.EverythingMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotations.DevelopersNote;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.annotations.OverrideOnDemand;
import com.helger.commons.annotations.UsedViaReflection;
import com.helger.commons.exceptions.LoggedRuntimeException;
import com.helger.schedule.quartz.listener.StatisticsJobListener;
import com.helger.scopes.IScope;
import com.helger.scopes.singleton.GlobalSingleton;

/**
 * Global scheduler instance.
 *
 * @author Philip Helger
 */
public class GlobalQuartzScheduler extends GlobalSingleton
{
  public static final String GROUP_NAME = "com.helger";

  private static final Logger s_aLogger = LoggerFactory.getLogger (GlobalQuartzScheduler.class);

  private final Scheduler m_aScheduler;
  private String m_sGroupName = GROUP_NAME;

  @UsedViaReflection
  @DevelopersNote ("Used from derived classes")
  public GlobalQuartzScheduler ()
  {
    // main scheduler
    m_aScheduler = QuartzSchedulerHelper.getScheduler ();

    // Always add the statistics listener
    addJobListener (new StatisticsJobListener ());
  }

  @Nonnull
  public static final GlobalQuartzScheduler getInstance ()
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
  public void addJobListener (@Nonnull final JobListener aJobListener)
  {
    ValueEnforcer.notNull (aJobListener, "JobListener");

    try
    {
      m_aScheduler.getListenerManager ().addJobListener (aJobListener, EverythingMatcher.allJobs ());
    }
    catch (final SchedulerException ex)
    {
      throw new IllegalStateException ("Failed to add job listener " + aJobListener, ex);
    }
  }

  /**
   * @return The underlying Quartz scheduler object. Never <code>null</code>.
   */
  @Nonnull
  protected final Scheduler getScheduler ()
  {
    return m_aScheduler;
  }

  /**
   * Modify the JobData map in derived classes. This is e.g. used in pdaf3 to
   * set the correct state data in executed jobs.
   *
   * @param aJobDataMap
   *        The job data map to modify. Never <code>null</code>.
   */
  @OverrideOnDemand
  protected void modifyJobDataMap (@Nonnull final JobDataMap aJobDataMap)
  {}

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
   */
  public final void scheduleJob (@Nonnull final String sJobName,
                                 @Nonnull final TriggerBuilder <? extends Trigger> aTriggerBuilder,
                                 @Nonnull final Class <? extends Job> aJobClass,
                                 @Nullable final Map <String, ? extends Object> aJobData)
  {
    ValueEnforcer.notNull (sJobName, "JobName");
    ValueEnforcer.notNull (aTriggerBuilder, "TriggerBuilder");
    ValueEnforcer.notNull (aJobClass, "JobClass");

    // what to do
    final JobDetail aJobDetail = JobBuilder.newJob (aJobClass).withIdentity (sJobName, m_sGroupName).build ();

    // add custom parameters
    final JobDataMap aJobDataMap = aJobDetail.getJobDataMap ();
    if (aJobData != null)
      for (final Map.Entry <String, ? extends Object> aEntry : aJobData.entrySet ())
        aJobDataMap.put (aEntry.getKey (), aEntry.getValue ());

    // Add parameters via callback as well
    modifyJobDataMap (aJobDataMap);

    try
    {
      // Schedule now
      final Trigger aTrigger = aTriggerBuilder.build ();
      m_aScheduler.scheduleJob (aJobDetail, aTrigger);

      s_aLogger.info ("Succesfully scheduled job '" + sJobName + "' with trigger " + aTrigger);
    }
    catch (final SchedulerException ex)
    {
      throw LoggedRuntimeException.newException (ex);
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
   */
  public void scheduleJobNowOnce (@Nonnull final String sJobName,
                                  @Nonnull final Class <? extends Job> aJobClass,
                                  @Nullable final Map <String, ? extends Object> aJobData)
  {
    scheduleJob (sJobName,
                 TriggerBuilder.newTrigger ()
                               .startNow ()
                               .withSchedule (SimpleScheduleBuilder.simpleSchedule ()
                                                                   .withIntervalInMinutes (1)
                                                                   .withRepeatCount (0)),
                 aJobClass,
                 aJobData);
  }

  /**
   * Shutdown the scheduler and wait for all jobs to complete.
   *
   * @throws SchedulerException
   *         If something goes wrong
   */
  public final void shutdown () throws SchedulerException
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
  protected final void onDestroy (@Nonnull final IScope aScopeInDestruction) throws Exception
  {
    shutdown ();
  }
}
