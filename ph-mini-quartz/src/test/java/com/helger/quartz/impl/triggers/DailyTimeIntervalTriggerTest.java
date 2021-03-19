/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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
package com.helger.quartz.impl.triggers;

import static com.helger.quartz.DateBuilder.dateOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;

import org.junit.Test;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.quartz.EIntervalUnit;
import com.helger.quartz.IDailyTimeIntervalTrigger;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.TriggerUtils;
import com.helger.quartz.impl.calendar.CronCalendar;

/**
 * Unit test for {@link DailyTimeIntervalTrigger}.
 *
 * @author Zemian Deng saltnlight5@gmail.com
 */
public class DailyTimeIntervalTriggerTest
{
  @Test
  public void testNormalExample () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (11, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (72); // this interval will give three firings per
                                    // day (8:00, 9:12, and 10:24)

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (10, 24, 0, 16, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testQuartzCalendarExclusion () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (PDTFactory.createLocalTime (8, 0, 0));
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    // exclude
    // 9-12
    final CronCalendar cronCal = new CronCalendar ("* * 9-12 * * ?");
    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, cronCal, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (13, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (1));
    assertEquals (dateOf (23, 0, 0, 4, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testValidateTimeOfDayOrder () throws Exception
  {
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTimeOfDay (PDTFactory.createLocalTime (12, 0, 0));
    trigger.setEndTimeOfDay (PDTFactory.createLocalTime (8, 0, 0));
    try
    {
      trigger.validate ();
      fail ("Trigger should be invalidate when time of day is not in order.");
    }
    catch (final SchedulerException e)
    {
      // expected.
    }
  }

  @Test
  public void testValidateInterval () throws Exception
  {
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setName ("test");
    trigger.setGroup ("test");
    trigger.setJobKey (JobKey.jobKey ("test"));

    trigger.setRepeatIntervalUnit (EIntervalUnit.HOUR);
    trigger.setRepeatInterval (25);
    try
    {
      trigger.validate ();
      fail ("Trigger should be invalidate when interval is greater than 24 hours.");
    }
    catch (final SchedulerException e)
    {
      // expected.
    }

    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60 * 25);
    try
    {
      trigger.validate ();
      fail ("Trigger should be invalidate when interval is greater than 24 hours.");
    }
    catch (final SchedulerException e)
    {
      // expected.
    }

    trigger.setRepeatIntervalUnit (EIntervalUnit.SECOND);
    trigger.setRepeatInterval (60 * 60 * 25);
    try
    {
      trigger.validate ();
      fail ("Trigger should be invalidate when interval is greater than 24 hours.");
    }
    catch (final SchedulerException e)
    {
      // expected.
    }

    try
    {
      trigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
      trigger.validate ();
      fail ("Trigger should be invalidate when interval unit > HOUR.");
    }
    catch (final Exception e)
    {
      // expected.
    }

    try
    {
      trigger.setRepeatIntervalUnit (EIntervalUnit.SECOND);
      trigger.setRepeatInterval (0);
      trigger.validate ();
      fail ("Trigger should be invalidate when interval is zero.");
    }
    catch (final Exception e)
    {
      // expected.
    }
  }

  @Test
  public void testStartTimeWithoutStartTimeOfDay () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (0, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (23, 0, 0, 2, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testEndTimeWithoutEndTimeOfDay () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final Date endTime = dateOf (22, 0, 0, 2, Month.JANUARY, 2011);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setEndTime (endTime);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (47, fireTimes.size ());
    assertEquals (dateOf (0, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (22, 0, 0, 2, Month.JANUARY, 2011), fireTimes.get (46));
  }

  @Test
  public void testStartTimeBeforeStartTimeOfDay () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (23, 0, 0, 3, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testStartTimeBeforeStartTimeOfDayOnInvalidDay () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011); // Jan 1,
                                                                     // 2011 was
                                                                     // a
    // saturday...
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    final EnumSet <DayOfWeek> daysOfWeek = EnumSet.of (DayOfWeek.MONDAY,
                                                       DayOfWeek.TUESDAY,
                                                       DayOfWeek.WEDNESDAY,
                                                       DayOfWeek.THURSDAY,
                                                       DayOfWeek.FRIDAY);
    trigger.setDaysOfWeek (daysOfWeek);
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    assertEquals (dateOf (8, 0, 0, 3, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (6, 0, 0, 22, Month.MAY, 2010)));

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 3, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (23, 0, 0, 5, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testStartTimeAfterStartTimeOfDay () throws Exception
  {
    final Date startTime = dateOf (9, 23, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (10, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (9, 0, 0, 4, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testEndTimeBeforeEndTimeOfDay () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final Date endTime = dateOf (16, 0, 0, 2, Month.JANUARY, 2011);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setEndTime (endTime);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (35, fireTimes.size ());
    assertEquals (dateOf (0, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (17, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (17));
    assertEquals (dateOf (16, 0, 0, 2, Month.JANUARY, 2011), fireTimes.get (34));
  }

  @Test
  public void testEndTimeAfterEndTimeOfDay () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final Date endTime = dateOf (18, 0, 0, 2, Month.JANUARY, 2011);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setEndTime (endTime);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (36, fireTimes.size ());
    assertEquals (dateOf (0, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (17, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (17));
    assertEquals (dateOf (17, 0, 0, 2, Month.JANUARY, 2011), fireTimes.get (35));
  }

  @Test
  public void testTimeOfDayWithStartTime () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (17, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (9)); // The
    // 10th
    // hours
    // is
    // the
    // end
    // of
    // day.
    assertEquals (dateOf (15, 0, 0, 5, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testTimeOfDayWithEndTime () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final Date endTime = dateOf (0, 0, 0, 4, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setEndTime (endTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (30, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (17, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (9)); // The
    // 10th
    // hours
    // is
    // the
    // end
    // of
    // day.
    assertEquals (dateOf (17, 0, 0, 3, Month.JANUARY, 2011), fireTimes.get (29));
  }

  @Test
  public void testTimeOfDayWithEndTime2 () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 23, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (23, 59, 59); // edge
                                                                            // case
                                                                            // when
    // endTime is
    // last second of
    // day, which is
    // default too.
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 23, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (23, 23, 0, 3, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testAllDaysOfTheWeek () throws Exception
  {
    final EnumSet <DayOfWeek> daysOfWeek = EnumSet.allOf (DayOfWeek.class);
    // SAT
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setDaysOfWeek (daysOfWeek);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (17, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (9));
    // The 10th hours is the end of day.
    assertEquals (dateOf (15, 0, 0, 5, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testMonThroughFri () throws Exception
  {
    final EnumSet <DayOfWeek> daysOfWeek = EnumSet.of (DayOfWeek.MONDAY,
                                                       DayOfWeek.TUESDAY,
                                                       DayOfWeek.WEDNESDAY,
                                                       DayOfWeek.THURSDAY,
                                                       DayOfWeek.FRIDAY);
    // SAT(7)
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setDaysOfWeek (daysOfWeek);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 3, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (Calendar.MONDAY, _getDayOfWeek (fireTimes.get (0)));
    assertEquals (dateOf (8, 0, 0, 4, Month.JANUARY, 2011), fireTimes.get (10));
    assertEquals (Calendar.TUESDAY, _getDayOfWeek (fireTimes.get (10)));
    assertEquals (dateOf (15, 0, 0, 7, Month.JANUARY, 2011), fireTimes.get (47));
    assertEquals (Calendar.FRIDAY, _getDayOfWeek (fireTimes.get (47)));
  }

  @Test
  public void testSatAndSun () throws Exception
  {
    final EnumSet <DayOfWeek> daysOfWeek = EnumSet.of (DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    // SAT
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setDaysOfWeek (daysOfWeek);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (Calendar.SATURDAY, _getDayOfWeek (fireTimes.get (0)));
    assertEquals (dateOf (8, 0, 0, 2, Month.JANUARY, 2011), fireTimes.get (10));
    assertEquals (Calendar.SUNDAY, _getDayOfWeek (fireTimes.get (10)));
    assertEquals (dateOf (15, 0, 0, 15, Month.JANUARY, 2011), fireTimes.get (47));
    assertEquals (Calendar.SATURDAY, _getDayOfWeek (fireTimes.get (47)));
  }

  @Test
  public void testMonOnly () throws Exception
  {
    final EnumSet <DayOfWeek> daysOfWeek = EnumSet.of (DayOfWeek.MONDAY);
    // SAT(7)
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (17, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setDaysOfWeek (daysOfWeek);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (60);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 3, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (Calendar.MONDAY, _getDayOfWeek (fireTimes.get (0)));
    assertEquals (dateOf (8, 0, 0, 10, Month.JANUARY, 2011), fireTimes.get (10));
    assertEquals (Calendar.MONDAY, _getDayOfWeek (fireTimes.get (10)));
    assertEquals (dateOf (15, 0, 0, 31, Month.JANUARY, 2011), fireTimes.get (47));
    assertEquals (Calendar.MONDAY, _getDayOfWeek (fireTimes.get (47)));
  }

  private static int _getDayOfWeek (final Date dateTime)
  {
    final Calendar cal = PDTFactory.createCalendar ();
    cal.setTime (dateTime);
    return cal.get (Calendar.DAY_OF_WEEK);
  }

  @Test
  public void testTimeOfDayWithEndTimeOddInterval () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final Date endTime = dateOf (0, 0, 0, 4, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (10, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setEndTime (endTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (23);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (18, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (9, 55, 0, 1, Month.JANUARY, 2011), fireTimes.get (5));
    assertEquals (dateOf (9, 55, 0, 3, Month.JANUARY, 2011), fireTimes.get (17));
  }

  @Test
  public void testHourInterval () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final Date endTime = dateOf (13, 0, 0, 15, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 1, 15);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (16, 1, 15);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTime (endTime);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.HOUR);
    trigger.setRepeatInterval (2);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 1, 15, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (12, 1, 15, 10, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testSecondInterval () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 2);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (13, 30, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.SECOND);
    trigger.setRepeatInterval (72);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 2, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (8, 56, 26, 1, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testRepeatCountInf () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (11, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (72);

    // Setting this (which is default) should make the trigger just as normal
    // one.
    trigger.setRepeatCount (IDailyTimeIntervalTrigger.REPEAT_INDEFINITELY);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (48, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (10, 24, 0, 16, Month.JANUARY, 2011), fireTimes.get (47));
  }

  @Test
  public void testRepeatCount () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (11, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (72);
    trigger.setRepeatCount (7);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (8, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
    assertEquals (dateOf (9, 12, 0, 3, Month.JANUARY, 2011), fireTimes.get (7));
  }

  @Test
  public void testRepeatCount0 () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (11, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    trigger.setRepeatInterval (72);
    trigger.setRepeatCount (0);

    final ICommonsList <Date> fireTimes = TriggerUtils.computeFireTimes (trigger, null, 48);
    assertEquals (1, fireTimes.size ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011), fireTimes.get (0));
  }

  @Test
  public void testGetFireTime () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2011);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (13, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.HOUR);
    trigger.setRepeatInterval (1);

    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (0, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (7, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (7, 59, 59, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (9, 0, 0, 1, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (8, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (10, 0, 0, 1, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (9, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (13, 0, 0, 1, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (12, 59, 59, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 2, Month.JANUARY, 2011),
                  trigger.getFireTimeAfter (dateOf (13, 0, 0, 1, Month.JANUARY, 2011)));
  }

  @Test
  public void testGetFireTimeWithDateBeforeStartTime () throws Exception
  {
    final Date startTime = dateOf (0, 0, 0, 1, Month.JANUARY, 2012);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (13, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.HOUR);
    trigger.setRepeatInterval (1);

    // NOTE that if you pass a date past the startTime, you will get the first
    // firing on or after the startTime back!
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (0, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (7, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (7, 59, 59, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (8, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (9, 0, 0, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (12, 59, 59, 1, Month.JANUARY, 2011)));
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (13, 0, 0, 1, Month.JANUARY, 2011)));

    // Now try some test times at or after startTime
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (0, 0, 0, 1, Month.JANUARY, 2012)));
    assertEquals (dateOf (8, 0, 0, 2, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (13, 0, 0, 1, Month.JANUARY, 2012)));
  }

  @Test
  public void testGetFireTimeWhenStartTimeAndTimeOfDayIsSame () throws Exception
  {
    // A test case for QTZ-369
    final Date startTime = dateOf (8, 0, 0, 1, Month.JANUARY, 2012);
    final LocalTime startTimeOfDay = PDTFactory.createLocalTime (8, 0, 0);
    final LocalTime endTimeOfDay = PDTFactory.createLocalTime (13, 0, 0);
    final DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ();
    trigger.setStartTime (startTime);
    trigger.setStartTimeOfDay (startTimeOfDay);
    trigger.setEndTimeOfDay (endTimeOfDay);
    trigger.setRepeatIntervalUnit (EIntervalUnit.HOUR);
    trigger.setRepeatInterval (1);

    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                  trigger.getFireTimeAfter (dateOf (0, 0, 0, 1, Month.JANUARY, 2012)));
  }

  @Test
  public void testExtraConstructors () throws Exception
  {
    // A test case for QTZ-389 - some extra constructors didn't set all
    // parameters
    DailyTimeIntervalTrigger trigger = new DailyTimeIntervalTrigger ("triggerName",
                                                                     "triggerGroup",
                                                                     "jobName",
                                                                     "jobGroup",
                                                                     dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                                                                     null,
                                                                     PDTFactory.createLocalTime (8, 0, 0),
                                                                     PDTFactory.createLocalTime (17, 0, 0),
                                                                     EIntervalUnit.HOUR,
                                                                     1);

    assertEquals ("triggerName", trigger.getName ());
    assertEquals ("triggerGroup", trigger.getGroup ());
    assertEquals ("jobName", trigger.getJobName ());
    assertEquals ("jobGroup", trigger.getJobGroup ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012), trigger.getStartTime ());
    assertEquals (null, trigger.getEndTime ());
    assertEquals (PDTFactory.createLocalTime (8, 0, 0), trigger.getStartTimeOfDay ());
    assertEquals (PDTFactory.createLocalTime (17, 0, 0), trigger.getEndTimeOfDay ());
    assertEquals (EIntervalUnit.HOUR, trigger.getRepeatIntervalUnit ());
    assertEquals (1, trigger.getRepeatInterval ());

    trigger = new DailyTimeIntervalTrigger ("triggerName",
                                            "triggerGroup",
                                            dateOf (8, 0, 0, 1, Month.JANUARY, 2012),
                                            null,
                                            PDTFactory.createLocalTime (8, 0, 0),
                                            PDTFactory.createLocalTime (17, 0, 0),
                                            EIntervalUnit.HOUR,
                                            1);

    assertEquals ("triggerName", trigger.getName ());
    assertEquals ("triggerGroup", trigger.getGroup ());
    assertEquals (null, trigger.getJobName ());
    assertEquals ("DEFAULT", trigger.getJobGroup ());
    assertEquals (dateOf (8, 0, 0, 1, Month.JANUARY, 2012), trigger.getStartTime ());
    assertEquals (null, trigger.getEndTime ());
    assertEquals (PDTFactory.createLocalTime (8, 0, 0), trigger.getStartTimeOfDay ());
    assertEquals (PDTFactory.createLocalTime (17, 0, 0), trigger.getEndTimeOfDay ());
    assertEquals (EIntervalUnit.HOUR, trigger.getRepeatIntervalUnit ());
    assertEquals (1, trigger.getRepeatInterval ());
  }
}
