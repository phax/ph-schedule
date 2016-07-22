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

package com.helger.quartz.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helger.quartz.ICalendar;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.CompletedExecutionInstruction;
import com.helger.quartz.ITrigger.TriggerState;
import com.helger.quartz.JobKey;
import com.helger.quartz.JobPersistenceException;
import com.helger.quartz.ObjectAlreadyExistsException;
import com.helger.quartz.SchedulerConfigException;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.GroupMatcher;

/**
 * <p>
 * The interface to be implemented by classes that want to provide a
 * <code>{@link com.helger.quartz.IJob}</code> and
 * <code>{@link com.helger.quartz.ITrigger}</code> storage mechanism for the
 * <code>{@link com.helger.quartz.core.QuartzScheduler}</code>'s use.
 * </p>
 * <p>
 * Storage of <code>Job</code> s and <code>Trigger</code> s should be keyed on
 * the combination of their name and group for uniqueness.
 * </p>
 *
 * @see com.helger.quartz.core.QuartzScheduler
 * @see com.helger.quartz.ITrigger
 * @see com.helger.quartz.IJob
 * @see com.helger.quartz.IJobDetail
 * @see com.helger.quartz.JobDataMap
 * @see com.helger.quartz.ICalendar
 * @author James House
 * @author Eric Mueller
 */
public interface IJobStore
{
  /**
   * Called by the QuartzScheduler before the <code>JobStore</code> is used, in
   * order to give the it a chance to initialize.
   */
  void initialize (IClassLoadHelper loadHelper, ISchedulerSignaler signaler) throws SchedulerConfigException;

  /**
   * Called by the QuartzScheduler to inform the <code>JobStore</code> that the
   * scheduler has started.
   */
  void schedulerStarted () throws SchedulerException;

  /**
   * Called by the QuartzScheduler to inform the <code>JobStore</code> that the
   * scheduler has been paused.
   */
  void schedulerPaused ();

  /**
   * Called by the QuartzScheduler to inform the <code>JobStore</code> that the
   * scheduler has resumed after being paused.
   */
  void schedulerResumed ();

  /**
   * Called by the QuartzScheduler to inform the <code>JobStore</code> that it
   * should free up all of it's resources because the scheduler is shutting
   * down.
   */
  void shutdown ();

  boolean supportsPersistence ();

  /**
   * How long (in milliseconds) the <code>JobStore</code> implementation
   * estimates that it will take to release a trigger and acquire a new one.
   */
  long getEstimatedTimeToReleaseAndAcquireTrigger ();

  /**
   * Whether or not the <code>JobStore</code> implementation is clustered.
   */
  boolean isClustered ();

  /////////////////////////////////////////////////////////////////////////////
  //
  // Job & Trigger Storage methods
  //
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Store the given <code>{@link com.helger.quartz.IJobDetail}</code> and
   * <code>{@link com.helger.quartz.ITrigger}</code>.
   *
   * @param newJob
   *        The <code>JobDetail</code> to be stored.
   * @param newTrigger
   *        The <code>Trigger</code> to be stored.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Job</code> with the same name/group already exists.
   */
  void storeJobAndTrigger (IJobDetail newJob, IOperableTrigger newTrigger) throws ObjectAlreadyExistsException,
                                                                           JobPersistenceException;

  /**
   * Store the given <code>{@link com.helger.quartz.IJobDetail}</code>.
   *
   * @param newJob
   *        The <code>JobDetail</code> to be stored.
   * @param replaceExisting
   *        If <code>true</code>, any <code>Job</code> existing in the
   *        <code>JobStore</code> with the same name &amp; group should be
   *        over-written.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Job</code> with the same name/group already exists, and
   *         replaceExisting is set to false.
   */
  void storeJob (IJobDetail newJob, boolean replaceExisting) throws ObjectAlreadyExistsException,
                                                             JobPersistenceException;

