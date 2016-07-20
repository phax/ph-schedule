/*
 * Copyright 2001-2013 Terracotta, Inc.
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
 *
 */
package org.quartz.integrations.tests;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.quartz.integrations.tests.TrackingJob.SCHEDULED_TIMES_KEY;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.TriggerBuilder;

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
    final CronTrigger trigger = TriggerBuilder.newTrigger ()
                                              .withIdentity ("test")
                                              .withSchedule (CronScheduleBuilder.cronSchedule ("* * * * * ?"))
                                              .build ();
    final List <Long> scheduledTimes = Collections.synchronizedList (new LinkedList <Long> ());
    scheduler.getContext ().put (SCHEDULED_TIMES_KEY, scheduledTimes);
    final JobDetail jobDetail = JobBuilder.newJob (TrackingJob.class).withIdentity ("test").build ();
    scheduler.scheduleJob (jobDetail, trigger);

    for (int i = 0; i < 20 && scheduledTimes.size () < 3; i++)
    {
      Thread.sleep (500);
    }
    assertThat (scheduledTimes, hasSize (greaterThanOrEqualTo (3)));

    final Long [] times = scheduledTimes.toArray (new Long [scheduledTimes.size ()]);

    final long baseline = times[0];
    assertThat (baseline % 1000, is (0L));
    for (int i = 1; i < times.length; i++)
    {
      assertThat (times[i], is (baseline + TimeUnit.SECONDS.toMillis (i)));
    }
  }
}
