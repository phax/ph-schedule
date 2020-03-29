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
package com.helger.quartz.integrations.tests;

import static com.helger.quartz.JobBuilder.newJob;
import static com.helger.quartz.SimpleScheduleBuilder.simpleSchedule;
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.lang.NonBlockingProperties;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.ISimpleTrigger;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.simpl.RAMJobStore;
import com.helger.quartz.spi.IOperableTrigger;

/**
 * Integration test for reproducing QTZ-336 where we don't check for the
 * scheduling change signal.
 */
public class QTZ336_MissSchedulingChangeSignalTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (QTZ336_MissSchedulingChangeSignalTest.class);

  @Test
  @Ignore ("Takes nearly a minute to execute and works")
  public void testSimpleScheduleAlwaysFiredUnder50s () throws Exception
  {
    final NonBlockingProperties properties = new NonBlockingProperties ();
    try (final InputStream propertiesIs = getClass ().getResourceAsStream ("/quartz/quartz.properties"))
    {
      properties.load (propertiesIs);
    }
    // Use a custom RAMJobStore to produce context switches leading to the race
    // condition
    properties.setProperty (StdSchedulerFactory.PROP_JOB_STORE_CLASS, SlowRAMJobStore.class.getName ());
    final ISchedulerFactory sf = new StdSchedulerFactory ().initialize (properties);
    final IScheduler sched = sf.getScheduler ();
    LOGGER.info ("------- Initialization Complete -----------");

    LOGGER.info ("------- Scheduling Job  -------------------");

    final IJobDetail job = newJob (CollectDuractionBetweenFireTimesJob.class).withIdentity ("job", "group").build ();

    final ISimpleTrigger trigger = newTrigger ().withIdentity ("trigger1", "group1")
                                                .startAt (new Date (System.currentTimeMillis () + 1000))
                                                .withSchedule (simpleSchedule ().withIntervalInSeconds (1)
                                                                                .repeatForever ()
                                                                                .withMisfireHandlingInstructionIgnoreMisfires ())
                                                .build ();

    sched.scheduleJob (job, trigger);

    // Start up the scheduler (nothing can actually run until the
    // scheduler has been started)
    sched.start ();

    LOGGER.info ("------- Scheduler Started -----------------");

    // wait long enough so that the scheduler has an opportunity to
    // run the job in theory around 50 times
    ThreadHelper.sleep (50000L);

    final List <Long> durationBetweenFireTimesInMillis = CollectDuractionBetweenFireTimesJob.getDurations ();

    assertFalse ("Job was not executed once!", durationBetweenFireTimesInMillis.isEmpty ());

    // Let's check that every call for around 1 second and not between 23 and 30
    // seconds
    // which would be the case if the scheduling change signal were not checked
    for (final long durationInMillis : durationBetweenFireTimesInMillis)
    {
      assertTrue ("Missed an execution with one duration being between two fires: " +
                  durationInMillis +
                  " (all: " +
                  durationBetweenFireTimesInMillis +
                  ")",
                  durationInMillis < 20000);
    }
  }

  /**
   * A simple job for collecting fire times in order to check that we did not
   * miss one call, for having the race condition the job must be real quick and
   * not allowing concurrent executions.
   */
  @DisallowConcurrentExecution
  public static class CollectDuractionBetweenFireTimesJob implements IJob
  {
    private static final Logger LOG = LoggerFactory.getLogger (CollectDuractionBetweenFireTimesJob.class);
    private static final List <Long> s_aDurationBetweenFireTimes = Collections.synchronizedList (new ArrayList <Long> ());
    private static Long m_aLastFireTime = null;

    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      final Date now = new Date ();
      LOG.info ("Fire time: " + now);
      if (m_aLastFireTime != null)
      {
        s_aDurationBetweenFireTimes.add (Long.valueOf (now.getTime () - m_aLastFireTime.longValue ()));
      }
      m_aLastFireTime = Long.valueOf (now.getTime ());
    }

    /**
     * Retrieves the durations between fire times.
     *
     * @return the durations in millis as an immutable list.
     */
    public static List <Long> getDurations ()
    {
      synchronized (s_aDurationBetweenFireTimes)
      {
        return Collections.unmodifiableList (s_aDurationBetweenFireTimes);
      }
    }
  }

  /**
   * Custom RAMJobStore for producing context switches.
   */
  public static class SlowRAMJobStore extends RAMJobStore
  {
    @Override
    public ICommonsList <IOperableTrigger> acquireNextTriggers (final long noLaterThan,
                                                                final int maxCount,
                                                                final long timeWindow)
    {
      final ICommonsList <IOperableTrigger> nextTriggers = super.acquireNextTriggers (noLaterThan,
                                                                                      maxCount,
                                                                                      timeWindow);
      // Wait just a bit for hopefully having a context switch leading to the
      // race condition
      ThreadHelper.sleep (10);
      return nextTriggers;
    }
  }
}