  public void storeJobsAndTriggers (Map <IJobDetail, Set <? extends ITrigger>> triggersAndJobs,
                                    boolean replace) throws ObjectAlreadyExistsException, JobPersistenceException;

  /**
   * Remove (delete) the <code>{@link com.helger.quartz.IJob}</code> with the
   * given key, and any <code>{@link com.helger.quartz.ITrigger}</code> s that
   * reference it.
   * <p>
   * If removal of the <code>Job</code> results in an empty group, the group
   * should be removed from the <code>JobStore</code>'s list of known group
   * names.
   * </p>
   *
   * @return <code>true</code> if a <code>Job</code> with the given name &amp;
   *         group was found and removed from the store.
   */
  boolean removeJob (JobKey jobKey) throws JobPersistenceException;

  public boolean removeJobs (List <JobKey> jobKeys) throws JobPersistenceException;

  /**
   * Retrieve the <code>{@link com.helger.quartz.IJobDetail}</code> for the
   * given <code>{@link com.helger.quartz.IJob}</code>.
   *
   * @return The desired <code>Job</code>, or null if there is no match.
   */
  IJobDetail retrieveJob (JobKey jobKey) throws JobPersistenceException;

  /**
   * Store the given <code>{@link com.helger.quartz.ITrigger}</code>.
   *
   * @param newTrigger
   *        The <code>Trigger</code> to be stored.
   * @param replaceExisting
   *        If <code>true</code>, any <code>Trigger</code> existing in the
   *        <code>JobStore</code> with the same name &amp; group should be
   *        over-written.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Trigger</code> with the same name/group already exists,
   *         and replaceExisting is set to false.
   * @see #pauseTriggers(com.helger.quartz.impl.matchers.GroupMatcher)
   */
  void storeTrigger (IOperableTrigger newTrigger, boolean replaceExisting) throws ObjectAlreadyExistsException,
                                                                           JobPersistenceException;

  /**
   * Remove (delete) the <code>{@link com.helger.quartz.ITrigger}</code> with
   * the given key.
   * <p>
   * If removal of the <code>Trigger</code> results in an empty group, the group
   * should be removed from the <code>JobStore</code>'s list of known group
   * names.
   * </p>
   * <p>
   * If removal of the <code>Trigger</code> results in an 'orphaned'
   * <code>Job</code> that is not 'durable', then the <code>Job</code> should be
   * deleted also.
   * </p>
   *
   * @return <code>true</code> if a <code>Trigger</code> with the given name
   *         &amp; group was found and removed from the store.
   */
  boolean removeTrigger (TriggerKey triggerKey) throws JobPersistenceException;

  public boolean removeTriggers (List <TriggerKey> triggerKeys) throws JobPersistenceException;

  /**
   * Remove (delete) the <code>{@link com.helger.quartz.ITrigger}</code> with
   * the given key, and store the new given one - which must be associated with
   * the same job.
   *
   * @param newTrigger
   *        The new <code>Trigger</code> to be stored.
   * @return <code>true</code> if a <code>Trigger</code> with the given name
   *         &amp; group was found and removed from the store.
   */
  boolean replaceTrigger (TriggerKey triggerKey, IOperableTrigger newTrigger) throws JobPersistenceException;

  /**
   * Retrieve the given <code>{@link com.helger.quartz.ITrigger}</code>.
   *
   * @return The desired <code>Trigger</code>, or null if there is no match.
   * @throws JobPersistenceException
   */
  IOperableTrigger retrieveTrigger (TriggerKey triggerKey) throws JobPersistenceException;

  /**
   * Determine whether a {@link IJob} with the given identifier already exists
   * within the scheduler.
   *
   * @param jobKey
   *        the identifier to check for
   * @return true if a Job exists with the given identifier
   * @throws JobPersistenceException
   */
  boolean checkExists (JobKey jobKey) throws JobPersistenceException;

