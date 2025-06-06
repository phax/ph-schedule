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

import java.time.DayOfWeek;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.datetime.PDTFactory;

/**
 * <code>DateBuilder</code> is used to conveniently create
 * <code>java.util.Date</code> instances that meet particular criteria.
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related
 * entities via a Domain-Specific Language (DSL). The DSL can best be utilized
 * through the usage of static imports of the methods on the classes
 * <code>TriggerBuilder</code>, <code>JobBuilder</code>,
 * <code>DateBuilder</code>, <code>JobKey</code>, <code>TriggerKey</code> and
 * the various <code>ScheduleBuilder</code> implementations.
 * </p>
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 *
 * <pre>
 * JobDetail job = newJob (MyJob.class).withIdentity ("myJob").build ();
 * Trigger trigger = newTrigger ().withIdentity (triggerKey ("myTrigger", "myTriggerGroup"))
 *                                .withSchedule (simpleSchedule ().withIntervalInHours (1).repeatForever ())
 *                                .startAt (futureDate (10, MINUTES))
 *                                .build ();
 * scheduler.scheduleJob (job, trigger);
 * </pre>
 *
 * @see TriggerBuilder
 * @see JobBuilder
 */
public final class DateBuilder
{
  public static final long SECONDS_IN_MOST_DAYS = CGlobal.SECONDS_PER_DAY;
  public static final long MILLISECONDS_IN_DAY = SECONDS_IN_MOST_DAYS * CGlobal.MILLISECONDS_PER_SECOND;

