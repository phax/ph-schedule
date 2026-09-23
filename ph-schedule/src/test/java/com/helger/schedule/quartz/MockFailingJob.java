/*
 * Copyright (C) 2014-2026 Philip Helger (www.helger.com)
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

import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobExecutionException;

/**
 * A job that always fails - used to verify the error collection of
 * {@link com.helger.schedule.quartz.listener.ErrorHistoryJobListener}.
 *
 * @author Philip Helger
 */
public final class MockFailingJob implements IJob
{
  /** The message of the exception that is always thrown */
  public static final String FAILURE_MESSAGE = "This job always fails";

  public void execute (final IJobExecutionContext context) throws JobExecutionException
  {
    throw new JobExecutionException (FAILURE_MESSAGE);
  }
}
