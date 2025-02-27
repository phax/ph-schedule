/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2025 Philip Helger (www.helger.com)
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
package com.helger.quartz.core;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerConfigException;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.spi.TriggerFiredBundle;

/**
 * <p>
 * Responsible for creating the instances of <code>{@link JobRunShell}</code> to
 * be used within the <code>{@link QuartzScheduler}</code> instance.
 * </p>
 *
 * @author James House
 */
public interface IJobRunShellFactory
{
  /**
   * <p>
   * Initialize the factory, providing a handle to the <code>Scheduler</code>
   * that should be made available within the <code>JobRunShell</code> and the
   * <code>JobExecutionContext</code> s within it.
   * </p>
   */
  void initialize (IScheduler scheduler) throws SchedulerConfigException;

  /**
   * <p>
   * Called by the
   * <code>{@link com.helger.quartz.core.QuartzSchedulerThread}</code> to obtain
   * instances of <code>{@link JobRunShell}</code>.
   * </p>
   */
  JobRunShell createJobRunShell (TriggerFiredBundle bundle) throws SchedulerException;
}
