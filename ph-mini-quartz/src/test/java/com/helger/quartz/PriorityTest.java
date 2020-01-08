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
package com.helger.quartz;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.lang.NonBlockingProperties;
import com.helger.quartz.impl.JobDetail;
import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.impl.triggers.SimpleTrigger;
import com.helger.quartz.simpl.SimpleThreadPool;
import com.helger.quartz.spi.IMutableTrigger;

/**
 * Test Trigger priority support.
 */
public class PriorityTest
{
  private static CountDownLatch s_aLatch;
  private static StringBuffer s_aResult;

  @PersistJobDataAfterExecution
  @DisallowConcurrentExecution
  public static class TestJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      s_aResult.append (context.getTrigger ().getKey ().getName ());
      s_aLatch.countDown ();
    }
  }

  @Before
  public void setUp () throws Exception
  {
    PriorityTest.s_aLatch = new CountDownLatch (2);
    PriorityTest.s_aResult = new StringBuffer ();
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testSameDefaultPriority () throws Exception
  {
    final NonBlockingProperties config = new NonBlockingProperties ();
    config.setProperty ("org.quartz.threadPool.threadCount", "1");
    config.setProperty ("org.quartz.threadPool.class", SimpleThreadPool.class.getName ());

    final IScheduler sched = new StdSchedulerFactory ().initialize (config).getScheduler ();

    final Calendar cal = PDTFactory.createCalendar ();
    cal.add (Calendar.SECOND, 1);

    final IMutableTrigger trig1 = new SimpleTrigger ("T1", null, cal.getTime ());
    final IMutableTrigger trig2 = new SimpleTrigger ("T2", null, cal.getTime ());

    final IJobDetail jobDetail = JobDetail.create ("JD", null, TestJob.class);

    sched.scheduleJob (jobDetail, trig1);

    trig2.setJobKey (new JobKey (jobDetail.getKey ().getName ()));
    sched.scheduleJob (trig2);

    sched.start ();

    s_aLatch.await ();

    assertEquals ("T1T2", s_aResult.toString ());

    sched.shutdown ();
  }

  @SuppressWarnings ("deprecation")
  @Test
  public void testDifferentPriority () throws Exception
  {
    final NonBlockingProperties config = new NonBlockingProperties ();
    config.setProperty ("org.quartz.threadPool.threadCount", "1");
    config.setProperty ("org.quartz.threadPool.class", SimpleThreadPool.class.getName ());

    final IScheduler sched = new StdSchedulerFactory ().initialize (config).getScheduler ();

    final Calendar cal = PDTFactory.createCalendar ();
    cal.add (Calendar.SECOND, 1);

    final IMutableTrigger trig1 = new SimpleTrigger ("T1", null, cal.getTime ());
    trig1.setPriority (5);

    final IMutableTrigger trig2 = new SimpleTrigger ("T2", null, cal.getTime ());
    trig2.setPriority (10);

    final IJobDetail jobDetail = JobDetail.create ("JD", null, TestJob.class);

    sched.scheduleJob (jobDetail, trig1);

    trig2.setJobKey (new JobKey (jobDetail.getKey ().getName (), null));
    sched.scheduleJob (trig2);

    sched.start ();

    s_aLatch.await ();

    assertEquals ("T2T1", s_aResult.toString ());

    sched.shutdown ();
  }
}
