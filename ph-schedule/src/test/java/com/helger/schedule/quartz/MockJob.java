/*
 * Copyright (C) 2014-2025 Philip Helger (www.helger.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobExecutionException;

public final class MockJob implements IJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MockJob.class);

  public void execute (final IJobExecutionContext context) throws JobExecutionException
  {
    LOGGER.info ("Scheduled routine executed!");
    GlobalQuartzSchedulerTest.EXEC_LOG.set (true);
  }
}
