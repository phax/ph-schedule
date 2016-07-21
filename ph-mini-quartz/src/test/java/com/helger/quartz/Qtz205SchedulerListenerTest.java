/*
 * Copyright 2001-2009 Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.helger.quartz;

import static com.helger.quartz.JobBuilder.newJob;
import static com.helger.quartz.SimpleScheduleBuilder.simpleSchedule;
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.Job;
import com.helger.quartz.JobDetail;
import com.helger.quartz.JobExecutionContext;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.JobKey;
import com.helger.quartz.Scheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SchedulerListener;
import com.helger.quartz.Trigger;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.TriggerListener;
import com.helger.quartz.Trigger.CompletedExecutionInstruction;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * A unit test to reproduce QTZ-205 bug: A TriggerListener vetoed job will
 * affect SchedulerListener's triggerFinalized() notification.
 *
 * @author Zemian Deng <saltnlight5@gmail.com>
 */
public class Qtz205SchedulerListenerTest
{
  private static Logger logger = LoggerFactory.getLogger (Qtz205SchedulerListenerTest.class);

  public static class Qtz205Job implements Job
  {
    private static volatile int jobExecutionCount = 0;

    public void execute (final JobExecutionContext context) throws JobExecutionException
    {
      jobExecutionCount++;
      logger.info ("Job executed. jobExecutionCount=" + jobExecutionCount);
    }

  }

  public static class Qtz205TriggerListener implements TriggerListener
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

    public void triggerFired (final Trigger trigger, final JobExecutionContext context)
    {
      fireCount++;
      logger.info ("Trigger fired. count " + fireCount);
    }

    public boolean vetoJobExecution (final Trigger trigger, final JobExecutionContext context)
    {
      if (fireCount >= 3)
      {
        logger.info ("Job execution vetoed.");
        return true;
      }
      return false;
    }

    public void triggerMisfired (final Trigger trigger)
    {}

    public void triggerComplete (final Trigger trigger,
                                 final JobExecutionContext context,
                                 final CompletedExecutionInstruction triggerInstructionCode)
    {}

  }

  public static class Qtz205ScheListener implements SchedulerListener
  {
    private int triggerFinalizedCount;

    public int getTriggerFinalizedCount ()
    {
      return triggerFinalizedCount;
    }

    public void jobScheduled (final Trigger trigger)
    {}

    public void jobUnscheduled (final TriggerKey triggerKey)
    {}

    public void triggerFinalized (final Trigger trigger)
    {
      triggerFinalizedCount++;
      logger.info ("triggerFinalized " + trigger);
    }

    public void triggerPaused (final TriggerKey triggerKey)
    {}

    public void triggersPaused (final String triggerGroup)
    {}

    public void triggerResumed (final TriggerKey triggerKey)
    {}

    public void triggersResumed (final String triggerGroup)
    {}

    public void jobAdded (final JobDetail jobDetail)
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
    final Properties props = new Properties ();
    props.setProperty ("org.quartz.scheduler.idleWaitTime", "1500");
    props.setProperty ("org.quartz.threadPool.threadCount", "2");
    final Scheduler scheduler = new StdSchedulerFactory (props).getScheduler ();
    scheduler.getListenerManager ().addSchedulerListener (schedulerListener);
    scheduler.getListenerManager ().addTriggerListener (triggerListener);
    scheduler.start ();
    scheduler.standby ();

    final JobDetail job = newJob (Qtz205Job.class).withIdentity ("test").build ();
    final Trigger trigger = newTrigger ().withIdentity ("test")
                                         .withSchedule (simpleSchedule ().withIntervalInMilliseconds (250)
                                                                         .withRepeatCount (2))
                                         .build ();
    scheduler.scheduleJob (job, trigger);
    scheduler.start ();
    Thread.sleep (5000);

    scheduler.shutdown (true);

    assertEquals (2, Qtz205Job.jobExecutionCount);
    assertEquals (3, triggerListener.getFireCount ());
    assertEquals (1, schedulerListener.getTriggerFinalizedCount ());
  }
}
