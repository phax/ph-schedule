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
 *
 */
package org.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.quartz.DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule;
import static org.quartz.DateBuilder.dateOf;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TimeOfDay.hourMinuteAndSecondOfDay;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.OperableTrigger;

/**
 * Unit test for DailyTimeIntervalScheduleBuilder.
 *
 * @author Zemian Deng <saltnlight5@gmail.com>
 */
public class DailyTimeIntervalScheduleBuilderTest
{
  @Test
  public void testScheduleActualTrigger () throws Exception
  {
    final Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler ();
    final JobDetail job = newJob (MyJob.class).build ();
    final DailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                          .withSchedule (dailyTimeIntervalSchedule ().withIntervalInSeconds (3))
                                                          .build ();
    scheduler.scheduleJob (job, trigger); // We are not verify anything other
                                          // than just run through the
                                          // scheduler.
    scheduler.shutdown ();
  }

  @Test
  public void testScheduleInMiddleOfDailyInterval () throws Exception
  {

    final java.util.Calendar currTime = java.util.Calendar.getInstance ();

    final int currHour = currTime.get (java.util.Calendar.HOUR);

    // this test won't work out well in the early hours, where 'backing up'
    // would give previous day,
    // or where daylight savings transitions could occur and confuse the
    // assertions...
    if (currHour < 3)
      return;

    final Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler ();
    JobDetail job = newJob (MyJob.class).build ();
    Trigger trigger = newTrigger ().withIdentity ("test")
                                   .withSchedule (dailyTimeIntervalSchedule ().startingDailyAt (TimeOfDay.hourAndMinuteOfDay (2,
                                                                                                                              15))
                                                                              .withIntervalInMinutes (5))
                                   .startAt (currTime.getTime ())
                                   .build ();
    scheduler.scheduleJob (job, trigger);

    trigger = scheduler.getTrigger (trigger.getKey ());

    System.out.println ("testScheduleInMiddleOfDailyInterval: currTime = " + currTime.getTime ());
    System.out.println ("testScheduleInMiddleOfDailyInterval: computed first fire time = " +
                        trigger.getNextFireTime ());

    assertTrue ("First fire time is not after now!", trigger.getNextFireTime ().after (currTime.getTime ()));

    final Date startTime = DateBuilder.todayAt (2, 15, 0);

    job = newJob (MyJob.class).build ();
    trigger = newTrigger ().withIdentity ("test2")
                           .withSchedule (dailyTimeIntervalSchedule ().startingDailyAt (TimeOfDay.hourAndMinuteOfDay (2,
                                                                                                                      15))
                                                                      .withIntervalInMinutes (5))
                           .startAt (startTime)
                           .build ();
    scheduler.scheduleJob (job, trigger);

    trigger = scheduler.getTrigger (trigger.getKey ());

    System.out.println ("testScheduleInMiddleOfDailyInterval: startTime = " + startTime);
    System.out.println ("testScheduleInMiddleOfDailyInterval: computed first fire time = " +
                        trigger.getNextFireTime ());

    assertTrue ("First fire time is not after now!", trigger.getNextFireTime ().equals (startTime));

    scheduler.shutdown ();
  }

