/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.hashcode.HashCodeGenerator;

/**
 * Represents a time in hour, minute and second of any given day.
 * <p>
 * The hour is in 24-hour convention, meaning values are from 0 to 23.
 * </p>
 *
 * @see DailyTimeIntervalScheduleBuilder
 * @since 2.0.3
 * @author James House
 * @author Zemian Deng saltnlight5@gmail.com
 */
public class TimeOfDay implements Serializable
{
  public static final TimeOfDay START_OF_DAY = new TimeOfDay (0, 0, 0);

  private final int m_nHour;
  private final int m_nMinute;
  private final int m_nSecond;

  /**
   * Create a TimeOfDay instance for the given hour, minute and second.
   *
   * @param hour
   *        The hour of day, between 0 and 23.
   * @param minute
   *        The minute of the hour, between 0 and 59.
   * @param second
   *        The second of the minute, between 0 and 59.
   * @throws IllegalArgumentException
   *         if one or more of the input values is out of their valid range.
   */
  public TimeOfDay (final int hour, final int minute, final int second)
  {
    m_nHour = hour;
    m_nMinute = minute;
    m_nSecond = second;
    _validate ();
  }

  private void _validate ()
  {
    if (m_nHour < 0 || m_nHour > 23)
      throw new IllegalArgumentException ("Hour must be from 0 to 23");
    if (m_nMinute < 0 || m_nMinute > 59)
      throw new IllegalArgumentException ("Minute must be from 0 to 59");
    if (m_nSecond < 0 || m_nSecond > 59)
      throw new IllegalArgumentException ("Second must be from 0 to 59");
  }

  /**
   * The hour of the day (between 0 and 23).
   *
   * @return The hour of the day (between 0 and 23).
   */
  public int getHour ()
  {
    return m_nHour;
  }

  /**
   * The minute of the hour.
   *
   * @return The minute of the hour (between 0 and 59).
   */
  public int getMinute ()
  {
    return m_nMinute;
  }

  /**
   * The second of the minute.
   *
   * @return The second of the minute (between 0 and 59).
   */
  public int getSecond ()
  {
    return m_nSecond;
  }

  /**
   * Determine with this time of day is before the given time of day.
   *
   * @return true this time of day is before the given time of day.
   */
  public boolean before (final TimeOfDay timeOfDay)
  {
    if (timeOfDay.m_nHour > m_nHour)
      return true;
    if (timeOfDay.m_nHour < m_nHour)
      return false;

    if (timeOfDay.m_nMinute > m_nMinute)
      return true;
    if (timeOfDay.m_nMinute < m_nMinute)
      return false;

    if (timeOfDay.m_nSecond > m_nSecond)
      return true;
    if (timeOfDay.m_nSecond < m_nSecond)
      return false;

    return false; // must be equal...
  }

