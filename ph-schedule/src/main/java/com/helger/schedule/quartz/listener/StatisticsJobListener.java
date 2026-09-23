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

import com.helger.annotation.Nonempty;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.JobExecutionException;
import com.helger.statistics.impl.StatisticsManager;

/**
 * A Quartz job listener that handles statistics for job executions. It handles vetoed job
 * executions as well as job executions.
 *
 * @author Philip Helger
 */
public class StatisticsJobListener implements IJobListener
{
  /** The prefix of all statistics handler names created by this class */
  public static final String STATS_PREFIX = "quartz.";
  /** The suffix of the counter that counts all finished job executions */
  public static final String STATS_SUFFIX_EXEC = "$EXEC";
  /** The suffix of the counter that counts all failed job executions */
  public static final String STATS_SUFFIX_ERROR = "$ERROR";
  /** The suffix of the counter that counts all vetoed job executions */
  public static final String STATS_SUFFIX_VETOED = "$VETOED";
  /**
   * The suffix of the timer that collects the runtime of all finished job executions.
   *
   * @since 6.2.0
   */
  public static final String STATS_SUFFIX_TIME = "$TIME";

  /**
   * Get the base name of all statistics handlers of a single job class. The suffixes
   * <code>STATS_SUFFIX_*</code> are appended to this name to get the name of a single handler.
   *
   * @param aJobClass
   *        The job class to get the name for. May not be <code>null</code>.
   * @return {@link #STATS_PREFIX} plus the local name of the provided class. Neither
   *         <code>null</code> nor empty.
   * @since 6.2.0
   */
  @NonNull
  @Nonempty
  public static String getStatisticsName (@NonNull final Class <? extends IJob> aJobClass)
  {
    return STATS_PREFIX + ClassHelper.getClassLocalName (aJobClass);
  }

  @NonNull
  @Nonempty
  public String getName ()
  {
    return "StatisticsJobListener";
  }

  @NonNull
  @Nonempty
  protected String getStatisticsName (@NonNull final IJobExecutionContext aContext)
  {
    return getStatisticsName (aContext.getJobDetail ().getJobClass ());
  }

  @Override
  public void jobToBeExecuted (@NonNull final IJobExecutionContext aContext)
  {}

  @Override
  public void jobExecutionVetoed (@NonNull final IJobExecutionContext aContext)
  {
    StatisticsManager.getCounterHandler (getStatisticsName (aContext) + STATS_SUFFIX_VETOED).increment ();
  }

  @Override
  public void jobWasExecuted (@NonNull final IJobExecutionContext aContext, final JobExecutionException aJobException)
  {
    final String sStatsName = getStatisticsName (aContext);
    StatisticsManager.getCounterHandler (sStatsName + STATS_SUFFIX_EXEC).increment ();
    if (aJobException != null)
      StatisticsManager.getCounterHandler (sStatsName + STATS_SUFFIX_ERROR).increment ();

    // The runtime is only valid after the job completed - it is -1 as long as the job is running
    final long nRunTimeMillis = aContext.getJobRunTime ();
    if (nRunTimeMillis >= 0)
      StatisticsManager.getTimerHandler (sStatsName + STATS_SUFFIX_TIME).addTime (nRunTimeMillis);
  }
}
