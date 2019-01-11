/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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

import static com.helger.quartz.integrations.tests.TrackingJob.SCHEDULED_TIMES_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.helger.quartz.CronScheduleBuilder;
import com.helger.quartz.ICronTrigger;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.JobBuilder;
import com.helger.quartz.TriggerBuilder;

/**
 * A integration test for Quartz In-Memory Scheduler with Cron Trigger.
 *
 * @author Zemian Deng
 */
public class QuartzMemoryCronTriggerTest extends QuartzMemoryTestSupport
{
  @Test
  public void testCronRepeatCount () throws Exception
  {
    final ICronTrigger trigger = TriggerBuilder.newTrigger ()
                                               .withIdentity ("test")
                                               .withSchedule (CronScheduleBuilder.cronSchedule ("* * * * * ?"))
                                               .build ();
    final List <Long> scheduledTimes = Collections.synchronizedList (new LinkedList <Long> ());
    scheduler.getContext ().put (SCHEDULED_TIMES_KEY, scheduledTimes);
    final IJobDetail jobDetail = JobBuilder.newJob (TrackingJob.class).withIdentity ("test").build ();
    scheduler.scheduleJob (jobDetail, trigger);

    for (int i = 0; i < 20 && scheduledTimes.size () < 3; i++)
    {
      Thread.sleep (500);
    }
    assertTrue (scheduledTimes.size () >= 3);

    final Long [] times = scheduledTimes.toArray (new Long [scheduledTimes.size ()]);

    final long baseline = times[0].longValue ();
    assertEquals (0, baseline % 1000);
    for (int i = 1; i < times.length; i++)
    {
      assertEquals (times[i].longValue (), baseline + TimeUnit.SECONDS.toMillis (i));
    }
  }
}
