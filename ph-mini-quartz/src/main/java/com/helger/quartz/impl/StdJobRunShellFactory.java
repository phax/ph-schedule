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
package com.helger.quartz.impl;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.core.IJobRunShellFactory;
import com.helger.quartz.core.JobRunShell;
import com.helger.quartz.spi.TriggerFiredBundle;

/**
 * <p>
 * Responsible for creating the instances of
 * <code>{@link com.helger.quartz.core.JobRunShell}</code> to be used within the
 * <code>{@link com.helger.quartz.core.QuartzScheduler}</code> instance.
 * </p>
 *
 * @author James House
 */
public class StdJobRunShellFactory implements IJobRunShellFactory
{
  private IScheduler m_aScheduler;

  /**
   * Initialize the factory, providing a handle to the {@link IScheduler} that
   * should be made available within the {@link JobRunShell} and the
   * JobExecutionContexts within it.
   */
  public void initialize (final IScheduler aScheduler)
  {
    m_aScheduler = aScheduler;
  }

  /**
   * <p>
   * Called by the {@link com.helger.quartz.core.QuartzSchedulerThread} to
   * obtain instances of {@link com.helger.quartz.core.JobRunShell}.
   * </p>
   */
  public JobRunShell createJobRunShell (final TriggerFiredBundle bndle) throws SchedulerException
  {
    return new JobRunShell (m_aScheduler, bndle);
  }
}
