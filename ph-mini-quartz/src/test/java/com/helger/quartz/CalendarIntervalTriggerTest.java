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
package com.helger.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.quartz.ITrigger.EMisfireInstruction;
import com.helger.quartz.impl.calendar.AbstractCalendar;
import com.helger.quartz.impl.triggers.CalendarIntervalTrigger;

/**
 * Unit tests for DateIntervalTrigger.
 */
public class CalendarIntervalTriggerTest
{
  @Test
  public void testQTZ331FireTimeAfterBoundary ()
  {
    final Calendar start = PDTFactory.createCalendar ();
    start.clear ();
    start.set (2013, Calendar.FEBRUARY, 15);

    final Date startTime = start.getTime ();
    start.add (Calendar.DAY_OF_MONTH, 1);
    final Date triggerTime = start.getTime ();

    final CalendarIntervalTrigger trigger = new CalendarIntervalTrigger ("test", startTime, null, EIntervalUnit.DAY, 1);
    assertEquals (trigger.getFireTimeAfter (startTime), triggerTime);

    final Date after = new Date (start.getTimeInMillis () - 500);
    assertEquals (trigger.getFireTimeAfter (after), triggerTime);
  }

  private static final class MockCalendar extends AbstractCalendar <MockCalendar>
  {
    public MockCalendar (@Nullable final ICalendar aBaseCalendar, @Nullable final TimeZone aTimeZone)
    {
      super (aBaseCalendar, aTimeZone);
    }

    public MockCalendar getClone ()
    {
      return new MockCalendar (getBaseCalendar (), getTimeZone ());
    }
  }

  @Test
  public void testQTZ330DaylightSavingsCornerCase ()
  {
    final TimeZone edt = TimeZone.getTimeZone ("America/New_York");

    final Calendar start = PDTFactory.createCalendar ();
    start.clear ();
    start.setTimeZone (edt);
    start.set (2012, Calendar.MARCH, 16, 2, 30, 0);

    final Calendar after = PDTFactory.createCalendar ();
    after.clear ();
    after.setTimeZone (edt);
    after.set (2013, Calendar.APRIL, 19, 2, 30, 0);

    final MockCalendar baseCalendar = new MockCalendar (null, edt);

    final CalendarIntervalTrigger intervalTrigger = new CalendarIntervalTrigger ("QTZ-330",
                                                                                 start.getTime (),
                                                                                 null,
                                                                                 EIntervalUnit.DAY,
                                                                                 1);
    intervalTrigger.setTimeZone (edt);
    intervalTrigger.setPreserveHourOfDayAcrossDaylightSavings (true);
    intervalTrigger.computeFirstFireTime (baseCalendar);

    final Date fireTime = intervalTrigger.getFireTimeAfter (after.getTime ());
    assertTrue (fireTime.after (after.getTime ()));
  }

  @Test
  public void testYearlyIntervalGetFireTimeAfter ()
  {

    final Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    final CalendarIntervalTrigger yearlyTrigger = new CalendarIntervalTrigger ();
    yearlyTrigger.setStartTime (startCalendar.getTime ());
    yearlyTrigger.setRepeatIntervalUnit (EIntervalUnit.YEAR);
    yearlyTrigger.setRepeatInterval (2); // every two years;

    final Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.set (2009, Calendar.JUNE, 1, 9, 30, 17); // jump 4 years (2
                                                            // intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    final List <Date> fireTimes = TriggerUtils.computeFireTimes (yearlyTrigger, null, 4);
    final Date secondTime = fireTimes.get (2); // get the third fire time

    assertEquals ("Year increment result not as expected.", targetCalendar.getTime (), secondTime);
  }

  @Test
  public void testMonthlyIntervalGetFireTimeAfter ()
  {

    final Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    final CalendarIntervalTrigger yearlyTrigger = new CalendarIntervalTrigger ();
    yearlyTrigger.setStartTime (startCalendar.getTime ());
    yearlyTrigger.setRepeatIntervalUnit (EIntervalUnit.MONTH);
    yearlyTrigger.setRepeatInterval (5); // every five months

    final Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.MONTH, 25); // jump 25 five months (5
                                             // intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    final List <Date> fireTimes = TriggerUtils.computeFireTimes (yearlyTrigger, null, 6);
    final Date fifthTime = fireTimes.get (5); // get the sixth fire time

