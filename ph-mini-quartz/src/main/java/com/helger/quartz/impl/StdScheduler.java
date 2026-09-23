/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2026 Philip Helger (www.helger.com)
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IListenerManager;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.ETriggerState;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerContext;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SchedulerMetaData;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.UnableToInterruptJobException;
import com.helger.quartz.core.QuartzScheduler;
import com.helger.quartz.impl.matchers.GroupMatcher;
import com.helger.quartz.spi.IJobFactory;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that directly proxies all method calls
 * to the equivalent call on a given <code>QuartzScheduler</code> instance.
 * </p>
 *
 * @see com.helger.quartz.IScheduler
 * @see com.helger.quartz.core.QuartzScheduler
 * @author James House
 */
public class StdScheduler implements IScheduler
{
  private final QuartzScheduler m_aSched;

  /**
   * <p>
   * Construct a <code>StdScheduler</code> instance to proxy the given <code>QuartzScheduler</code>
   * instance, and with the given <code>SchedulingContext</code>.
   * </p>
   */
  public StdScheduler (@NonNull final QuartzScheduler sched)
  {
    ValueEnforcer.notNull (sched, "Scheduler");

    m_aSched = sched;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Returns the name of the <code>Scheduler</code>.
   * </p>
   */
  @NonNull
  @Nonempty
  public String getSchedulerName ()
  {
    return m_aSched.getSchedulerName ();
  }

  /**
   * <p>
   * Returns the instance Id of the <code>Scheduler</code>.
   * </p>
   */
  @NonNull
  @Nonempty
  public String getSchedulerInstanceId ()
  {
    return m_aSched.getSchedulerInstanceId ();
  }

  @NonNull
  public SchedulerMetaData getMetaData ()
  {
    return new SchedulerMetaData (getSchedulerName (),
                                  getSchedulerInstanceId (),
                                  getClass (),
                                  isStarted (),
                                  isInStandbyMode (),
                                  isShutdown (),
                                  m_aSched.runningSince (),
                                  m_aSched.numJobsExecuted (),
                                  m_aSched.getJobStoreClass (),
                                  m_aSched.supportsPersistence (),
                                  m_aSched.isClustered (),
                                  m_aSched.getThreadPoolClass (),
                                  m_aSched.getThreadPoolSize (),
                                  m_aSched.getVersion ());
  }

  /**
   * <p>
   * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
   * </p>
   */
  @NonNull
  public SchedulerContext getContext () throws SchedulerException
  {
    return m_aSched.getSchedulerContext ();
  }

  ///////////////////////////////////////////////////////////////////////////
  ///
  /// Schedululer State Management Methods
  ///
  ///////////////////////////////////////////////////////////////////////////

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void start () throws SchedulerException
  {
    m_aSched.start ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void startDelayed (final int seconds) throws SchedulerException
  {
    m_aSched.startDelayed (seconds);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void standby ()
  {
    m_aSched.standby ();
  }

  /**
   * Whether the scheduler has been started.
   * <p>
   * Note: This only reflects whether <code>{@link #start()}</code> has ever been called on this
   * Scheduler, so it will return <code>true</code> even if the <code>Scheduler</code> is currently
   * in standby mode or has been since shutdown.
   * </p>
   *
   * @see #start()
   * @see #isShutdown()
   * @see #isInStandbyMode()
   */
  public boolean isStarted ()
  {
    return (m_aSched.runningSince () != null);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public boolean isInStandbyMode ()
  {
    return m_aSched.isInStandbyMode ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void shutdown ()
  {
    m_aSched.shutdown ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void shutdown (final boolean waitForJobsToComplete)
  {
    m_aSched.shutdown (waitForJobsToComplete);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public boolean isShutdown ()
  {
    return m_aSched.isShutdown ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IJobExecutionContext> getCurrentlyExecutingJobs ()
  {
    return m_aSched.getCurrentlyExecutingJobs ();
  }

  /// Scheduling-related Methods

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void clear () throws SchedulerException
  {
    m_aSched.clear ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  public Date scheduleJob (@NonNull final IJobDetail jobDetail,
                           @NonNull final ITrigger trigger) throws SchedulerException
  {
    return m_aSched.scheduleJob (jobDetail, trigger);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  public Date scheduleJob (@NonNull final ITrigger trigger) throws SchedulerException
  {
    return m_aSched.scheduleJob (trigger);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void addJob (@NonNull final IJobDetail jobDetail, final boolean replace) throws SchedulerException
  {
    m_aSched.addJob (jobDetail, replace);
  }

  public void addJob (@NonNull final IJobDetail jobDetail,
                      final boolean replace,
                      final boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException
  {
    m_aSched.addJob (jobDetail, replace, storeNonDurableWhileAwaitingScheduling);
  }

  public boolean deleteJobs (@NonNull final List <JobKey> jobKeys) throws SchedulerException
  {
    return m_aSched.deleteJobs (jobKeys);
  }

  public void scheduleJobs (@NonNull final Map <IJobDetail, Set <? extends ITrigger>> triggersAndJobs,
                            final boolean replace) throws SchedulerException
  {
    m_aSched.scheduleJobs (triggersAndJobs, replace);
  }

  public void scheduleJob (@NonNull final IJobDetail jobDetail,
                           @NonNull final Set <? extends ITrigger> triggersForJob,
                           final boolean replace) throws SchedulerException
  {
    m_aSched.scheduleJob (jobDetail, triggersForJob, replace);
  }

  public boolean unscheduleJobs (@NonNull final List <TriggerKey> triggerKeys) throws SchedulerException
  {
    return m_aSched.unscheduleJobs (triggerKeys);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public boolean deleteJob (@NonNull final JobKey jobKey) throws SchedulerException
  {
    return m_aSched.deleteJob (jobKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public boolean unscheduleJob (@NonNull final TriggerKey triggerKey) throws SchedulerException
  {
    return m_aSched.unscheduleJob (triggerKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @Nullable
  public Date rescheduleJob (@NonNull final TriggerKey triggerKey,
                             @NonNull final ITrigger newTrigger) throws SchedulerException
  {
    return m_aSched.rescheduleJob (triggerKey, newTrigger);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void triggerJob (@NonNull final JobKey jobKey) throws SchedulerException
  {
    triggerJob (jobKey, null);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void triggerJob (@NonNull final JobKey jobKey, @Nullable final JobDataMap data) throws SchedulerException
  {
    m_aSched.triggerJob (jobKey, data);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void pauseTrigger (@NonNull final TriggerKey triggerKey) throws SchedulerException
  {
    m_aSched.pauseTrigger (triggerKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void pauseTriggers (@NonNull final GroupMatcher <TriggerKey> matcher) throws SchedulerException
  {
    m_aSched.pauseTriggers (matcher);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void pauseJob (@NonNull final JobKey jobKey) throws SchedulerException
  {
    m_aSched.pauseJob (jobKey);
  }

  /**
   * @see com.helger.quartz.IScheduler#getPausedTriggerGroups()
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getPausedTriggerGroups () throws SchedulerException
  {
    return m_aSched.getPausedTriggerGroups ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void pauseJobs (@NonNull final GroupMatcher <JobKey> matcher) throws SchedulerException
  {
    m_aSched.pauseJobs (matcher);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void resumeTrigger (@NonNull final TriggerKey triggerKey) throws SchedulerException
  {
    m_aSched.resumeTrigger (triggerKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void resumeTriggers (@NonNull final GroupMatcher <TriggerKey> matcher) throws SchedulerException
  {
    m_aSched.resumeTriggers (matcher);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void resumeJob (@NonNull final JobKey jobKey) throws SchedulerException
  {
    m_aSched.resumeJob (jobKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void resumeJobs (@NonNull final GroupMatcher <JobKey> matcher) throws SchedulerException
  {
    m_aSched.resumeJobs (matcher);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void pauseAll () throws SchedulerException
  {
    m_aSched.pauseAll ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void resumeAll () throws SchedulerException
  {
    m_aSched.resumeAll ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <String> getJobGroupNames () throws SchedulerException
  {
    return m_aSched.getJobGroupNames ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <? extends ITrigger> getTriggersOfJob (@NonNull final JobKey jobKey) throws SchedulerException
  {
    return m_aSched.getTriggersOfJob (jobKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <JobKey> getJobKeys (@NonNull final GroupMatcher <JobKey> matcher) throws SchedulerException
  {
    return m_aSched.getJobKeys (matcher);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <String> getTriggerGroupNames () throws SchedulerException
  {
    return m_aSched.getTriggerGroupNames ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <TriggerKey> getTriggerKeys (@NonNull
                                                  final GroupMatcher <TriggerKey> matcher) throws SchedulerException
  {
    return m_aSched.getTriggerKeys (matcher);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @Nullable
  public IJobDetail getJobDetail (@NonNull final JobKey jobKey) throws SchedulerException
  {
    return m_aSched.getJobDetail (jobKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @Nullable
  public ITrigger getTrigger (@NonNull final TriggerKey triggerKey) throws SchedulerException
  {
    return m_aSched.getTrigger (triggerKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  public ETriggerState getTriggerState (@NonNull final TriggerKey triggerKey) throws SchedulerException
  {
    return m_aSched.getTriggerState (triggerKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public void addCalendar (@NonNull final String calName,
                           @NonNull final ICalendar calendar,
                           final boolean replace,
                           final boolean updateTriggers) throws SchedulerException
  {
    m_aSched.addCalendar (calName, calendar, replace, updateTriggers);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public boolean deleteCalendar (@NonNull final String calName) throws SchedulerException
  {
    return m_aSched.deleteCalendar (calName);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @Nullable
  public ICalendar getCalendar (@NonNull final String calName) throws SchedulerException
  {
    return m_aSched.getCalendar (calName);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <String> getCalendarNames () throws SchedulerException
  {
    return m_aSched.getCalendarNames ();
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public boolean checkExists (@NonNull final JobKey jobKey) throws SchedulerException
  {
    return m_aSched.checkExists (jobKey);
  }

  /**
   * <p>
   * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
   * </p>
   */
  public boolean checkExists (@NonNull final TriggerKey triggerKey) throws SchedulerException
  {
    return m_aSched.checkExists (triggerKey);
  }

  ///////////////////////////////////////////////////////////////////////////
  ///
  /// Other Methods
  ///
  ///////////////////////////////////////////////////////////////////////////

  /**
   * @see com.helger.quartz.IScheduler#setJobFactory(com.helger.quartz.spi.IJobFactory)
   */
  public void setJobFactory (@NonNull final IJobFactory factory) throws SchedulerException
  {
    m_aSched.setJobFactory (factory);
  }

  /**
   * @see com.helger.quartz.IScheduler#getListenerManager()
   */
  @NonNull
  public IListenerManager getListenerManager () throws SchedulerException
  {
    return m_aSched.getListenerManager ();
  }

  public boolean interrupt (@NonNull final JobKey jobKey) throws UnableToInterruptJobException
  {
    return m_aSched.interrupt (jobKey);
  }

  public boolean interrupt (@NonNull final String fireInstanceId) throws UnableToInterruptJobException
  {
    return m_aSched.interrupt (fireInstanceId);
  }

}
