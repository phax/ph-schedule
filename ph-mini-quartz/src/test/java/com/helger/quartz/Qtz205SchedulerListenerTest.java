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

import static com.helger.quartz.JobBuilder.newJob;
import static com.helger.quartz.SimpleScheduleBuilder.simpleSchedule;
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.base.rt.NonBlockingProperties;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * A unit test to reproduce QTZ-205 bug: A TriggerListener vetoed job will
 * affect SchedulerListener's triggerFinalized() notification.
 *
 * @author Zemian Deng saltnlight5@gmail.com
 */
public class Qtz205SchedulerListenerTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Qtz205SchedulerListenerTest.class);

  public static class Qtz205Job implements IJob
  {
    private static volatile int jobExecutionCount = 0;

    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      jobExecutionCount++;
      LOGGER.info ("Job executed. jobExecutionCount=" + jobExecutionCount);
    }
  }

  public static class Qtz205TriggerListener implements ITriggerListener
  {
    private volatile int fireCount;

    public int getFireCount ()
    {
      return fireCount;
    }

    public String getName ()
    {
      return "Qtz205TriggerListener";
    }

    public void triggerFired (final ITrigger trigger, final IJobExecutionContext context)
    {
      fireCount++;
      LOGGER.info ("Trigger fired. count " + fireCount);
    }

    public boolean vetoJobExecution (final ITrigger trigger, final IJobExecutionContext context)
    {
      if (fireCount >= 3)
      {
        LOGGER.info ("Job execution vetoed.");
        return true;
      }
      return false;
    }

    public void triggerMisfired (final ITrigger trigger)
    {}

    public void triggerComplete (final ITrigger trigger,
                                 final IJobExecutionContext context,
                                 final ECompletedExecutionInstruction triggerInstructionCode)
    {}

  }

  public static class Qtz205ScheListener implements ISchedulerListener
  {
    private int triggerFinalizedCount;

    public int getTriggerFinalizedCount ()
    {
      return triggerFinalizedCount;
    }

    public void jobScheduled (final ITrigger trigger)
    {}

    public void jobUnscheduled (final TriggerKey triggerKey)
    {}

    public void triggerFinalized (final ITrigger trigger)
    {
      triggerFinalizedCount++;
      LOGGER.info ("triggerFinalized " + trigger);
    }

    public void triggerPaused (final TriggerKey triggerKey)
    {}

    public void triggersPaused (final String triggerGroup)
    {}

    public void triggerResumed (final TriggerKey triggerKey)
    {}

    public void triggersResumed (final String triggerGroup)
    {}

    public void jobAdded (final IJobDetail jobDetail)
    {}

    public void jobDeleted (final JobKey jobKey)
    {}

    public void jobPaused (final JobKey jobKey)
    {}

    public void jobsPaused (final String jobGroup)
    {}

    public void jobResumed (final JobKey jobKey)
    {}

    public void jobsResumed (final String jobGroup)
    {

    }

    public void schedulerError (final String msg, final SchedulerException cause)
    {}

    public void schedulerInStandbyMode ()
    {}

    public void schedulerStarted ()
    {}

    public void schedulerStarting ()
    {}

    public void schedulerShutdown ()
    {}

    public void schedulerShuttingdown ()
    {}

    public void schedulingDataCleared ()
    {}
  }

  /** QTZ-205 */

  @Test
  public void testTriggerFinalized () throws Exception
  {
    final Qtz205TriggerListener triggerListener = new Qtz205TriggerListener ();
    final Qtz205ScheListener schedulerListener = new Qtz205ScheListener ();
    final NonBlockingProperties props = new NonBlockingProperties ();
    props.setProperty ("org.quartz.scheduler.idleWaitTime", "1500");
    props.setProperty ("org.quartz.threadPool.threadCount", "2");
    final IScheduler scheduler = new StdSchedulerFactory ().initialize (props).getScheduler ();
    scheduler.getListenerManager ().addSchedulerListener (schedulerListener);
    scheduler.getListenerManager ().addTriggerListener (triggerListener);
    scheduler.start ();
    scheduler.standby ();

    final IJobDetail job = newJob (Qtz205Job.class).withIdentity ("test").build ();
    final ITrigger trigger = newTrigger ().withIdentity ("test")
                                          .withSchedule (simpleSchedule ().withIntervalInMilliseconds (250)
                                                                          .withRepeatCount (2))
                                          .build ();
    scheduler.scheduleJob (job, trigger);
    scheduler.start ();
    ThreadHelper.sleep (5000);

    scheduler.shutdown (true);

    assertEquals (2, Qtz205Job.jobExecutionCount);
    assertEquals (3, triggerListener.getFireCount ());
    assertEquals (1, schedulerListener.getTriggerFinalizedCount ());
  }
}