  private TimeZone m_aTZ;
  private Locale m_aLocale;
  private int m_nMonth;
  private int m_nDay;
  private int m_nYear;
  private int m_nHour;
  private int m_nMinute;
  private int m_nSecond;

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the system default timezone.
   */
  private DateBuilder ()
  {
    this (TimeZone.getDefault (), Locale.getDefault (Locale.Category.FORMAT));
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the given timezone.
   */
  private DateBuilder (final TimeZone tz)
  {
    this (tz, Locale.getDefault (Locale.Category.FORMAT));
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the given locale.
   */
  private DateBuilder (final Locale lc)
  {
    this (TimeZone.getDefault (), lc);
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the given timezone and locale.
   */
  private DateBuilder (final TimeZone tz, final Locale lc)
  {
    final Calendar cal = Calendar.getInstance (tz, lc);

    m_aTZ = tz;
    m_aLocale = lc;
    m_nMonth = cal.get (Calendar.MONTH) + 1;
    m_nDay = cal.get (Calendar.DAY_OF_MONTH);
    m_nYear = cal.get (Calendar.YEAR);
    m_nHour = cal.get (Calendar.HOUR_OF_DAY);
    m_nMinute = cal.get (Calendar.MINUTE);
    m_nSecond = cal.get (Calendar.SECOND);
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the system default timezone.
   */
  public static DateBuilder newDate ()
  {
    return new DateBuilder ();
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the given timezone.
   */
  public static DateBuilder newDateInTimezone (final TimeZone tz)
  {
    return new DateBuilder (tz);
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the given locale.
   */
  public static DateBuilder newDateInLocale (final Locale lc)
  {
    return new DateBuilder (lc);
  }

  /**
   * Create a DateBuilder, with initial settings for the current date and time
   * in the given timezone and locale.
   */
  public static DateBuilder newDateInTimeZoneAndLocale (final TimeZone tz, final Locale lc)
  {
    return new DateBuilder (tz, lc);
  }

  /**
   * Build the Date defined by this builder instance.
   */
  @Nonnull
  public Date build ()
  {
    Calendar cal;
    if (m_aTZ != null && m_aLocale != null)
      cal = Calendar.getInstance (m_aTZ, m_aLocale);
    else
      if (m_aTZ != null)
        cal = Calendar.getInstance (m_aTZ, Locale.getDefault (Locale.Category.FORMAT));
      else
        if (m_aLocale != null)
          cal = Calendar.getInstance (TimeZone.getDefault (), m_aLocale);
        else
          cal = PDTFactory.createCalendar ();

    cal.set (Calendar.YEAR, m_nYear);
    cal.set (Calendar.MONTH, m_nMonth - 1);
    cal.set (Calendar.DAY_OF_MONTH, m_nDay);
    cal.set (Calendar.HOUR_OF_DAY, m_nHour);
    cal.set (Calendar.MINUTE, m_nMinute);
    cal.set (Calendar.SECOND, m_nSecond);
    cal.set (Calendar.MILLISECOND, 0);
    return cal.getTime ();
  }

  /**
   * Set the hour (0-23) for the Date that will be built by this builder.
   */
  public DateBuilder atHourOfDay (final int atHour)
  {
    validateHour (atHour);

    m_nHour = atHour;
    return this;
  }

  /**
   * Set the minute (0-59) for the Date that will be built by this builder.
   */
  public DateBuilder atMinute (final int atMinute)
  {
    validateMinute (atMinute);

    m_nMinute = atMinute;
    return this;
  }

  /**
   * Set the second (0-59) for the Date that will be built by this builder, and
   * truncate the milliseconds to 000.
   */
  public DateBuilder atSecond (final int atSecond)
  {
    validateSecond (atSecond);

    m_nSecond = atSecond;
    return this;
  }

  public DateBuilder atHourMinuteAndSecond (final int atHour, final int atMinute, final int atSecond)
  {
    validateHour (atHour);
    validateMinute (atMinute);
    validateSecond (atSecond);

    m_nHour = atHour;
    m_nSecond = atSecond;
    m_nMinute = atMinute;
    return this;
  }

  /**
   * Set the day of month (1-31) for the Date that will be built by this
   * builder.
   */
  public DateBuilder onDay (final int onDay)
  {
    validateDayOfMonth (onDay);

    m_nDay = onDay;
    return this;
  }

  /**
   * Set the month (1-12) for the Date that will be built by this builder.
   */
  public DateBuilder inMonth (final Month inMonth)
  {
    validateMonth (inMonth);

    m_nMonth = inMonth.getValue ();
    return this;
  }

  public DateBuilder inMonthOnDay (final Month inMonth, final int onDay)
  {
    validateMonth (inMonth);
    validateDayOfMonth (onDay);

    m_nMonth = inMonth.getValue ();
    m_nDay = onDay;
    return this;
  }

  /**
   * Set the year for the Date that will be built by this builder.
   */
  public DateBuilder inYear (final int inYear)
  {
    validateYear (inYear);

    m_nYear = inYear;
    return this;
  }

  /**
   * Set the TimeZone for the Date that will be built by this builder (if
   * "null", system default will be used)
   */
  public DateBuilder inTimeZone (final TimeZone timezone)
  {
    m_aTZ = timezone;
    return this;
  }

  /**
   * Set the Locale for the Date that will be built by this builder (if "null",
   * system default will be used)
   */
  public DateBuilder inLocale (final Locale locale)
  {
    m_aLocale = locale;
    return this;
  }

  public static Date futureDate (final int interval, final EIntervalUnit unit)
  {

    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (new Date ());
    c.setLenient (true);

    c.add (_translate (unit), interval);

    return c.getTime ();
  }

  private static int _translate (final EIntervalUnit unit)
  {
    switch (unit)
    {
      case DAY:
        return Calendar.DAY_OF_YEAR;
      case HOUR:
        return Calendar.HOUR_OF_DAY;
      case MINUTE:
        return Calendar.MINUTE;
      case MONTH:
        return Calendar.MONTH;
      case SECOND:
        return Calendar.SECOND;
      case MILLISECOND:
        return Calendar.MILLISECOND;
      case WEEK:
        return Calendar.WEEK_OF_YEAR;
      case YEAR:
        return Calendar.YEAR;
      default:
        throw new IllegalArgumentException ("Unknown IntervalUnit");
    }
  }

  /**
   * <p>
   * Get a <code>Date</code> object that represents the given time, on
   * tomorrow's date.
   * </p>
   *
   * @param second
   *        The value (0-59) to give the seconds field of the date
   * @param minute
   *        The value (0-59) to give the minutes field of the date
   * @param hour
   *        The value (0-23) to give the hours field of the date
   * @return the new date
   */
  public static Date tomorrowAt (final int hour, final int minute, final int second)
  {
    validateSecond (second);
    validateMinute (minute);
    validateHour (hour);

    final Date date = new Date ();

    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date);
    c.setLenient (true);

    // advance one day
    c.add (Calendar.DAY_OF_YEAR, 1);

    c.set (Calendar.HOUR_OF_DAY, hour);
    c.set (Calendar.MINUTE, minute);
    c.set (Calendar.SECOND, second);
    c.set (Calendar.MILLISECOND, 0);

    return c.getTime ();
  }

  /**
   * <p>
   * Get a <code>Date</code> object that represents the given time, on today's
   * date (equivalent to {@link #dateOf(int, int, int)}).
   * </p>
   *
   * @param second
   *        The value (0-59) to give the seconds field of the date
   * @param minute
   *        The value (0-59) to give the minutes field of the date
   * @param hour
   *        The value (0-23) to give the hours field of the date
   * @return the new date
   */
  public static Date todayAt (final int hour, final int minute, final int second)
  {
    return dateOf (hour, minute, second);
  }

  /**
   * <p>
   * Get a <code>Date</code> object that represents the given time, on today's
   * date (equivalent to {@link #todayAt(int, int, int)}).
   * </p>
   *
   * @param second
   *        The value (0-59) to give the seconds field of the date
   * @param minute
   *        The value (0-59) to give the minutes field of the date
   * @param hour
   *        The value (0-23) to give the hours field of the date
   * @return the new date
   */
  public static Date dateOf (final int hour, final int minute, final int second)
  {
    validateSecond (second);
    validateMinute (minute);
    validateHour (hour);

    final Date date = new Date ();

    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date);
    c.setLenient (true);

    c.set (Calendar.HOUR_OF_DAY, hour);
    c.set (Calendar.MINUTE, minute);
    c.set (Calendar.SECOND, second);
    c.set (Calendar.MILLISECOND, 0);

    return c.getTime ();
  }

  /**
   * <p>
   * Get a <code>Date</code> object that represents the given time, on the given
   * date.
   * </p>
   *
   * @param second
   *        The value (0-59) to give the seconds field of the date
   * @param minute
   *        The value (0-59) to give the minutes field of the date
   * @param hour
   *        The value (0-23) to give the hours field of the date
   * @param dayOfMonth
   *        The value (1-31) to give the day of month field of the date
   * @param month
   *        The value (1-12) to give the month field of the date
   * @return the new date
   */
  public static Date dateOf (final int hour,
                             final int minute,
                             final int second,
                             final int dayOfMonth,
                             final Month month)
  {
    validateSecond (second);
    validateMinute (minute);
    validateHour (hour);
    validateDayOfMonth (dayOfMonth);
    validateMonth (month);

    final Date date = new Date ();

    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date);

    c.set (Calendar.MONTH, month.getValue () - 1);
    c.set (Calendar.DAY_OF_MONTH, dayOfMonth);
    c.set (Calendar.HOUR_OF_DAY, hour);
    c.set (Calendar.MINUTE, minute);
    c.set (Calendar.SECOND, second);
    c.set (Calendar.MILLISECOND, 0);

    return c.getTime ();
  }

  /**
   * <p>
   * Get a <code>Date</code> object that represents the given time, on the given
   * date.
   * </p>
   *
   * @param second
   *        The value (0-59) to give the seconds field of the date
   * @param minute
   *        The value (0-59) to give the minutes field of the date
   * @param hour
   *        The value (0-23) to give the hours field of the date
   * @param dayOfMonth
   *        The value (1-31) to give the day of month field of the date
   * @param month
   *        The value (1-12) to give the month field of the date
   * @param year
   *        The value (1970-2099) to give the year field of the date
   * @return the new date
   */
  public static Date dateOf (final int hour,
                             final int minute,
                             final int second,
                             final int dayOfMonth,
                             final Month month,
                             final int year)
  {
    validateSecond (second);
    validateMinute (minute);
    validateHour (hour);
    validateDayOfMonth (dayOfMonth);
    validateMonth (month);
    validateYear (year);

    final Date date = new Date ();

    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date);

    c.set (Calendar.YEAR, year);
    c.set (Calendar.MONTH, month.getValue () - 1);
    c.set (Calendar.DAY_OF_MONTH, dayOfMonth);
    c.set (Calendar.HOUR_OF_DAY, hour);
    c.set (Calendar.MINUTE, minute);
    c.set (Calendar.SECOND, second);
    c.set (Calendar.MILLISECOND, 0);

    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even hour after the current
   * time.
   * </p>
   * <p>
   * For example a current time of 08:13:54 would result in a date with the time
   * of 09:00:00. If the date's time is in the 23rd hour, the date's 'day' will
   * be promoted, and the time will be set to 00:00:00.
   * </p>
   *
   * @return the new rounded date
   */
  public static Date evenHourDateAfterNow ()
  {
    return evenHourDate (null);
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even hour above the given date.
   * </p>
   * <p>
   * For example an input date with a time of 08:13:54 would result in a date
   * with the time of 09:00:00. If the date's time is in the 23rd hour, the
   * date's 'day' will be promoted, and the time will be set to 00:00:00.
   * </p>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @return the new rounded date
   */
  public static Date evenHourDate (final Date date)
  {
    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());
    c.setLenient (true);

    c.set (Calendar.HOUR_OF_DAY, c.get (Calendar.HOUR_OF_DAY) + 1);
    c.set (Calendar.MINUTE, 0);
    c.set (Calendar.SECOND, 0);
    c.set (Calendar.MILLISECOND, 0);

    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the previous even hour below the given
   * date.
   * </p>
   * <p>
   * For example an input date with a time of 08:13:54 would result in a date
   * with the time of 08:00:00.
   * </p>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @return the new rounded date
   */
  public static Date evenHourDateBefore (final Date date)
  {
    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());

    c.set (Calendar.MINUTE, 0);
    c.set (Calendar.SECOND, 0);
    c.set (Calendar.MILLISECOND, 0);

    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even minute after the current
   * time.
   * </p>
   * <p>
   * For example a current time of 08:13:54 would result in a date with the time
   * of 08:14:00. If the date's time is in the 59th minute, then the hour (and
   * possibly the day) will be promoted.
   * </p>
   *
   * @return the new rounded date
   */
  public static Date evenMinuteDateAfterNow ()
  {
    return evenMinuteDate (null);
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even minute above the given
   * date.
   * </p>
   * <p>
   * For example an input date with a time of 08:13:54 would result in a date
   * with the time of 08:14:00. If the date's time is in the 59th minute, then
   * the hour (and possibly the day) will be promoted.
   * </p>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @return the new rounded date
   */
  public static Date evenMinuteDate (final Date date)
  {
    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());
    c.setLenient (true);

    c.set (Calendar.MINUTE, c.get (Calendar.MINUTE) + 1);
    c.set (Calendar.SECOND, 0);
    c.set (Calendar.MILLISECOND, 0);
    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the previous even minute below the given
   * date.
   * </p>
   * <p>
   * For example an input date with a time of 08:13:54 would result in a date
   * with the time of 08:13:00.
   * </p>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @return the new rounded date
   */
  public static Date evenMinuteDateBefore (final Date date)
  {
    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());
    c.set (Calendar.SECOND, 0);
    c.set (Calendar.MILLISECOND, 0);
    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even second after the current
   * time.
   * </p>
   *
   * @return the new rounded date
   */
  public static Date evenSecondDateAfterNow ()
  {
    return evenSecondDate (null);
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even second above the given
   * date.
   * </p>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @return the new rounded date
   */
  public static Date evenSecondDate (final Date date)
  {
    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());
    c.setLenient (true);
    c.set (Calendar.SECOND, c.get (Calendar.SECOND) + 1);
    c.set (Calendar.MILLISECOND, 0);
    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the previous even second below the given
   * date.
   * </p>
   * <p>
   * For example an input date with a time of 08:13:54.341 would result in a
   * date with the time of 08:13:54.000.
   * </p>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @return the new rounded date
   */
  public static Date evenSecondDateBefore (final Date date)
  {
    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());
    c.set (Calendar.MILLISECOND, 0);
    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even multiple of the given
   * minute.
   * </p>
   * <p>
   * For example an input date with a time of 08:13:54, and an input minute-base
   * of 5 would result in a date with the time of 08:15:00. The same input date
   * with an input minute-base of 10 would result in a date with the time of
   * 08:20:00. But a date with the time 08:53:31 and an input minute-base of 45
   * would result in 09:00:00, because the even-hour is the next 'base' for
   * 45-minute intervals.
   * </p>
   * <p>
   * More examples:
   * </p>
   * <table summary="examples">
   * <tr>
   * <th>Input Time</th>
   * <th>Minute-Base</th>
   * <th>Result Time</th>
   * </tr>
   * <tr>
   * <td>11:16:41</td>
   * <td>20</td>
   * <td>11:20:00</td>
   * </tr>
   * <tr>
   * <td>11:36:41</td>
   * <td>20</td>
   * <td>11:40:00</td>
   * </tr>
   * <tr>
   * <td>11:46:41</td>
   * <td>20</td>
   * <td>12:00:00</td>
   * </tr>
   * <tr>
   * <td>11:26:41</td>
   * <td>30</td>
   * <td>11:30:00</td>
   * </tr>
   * <tr>
   * <td>11:36:41</td>
   * <td>30</td>
   * <td>12:00:00</td>
   * </tr>
   * <tr>
   * <td>11:16:41</td>
   * <td>17</td>
   * <td>11:17:00</td>
   * </tr>
   * <tr>
   * <td>11:17:41</td>
   * <td>17</td>
   * <td>11:34:00</td>
   * </tr>
   * <tr>
   * <td>11:52:41</td>
   * <td>17</td>
   * <td>12:00:00</td>
   * </tr>
   * <tr>
   * <td>11:52:41</td>
   * <td>5</td>
   * <td>11:55:00</td>
   * </tr>
   * <tr>
   * <td>11:57:41</td>
   * <td>5</td>
   * <td>12:00:00</td>
   * </tr>
   * <tr>
   * <td>11:17:41</td>
   * <td>0</td>
   * <td>12:00:00</td>
   * </tr>
   * <tr>
   * <td>11:17:41</td>
   * <td>1</td>
   * <td>11:08:00</td>
   * </tr>
   * </table>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @param minuteBase
   *        the base-minute to set the time on
   * @return the new rounded date
   * @see #nextGivenSecondDate(Date, int)
   */
  public static Date nextGivenMinuteDate (final Date date, final int minuteBase)
  {
    if (minuteBase < 0 || minuteBase > 59)
    {
      throw new IllegalArgumentException ("minuteBase must be >=0 and <= 59");
    }

    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());
    c.setLenient (true);

    if (minuteBase == 0)
    {
      c.set (Calendar.HOUR_OF_DAY, c.get (Calendar.HOUR_OF_DAY) + 1);
      c.set (Calendar.MINUTE, 0);
      c.set (Calendar.SECOND, 0);
      c.set (Calendar.MILLISECOND, 0);
    }
    else
    {
      final int minute = c.get (Calendar.MINUTE);
      final int arItr = minute / minuteBase;
      final int nextMinuteOccurance = minuteBase * (arItr + 1);

      if (nextMinuteOccurance < 60)
      {
        c.set (Calendar.MINUTE, nextMinuteOccurance);
        c.set (Calendar.SECOND, 0);
        c.set (Calendar.MILLISECOND, 0);
      }
      else
      {
        c.set (Calendar.HOUR_OF_DAY, c.get (Calendar.HOUR_OF_DAY) + 1);
        c.set (Calendar.MINUTE, 0);
        c.set (Calendar.SECOND, 0);
        c.set (Calendar.MILLISECOND, 0);
      }
    }
    return c.getTime ();
  }

  /**
   * <p>
   * Returns a date that is rounded to the next even multiple of the given
   * minute.
   * </p>
   * <p>
   * The rules for calculating the second are the same as those for calculating
   * the minute in the method <code>getNextGivenMinuteDate(..)</code>.
   * </p>
   *
   * @param date
   *        the Date to round, if <code>null</code> the current time will be
   *        used
   * @param secondBase
   *        the base-second to set the time on
   * @return the new rounded date
   * @see #nextGivenMinuteDate(Date, int)
   */
  public static Date nextGivenSecondDate (final Date date, final int secondBase)
  {
    if (secondBase < 0 || secondBase > 59)
      throw new IllegalArgumentException ("secondBase must be >=0 and <= 59");

    final Calendar c = PDTFactory.createCalendar ();
    c.setTime (date != null ? date : new Date ());
    c.setLenient (true);

    if (secondBase == 0)
    {
      c.set (Calendar.MINUTE, c.get (Calendar.MINUTE) + 1);
      c.set (Calendar.SECOND, 0);
      c.set (Calendar.MILLISECOND, 0);
    }
    else
    {
      final int second = c.get (Calendar.SECOND);
      final int arItr = second / secondBase;
      final int nextSecondOccurance = secondBase * (arItr + 1);
      if (nextSecondOccurance < 60)
      {
        c.set (Calendar.SECOND, nextSecondOccurance);
        c.set (Calendar.MILLISECOND, 0);
      }
      else
      {
        c.set (Calendar.MINUTE, c.get (Calendar.MINUTE) + 1);
        c.set (Calendar.SECOND, 0);
        c.set (Calendar.MILLISECOND, 0);
      }
    }
    return c.getTime ();
  }

  /**
   * Translate a date &amp; time from a users time zone to the another (probably
   * server) time zone to assist in creating a simple trigger with the right
   * date &amp; time.
   *
   * @param date
   *        the date to translate
   * @param aSrcTZ
   *        the original time-zone
   * @param aDestTZ
   *        the destination time-zone
   * @return the translated date
   */
  public static Date translateTime (final Date date, final TimeZone aSrcTZ, final TimeZone aDestTZ)
  {
    final Date newDate = new Date ();
    final int offset = aDestTZ.getOffset (date.getTime ()) - aSrcTZ.getOffset (date.getTime ());
    newDate.setTime (date.getTime () - offset);
    return newDate;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void validateDayOfWeek (final DayOfWeek dayOfWeek)
  {
    ValueEnforcer.notNull (dayOfWeek, "DayOfWeek");
  }

  public static void validateHour (final int hour)
  {
    if (hour < 0 || hour > 23)
    {
      throw new IllegalArgumentException ("Invalid hour (must be >= 0 and <= 23).");
    }
  }

  public static void validateMinute (final int minute)
  {
    if (minute < 0 || minute > 59)
    {
      throw new IllegalArgumentException ("Invalid minute (must be >= 0 and <= 59).");
    }
  }

  public static void validateSecond (final int second)
  {
    if (second < 0 || second > 59)
    {
      throw new IllegalArgumentException ("Invalid second (must be >= 0 and <= 59).");
    }
  }

  public static void validateDayOfMonth (final int day)
  {
    if (day < 1 || day > 31)
    {
      throw new IllegalArgumentException ("Invalid day of month.");
    }
  }

  public static void validateMonth (final Month month)
  {
    ValueEnforcer.notNull (month, "Month");
  }

  public static void validateYear (final int year)
  {
    if (year < 0 || year > CQuartz.MAX_YEAR)
    {
      throw new IllegalArgumentException ("Invalid year (must be >= 0 and <= " + CQuartz.MAX_YEAR);
    }
  }
}