  /**
   * Determine whether a {@link ITrigger} with the given identifier already
   * exists within the scheduler.
   *
   * @param triggerKey
   *        the identifier to check for
   * @return true if a Trigger exists with the given identifier
   * @throws JobPersistenceException
   */
  boolean checkExists (TriggerKey triggerKey) throws JobPersistenceException;

  /**
   * Clear (delete!) all scheduling data - all {@link IJob}s, {@link ITrigger}s
   * {@link ICalendar}s.
   *
   * @throws JobPersistenceException
   */
  void clearAllSchedulingData () throws JobPersistenceException;

  /**
   * Store the given <code>{@link com.helger.quartz.ICalendar}</code>.
   *
   * @param calendar
   *        The <code>Calendar</code> to be stored.
   * @param replaceExisting
   *        If <code>true</code>, any <code>Calendar</code> existing in the
   *        <code>JobStore</code> with the same name &amp; group should be
   *        over-written.
   * @param updateTriggers
   *        If <code>true</code>, any <code>Trigger</code>s existing in the
   *        <code>JobStore</code> that reference an existing Calendar with the
   *        same name with have their next fire time re-computed with the new
   *        <code>Calendar</code>.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Calendar</code> with the same name already exists, and
   *         replaceExisting is set to false.
   */
  void storeCalendar (String name,
                      ICalendar calendar,
                      boolean replaceExisting,
                      boolean updateTriggers) throws ObjectAlreadyExistsException, JobPersistenceException;

  /**
   * Remove (delete) the <code>{@link com.helger.quartz.ICalendar}</code> with
   * the given name.
   * <p>
   * If removal of the <code>Calendar</code> would result in
   * <code>Trigger</code>s pointing to non-existent calendars, then a
   * <code>JobPersistenceException</code> will be thrown.
   * </p>
   * *
   *
   * @param calName
   *        The name of the <code>Calendar</code> to be removed.
   * @return <code>true</code> if a <code>Calendar</code> with the given name
   *         was found and removed from the store.
   */
  boolean removeCalendar (String calName) throws JobPersistenceException;

  /**
   * Retrieve the given <code>{@link com.helger.quartz.ITrigger}</code>.
   *
   * @param calName
   *        The name of the <code>Calendar</code> to be retrieved.
   * @return The desired <code>Calendar</code>, or null if there is no match.
   */
  ICalendar retrieveCalendar (String calName) throws JobPersistenceException;

  /////////////////////////////////////////////////////////////////////////////
  //
  // Informational methods
  //
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Get the number of <code>{@link com.helger.quartz.IJob}</code> s that are
   * stored in the <code>JobsStore</code>.
   */
  int getNumberOfJobs () throws JobPersistenceException;

  /**
   * Get the number of <code>{@link com.helger.quartz.ITrigger}</code> s that
   * are stored in the <code>JobsStore</code>.
   */
  int getNumberOfTriggers () throws JobPersistenceException;

  /**
   * Get the number of <code>{@link com.helger.quartz.ICalendar}</code> s that
   * are stored in the <code>JobsStore</code>.
   */
  int getNumberOfCalendars () throws JobPersistenceException;

  /**
   * Get the keys of all of the <code>{@link com.helger.quartz.IJob}</code> s
   * that have the given group name.
   * <p>
   * If there are no jobs in the given group name, the result should be an empty
   * collection (not <code>null</code>).
   * </p>
   */
  Set <JobKey> getJobKeys (GroupMatcher <JobKey> matcher) throws JobPersistenceException;

  /**
   * Get the names of all of the <code>{@link com.helger.quartz.ITrigger}</code>
   * s that have the given group name.
   * <p>
   * If there are no triggers in the given group name, the result should be a
   * zero-length array (not <code>null</code>).
   * </p>
   */
  Set <TriggerKey> getTriggerKeys (GroupMatcher <TriggerKey> matcher) throws JobPersistenceException;