    assertEquals ("Month increment result not as expected.", targetCalendar.getTime (), fifthTime);
  }

  @Test
  public void testWeeklyIntervalGetFireTimeAfter ()
  {

    final Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    final CalendarIntervalTrigger yearlyTrigger = new CalendarIntervalTrigger ();
    yearlyTrigger.setStartTime (startCalendar.getTime ());
    yearlyTrigger.setRepeatIntervalUnit (EIntervalUnit.WEEK);
    yearlyTrigger.setRepeatInterval (6); // every six weeks

    final Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 7 * 6 * 4); // jump 24 weeks (4
                                                          // intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    final List <Date> fireTimes = TriggerUtils.computeFireTimes (yearlyTrigger, null, 7);
    final Date fifthTime = fireTimes.get (4); // get the fifth fire time

    assertEquals ("Week increment result not as expected.", targetCalendar.getTime (), fifthTime);
  }

  @Test
  public void testDailyIntervalGetFireTimeAfter ()
  {

    final Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    final CalendarIntervalTrigger dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (90); // every ninety days

    final Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 360); // jump 360 days (4
                                                    // intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    final List <Date> fireTimes = TriggerUtils.computeFireTimes (dailyTrigger, null, 6);
    final Date fifthTime = fireTimes.get (4); // get the fifth fire time

    assertEquals ("Day increment result not as expected.", targetCalendar.getTime (), fifthTime);
  }

  @Test
  public void testHourlyIntervalGetFireTimeAfter ()
  {

    final Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    final CalendarIntervalTrigger yearlyTrigger = new CalendarIntervalTrigger ();
    yearlyTrigger.setStartTime (startCalendar.getTime ());
    yearlyTrigger.setRepeatIntervalUnit (EIntervalUnit.HOUR);
    yearlyTrigger.setRepeatInterval (100); // every 100 hours

    final Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.HOUR, 400); // jump 400 hours (4 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    final List <Date> fireTimes = TriggerUtils.computeFireTimes (yearlyTrigger, null, 6);
    final Date fifthTime = fireTimes.get (4); // get the fifth fire time

    assertEquals ("Hour increment result not as expected.", targetCalendar.getTime (), fifthTime);
  }

  @Test
  public void testMinutelyIntervalGetFireTimeAfter ()
  {

    final Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    final CalendarIntervalTrigger yearlyTrigger = new CalendarIntervalTrigger ();
    yearlyTrigger.setStartTime (startCalendar.getTime ());
    yearlyTrigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    yearlyTrigger.setRepeatInterval (100); // every 100 minutes

    final Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.MINUTE, 400); // jump 400 minutes (4 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    final List <Date> fireTimes = TriggerUtils.computeFireTimes (yearlyTrigger, null, 6);
    final Date fifthTime = fireTimes.get (4); // get the fifth fire time

    assertEquals ("Minutes increment result not as expected.", targetCalendar.getTime (), fifthTime);
  }

  @Test
  public void testSecondlyIntervalGetFireTimeAfter ()
  {

    final Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    final CalendarIntervalTrigger yearlyTrigger = new CalendarIntervalTrigger ();
    yearlyTrigger.setStartTime (startCalendar.getTime ());
    yearlyTrigger.setRepeatIntervalUnit (EIntervalUnit.SECOND);
    yearlyTrigger.setRepeatInterval (100); // every 100 seconds

    final Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.set (2005, Calendar.JUNE, 1, 9, 30, 17);
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.SECOND, 400); // jump 400 seconds (4 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    final List <Date> fireTimes = TriggerUtils.computeFireTimes (yearlyTrigger, null, 6);
    final Date fifthTime = fireTimes.get (4); // get the third fire time

    assertEquals ("Seconds increment result not as expected.", targetCalendar.getTime (), fifthTime);
  }

  @Test
  public void testDaylightSavingsTransitions ()
  {

    // Pick a day before a spring daylight savings transition...

    Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2010, Calendar.MARCH, 12, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    CalendarIntervalTrigger dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (5); // every 5 days

    Calendar targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.setTime (startCalendar.getTime ());
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 10); // jump 10 days (2 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    List <Date> fireTimes = TriggerUtils.computeFireTimes (dailyTrigger, null, 6);
    Date testTime = fireTimes.get (2); // get the third fire time

    assertEquals ("Day increment result not as expected over spring 2010 daylight savings transition.",
                  targetCalendar.getTime (),
                  testTime);

    // And again, Pick a day before a spring daylight savings transition...
    // (QTZ-240)

    startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2011, Calendar.MARCH, 12, 1, 0, 0);
    startCalendar.clear (Calendar.MILLISECOND);

    dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (1); // every day

    targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.setTime (startCalendar.getTime ());
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 2); // jump 2 days (2 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    fireTimes = TriggerUtils.computeFireTimes (dailyTrigger, null, 6);
    testTime = fireTimes.get (2); // get the third fire time

    assertEquals ("Day increment result not as expected over spring 2011 daylight savings transition.",
                  targetCalendar.getTime (),
                  testTime);

    // And again, Pick a day before a spring daylight savings transition...
    // (QTZ-240) - and prove time of day is not preserved without
    // setPreserveHourOfDayAcrossDaylightSavings(true)

    startCalendar = PDTFactory.createCalendar ();
    startCalendar.setTimeZone (TimeZone.getTimeZone ("CET"));
    startCalendar.set (2011, Calendar.MARCH, 26, 4, 0, 0);
    startCalendar.clear (Calendar.MILLISECOND);

    dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (1); // every day
    dailyTrigger.setTimeZone (TimeZone.getTimeZone ("EST"));

    targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.setTimeZone (TimeZone.getTimeZone ("CET"));
    targetCalendar.setTime (startCalendar.getTime ());
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 2); // jump 2 days (2 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    fireTimes = TriggerUtils.computeFireTimes (dailyTrigger, null, 6);

    testTime = fireTimes.get (2); // get the third fire time

    Calendar testCal = Calendar.getInstance (TimeZone.getTimeZone ("CET"), Locale.getDefault (Locale.Category.FORMAT));
    testCal.setTimeInMillis (testTime.getTime ());

    assertFalse ("Day increment time-of-day result not as expected over spring 2011 daylight savings transition.",
                 targetCalendar.get (Calendar.HOUR_OF_DAY) == testCal.get (Calendar.HOUR_OF_DAY));

    // And again, Pick a day before a spring daylight savings transition...
    // (QTZ-240) - and prove time of day is preserved with
    // setPreserveHourOfDayAcrossDaylightSavings(true)

    startCalendar = PDTFactory.createCalendar ();
    startCalendar.setTimeZone (TimeZone.getTimeZone ("CET"));
    startCalendar.set (2011, Calendar.MARCH, 26, 4, 0, 0);
    startCalendar.clear (Calendar.MILLISECOND);

    dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (1); // every day
    dailyTrigger.setTimeZone (TimeZone.getTimeZone ("CET"));
    dailyTrigger.setPreserveHourOfDayAcrossDaylightSavings (true);

    targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.setTimeZone (TimeZone.getTimeZone ("CET"));
    targetCalendar.setTime (startCalendar.getTime ());
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 2); // jump 2 days (2 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    fireTimes = TriggerUtils.computeFireTimes (dailyTrigger, null, 6);

    testTime = fireTimes.get (2); // get the third fire time

    testCal = Calendar.getInstance (TimeZone.getTimeZone ("CET"), Locale.getDefault (Locale.Category.FORMAT));
    testCal.setTimeInMillis (testTime.getTime ());

    assertTrue ("Day increment time-of-day result not as expected over spring 2011 daylight savings transition.",
                targetCalendar.get (Calendar.HOUR_OF_DAY) == testCal.get (Calendar.HOUR_OF_DAY));

    // Pick a day before a fall daylight savings transition...

    startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2010, Calendar.OCTOBER, 31, 9, 30, 17);
    startCalendar.clear (Calendar.MILLISECOND);

    dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (5); // every 5 days

    targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.setTime (startCalendar.getTime ());
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 15); // jump 15 days (3 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    fireTimes = TriggerUtils.computeFireTimes (dailyTrigger, null, 6);
    testTime = fireTimes.get (3); // get the fourth fire time

    assertEquals ("Day increment result not as expected over fall 2010 daylight savings transition.",
                  targetCalendar.getTime (),
                  testTime);

    // And again, Pick a day before a fall daylight savings transition...
    // (QTZ-240)

    startCalendar = PDTFactory.createCalendar ();
    startCalendar.setTimeZone (TimeZone.getTimeZone ("CEST"));
    startCalendar.set (2011, Calendar.OCTOBER, 29, 1, 30, 00);
    startCalendar.clear (Calendar.MILLISECOND);

    dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (1); // every day
    dailyTrigger.setTimeZone (TimeZone.getTimeZone ("EST"));

    targetCalendar = PDTFactory.createCalendar ();
    targetCalendar.setTimeZone (TimeZone.getTimeZone ("CEST"));
    targetCalendar.setTime (startCalendar.getTime ());
    targetCalendar.setLenient (true);
    targetCalendar.add (Calendar.DAY_OF_YEAR, 3); // jump 3 days (3 intervals)
    targetCalendar.clear (Calendar.MILLISECOND);

    fireTimes = TriggerUtils.computeFireTimes (dailyTrigger, null, 6);
    testTime = fireTimes.get (3); // get the fourth fire time

    assertEquals ("Day increment result not as expected over fall 2011 daylight savings transition.",
                  targetCalendar.getTime (),
                  testTime);
  }

  @Test
  public void testFinalFireTimes ()
  {

    Calendar startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2010, Calendar.MARCH, 12, 9, 0, 0);
    startCalendar.clear (Calendar.MILLISECOND);

    CalendarIntervalTrigger dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.DAY);
    dailyTrigger.setRepeatInterval (5); // every 5 days

    Calendar endCalendar = PDTFactory.createCalendar ();
    endCalendar.setTime (startCalendar.getTime ());
    endCalendar.setLenient (true);
    endCalendar.add (Calendar.DAY_OF_YEAR, 10); // jump 10 days (2 intervals)
    endCalendar.clear (Calendar.MILLISECOND);
    dailyTrigger.setEndTime (endCalendar.getTime ());

    Date testTime = dailyTrigger.getFinalFireTime ();

    assertEquals ("Final fire time not computed correctly for day interval.", endCalendar.getTime (), testTime);

    startCalendar = PDTFactory.createCalendar ();
    startCalendar.set (2010, Calendar.MARCH, 12, 9, 0, 0);
    startCalendar.clear (Calendar.MILLISECOND);

    dailyTrigger = new CalendarIntervalTrigger ();
    dailyTrigger.setStartTime (startCalendar.getTime ());
    dailyTrigger.setRepeatIntervalUnit (EIntervalUnit.MINUTE);
    dailyTrigger.setRepeatInterval (5); // every 5 minutes

    endCalendar = PDTFactory.createCalendar ();
    endCalendar.setTime (startCalendar.getTime ());
    endCalendar.setLenient (true);
    endCalendar.add (Calendar.DAY_OF_YEAR, 15); // jump 15 days
    endCalendar.add (Calendar.MINUTE, -2); // back up two minutes
    endCalendar.clear (Calendar.MILLISECOND);
    dailyTrigger.setEndTime (endCalendar.getTime ());

    testTime = dailyTrigger.getFinalFireTime ();

    assertTrue ("Final fire time not computed correctly for minutely interval.",
                (endCalendar.getTime ().after (testTime)));

    endCalendar.add (Calendar.MINUTE, -3); // back up three more minutes

    assertTrue ("Final fire time not computed correctly for minutely interval.",
                (endCalendar.getTime ().equals (testTime)));
  }

  @Test
  public void testMisfireInstructionValidity ()
  {
    final CalendarIntervalTrigger trigger = new CalendarIntervalTrigger ();

    try
    {
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_DO_NOTHING);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }
    catch (final Exception e)
    {
      fail ("Unexpected exception while setting misfire instruction.");
    }

    try
    {
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);

      fail ("Expected exception while setting invalid misfire instruction but did not get it.");
    }
    catch (final Exception e)
    {}
  }

}
