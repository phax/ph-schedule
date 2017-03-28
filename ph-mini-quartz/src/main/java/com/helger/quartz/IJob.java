/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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
 * <p>
 * The interface to be implemented by classes which represent a 'job' to be
 * performed.
 * </p>
 * <p>
 * Instances of <code>Job</code> must have a <code>public</code> no-argument
 * constructor.
 * </p>
 * <p>
 * <code>JobDataMap</code> provides a mechanism for 'instance member data' that
 * may be required by some implementations of this interface.
 * </p>
 *
 * @see IJobDetail
 * @see JobBuilder
 * @see DisallowConcurrentExecution
 * @see PersistJobDataAfterExecution
 * @see ITrigger
 * @see IScheduler
 * @author James House
 */
public interface IJob extends Serializable
{
  /**
   * <p>
   * Called by the <code>{@link IScheduler}</code> when a
   * <code>{@link ITrigger}</code> fires that is associated with the
   * <code>Job</code>.
   * </p>
   * <p>
   * The implementation may wish to set a
   * {@link IJobExecutionContext#setResult(Object) result} object on the
   * {@link IJobExecutionContext} before this method exits. The result itself is
   * meaningless to Quartz, but may be informative to
   * <code>{@link IJobListener}s</code> or
   * <code>{@link ITriggerListener}s</code> that are watching the job's
   * execution.
   * </p>
   *
   * @throws JobExecutionException
   *         if there is an exception while executing the job.
   */
  void execute (IJobExecutionContext context) throws JobExecutionException;
}
