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

import static com.helger.quartz.DateBuilder.MILLISECONDS_IN_DAY;
import static com.helger.quartz.DateBuilder.dateOf;
import static com.helger.quartz.DateBuilder.evenHourDate;
import static com.helger.quartz.DateBuilder.evenHourDateAfterNow;
import static com.helger.quartz.DateBuilder.evenHourDateBefore;
import static com.helger.quartz.DateBuilder.evenMinuteDate;
import static com.helger.quartz.DateBuilder.evenMinuteDateBefore;
import static com.helger.quartz.DateBuilder.evenSecondDate;
import static com.helger.quartz.DateBuilder.evenSecondDateBefore;
import static com.helger.quartz.DateBuilder.newDate;
import static com.helger.quartz.DateBuilder.newDateInLocale;
import static com.helger.quartz.DateBuilder.newDateInTimeZoneAndLocale;
import static com.helger.quartz.DateBuilder.newDateInTimezone;
import static com.helger.quartz.DateBuilder.nextGivenMinuteDate;
import static com.helger.quartz.DateBuilder.todayAt;
import static com.helger.quartz.DateBuilder.tomorrowAt;
import static com.helger.quartz.DateBuilder.translateTime;
import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.DECEMBER;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.Month.MAY;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;

/**
 * Unit test for JobDetail.
 */
public class DateBuilderTest
{
  @Test
  public void testBasicBuilding ()
  {
    final Date t = dateOf (10, 30, 0, 1, JULY, 2013); // july 1 10:30:00 am

    final Calendar vc = PDTFactory.createCalendar ();
    vc.set (Calendar.YEAR, 2013);
    vc.set (Calendar.MONTH, Calendar.JULY);
    vc.set (Calendar.DAY_OF_MONTH, 1);
    vc.set (Calendar.HOUR_OF_DAY, 10);
    vc.set (Calendar.MINUTE, 30);
    vc.set (Calendar.SECOND, 0);
    vc.set (Calendar.MILLISECOND, 0);

    final Date v = vc.getTime ();

    assertEquals ("DateBuilder-produced date is not as expected.", t, v);
  }