  /**
   * Get the names of all of the <code>{@link com.helger.quartz.IJob}</code>
   * groups.
   * <p>
   * If there are no known group names, the result should be a zero-length array
   * (not <code>null</code>).
   * </p>
   */
  List <String> getJobGroupNames () throws JobPersistenceException;

  /**
   * Get the names of all of the <code>{@link com.helger.quartz.ITrigger}</code>
   * groups.
   * <p>
   * If there are no known group names, the result should be a zero-length array
   * (not <code>null</code>).
   * </p>
   */
  List <String> getTriggerGroupNames () throws JobPersistenceException;

  /**
   * Get the names of all of the
   * <code>{@link com.helger.quartz.ICalendar}</code> s in the
   * <code>JobStore</code>.
   * <p>
   * If there are no Calendars in the given group name, the result should be a
   * zero-length array (not <code>null</code>).
   * </p>
   */
  List <String> getCalendarNames () throws JobPersistenceException;

  /**
   * Get all of the Triggers that are associated to the given Job.
   * <p>
   * If there are no matches, a zero-length array should be returned.
   * </p>
   */
  List <IOperableTrigger> getTriggersForJob (JobKey jobKey) throws JobPersistenceException;

  /**
   * Get the current state of the identified <code>{@link ITrigger}</code>.
   *
   * @see ITrigger.TriggerState
   */
  TriggerState getTriggerState (TriggerKey triggerKey) throws JobPersistenceException;

  /////////////////////////////////////////////////////////////////////////////
  //
  // Trigger State manipulation methods
  //
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Pause the <code>{@link com.helger.quartz.ITrigger}</code> with the given
   * key.
   *
   * @see #resumeTrigger(TriggerKey)
   */
  void pauseTrigger (TriggerKey triggerKey) throws JobPersistenceException;

  /**
   * Pause all of the <code>{@link com.helger.quartz.ITrigger}s</code> in the
   * given group.
   * <p>
   * The JobStore should "remember" that the group is paused, and impose the
   * pause on any new triggers that are added to the group while the group is
   * paused.
   * </p>
   *
   * @see #resumeTriggers(GroupMatcher)
   */
  Collection <String> pauseTriggers (GroupMatcher <TriggerKey> matcher) throws JobPersistenceException;

  /**
   * Pause the <code>{@link com.helger.quartz.IJob}</code> with the given name -
   * by pausing all of its current <code>Trigger</code>s.
   *
   * @see #resumeJob(JobKey)
   */
  void pauseJob (JobKey jobKey) throws JobPersistenceException;

  /**
   * Pause all of the <code>{@link com.helger.quartz.IJob}s</code> in the given
   * group - by pausing all of their <code>Trigger</code>s.
   * <p>
   * The JobStore should "remember" that the group is paused, and impose the
   * pause on any new jobs that are added to the group while the group is
   * paused.
   * </p>
   *
   * @see #resumeJobs(GroupMatcher)
   */
  Collection <String> pauseJobs (GroupMatcher <JobKey> groupMatcher) throws JobPersistenceException;

  /**
   * Resume (un-pause) the <code>{@link com.helger.quartz.ITrigger}</code> with
   * the given key.
   * <p>
   * If the <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   *
   * @see #pauseTrigger(TriggerKey)
   */
  void resumeTrigger (TriggerKey triggerKey) throws JobPersistenceException;

  /**
   * Resume (un-pause) all of the
   * <code>{@link com.helger.quartz.ITrigger}s</code> in the given group.
   * <p>
   * If any <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   *
   * @see #pauseTriggers(GroupMatcher)
   */
  Collection <String> resumeTriggers (GroupMatcher <TriggerKey> matcher) throws JobPersistenceException;

  Set <String> getPausedTriggerGroups () throws JobPersistenceException;

