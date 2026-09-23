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
package com.helger.quartz;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The interface to be implemented by classes that want to be informed of major
 * <code>{@link IScheduler}</code> events.
 *
 * @see IScheduler
 * @see IJobListener
 * @see ITriggerListener
 * @author James House
 */
public interface ISchedulerListener
{
  /**
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> is scheduled.
   *
   * @param trigger
   */
  default void jobScheduled (@NonNull final ITrigger trigger)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> is unscheduled.
   *
   * @param triggerKey
   * @see ISchedulerListener#schedulingDataCleared()
   */
  default void jobUnscheduled (@NonNull final TriggerKey triggerKey)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a <code>{@link ITrigger}</code> has reached
   * the condition in which it will never fire again.
   *
   * @param trigger
   */
  default void triggerFinalized (@NonNull final ITrigger trigger)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a <code>{@link ITrigger}</code> has been
   * paused.
   *
   * @param triggerKey
   */
  default void triggerPaused (@Nullable final TriggerKey triggerKey)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a group of <code>{@link ITrigger}s</code>
   * has been paused. If all groups were paused then triggerGroup will be null.
   *
   * @param triggerGroup
   *        the paused group, or null if all were paused
   */
  default void triggersPaused (@Nullable final String triggerGroup)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a <code>{@link ITrigger}</code> has been
   * un-paused.
   *
   * @param triggerKey
   */
  default void triggerResumed (@Nullable final TriggerKey triggerKey)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a group of <code>{@link ITrigger}s</code>
   * has been un-paused.
   *
   * @param triggerGroup
   */
  default void triggersResumed (@Nullable final String triggerGroup)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> has been added.
   *
   * @param jobDetail
   */
  default void jobAdded (@NonNull final IJobDetail jobDetail)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> has been deleted.
   *
   * @param jobKey
   */
  default void jobDeleted (@NonNull final JobKey jobKey)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> has been paused.
   *
   * @param jobKey
   */
  default void jobPaused (@Nullable final JobKey jobKey)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a group of
   * <code>{@link com.helger.quartz.IJobDetail}s</code> has been paused.
   *
   * @param jobGroup
   *        the paused group, or null if all were paused
   */
  default void jobsPaused (@Nullable final String jobGroup)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> has been un-paused.
   *
   * @param jobKey
   */
  default void jobResumed (@Nullable final JobKey jobKey)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> when a group of
   * <code>{@link com.helger.quartz.IJobDetail}s</code> has been un-paused.
   *
   * @param jobGroup
   */
  default void jobsResumed (@Nullable final String jobGroup)
  {}

  /**
   * <p>
   * Called by the <code>{@link IScheduler}</code> when a serious error has occurred within the
   * scheduler - such as repeated failures in the <code>JobStore</code>, or the inability to
   * instantiate a <code>Job</code> instance when its <code>Trigger</code> has fired.
   * </p>
   * <p>
   * The <code>getErrorCode()</code> method of the given SchedulerException can be used to determine
   * more specific information about the type of error that was encountered.
   * </p>
   *
   * @param msg
   * @param cause
   */
  default void schedulerError (@Nullable final String msg, @Nullable final SchedulerException cause)
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> to inform the listener that it has move to
   * standby mode.
   */
  default void schedulerInStandbyMode ()
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> to inform the listener that it has started.
   */
  default void schedulerStarted ()
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> to inform the listener that it is starting.
   */
  default void schedulerStarting ()
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> to inform the listener that it has shutdown.
   */
  default void schedulerShutdown ()
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> to inform the listener that it has begun the
   * shutdown sequence.
   */
  default void schedulerShuttingdown ()
  {}

  /**
   * Called by the <code>{@link IScheduler}</code> to inform the listener that all jobs, triggers
   * and calendars were deleted.
   */
  default void schedulingDataCleared ()
  {}
}
