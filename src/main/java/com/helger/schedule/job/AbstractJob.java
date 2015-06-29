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
package com.helger.schedule.job;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedTimer;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.commons.timing.StopWatch;

/**
 * Abstract {@link Job} implementation with an exception handler etc.
 *
 * @author Philip Helger
 */
@ThreadSafe
public abstract class AbstractJob implements Job
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractJob.class);
  private static final IMutableStatisticsHandlerKeyedTimer s_aStatsTimer = StatisticsManager.getKeyedTimerHandler (AbstractJob.class);
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterSuccess = StatisticsManager.getKeyedCounterHandler (AbstractJob.class +
                                                                                                                                "$success");
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterFailure = StatisticsManager.getKeyedCounterHandler (AbstractJob.class +
                                                                                                                                "$failure");
  private static final CallbackList <IJobExceptionCallback> s_aExceptionCallbacks = new CallbackList <IJobExceptionCallback> ();

  public AbstractJob ()
  {}

  /**
   * @return The custom exception handler. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject ("design")
  public static CallbackList <IJobExceptionCallback> getExceptionCallbacks ()
  {
    return s_aExceptionCallbacks;
  }

  /**
   * Called before the job gets executed. This method is called before the
   * scopes are initialized!
   *
   * @param aJobDataMap
   *        The current job data map. Never <code>null</code>. The map might be
   *        modified inside of this method.
   * @param aContext
   *        The current job execution context. Never <code>null</code>.
   */
  @OverrideOnDemand
  protected void beforeExecute (@Nonnull final JobDataMap aJobDataMap, @Nonnull final JobExecutionContext aContext)
  {}

  /**
   * This is the method with the main actions to be executed.
   *
   * @param aContext
   *        The Quartz context
   * @throws JobExecutionException
   *         In case of an error in execution
   */
  protected abstract void onExecute (@Nonnull final JobExecutionContext aContext) throws JobExecutionException;

  /**
   * Called after the job gets executed. This method is called after the scopes
   * are destroyed.
   *
   * @param aJobDataMap
   *        The current job data map. Never <code>null</code>.
   * @param aContext
   *        The current job execution context. Never <code>null</code>.
   * @param eExecSuccess
   *        The execution success state. Never <code>null</code>.
   */
  @OverrideOnDemand
  protected void afterExecute (@Nonnull final JobDataMap aJobDataMap,
                               @Nonnull final JobExecutionContext aContext,
                               @Nonnull final ESuccess eExecSuccess)
  {}

  /**
   * Called when an exception of the specified type occurred
   *
   * @param t
   *        The exception. Never <code>null</code>.
   * @param sJobClassName
   *        The name of the job class
   * @param aJob
   *        The {@link Job} instance
   */
  protected static void triggerCustomExceptionHandler (@Nonnull final Throwable t,
                                                       @Nullable final String sJobClassName,
                                                       @Nonnull final Job aJob)
  {
    for (final IJobExceptionCallback aCustomExceptionHandler : getExceptionCallbacks ().getAllCallbacks ())
      try
      {
        aCustomExceptionHandler.onScheduledJobException (t, sJobClassName, aJob);
      }
      catch (final Throwable t2)
      {
        s_aLogger.error ("Exception in custom scheduled job exception handler " +
                         aCustomExceptionHandler +
                         " for job class '" +
                         sJobClassName +
                         "'", t2);
      }
  }

  public final void execute (@Nonnull final JobExecutionContext aContext) throws JobExecutionException
  {
    // State variables
    ESuccess eExecSuccess = ESuccess.FAILURE;

    // Create a local copy of the job data map to allow for modifications and
    // alteration
    final JobDataMap aJobDataMap = new JobDataMap (aContext.getMergedJobDataMap ());

    beforeExecute (aJobDataMap, aContext);
    try
    {
      final String sJobClassName = getClass ().getName ();
      try
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Executing scheduled job " + sJobClassName);

        final StopWatch aSW = StopWatch.createdStarted ();

        // Main execution
        onExecute (aContext);

        // Execution without exception -> success
        eExecSuccess = ESuccess.SUCCESS;

        // Increment statistics
        s_aStatsTimer.addTime (sJobClassName, aSW.stopAndGetMillis ());
        s_aStatsCounterSuccess.increment (sJobClassName);

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Successfully finished executing scheduled job " + sJobClassName);
      }
      catch (final Throwable t)
      {
        // Increment statistics
        s_aStatsCounterFailure.increment (sJobClassName);

        // Notify custom exception handler
        triggerCustomExceptionHandler (t, sJobClassName, this);

        if (t instanceof JobExecutionException)
          throw (JobExecutionException) t;
        throw new JobExecutionException ("Internal job execution error of " + sJobClassName, t);
      }
    }
    finally
    {
      // Invoke callback
      afterExecute (aJobDataMap, aContext, eExecSuccess);
    }
  }
}