  @Test
  public void testHourlyTrigger ()
  {
    final DailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                          .withSchedule (dailyTimeIntervalSchedule ().withIntervalInHours (1))
                                                          .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.HOUR, trigger.getRepeatIntervalUnit ());
    assertEquals (1, trigger.getRepeatInterval ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((OperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
  }

  @Test
  public void testMinutelyTriggerWithTimeOfDay ()
  {
    final DailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test", "group")
                                                          .withSchedule (dailyTimeIntervalSchedule ().withIntervalInMinutes (72)
                                                                                                     .startingDailyAt (TimeOfDay.hourAndMinuteOfDay (8,
                                                                                                                                                     0))
                                                                                                     .endingDailyAt (TimeOfDay.hourAndMinuteOfDay (17,
                                                                                                                                                   0))
                                                                                                     .onMondayThroughFriday ())
                                                          .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("group", trigger.getKey ().getGroup ());
    assertEquals (true, new Date ().getTime () >= trigger.getStartTime ().getTime ());
    assertEquals (true, null == trigger.getEndTime ());
    assertEquals (IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit ());
    assertEquals (72, trigger.getRepeatInterval ());
    assertEquals (new TimeOfDay (8, 0), trigger.getStartTimeOfDay ());
    assertEquals (new TimeOfDay (17, 0), trigger.getEndTimeOfDay ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((OperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
  }

  @Test
  public void testSecondlyTriggerWithStartAndEndTime ()
  {
    final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
    final Date endTime = DateBuilder.dateOf (0, 0, 0, 2, 1, 2011);
    final DailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test", "test")
                                                          .withSchedule (dailyTimeIntervalSchedule ().withIntervalInSeconds (121)
                                                                                                     .startingDailyAt (hourMinuteAndSecondOfDay (10,
                                                                                                                                                 0,
                                                                                                                                                 0))
                                                                                                     .endingDailyAt (hourMinuteAndSecondOfDay (23,
                                                                                                                                               59,
                                                                                                                                               59))
                                                                                                     .onSaturdayAndSunday ())
                                                          .startAt (startTime)
                                                          .endAt (endTime)
                                                          .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("test", trigger.getKey ().getGroup ());
    assertEquals (true, startTime.getTime () == trigger.getStartTime ().getTime ());
    assertEquals (true, endTime.getTime () == trigger.getEndTime ().getTime ());
    assertEquals (IntervalUnit.SECOND, trigger.getRepeatIntervalUnit ());
    assertEquals (121, trigger.getRepeatInterval ());
    assertEquals (new TimeOfDay (10, 0, 0), trigger.getStartTimeOfDay ());
    assertEquals (new TimeOfDay (23, 59, 59), trigger.getEndTimeOfDay ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((OperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
  }

  @Test
  public void testRepeatCountTrigger ()
  {
    final DailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                          .withSchedule (dailyTimeIntervalSchedule ().withIntervalInHours (1)
                                                                                                     .withRepeatCount (9))
                                                          .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.HOUR, trigger.getRepeatIntervalUnit ());
    assertEquals (1, trigger.getRepeatInterval ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((OperableTrigger) trigger, null, 48);
    assertEquals (10, fireTimes.size ());
  }

  @Test
  public void testEndingAtAfterCount ()
  {
    final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
    final DailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                          .withSchedule (dailyTimeIntervalSchedule ().withIntervalInMinutes (15)
                                                                                                     .startingDailyAt (TimeOfDay.hourAndMinuteOfDay (8,
                                                                                                                                                     0))
                                                                                                     .endingDailyAfterCount (12))
                                                          .startAt (startTime)
                                                          .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((OperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, 1, 2011), fireTimes.get (0));
    assertEquals (dateOf (10, 45, 0, 4, 1, 2011), fireTimes.get (47));
    assertEquals (new TimeOfDay (10, 45), trigger.getEndTimeOfDay ());
  }

  @Test
  public void testEndingAtAfterCountOf1 ()
  {
    final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
    final DailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                          .withSchedule (dailyTimeIntervalSchedule ().withIntervalInMinutes (15)
                                                                                                     .startingDailyAt (TimeOfDay.hourAndMinuteOfDay (8,
                                                                                                                                                     0))
                                                                                                     .endingDailyAfterCount (1))
                                                          .startAt (startTime)
                                                          .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((OperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, 1, 2011), fireTimes.get (0));
    assertEquals (dateOf (8, 0, 0, 17, 2, 2011), fireTimes.get (47));
    assertEquals (new TimeOfDay (8, 0), trigger.getEndTimeOfDay ());
  }

  @Test
  public void testEndingAtAfterCountOf0 ()
  {
    try
    {
      final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
      newTrigger ().withIdentity ("test")
                   .withSchedule (dailyTimeIntervalSchedule ().withIntervalInMinutes (15)
                                                              .startingDailyAt (TimeOfDay.hourAndMinuteOfDay (8, 0))
                                                              .endingDailyAfterCount (0))
                   .startAt (startTime)
                   .build ();
      fail ("We should not accept endingDailyAfterCount(0)");
    }
    catch (final IllegalArgumentException e)
    {
      // Expected.
    }

    try
    {
      final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
      newTrigger ().withIdentity ("test")
                   .withSchedule (dailyTimeIntervalSchedule ().withIntervalInMinutes (15).endingDailyAfterCount (1))
                   .startAt (startTime)
                   .build ();
      fail ("We should not accept endingDailyAfterCount(x) without first setting startingDailyAt.");
    }
    catch (final IllegalArgumentException e)
    {
      // Expected.
    }
  }

  /** An empty job for testing purpose. */
  public static class MyJob implements Job
  {
    public void execute (final JobExecutionContext context) throws JobExecutionException
    {
      //
    }
  }
}
