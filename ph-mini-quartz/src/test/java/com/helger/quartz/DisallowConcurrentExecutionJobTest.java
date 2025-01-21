/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2025 Philip Helger (www.helger.com)
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
package com.helger.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.lang.NonBlockingProperties;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * Integration test for using DisallowConcurrentExecution annotation.
 *
 * @author Zemian Deng saltnlight5@gmail.com
 */
public class DisallowConcurrentExecutionJobTest
{
  private static final long JOB_BLOCK_TIME = 300L;

  private static final String BARRIER = "BARRIER";
  private static final String DATE_STAMPS = "DATE_STAMPS";

  @DisallowConcurrentExecution
  public static class TestJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      try
      {
        @SuppressWarnings ("unchecked")
        final List <Date> jobExecDates = (List <Date>) context.getScheduler ().getContext ().get (DATE_STAMPS);
        final long firedAt = System.currentTimeMillis ();
        jobExecDates.add (new Date (firedAt));
        final long sleepTill = firedAt + JOB_BLOCK_TIME;
        for (long sleepFor = sleepTill - System.currentTimeMillis (); sleepFor > 0; sleepFor = sleepTill -
                                                                                               System.currentTimeMillis ())
        {
          ThreadHelper.sleep (sleepFor);
        }
      }
      catch (final SchedulerException e)
      {
        throw new JobExecutionException ("Failed to wait/lookup datestamp collection.", e);
      }
    }
  }

  public static class TestJobListener implements IJobListener
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (DisallowConcurrentExecutionJobTest.TestJobListener.class);

    private final AtomicInteger m_aJobExCount = new AtomicInteger (0);
    private final int m_nJobExecutionCountToSyncAfter;

    public TestJobListener (final int jobExecutionCountToSyncAfter)
    {
      m_nJobExecutionCountToSyncAfter = jobExecutionCountToSyncAfter;
    }

    public String getName ()
    {
      return "TestJobListener";
    }

    @Override
    public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
    {
      if (m_aJobExCount.incrementAndGet () == m_nJobExecutionCountToSyncAfter)
      {
        try
        {
          final CyclicBarrier barrier = (CyclicBarrier) context.getScheduler ().getContext ().get (BARRIER);
          barrier.await (125, TimeUnit.SECONDS);
        }
        catch (final Throwable e)
        {
          LOGGER.error ("Await on barrier was interrupted", e);
          throw new AssertionError ("Await on barrier was interrupted: " + e.toString ());
        }
      }
    }
  }

  @Test
  public void testNoConcurrentExecOnSameJob () throws Exception
  {
    final List <Date> jobExecDates = Collections.synchronizedList (new ArrayList <> ());
    final CyclicBarrier barrier = new CyclicBarrier (2);

    // make the triggers fire at the same time.
    final Date startTime = new Date (System.currentTimeMillis () + 100);

    final IJobDetail job1 = JobBuilder.newJob (TestJob.class).withIdentity ("job1").build ();
    final ITrigger trigger1 = TriggerBuilder.newTrigger ()
                                            .withSchedule (SimpleScheduleBuilder.simpleSchedule ())
                                            .startAt (startTime)
                                            .build ();

    final ITrigger trigger2 = TriggerBuilder.newTrigger ()
                                            .withSchedule (SimpleScheduleBuilder.simpleSchedule ())
                                            .startAt (startTime)
                                            .forJob (job1.getKey ())
                                            .build ();

    final NonBlockingProperties props = new NonBlockingProperties ();
    props.setProperty (StdSchedulerFactory.PROP_SCHED_IDLE_WAIT_TIME, "1500");
    props.setProperty ("org.quartz.threadPool.threadCount", "2");
    final IScheduler scheduler = new StdSchedulerFactory ().initialize (props).getScheduler ();
    scheduler.getContext ().put (BARRIER, barrier);
    scheduler.getContext ().put (DATE_STAMPS, jobExecDates);
    scheduler.getListenerManager ().addJobListener (new TestJobListener (2));
    scheduler.scheduleJob (job1, trigger1);
    scheduler.scheduleJob (trigger2);
    scheduler.start ();

    barrier.await (125, TimeUnit.SECONDS);

    scheduler.shutdown (true);

    assertEquals (2, jobExecDates.size ());
    final long fireTimeTrigger1 = jobExecDates.get (0).getTime ();
    final long fireTimeTrigger2 = jobExecDates.get (1).getTime ();
    assertTrue (fireTimeTrigger2 - fireTimeTrigger1 >= JOB_BLOCK_TIME);
  }

  /** QTZ-202 */
  @Test
  public void testNoConcurrentExecOnSameJobWithBatching () throws Exception
  {
    final List <Date> jobExecDates = Collections.synchronizedList (new ArrayList <> ());
    final CyclicBarrier barrier = new CyclicBarrier (2);

    // make the triggers fire at the same time.
    final Date startTime = new Date (System.currentTimeMillis () + 100);

    final IJobDetail job1 = JobBuilder.newJob (TestJob.class).withIdentity ("job1").build ();
    final ITrigger trigger1 = TriggerBuilder.newTrigger ()
                                            .withSchedule (SimpleScheduleBuilder.simpleSchedule ())
                                            .startAt (startTime)
                                            .build ();

    final ITrigger trigger2 = TriggerBuilder.newTrigger ()
                                            .withSchedule (SimpleScheduleBuilder.simpleSchedule ())
                                            .startAt (startTime)
                                            .forJob (job1.getKey ())
                                            .build ();

    final NonBlockingProperties props = new NonBlockingProperties ();
    props.setProperty (StdSchedulerFactory.PROP_SCHED_IDLE_WAIT_TIME, "1500");
    props.setProperty ("org.quartz.scheduler.batchTriggerAcquisitionMaxCount", "2");
    props.setProperty ("org.quartz.threadPool.threadCount", "2");
    final IScheduler scheduler = new StdSchedulerFactory ().initialize (props).getScheduler ();
    scheduler.getContext ().put (BARRIER, barrier);
    scheduler.getContext ().put (DATE_STAMPS, jobExecDates);
    scheduler.getListenerManager ().addJobListener (new TestJobListener (2));
    scheduler.scheduleJob (job1, trigger1);
    scheduler.scheduleJob (trigger2);
    scheduler.start ();

    barrier.await (125, TimeUnit.SECONDS);

    scheduler.shutdown (true);

    assertEquals (2, jobExecDates.size ());
    final long fireTimeTrigger1 = jobExecDates.get (0).getTime ();
    final long fireTimeTrigger2 = jobExecDates.get (1).getTime ();
    assertTrue (fireTimeTrigger2 - fireTimeTrigger1 >= JOB_BLOCK_TIME);
  }
}
