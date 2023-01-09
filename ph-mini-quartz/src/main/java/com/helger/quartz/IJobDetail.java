/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2023 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.lang.ICloneable;

/**
 * Conveys the detail properties of a given <code>Job</code> instance.
 * JobDetails are to be created/defined with {@link JobBuilder}.
 * <p>
 * Quartz does not store an actual instance of a <code>Job</code> class, but
 * instead allows you to define an instance of one, through the use of a
 * <code>JobDetail</code>.
 * </p>
 * <p>
 * <code>Job</code>s have a name and group associated with them, which should
 * uniquely identify them within a single <code>{@link IScheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s are
 * scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 *
 * @see JobBuilder
 * @see IJob
 * @see JobDataMap
 * @see ITrigger
 * @author James House
 */
public interface IJobDetail extends ICloneable <IJobDetail>
{
  @Nullable
  JobKey getKey ();

  /**
   * Return the description given to the <code>Job</code> instance by its
   * creator (if any).
   *
   * @return null if no description was set.
   */
  @Nullable
  String getDescription ();

  /**
   * Get the instance of <code>Job</code> that will be executed.
   */
  @Nullable
  Class <? extends IJob> getJobClass ();

  /**
   * Get the <code>JobDataMap</code> that is associated with the
   * <code>Job</code>.
   */
  @Nonnull
  JobDataMap getJobDataMap ();

  /**
   * Whether or not the <code>Job</code> should remain stored after it is
   * orphaned (no <code>{@link ITrigger}s</code> point to it).<br>
   * If not explicitly set, the default value is <code>false</code>.
   *
   * @return <code>true</code> if the Job should remain persisted after being
   *         orphaned.
   */
  boolean isDurable ();

  /**
   * @see PersistJobDataAfterExecution
   * @return whether the associated Job class carries the
   *         {@link PersistJobDataAfterExecution} annotation.
   */
  boolean isPersistJobDataAfterExecution ();

  /**
   * @see DisallowConcurrentExecution
   * @return whether the associated Job class carries the
   *         {@link DisallowConcurrentExecution} annotation.
   */
  boolean isConcurrentExectionDisallowed ();

  /**
   * <p>
   * Instructs the <code>Scheduler</code> whether or not the <code>Job</code>
   * should be re-executed if a 'recovery' or 'fail-over' situation is
   * encountered.
   * </p>
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   *
   * @see IJobExecutionContext#isRecovering()
   */
  boolean requestsRecovery ();

  /**
   * Get a {@link JobBuilder} that is configured to produce a
   * <code>JobDetail</code> identical to this one.
   */
  @Nonnull
  JobBuilder getJobBuilder ();
}
