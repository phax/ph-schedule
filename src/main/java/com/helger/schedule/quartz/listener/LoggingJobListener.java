/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotations.Nonempty;
import com.helger.commons.lang.CGStringHelper;

/**
 * An implementation of the {@link JobListener} interface that logs job
 * executions. Before execution debug log level is used, for vetoed executions
 * warning level is used and after job execution either info (upon success) or
 * error (in case of an execution) is used.
 * 
 * @author Philip Helger
 */
public class LoggingJobListener implements JobListener
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (LoggingJobListener.class);

  @Nonnull
  @Nonempty
  public String getName ()
  {
    return "LoggingJobListener";
  }

  @Nonnull
  @Nonempty
  protected String getJobName (@Nonnull final JobExecutionContext aContext)
  {
    return CGStringHelper.getClassLocalName (aContext.getJobDetail ().getJobClass ());
  }

  public void jobToBeExecuted (@Nonnull final JobExecutionContext aContext)
  {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Job to be executed: " + getJobName (aContext));
  }

  public void jobExecutionVetoed (@Nonnull final JobExecutionContext aContext)
  {
    s_aLogger.warn ("Job execution vetoed by trigger listener: " + getJobName (aContext));
  }

  public void jobWasExecuted (@Nonnull final JobExecutionContext aContext, final JobExecutionException aJobException)
  {
    final Object aResult = aContext.getResult ();
    final long nRuntimeMilliSecs = aContext.getJobRunTime ();
    final String sMsg = "Job was executed: " +
                        getJobName (aContext) +
                        (aResult == null ? "" : "; result=" + aResult) +
                        "; duration=" +
                        nRuntimeMilliSecs +
                        "ms";
    if (aJobException == null)
      s_aLogger.info (sMsg);
    else
      s_aLogger.error (sMsg, aJobException);
  }
}