  @Override
  public boolean equals (final Object obj)
  {
    if (obj == this)
      return true;
    if (obj == null || !getClass ().equals (TimeOfDay.class))
      return false;

    final TimeOfDay rhs = (TimeOfDay) obj;
    return rhs.m_nHour == m_nHour && rhs.m_nMinute == m_nMinute && rhs.m_nSecond == m_nSecond;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_nHour).append (m_nMinute).append (m_nSecond).getHashCode ();
  }

  /**
   * Return a date with time of day reset to this object values. The millisecond
   * value will be zero.
   */
  @Nullable
  public Date getTimeOfDayForDate (final Date dateTime)
  {
    if (dateTime == null)
      return null;
    final Calendar cal = PDTFactory.createCalendar ();
    cal.setTime (dateTime);
    cal.set (Calendar.HOUR_OF_DAY, m_nHour);
    cal.set (Calendar.MINUTE, m_nMinute);
    cal.set (Calendar.SECOND, m_nSecond);
    cal.clear (Calendar.MILLISECOND);
    return cal.getTime ();
  }

  @Override
  public String toString ()
  {
    return "TimeOfDay[" + m_nHour + ":" + m_nMinute + ":" + m_nSecond + "]";
  }

  /**
   * Create a {@link TimeOfDay} instance for the given hour, minute and second.
   *
   * @param hour
   *        The hour of day, between 0 and 23.
   * @param minute
   *        The minute of the hour, between 0 and 59.
   * @param second
   *        The second of the minute, between 0 and 59.
   * @throws IllegalArgumentException
   *         if one or more of the input values is out of their valid range.
   */
  @Nonnull
  public static TimeOfDay hourMinuteAndSecondOfDay (final int hour, final int minute, final int second)
  {
    return new TimeOfDay (hour, minute, second);
  }

  /**
   * Create a {@link TimeOfDay} instance for the given hour and minute (at the
   * zero second of the minute).
   *
   * @param hour
   *        The hour of day, between 0 and 23.
   * @param minute
   *        The minute of the hour, between 0 and 59.
   * @throws IllegalArgumentException
   *         if one or more of the input values is out of their valid range.
   */
  @Nonnull
  public static TimeOfDay hourAndMinuteOfDay (final int hour, final int minute)
  {
    return new TimeOfDay (hour, minute, 0);
  }

  /**
   * Create a {@link TimeOfDay} instance for the given hour (at the zero minute
   * and zero second of the minute).
   *
   * @param hour
   *        The hour of day, between 0 and 23.
   * @throws IllegalArgumentException
   *         if one or more of the input values is out of their valid range.
   */
  @Nonnull
  public static TimeOfDay hourOfDay (final int hour)
  {
    return new TimeOfDay (hour, 0, 0);
  }

  /**
   * Create a {@link TimeOfDay} from the given date, in the system default
   * TimeZone.
   *
   * @param dateTime
   *        The {@link Date} from which to extract Hour, Minute and Second.
   */
  @Nullable
  public static TimeOfDay hourAndMinuteAndSecondFromDate (@Nullable final Date dateTime)
  {
    return hourAndMinuteAndSecondFromDate (dateTime, null);
  }

  /**
   * Create a {@link TimeOfDay} from the given date, in the given TimeZone.
   *
   * @param dateTime
   *        The {@link Date} from which to extract Hour, Minute and Second.
   * @param tz
   *        The {@link TimeZone} from which relate Hour, Minute and Second for
   *        the given date. If null, system default TimeZone will be used.
   */
  @Nullable
  public static TimeOfDay hourAndMinuteAndSecondFromDate (@Nullable final Date dateTime, @Nullable final TimeZone tz)
  {
    if (dateTime == null)
      return null;
    final Calendar cal = PDTFactory.createCalendar ();
    cal.setTime (dateTime);
    if (tz != null)
      cal.setTimeZone (tz);

    return new TimeOfDay (cal.get (Calendar.HOUR_OF_DAY), cal.get (Calendar.MINUTE), cal.get (Calendar.SECOND));
  }

  /**
   * Create a {@link TimeOfDay} from the given date (at the zero-second), in the
   * system default TimeZone.
   *
   * @param dateTime
   *        The {@link Date} from which to extract Hour and Minute.
   */
  @Nullable
  public static TimeOfDay hourAndMinuteFromDate (@Nullable final Date dateTime)
  {
    return hourAndMinuteFromDate (dateTime, null);
  }

  /**
   * Create a {@link TimeOfDay} from the given date (at the zero-second), in the
   * system default TimeZone.
   *
   * @param dateTime
   *        The {@link Date} from which to extract Hour and Minute.
   * @param tz
   *        The {@link TimeZone} from which relate Hour and Minute for the given
   *        date. If null, system default TimeZone will be used.
   */
  @Nullable
  public static TimeOfDay hourAndMinuteFromDate (@Nullable final Date dateTime, @Nullable final TimeZone tz)
  {
    if (dateTime == null)
      return null;
    final Calendar cal = PDTFactory.createCalendar ();
    cal.setTime (dateTime);
    if (tz != null)
      cal.setTimeZone (tz);

    return new TimeOfDay (cal.get (Calendar.HOUR_OF_DAY), cal.get (Calendar.MINUTE), 0);
  }
}
