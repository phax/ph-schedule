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
package com.helger.schedule.quartz.listener;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.datetime.helper.PDTFactory;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.JobExecutionException;

/**
 * A Quartz job listener that remembers the most recent failed job executions in
 * {@link JobExecutionErrorRegistry}. Without it, the reason why a job failed is only present in the
 * log file, which is regularly not accessible from where the failure is noticed.
 *
 * @author Philip Helger
 * @since 6.2.0
 */
public class ErrorHistoryJobListener implements IJobListener
{
  @NonNull
  @Nonempty
  public String getName ()
  {
    return "ErrorHistoryJobListener";
  }

  @Override
  public void jobToBeExecuted (@NonNull final IJobExecutionContext aContext)
  {}

  @Override
  public void jobExecutionVetoed (@NonNull final IJobExecutionContext aContext)
  {}

  @Override
  public void jobWasExecuted (@NonNull final IJobExecutionContext aContext,
                              @Nullable final JobExecutionException aJobException)
  {
    if (aJobException == null)
    {
      // Successful execution - nothing to remember
      return;
    }

    final IJobDetail aJobDetail = aContext.getJobDetail ();
    JobExecutionErrorRegistry.addError (new JobExecutionError (PDTFactory.getCurrentLocalDateTime (),
                                                               aJobDetail.getKey (),
                                                               aJobDetail.getJobClass ().getName (),
                                                               aContext.getFireInstanceId (),
                                                               aJobException));
  }
}
