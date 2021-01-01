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
package com.helger.quartz;

import java.io.Serializable;

/**
 * The interface to be implemented by classes that want to be informed when a
 * <code>{@link com.helger.quartz.IJobDetail}</code> executes. In general,
 * applications that use a <code>Scheduler</code> will not have use for this
 * mechanism.
 *
 * @see IListenerManager#addJobListener(IJobListener, IMatcher)
 * @see IMatcher
 * @see IJob
 * @see IJobExecutionContext
 * @see JobExecutionException
 * @see ITriggerListener
 * @author James House
 */
public interface IJobListener extends Serializable
{
  /**
   * <p>
   * Get the name of the <code>JobListener</code>.
   * </p>
   */
  String getName ();

  /**
   * <p>
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> is about to be executed
   * (an associated <code>{@link ITrigger}</code> has occurred).
   * </p>
   * <p>
   * This method will not be invoked if the execution of the Job was vetoed by a
   * <code>{@link ITriggerListener}</code>.
   * </p>
   *
   * @param context
   * @see #jobExecutionVetoed(IJobExecutionContext)
   */
  default void jobToBeExecuted (final IJobExecutionContext context)
  {}

  /**
   * <p>
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link com.helger.quartz.IJobDetail}</code> was about to be executed
   * (an associated <code>{@link ITrigger}</code> has occurred), but a
   * <code>{@link ITriggerListener}</code> vetoed it's execution.
   * </p>
   *
   * @param context
   * @see #jobToBeExecuted(IJobExecutionContext)
   */
  default void jobExecutionVetoed (final IJobExecutionContext context)
  {}

  /**
   * <p>
   * Called by the <code>{@link IScheduler}</code> after a
   * <code>{@link com.helger.quartz.IJobDetail}</code> has been executed, and be
   * for the associated <code>Trigger</code>'s <code>triggered(xx)</code> method
   * has been called.
   * </p>
   *
   * @param context
   * @param jobException
   */
  default void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {}
}
