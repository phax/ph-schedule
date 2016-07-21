package com.helger.quartz.core;

import java.util.Timer;

import com.helger.quartz.JobDetail;
import com.helger.quartz.JobExecutionContext;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.JobListener;
import com.helger.quartz.Trigger;
import com.helger.quartz.listeners.SchedulerListenerSupport;
import com.helger.quartz.utils.counter.CounterConfig;
import com.helger.quartz.utils.counter.CounterManager;
import com.helger.quartz.utils.counter.CounterManagerImpl;
import com.helger.quartz.utils.counter.sampled.SampledCounter;
import com.helger.quartz.utils.counter.sampled.SampledCounterConfig;
import com.helger.quartz.utils.counter.sampled.SampledRateCounterConfig;

public class SampledStatisticsImpl extends SchedulerListenerSupport implements SampledStatistics, JobListener
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

  private volatile CounterManager counterManager;
  private final SampledCounter jobsScheduledCount;
  private final SampledCounter jobsExecutingCount;
  private final SampledCounter jobsCompletedCount;

  SampledStatisticsImpl (final QuartzScheduler scheduler)
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

  private SampledCounter createSampledCounter (final CounterConfig defaultCounterConfig)
  {
    return (SampledCounter) counterManager.createCounter (defaultCounterConfig);
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
  public void jobScheduled (final Trigger trigger)
  {
    jobsScheduledCount.increment ();
  }

  public void jobExecutionVetoed (final JobExecutionContext context)
  {
    /**/
  }

  public void jobToBeExecuted (final JobExecutionContext context)
  {
    jobsExecutingCount.increment ();
  }

  public void jobWasExecuted (final JobExecutionContext context, final JobExecutionException jobException)
  {
    jobsCompletedCount.increment ();
  }

  @Override
  public void jobAdded (final JobDetail jobDetail)
  {
    /**/
  }
}