  /**
   * Resume (un-pause) the <code>{@link com.helger.quartz.IJob}</code> with the
   * given key.
   * <p>
   * If any of the <code>Job</code>'s<code>Trigger</code> s missed one or more
   * fire-times, then the <code>Trigger</code>'s misfire instruction will be
   * applied.
   * </p>
   *
   * @see #pauseJob(JobKey)
   */
  void resumeJob (JobKey jobKey) throws JobPersistenceException;

  /**
   * Resume (un-pause) all of the <code>{@link com.helger.quartz.IJob}s</code>
   * in the given group.
   * <p>
   * If any of the <code>Job</code> s had <code>Trigger</code> s that missed one
   * or more fire-times, then the <code>Trigger</code>'s misfire instruction
   * will be applied.
   * </p>
   *
   * @see #pauseJobs(GroupMatcher)
   */
  Collection <String> resumeJobs (GroupMatcher <JobKey> matcher) throws JobPersistenceException;

  /**
   * Pause all triggers - equivalent of calling
   * <code>pauseTriggerGroup(group)</code> on every group.
   * <p>
   * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
   * instructions WILL be applied.
   * </p>
   *
   * @see #resumeAll()
   * @see #pauseTriggers(GroupMatcher)
   */
  void pauseAll () throws JobPersistenceException;

  /**
   * Resume (un-pause) all triggers - equivalent of calling
   * <code>resumeTriggerGroup(group)</code> on every group.
   * <p>
   * If any <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   *
   * @see #pauseAll()
   */
  void resumeAll () throws JobPersistenceException;

  /////////////////////////////////////////////////////////////////////////////
  //
  // Trigger-Firing methods
  //
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Get a handle to the next trigger to be fired, and mark it as 'reserved' by
   * the calling scheduler.
   *
   * @param noLaterThan
   *        If &gt; 0, the JobStore should only return a Trigger that will fire
   *        no later than the time represented in this value as milliseconds.
   * @see #releaseAcquiredTrigger(IOperableTrigger)
   */
  List <IOperableTrigger> acquireNextTriggers (long noLaterThan,
                                               int maxCount,
                                               long timeWindow) throws JobPersistenceException;

  /**
   * Inform the <code>JobStore</code> that the scheduler no longer plans to fire
   * the given <code>Trigger</code>, that it had previously acquired (reserved).
   */
  void releaseAcquiredTrigger (IOperableTrigger trigger);

  /**
   * Inform the <code>JobStore</code> that the scheduler is now firing the given
   * <code>Trigger</code> (executing its associated <code>Job</code>), that it
   * had previously acquired (reserved).
   *
   * @return may return null if all the triggers or their calendars no longer
   *         exist, or if the trigger was not successfully put into the
   *         'executing' state. Preference is to return an empty list if none of
   *         the triggers could be fired.
   */
  List <TriggerFiredResult> triggersFired (List <IOperableTrigger> triggers) throws JobPersistenceException;

  /**
   * Inform the <code>JobStore</code> that the scheduler has completed the
   * firing of the given <code>Trigger</code> (and the execution of its
   * associated <code>Job</code> completed, threw an exception, or was vetoed),
   * and that the <code>{@link com.helger.quartz.JobDataMap}</code> in the given
   * <code>JobDetail</code> should be updated if the <code>Job</code> is
   * stateful.
   */
  void triggeredJobComplete (IOperableTrigger trigger,
                             IJobDetail jobDetail,
                             CompletedExecutionInstruction triggerInstCode);

  /**
   * Inform the <code>JobStore</code> of the Scheduler instance's Id, prior to
   * initialize being invoked.
   *
   * @since 1.7
   */
  void setInstanceId (String schedInstId);

  /**
   * Inform the <code>JobStore</code> of the Scheduler instance's name, prior to
   * initialize being invoked.
   *
   * @since 1.7
   */
  void setInstanceName (String schedName);

  /**
   * Tells the JobStore the pool size used to execute jobs
   *
   * @param poolSize
   *        amount of threads allocated for job execution
   * @since 2.0
   */
  void setThreadPoolSize (int poolSize);
}
