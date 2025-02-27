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
package com.helger.schedule.job;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

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
import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;

/**
 * Abstract {@link IJob} implementation with an exception handler etc.
 *
 * @author Philip Helger
 */
@ThreadSafe
public abstract class AbstractJob implements IJob
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractJob.class);
  private static final IMutableStatisticsHandlerKeyedTimer STATS_TIMER = StatisticsManager.getKeyedTimerHandler (AbstractJob.class);
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_SUCCESS = StatisticsManager.getKeyedCounterHandler (AbstractJob.class +
                                                                                                                               "$success");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_FAILURE = StatisticsManager.getKeyedCounterHandler (AbstractJob.class +
                                                                                                                               "$failure");
  private static final CallbackList <IJobExceptionCallback> EXCEPTION_CALLBACKS = new CallbackList <> ();

  public AbstractJob ()
  {}

  /**
   * @return The custom exception handler. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableObject
  public static CallbackList <IJobExceptionCallback> exceptionCallbacks ()
  {
    return EXCEPTION_CALLBACKS;
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
  protected void beforeExecute (@Nonnull final JobDataMap aJobDataMap, @Nonnull final IJobExecutionContext aContext)
  {}

  /**
   * This is the method with the main actions to be executed.
   *
   * @param aJobDataMap
   *        The current job data map. Never <code>null</code>.
   * @param aContext
   *        The Quartz context
   * @throws JobExecutionException
   *         In case of an error in execution
   */
  @OverrideOnDemand
  protected abstract void onExecute (@Nonnull final JobDataMap aJobDataMap,
                                     @Nonnull final IJobExecutionContext aContext) throws JobExecutionException;

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
                               @Nonnull final IJobExecutionContext aContext,
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
   *        The {@link IJob} instance
   */
  protected static void triggerCustomExceptionHandler (@Nonnull final Throwable t,
                                                       @Nullable final String sJobClassName,
                                                       @Nonnull final IJob aJob)
  {
    exceptionCallbacks ().forEach (x -> x.onScheduledJobException (t, sJobClassName, aJob));
  }

  public final void execute (@Nonnull final IJobExecutionContext aContext) throws JobExecutionException
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
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Executing scheduled job " + sJobClassName);

        final StopWatch aSW = StopWatch.createdStarted ();

        // Main execution
        onExecute (aJobDataMap, aContext);

        // Execution without exception -> success
        eExecSuccess = ESuccess.SUCCESS;

        // Increment statistics
        STATS_TIMER.addTime (sJobClassName, aSW.stopAndGetMillis ());
        STATS_COUNTER_SUCCESS.increment (sJobClassName);

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Successfully finished executing scheduled job " + sJobClassName);
      }
      catch (final Exception ex)
      {
        // Increment statistics
        STATS_COUNTER_FAILURE.increment (sJobClassName);

        // Notify custom exception handler
        triggerCustomExceptionHandler (ex, sJobClassName, this);

        if (ex instanceof JobExecutionException)
          throw (JobExecutionException) ex;
        throw new JobExecutionException ("Internal job execution error of " + sJobClassName, ex);
      }
    }
    finally
    {
      // Invoke callback
      afterExecute (aJobDataMap, aContext, eExecSuccess);
    }
  }
}
