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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.datetime.PDTFactory;

public final class CronExpressionTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CronExpressionTest.class);
  private static final TimeZone EST_TIME_ZONE = TimeZone.getTimeZone ("US/Eastern");

  @Test
  public void testIsSatisfiedBy () throws Exception
  {
    final CronExpression cronExpression = new CronExpression ("0 15 10 * * ? 2005");

    Calendar cal = PDTFactory.createCalendar ();

    cal.set (2005, Calendar.JUNE, 1, 10, 15, 0);
    assertTrue (cronExpression.isSatisfiedBy (cal.getTime ()));

    cal.set (Calendar.YEAR, 2006);
    assertFalse (cronExpression.isSatisfiedBy (cal.getTime ()));

    cal = PDTFactory.createCalendar ();
    cal.set (2005, Calendar.JUNE, 1, 10, 16, 0);
    assertFalse (cronExpression.isSatisfiedBy (cal.getTime ()));

    cal = PDTFactory.createCalendar ();
    cal.set (2005, Calendar.JUNE, 1, 10, 14, 0);
    assertFalse (cronExpression.isSatisfiedBy (cal.getTime ()));
  }

  @Test
  public void testLastDayOffset () throws Exception
  {
    CronExpression cronExpression = new CronExpression ("0 15 10 L-2 * ? 2010");

    final Calendar cal = PDTFactory.createCalendar ();

    cal.set (2010, Calendar.OCTOBER, 29, 10, 15, 0); // last day - 2
    assertTrue (cronExpression.isSatisfiedBy (cal.getTime ()));

    cal.set (2010, Calendar.OCTOBER, 28, 10, 15, 0);
    assertFalse (cronExpression.isSatisfiedBy (cal.getTime ()));

    cronExpression = new CronExpression ("0 15 10 L-5W * ? 2010");

    cal.set (2010, Calendar.OCTOBER, 26, 10, 15, 0); // last day - 5
    assertTrue (cronExpression.isSatisfiedBy (cal.getTime ()));

    cronExpression = new CronExpression ("0 15 10 L-1 * ? 2010");

    cal.set (2010, Calendar.OCTOBER, 30, 10, 15, 0); // last day - 1
    assertTrue (cronExpression.isSatisfiedBy (cal.getTime ()));

    cronExpression = new CronExpression ("0 15 10 L-1W * ? 2010");

    cal.set (2010, Calendar.OCTOBER, 29, 10, 15, 0); // nearest weekday to last
                                                     // day - 1 (29th is a
                                                     // friday in 2010)
    assertTrue (cronExpression.isSatisfiedBy (cal.getTime ()));

  }

  /**
   * QTZ-259 : last day offset causes repeating fire time
   */
  @Test
  public void testQtz259 () throws Exception
  {
    final CronScheduleBuilder schedBuilder = CronScheduleBuilder.cronSchedule ("0 0 0 L-2 * ? *");
    final ITrigger trigger = TriggerBuilder.newTrigger ().withIdentity ("test").withSchedule (schedBuilder).build ();

    int i = 0;
    Date pdate = trigger.getFireTimeAfter (new Date ());
    while (++i < 26)
    {
      final Date date = trigger.getFireTimeAfter (pdate);
      LOGGER.info ("fireTime: " + date + ", previousFireTime: " + pdate);
      assertFalse ("Next fire time is the same as previous fire time!", pdate.equals (date));
      pdate = date;
    }
  }

  /**
   * QTZ-259 : last day offset causes repeating fire time
   */
  @Test
  public void testQtz259LW () throws Exception
  {
    final CronScheduleBuilder schedBuilder = CronScheduleBuilder.cronSchedule ("0 0 0 LW * ? *");
    final ITrigger trigger = TriggerBuilder.newTrigger ().withIdentity ("test").withSchedule (schedBuilder).build ();

    int i = 0;
    Date pdate = trigger.getFireTimeAfter (new Date ());
    while (++i < 26)
    {
      final Date date = trigger.getFireTimeAfter (pdate);
      LOGGER.info ("fireTime: " + date + ", previousFireTime: " + pdate);
      assertFalse ("Next fire time is the same as previous fire time!", pdate.equals (date));
      pdate = date;
    }
  }

  /*
   * QUARTZ-574: Showing that storeExpressionVals correctly calculates the month
   * number
   */
  @Test
  public void testQuartz574 ()
  {
    try
    {
      CronExpression.validateExpression ("* * * * Foo ? ");
      fail ("Expected ParseException did not fire for non-existent month");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown", pe.getMessage ().startsWith ("Invalid Month value:"));
    }

    try
    {
      CronExpression.validateExpression ("* * * * Jan-Foo ? ");
      fail ("Expected ParseException did not fire for non-existent month");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown", pe.getMessage ().startsWith ("Invalid Month value:"));
    }
  }

  @Test
  public void testQuartz621 ()
  {
    try
    {
      CronExpression.validateExpression ("0 0 * * * *");
      fail ("Expected ParseException did not fire for wildcard day-of-month and day-of-week");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown",
                  pe.getMessage ()
                    .startsWith ("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented."));
    }
    try
    {
      CronExpression.validateExpression ("0 0 * 4 * *");
      fail ("Expected ParseException did not fire for specified day-of-month and wildcard day-of-week");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown",
                  pe.getMessage ()
                    .startsWith ("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented."));
    }
    try
    {
      CronExpression.validateExpression ("0 0 * * * 4");
      fail ("Expected ParseException did not fire for wildcard day-of-month and specified day-of-week");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown",
                  pe.getMessage ()
                    .startsWith ("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented."));
    }
  }

  @Test
  public void testQuartz640 ()
  {
    try
    {
      CronExpression.validateExpression ("0 43 9 1,5,29,L * ?");
      fail ("Expected ParseException did not fire for L combined with other days of the month");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown",
                  pe.getMessage ()
                    .startsWith ("Support for specifying 'L' and 'LW' with other days of the month is not implemented"));
    }
    try
    {
      CronExpression.validateExpression ("0 43 9 ? * SAT,SUN,L");
      fail ("Expected ParseException did not fire for L combined with other days of the week");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown",
                  pe.getMessage ()
                    .startsWith ("Support for specifying 'L' with other days of the week is not implemented"));
    }
    try
    {
      CronExpression.validateExpression ("0 43 9 ? * 6,7,L");
      fail ("Expected ParseException did not fire for L combined with other days of the week");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown",
                  pe.getMessage ()
                    .startsWith ("Support for specifying 'L' with other days of the week is not implemented"));
    }
    try
    {
      CronExpression.validateExpression ("0 43 9 ? * 5L");
    }
    catch (final ParseException pe)
    {
      fail ("Unexpected ParseException thrown for supported '5L' expression.");
    }
  }

  @Test
  public void testQtz96 ()
  {
    try
    {
      CronExpression.validateExpression ("0/5 * * 32W 1 ?");
      fail ("Expected ParseException did not fire for W with value larger than 31");
    }
    catch (final ParseException pe)
    {
      assertTrue ("Incorrect ParseException thrown: " + pe.getMessage (),
                  pe.getMessage ().startsWith ("The 'W' option does not make sense with values larger than"));
    }
  }

  @Test
  public void testQtz395_CopyConstructorMustPreserveTimeZone () throws ParseException
  {
    TimeZone nonDefault = TimeZone.getTimeZone ("Europe/Brussels");
    if (nonDefault.equals (TimeZone.getDefault ()))
    {
      nonDefault = EST_TIME_ZONE;
    }
    final CronExpression cronExpression = new CronExpression ("0 15 10 * * ? 2005");
    cronExpression.setTimeZone (nonDefault);

    final CronExpression copyCronExpression = new CronExpression (cronExpression);
    assertEquals (nonDefault, copyCronExpression.getTimeZone ());
  }
}