  @Test
  public void testBuilder ()
  {

    Calendar vc = PDTFactory.createCalendar ();
    vc.set (Calendar.YEAR, 2013);
    vc.set (Calendar.MONTH, Calendar.JULY);
    vc.set (Calendar.DAY_OF_MONTH, 1);
    vc.set (Calendar.HOUR_OF_DAY, 10);
    vc.set (Calendar.MINUTE, 30);
    vc.set (Calendar.SECOND, 0);
    vc.set (Calendar.MILLISECOND, 0);

    Date bd = newDate ().inYear (2013).inMonth (JULY).onDay (1).atHourOfDay (10).atMinute (30).atSecond (0).build ();
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    bd = newDate ().inYear (2013).inMonthOnDay (JULY, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    final TimeZone tz = TimeZone.getTimeZone ("GMT-4:00");
    final Locale lz = Locale.TAIWAN;
    vc = Calendar.getInstance (tz, lz);
    vc.set (Calendar.YEAR, 2013);
    vc.set (Calendar.MONTH, Calendar.JUNE);
    vc.set (Calendar.DAY_OF_MONTH, 1);
    vc.set (Calendar.HOUR_OF_DAY, 10);
    vc.set (Calendar.MINUTE, 33);
    vc.set (Calendar.SECOND, 12);
    vc.set (Calendar.MILLISECOND, 0);

    bd = newDate ().inYear (2013)
                   .inMonth (JUNE)
                   .onDay (1)
                   .atHourOfDay (10)
                   .atMinute (33)
                   .atSecond (12)
                   .inTimeZone (tz)
                   .inLocale (lz)
                   .build ();
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    bd = newDateInLocale (lz).inYear (2013)
                             .inMonth (JUNE)
                             .onDay (1)
                             .atHourOfDay (10)
                             .atMinute (33)
                             .atSecond (12)
                             .inTimeZone (tz)
                             .build ();
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    bd = newDateInTimezone (tz).inYear (2013)
                               .inMonth (JUNE)
                               .onDay (1)
                               .atHourOfDay (10)
                               .atMinute (33)
                               .atSecond (12)
                               .inLocale (lz)
                               .build ();
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    bd = newDateInTimeZoneAndLocale (tz, lz).inYear (2013)
                                            .inMonth (JUNE)
                                            .onDay (1)
                                            .atHourOfDay (10)
                                            .atMinute (33)
                                            .atSecond (12)
                                            .build ();
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

  }

  @Test
  public void testEvensBuilders ()
  {

    final Calendar vc = PDTFactory.createCalendar ();
    vc.set (Calendar.YEAR, 2013);
    vc.set (Calendar.MONTH, Calendar.JUNE);
    vc.set (Calendar.DAY_OF_MONTH, 1);
    vc.set (Calendar.HOUR_OF_DAY, 10);
    vc.set (Calendar.MINUTE, 33);
    vc.set (Calendar.SECOND, 12);
    vc.set (Calendar.MILLISECOND, 0);

    final Calendar rd = QCloneUtils.getClone (vc);

    Date bd = newDate ().inYear (2013).inMonth (JUNE).onDay (1).atHourOfDay (10).atMinute (33).atSecond (12).build ();
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    rd.set (Calendar.MILLISECOND, 13);
    bd = evenSecondDateBefore (rd.getTime ());
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    vc.set (Calendar.SECOND, 13);
    rd.set (Calendar.MILLISECOND, 13);
    bd = evenSecondDate (rd.getTime ());
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    vc.set (Calendar.SECOND, 0);
    vc.set (Calendar.MINUTE, 34);
    rd.set (Calendar.SECOND, 13);
    bd = evenMinuteDate (rd.getTime ());
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    vc.set (Calendar.SECOND, 0);
    vc.set (Calendar.MINUTE, 33);
    rd.set (Calendar.SECOND, 13);
    bd = evenMinuteDateBefore (rd.getTime ());
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    vc.set (Calendar.SECOND, 0);
    vc.set (Calendar.MINUTE, 0);
    vc.set (Calendar.HOUR_OF_DAY, 11);
    rd.set (Calendar.SECOND, 13);
    bd = evenHourDate (rd.getTime ());
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    vc.set (Calendar.SECOND, 0);
    vc.set (Calendar.MINUTE, 0);
    vc.set (Calendar.HOUR_OF_DAY, 10);
    rd.set (Calendar.SECOND, 13);
    bd = evenHourDateBefore (rd.getTime ());
    assertEquals ("DateBuilder-produced date is not as expected.", vc.getTime (), bd);

    final Date td = new Date ();
    bd = evenHourDateAfterNow ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MINUTE));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.SECOND));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MILLISECOND));
    assertTrue ("DateBuilder-produced date is not as expected.", bd.after (td));

    vc.set (Calendar.SECOND, 54);
    vc.set (Calendar.MINUTE, 13);
    vc.set (Calendar.HOUR_OF_DAY, 8);
    bd = nextGivenMinuteDate (vc.getTime (), 15);
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", 8, vc.get (Calendar.HOUR_OF_DAY));
    assertEquals ("DateBuilder-produced date is not as expected.", 15, vc.get (Calendar.MINUTE));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.SECOND));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MILLISECOND));
  }

  @Test
  public void testGivenBuilders ()
  {

    final Calendar vc = PDTFactory.createCalendar ();

    vc.set (Calendar.SECOND, 54);
    vc.set (Calendar.MINUTE, 13);
    vc.set (Calendar.HOUR_OF_DAY, 8);
    Date bd = nextGivenMinuteDate (vc.getTime (), 45);
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", 8, vc.get (Calendar.HOUR_OF_DAY));
    assertEquals ("DateBuilder-produced date is not as expected.", 45, vc.get (Calendar.MINUTE));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.SECOND));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MILLISECOND));

    vc.set (Calendar.SECOND, 54);
    vc.set (Calendar.MINUTE, 46);
    vc.set (Calendar.HOUR_OF_DAY, 8);
    bd = nextGivenMinuteDate (vc.getTime (), 45);
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", 9, vc.get (Calendar.HOUR_OF_DAY));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MINUTE));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.SECOND));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MILLISECOND));
  }

  @Test
  public void testAtBuilders ()
  {

    final Calendar rd = PDTFactory.createCalendar ();
    final Calendar vc = PDTFactory.createCalendar ();

    rd.setTime (new Date ());
    Date bd = todayAt (10, 33, 12);
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", 10, vc.get (Calendar.HOUR_OF_DAY));
    assertEquals ("DateBuilder-produced date is not as expected.", 33, vc.get (Calendar.MINUTE));
    assertEquals ("DateBuilder-produced date is not as expected.", 12, vc.get (Calendar.SECOND));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MILLISECOND));
    assertEquals ("DateBuilder-produced date is not as expected.",
                  rd.get (Calendar.DAY_OF_YEAR),
                  vc.get (Calendar.DAY_OF_YEAR));

    rd.setTime (new Date ());
    rd.add (Calendar.MILLISECOND, (int) MILLISECONDS_IN_DAY); // increment the
                                                              // day (using this
                                                              // means on
                                                              // purpose - to
                                                              // test const)
    bd = tomorrowAt (10, 33, 12);
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", 10, vc.get (Calendar.HOUR_OF_DAY));
    assertEquals ("DateBuilder-produced date is not as expected.", 33, vc.get (Calendar.MINUTE));
    assertEquals ("DateBuilder-produced date is not as expected.", 12, vc.get (Calendar.SECOND));
    assertEquals ("DateBuilder-produced date is not as expected.", 0, vc.get (Calendar.MILLISECOND));
    assertEquals ("DateBuilder-produced date is not as expected.",
                  rd.get (Calendar.DAY_OF_YEAR),
                  vc.get (Calendar.DAY_OF_YEAR));
  }

  @Test
  public void testTranslate ()
  {

    final TimeZone tz1 = TimeZone.getTimeZone ("GMT-2:00");
    final TimeZone tz2 = TimeZone.getTimeZone ("GMT-4:00");

    Calendar vc = Calendar.getInstance (tz1, Locale.getDefault (Locale.Category.FORMAT));
    vc.set (Calendar.YEAR, 2013);
    vc.set (Calendar.MONTH, Calendar.JUNE);
    vc.set (Calendar.DAY_OF_MONTH, 1);
    vc.set (Calendar.HOUR_OF_DAY, 10);
    vc.set (Calendar.MINUTE, 33);
    vc.set (Calendar.SECOND, 12);
    vc.set (Calendar.MILLISECOND, 0);

    vc.setTime (translateTime (vc.getTime (), tz1, tz2));
    assertEquals ("DateBuilder-produced date is not as expected.", 12, vc.get (Calendar.HOUR_OF_DAY));

    vc = Calendar.getInstance (tz2, Locale.getDefault (Locale.Category.FORMAT));
    vc.set (Calendar.YEAR, 2013);
    vc.set (Calendar.MONTH, Calendar.JUNE);
    vc.set (Calendar.DAY_OF_MONTH, 1);
    vc.set (Calendar.HOUR_OF_DAY, 10);
    vc.set (Calendar.MINUTE, 33);
    vc.set (Calendar.SECOND, 12);
    vc.set (Calendar.MILLISECOND, 0);

    vc.setTime (translateTime (vc.getTime (), tz2, tz1));
    assertEquals ("DateBuilder-produced date is not as expected.", 8, vc.get (Calendar.HOUR_OF_DAY));
  }

  @Test
  public void testMonthTranslations ()
  {

    final Calendar vc = PDTFactory.createCalendar ();

    Date bd = newDate ().inYear (2013).inMonthOnDay (JANUARY, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.JANUARY, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (FEBRUARY, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.FEBRUARY, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (MARCH, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.MARCH, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (APRIL, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.APRIL, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (MAY, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.MAY, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (JUNE, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.JUNE, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (JULY, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.JULY, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (AUGUST, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.AUGUST, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (SEPTEMBER, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.SEPTEMBER, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (OCTOBER, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.OCTOBER, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (NOVEMBER, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.NOVEMBER, vc.get (Calendar.MONTH));

    bd = newDate ().inYear (2013).inMonthOnDay (DECEMBER, 1).atHourMinuteAndSecond (10, 30, 0).build ();
    vc.setTime (bd);
    assertEquals ("DateBuilder-produced date is not as expected.", Calendar.DECEMBER, vc.get (Calendar.MONTH));

  }

}
