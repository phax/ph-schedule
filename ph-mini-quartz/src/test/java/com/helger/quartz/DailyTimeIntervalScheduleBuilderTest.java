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

import static com.helger.quartz.DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule;
import static com.helger.quartz.DateBuilder.dateOf;
import static com.helger.quartz.JobBuilder.newJob;
import static com.helger.quartz.TimeOfDay.hourMinuteAndSecondOfDay;
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.DateBuilder.IntervalUnit;
import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.spi.IOperableTrigger;

/**
 * Unit test for DailyTimeIntervalScheduleBuilder.
 *
 * @author Zemian Deng saltnlight5@gmail.com
 */
public class DailyTimeIntervalScheduleBuilderTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (DailyTimeIntervalScheduleBuilderTest.class);

  @Test
  public void testScheduleActualTrigger () throws Exception
  {
    final IScheduler scheduler = StdSchedulerFactory.getDefaultScheduler ();
    final IJobDetail job = newJob (MyJob.class).build ();
    final IDailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
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

    final Calendar currTime = Calendar.getInstance ();

    final int currHour = currTime.get (Calendar.HOUR);

    // this test won't work out well in the early hours, where 'backing up'
    // would give previous day,
    // or where daylight savings transitions could occur and confuse the
    // assertions...
    if (currHour < 3)
      return;

    final IScheduler scheduler = StdSchedulerFactory.getDefaultScheduler ();
    IJobDetail job = newJob (MyJob.class).build ();
    ITrigger trigger = newTrigger ().withIdentity ("test")
                                    .withSchedule (dailyTimeIntervalSchedule ().startingDailyAt (TimeOfDay.hourAndMinuteOfDay (2,
                                                                                                                               15))
                                                                               .withIntervalInMinutes (5))
                                    .startAt (currTime.getTime ())
                                    .build ();
    scheduler.scheduleJob (job, trigger);

    trigger = scheduler.getTrigger (trigger.getKey ());

    s_aLogger.info ("testScheduleInMiddleOfDailyInterval: currTime = " + currTime.getTime ());
    s_aLogger.info ("testScheduleInMiddleOfDailyInterval: computed first fire time = " + trigger.getNextFireTime ());

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

    s_aLogger.info ("testScheduleInMiddleOfDailyInterval: startTime = " + startTime);
    s_aLogger.info ("testScheduleInMiddleOfDailyInterval: computed first fire time = " + trigger.getNextFireTime ());

    assertTrue ("First fire time is not after now!", trigger.getNextFireTime ().equals (startTime));

    scheduler.shutdown ();
  }

  @Test
  public void testHourlyTrigger ()
  {
    final IDailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                           .withSchedule (dailyTimeIntervalSchedule ().withIntervalInHours (1))
                                                           .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.HOUR, trigger.getRepeatIntervalUnit ());
    assertEquals (1, trigger.getRepeatInterval ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((IOperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
  }

  @Test
  public void testMinutelyTriggerWithTimeOfDay ()
  {
    final IDailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test", "group")
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
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((IOperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
  }

  @Test
  public void testSecondlyTriggerWithStartAndEndTime ()
  {
    final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
    final Date endTime = DateBuilder.dateOf (0, 0, 0, 2, 1, 2011);
    final IDailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test", "test")
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
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((IOperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
  }

  @Test
  public void testRepeatCountTrigger ()
  {
    final IDailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                           .withSchedule (dailyTimeIntervalSchedule ().withIntervalInHours (1)
                                                                                                      .withRepeatCount (9))
                                                           .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.HOUR, trigger.getRepeatIntervalUnit ());
    assertEquals (1, trigger.getRepeatInterval ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((IOperableTrigger) trigger, null, 48);
    assertEquals (10, fireTimes.size ());
  }

  @Test
  public void testEndingAtAfterCount ()
  {
    final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
    final IDailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                           .withSchedule (dailyTimeIntervalSchedule ().withIntervalInMinutes (15)
                                                                                                      .startingDailyAt (TimeOfDay.hourAndMinuteOfDay (8,
                                                                                                                                                      0))
                                                                                                      .endingDailyAfterCount (12))
                                                           .startAt (startTime)
                                                           .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((IOperableTrigger) trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, 1, 2011), fireTimes.get (0));
    assertEquals (dateOf (10, 45, 0, 4, 1, 2011), fireTimes.get (47));
    assertEquals (new TimeOfDay (10, 45), trigger.getEndTimeOfDay ());
  }

  @Test
  public void testEndingAtAfterCountOf1 ()
  {
    final Date startTime = DateBuilder.dateOf (0, 0, 0, 1, 1, 2011);
    final IDailyTimeIntervalTrigger trigger = newTrigger ().withIdentity ("test")
                                                           .withSchedule (dailyTimeIntervalSchedule ().withIntervalInMinutes (15)
                                                                                                      .startingDailyAt (TimeOfDay.hourAndMinuteOfDay (8,
                                                                                                                                                      0))
                                                                                                      .endingDailyAfterCount (1))
                                                           .startAt (startTime)
                                                           .build ();
    assertEquals ("test", trigger.getKey ().getName ());
    assertEquals ("DEFAULT", trigger.getKey ().getGroup ());
    assertEquals (IntervalUnit.MINUTE, trigger.getRepeatIntervalUnit ());
    final List <Date> fireTimes = TriggerUtils.computeFireTimes ((IOperableTrigger) trigger, null, 48);
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
  public static class MyJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      //
    }
  }
}
