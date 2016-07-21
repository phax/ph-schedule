package com.helger.quartz.core;

import java.util.Timer;

import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.IJobListener;
import com.helger.quartz.ITrigger;
import com.helger.quartz.listeners.AbstractSchedulerListenerSupport;
import com.helger.quartz.utils.counter.CounterConfig;
import com.helger.quartz.utils.counter.ICounterManager;
import com.helger.quartz.utils.counter.CounterManagerImpl;
import com.helger.quartz.utils.counter.sampled.ISampledCounter;
import com.helger.quartz.utils.counter.sampled.SampledCounterConfig;
import com.helger.quartz.utils.counter.sampled.SampledRateCounterConfig;

public class SampledStatistics extends AbstractSchedulerListenerSupport implements ISampledStatistics, IJobListener
{
  @SuppressWarnings ("unused")
  private final QuartzScheduler scheduler;

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

  private volatile ICounterManager counterManager;
  private final ISampledCounter jobsScheduledCount;
  private final ISampledCounter jobsExecutingCount;
  private final ISampledCounter jobsCompletedCount;

  SampledStatistics (final QuartzScheduler scheduler)
  {
    this.scheduler = scheduler;

    counterManager = new CounterManagerImpl (new Timer (NAME + "Timer"));
    jobsScheduledCount = createSampledCounter (DEFAULT_SAMPLED_COUNTER_CONFIG);
    jobsExecutingCount = createSampledCounter (DEFAULT_SAMPLED_COUNTER_CONFIG);
    jobsCompletedCount = createSampledCounter (DEFAULT_SAMPLED_COUNTER_CONFIG);

    scheduler.addInternalSchedulerListener (this);
    scheduler.addInternalJobListener (this);
  }

  public void shutdown ()
  {
    counterManager.shutdown (true);
  }

  private ISampledCounter createSampledCounter (final CounterConfig defaultCounterConfig)
  {
    return (ISampledCounter) counterManager.createCounter (defaultCounterConfig);
  }

  /**
   * Clears the collected statistics. Resets all counters to zero
   */
  public void clearStatistics ()
  {
    jobsScheduledCount.getAndReset ();
    jobsExecutingCount.getAndReset ();
    jobsCompletedCount.getAndReset ();
  }

  public long getJobsCompletedMostRecentSample ()
  {
    return jobsCompletedCount.getMostRecentSample ().getCounterValue ();
  }

  public long getJobsExecutingMostRecentSample ()
  {
    return jobsExecutingCount.getMostRecentSample ().getCounterValue ();
  }

  public long getJobsScheduledMostRecentSample ()
  {
    return jobsScheduledCount.getMostRecentSample ().getCounterValue ();
  }

  public String getName ()
  {
    return NAME;
  }

  @Override
  public void jobScheduled (final ITrigger trigger)
  {
    jobsScheduledCount.increment ();
  }

  public void jobExecutionVetoed (final IJobExecutionContext context)
  {
    /**/
  }

  public void jobToBeExecuted (final IJobExecutionContext context)
  {
    jobsExecutingCount.increment ();
  }

  public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {
    jobsCompletedCount.increment ();
  }

  @Override
  public void jobAdded (final IJobDetail jobDetail)
  {
    /**/
  }
}
