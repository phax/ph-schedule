/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.simpl.SimpleThreadPool;

/**
 * Test job interruption
 */
public class InterruptableJobTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (InterruptableJobTest.class);

  static final CyclicBarrier sync = new CyclicBarrier (2);

  public static class TestInterruptableJob implements IInterruptableJob
  {
    public static final AtomicBoolean interrupted = new AtomicBoolean (false);

    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      s_aLogger.info ("TestInterruptableJob is executing.");
      try
      {
        sync.await (); // wait for test thread to notice the job is now running
      }
      catch (final InterruptedException e1)
      {}
      catch (final BrokenBarrierException e1)
      {}
      for (int i = 0; i < 200; i++)
      {
        try
        {
          Thread.sleep (50); // simulate being busy for a while, then checking
                             // interrupted flag...
        }
        catch (final InterruptedException ingore)
        {}
        if (TestInterruptableJob.interrupted.get ())
        {
          s_aLogger.info ("TestInterruptableJob main loop detected interrupt signal.");
          break;
        }
      }
      try
      {
        s_aLogger.info ("TestInterruptableJob exiting with interrupted = " + interrupted);
        sync.await ();
      }
      catch (final InterruptedException e)
      {}
      catch (final BrokenBarrierException e)
      {}
    }

    public void interrupt () throws UnableToInterruptJobException
    {
      TestInterruptableJob.interrupted.set (true);
      s_aLogger.info ("TestInterruptableJob.interrupt() called.");
    }
  }

  @Test
  public void testJobInterruption () throws Exception
  {
    // create a simple scheduler
    final Properties config = new Properties ();
    config.setProperty (StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "InterruptableJobTest_Scheduler");
    config.setProperty (StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, "AUTO");
    config.setProperty ("org.quartz.threadPool.threadCount", "2");
    config.setProperty ("org.quartz.threadPool.class", SimpleThreadPool.class.getName ());
    final IScheduler sched = new StdSchedulerFactory (config).getScheduler ();
    sched.start ();

    // add a job with a trigger that will fire immediately
    final IJobDetail job = newJob ().ofType (TestInterruptableJob.class).withIdentity ("j1").build ();
    final ITrigger trigger = newTrigger ().withIdentity ("t1").forJob (job).startNow ().build ();
    sched.scheduleJob (job, trigger);
    sync.await (); // make sure the job starts running...
    final List <IJobExecutionContext> executingJobs = sched.getCurrentlyExecutingJobs ();
    assertTrue ("Number of executing jobs should be 1 ", executingJobs.size () == 1);
    final IJobExecutionContext jec = executingJobs.get (0);
    final boolean interruptResult = sched.interrupt (jec.getFireInstanceId ());
    sync.await (); // wait for the job to terminate
    assertTrue ("Expected successful result from interruption of job ", interruptResult);
    assertTrue ("Expected interrupted flag to be set on job class ", TestInterruptableJob.interrupted.get ());
    sched.clear ();
    sched.shutdown ();
  }

}