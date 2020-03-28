/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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

import java.util.Timer;

import javax.annotation.Nonnull;

import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.listeners.AbstractSchedulerListenerSupport;
import com.helger.quartz.utils.counter.CounterConfig;
import com.helger.quartz.utils.counter.CounterManager;
import com.helger.quartz.utils.counter.ICounterManager;
import com.helger.quartz.utils.counter.sampled.ISampledCounter;
import com.helger.quartz.utils.counter.sampled.SampledCounterConfig;
import com.helger.quartz.utils.counter.sampled.SampledRateCounterConfig;

public class SampledStatistics extends AbstractSchedulerListenerSupport implements ISampledStatistics, IJobListener
{
  private static final String NAME = "QuartzSampledStatistics";

  private static final int DEFAULT_HISTORY_SIZE = 30;
  private static final int DEFAULT_INTERVAL_SECS = 1;
  private final static SampledCounterConfig DEFAULT_SAMPLED_COUNTER_CONFIG = new SampledCounterConfig (DEFAULT_INTERVAL_SECS,
                                                                                                       DEFAULT_HISTORY_SIZE,
                                                                                                       true,
                                                                                                       0L);
  @SuppressWarnings ("unused")
  private final static SampledRateCounterConfig DEFAULT_SAMPLED_RATE_COUNTER_CONFIG = new SampledRateCounterConfig (DEFAULT_INTERVAL_SECS,
                                                                                                                    DEFAULT_HISTORY_SIZE,
                                                                                                                    true);

  private final ICounterManager m_aCounterManager;
  private final ISampledCounter m_aJobsScheduledCount;
  private final ISampledCounter m_aJobsExecutingCount;
  private final ISampledCounter m_aJobsCompletedCount;

  SampledStatistics (@Nonnull final QuartzScheduler aScheduler)
  {
    m_aCounterManager = new CounterManager (new Timer (NAME + "Timer"));
    m_aJobsScheduledCount = createSampledCounter (DEFAULT_SAMPLED_COUNTER_CONFIG);
    m_aJobsExecutingCount = createSampledCounter (DEFAULT_SAMPLED_COUNTER_CONFIG);
    m_aJobsCompletedCount = createSampledCounter (DEFAULT_SAMPLED_COUNTER_CONFIG);

    aScheduler.addInternalSchedulerListener (this);
    aScheduler.addInternalJobListener (this);
  }

  public void shutdown ()
  {
    m_aCounterManager.shutdown (true);
  }

  private ISampledCounter createSampledCounter (final CounterConfig defaultCounterConfig)
  {
    return (ISampledCounter) m_aCounterManager.createCounter (defaultCounterConfig);
  }

  /**
   * Clears the collected statistics. Resets all counters to zero
   */
  public void clearStatistics ()
  {
    m_aJobsScheduledCount.getAndReset ();
    m_aJobsExecutingCount.getAndReset ();
    m_aJobsCompletedCount.getAndReset ();
  }

  public long getJobsCompletedMostRecentSample ()
  {
    return m_aJobsCompletedCount.getMostRecentSample ().getCounterValue ();
  }

  public long getJobsExecutingMostRecentSample ()
  {
    return m_aJobsExecutingCount.getMostRecentSample ().getCounterValue ();
  }

  public long getJobsScheduledMostRecentSample ()
  {
    return m_aJobsScheduledCount.getMostRecentSample ().getCounterValue ();
  }

  public String getName ()
  {
    return NAME;
  }

  @Override
  public void jobScheduled (final ITrigger trigger)
  {
    m_aJobsScheduledCount.increment ();
  }

  public void jobExecutionVetoed (final IJobExecutionContext context)
  {
    /**/
  }

  public void jobToBeExecuted (final IJobExecutionContext context)
  {
    m_aJobsExecutingCount.increment ();
  }

  public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {
    m_aJobsCompletedCount.increment ();
  }

  @Override
  public void jobAdded (final IJobDetail jobDetail)
  {
    /**/
  }
}
