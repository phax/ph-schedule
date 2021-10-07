/*
 * Copyright (C) 2014-2021 Philip Helger (www.helger.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.ClassHelper;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.JobExecutionException;

/**
 * An implementation of the {@link IJobListener} interface that logs job
 * executions. Before execution debug log level is used, for vetoed executions
 * warning level is used and after job execution either info (upon success) or
 * error (in case of an execution) is used.
 *
 * @author Philip Helger
 */
public class LoggingJobListener implements IJobListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingJobListener.class);

  @Nonnull
  @Nonempty
  public String getName ()
  {
    return "LoggingJobListener";
  }

  @Nonnull
  @Nonempty
  protected String getJobName (@Nonnull final IJobExecutionContext aContext)
  {
    String ret = aContext.getJobDetail ().getKey ().toString ();
    final String sClassName = ClassHelper.getClassLocalName (aContext.getJobDetail ().getJobClass ());
    if (!ret.contains (sClassName))
      ret += "@" + sClassName;
    return ret;
  }

  @Override
  public void jobToBeExecuted (@Nonnull final IJobExecutionContext aContext)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Job to be executed: " + getJobName (aContext));
  }

  @Override
  public void jobExecutionVetoed (@Nonnull final IJobExecutionContext aContext)
  {
    LOGGER.warn ("Job execution vetoed by trigger listener: " + getJobName (aContext));
  }

  @Override
  public void jobWasExecuted (@Nonnull final IJobExecutionContext aContext, final JobExecutionException aJobException)
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
      LOGGER.info (sMsg);
    else
      LOGGER.error (sMsg, aJobException);
  }
}
