
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

package org.quartz;

/**
 * The interface to be implemented by classes that want to be informed of major
 * <code>{@link Scheduler}</code> events.
 *
 * @see Scheduler
 * @see JobListener
 * @see TriggerListener
 * @author James House
 */
public interface SchedulerListener
{
  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link org.quartz.JobDetail}</code> is scheduled.
   * </p>
   * 
   * @param trigger
   */
  default void jobScheduled (final Trigger trigger)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link org.quartz.JobDetail}</code> is unscheduled.
   * </p>
   *
   * @param triggerKey
   * @see SchedulerListener#schedulingDataCleared()
   */
  default void jobUnscheduled (final TriggerKey triggerKey)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link Trigger}</code> has reached the condition in which it will
   * never fire again.
   * </p>
   * 
   * @param trigger
   */
  default void triggerFinalized (final Trigger trigger)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link Trigger}</code> has been paused.
   * </p>
   * 
   * @param triggerKey
   */
  default void triggerPaused (final TriggerKey triggerKey)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a group of
   * <code>{@link Trigger}s</code> has been paused.
   * </p>
   * <p>
   * If all groups were paused then triggerGroup will be null
   * </p>
   *
   * @param triggerGroup
   *        the paused group, or null if all were paused
   */
  default void triggersPaused (final String triggerGroup)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link Trigger}</code> has been un-paused.
   * </p>
   *
   * @param triggerKey
   */
  default void triggerResumed (final TriggerKey triggerKey)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a group of
   * <code>{@link Trigger}s</code> has been un-paused.
   * </p>
   *
   * @param triggerGroup
   */
  default void triggersResumed (final String triggerGroup)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link org.quartz.JobDetail}</code> has been added.
   * </p>
   *
   * @param jobDetail
   */
  default void jobAdded (final JobDetail jobDetail)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link org.quartz.JobDetail}</code> has been deleted.
   * </p>
   *
   * @param jobKey
   */
  default void jobDeleted (final JobKey jobKey)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link org.quartz.JobDetail}</code> has been paused.
   * </p>
   *
   * @param jobKey
   */
  default void jobPaused (final JobKey jobKey)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a group of
   * <code>{@link org.quartz.JobDetail}s</code> has been paused.
   * </p>
   *
   * @param jobGroup
   *        the paused group, or null if all were paused
   */
  default void jobsPaused (final String jobGroup)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a
   * <code>{@link org.quartz.JobDetail}</code> has been un-paused.
   * </p>
   *
   * @param jobKey
   */
  default void jobResumed (final JobKey jobKey)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a group of
   * <code>{@link org.quartz.JobDetail}s</code> has been un-paused.
   * </p>
   *
   * @param jobGroup
   */
  default void jobsResumed (final String jobGroup)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> when a serious error has
   * occurred within the scheduler - such as repeated failures in the
   * <code>JobStore</code>, or the inability to instantiate a <code>Job</code>
   * instance when its <code>Trigger</code> has fired.
   * </p>
   * <p>
   * The <code>getErrorCode()</code> method of the given SchedulerException can
   * be used to determine more specific information about the type of error that
   * was encountered.
   * </p>
   *
   * @param msg
   * @param cause
   */
  default void schedulerError (final String msg, final SchedulerException cause)
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it
   * has move to standby mode.
   * </p>
   */
  default void schedulerInStandbyMode ()
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it
   * has started.
   * </p>
   */
  default void schedulerStarted ()
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it
   * is starting.
   * </p>
   */
  default void schedulerStarting ()
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it
   * has shutdown.
   * </p>
   */
  default void schedulerShutdown ()
  {}

  /**
   * <p>
   * Called by the <code>{@link Scheduler}</code> to inform the listener that it
   * has begun the shutdown sequence.
   * </p>
   */
  default void schedulerShuttingdown ()
  {}

  /**
   * Called by the <code>{@link Scheduler}</code> to inform the listener that
   * all jobs, triggers and calendars were deleted.
   */
  default void schedulingDataCleared ()
  {}
}
