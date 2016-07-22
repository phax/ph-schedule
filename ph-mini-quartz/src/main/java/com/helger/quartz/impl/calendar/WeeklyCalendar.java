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

package com.helger.quartz.impl.calendar;

import java.util.Calendar;
import java.util.TimeZone;

import com.helger.quartz.ICalendar;

/**
 * <p>
 * This implementation of the Calendar excludes a set of days of the week. You
 * may use it to exclude weekends for example. But you may define any day of the
 * week. By default it excludes SATURDAY and SUNDAY.
 * </p>
 *
 * @see com.helger.quartz.ICalendar
 * @see com.helger.quartz.impl.calendar.BaseCalendar
 * @author Juergen Donnerstag
 */
public class WeeklyCalendar extends BaseCalendar
{
  // An array to store the week days which are to be excluded.
  // Calendar.MONDAY etc. are used as index.
  private boolean [] excludeDays = new boolean [8];

  // Will be set to true, if all week days are excluded
  private boolean excludeAll = false;

  public WeeklyCalendar ()
  {
    this (null, null);
  }

  public WeeklyCalendar (final ICalendar baseCalendar)
  {
    this (baseCalendar, null);
  }

  public WeeklyCalendar (final TimeZone timeZone)
  {
    super (null, timeZone);
  }

  public WeeklyCalendar (final ICalendar baseCalendar, final TimeZone timeZone)
  {
    super (baseCalendar, timeZone);

    excludeDays[Calendar.SUNDAY] = true;
    excludeDays[Calendar.SATURDAY] = true;
    excludeAll = areAllDaysExcluded ();
  }

  @Override
  public Object clone ()
  {
    final WeeklyCalendar clone = (WeeklyCalendar) super.clone ();
    clone.excludeDays = excludeDays.clone ();
    return clone;
  }

  /**
   * <p>
   * Get the array with the week days
   * </p>
   */
  public boolean [] getDaysExcluded ()
  {
    return excludeDays;
  }

  /**
   * <p>
   * Return true, if wday (see Calendar.get()) is defined to be exluded. E. g.
   * saturday and sunday.
   * </p>
   */
  public boolean isDayExcluded (final int wday)
  {
    return excludeDays[wday];
  }

  /**
   * <p>
   * Redefine the array of days excluded. The array must of size greater or
   * equal 8. Calendar's constants like MONDAY should be used as index. A value
   * of true is regarded as: exclude it.
   * </p>
   */
  public void setDaysExcluded (final boolean [] weekDays)
  {
    if (weekDays == null)
    {
      return;
    }

    excludeDays = weekDays;
    excludeAll = areAllDaysExcluded ();
  }

  /**
   * <p>
   * Redefine a certain day of the week to be excluded (true) or included
   * (false). Use Calendar's constants like MONDAY to determine the wday.
   * </p>
   */
  public void setDayExcluded (final int wday, final boolean exclude)
  {
    excludeDays[wday] = exclude;
    excludeAll = areAllDaysExcluded ();
  }

  /**
   * <p>
   * Check if all week days are excluded. That is no day is included.
   * </p>
   *
   * @return boolean
   */
  public boolean areAllDaysExcluded ()
  {
    return isDayExcluded (Calendar.SUNDAY) &&
           isDayExcluded (Calendar.MONDAY) &&
           isDayExcluded (Calendar.TUESDAY) &&
           isDayExcluded (Calendar.WEDNESDAY) &&
           isDayExcluded (Calendar.THURSDAY) &&
           isDayExcluded (Calendar.FRIDAY) &&
           isDayExcluded (Calendar.SATURDAY);
  }

  /**
   * <p>
   * Determine whether the given time (in milliseconds) is 'included' by the
   * Calendar.
   * </p>
   * <p>
   * Note that this Calendar is only has full-day precision.
   * </p>
   */
  @Override
  public boolean isTimeIncluded (final long timeStamp)
  {
    if (excludeAll == true)
    {
      return false;
    }

    // Test the base calendar first. Only if the base calendar not already
    // excludes the time/date, continue evaluating this calendar instance.
    if (super.isTimeIncluded (timeStamp) == false)
    {
      return false;
    }

    final Calendar cl = createJavaCalendar (timeStamp);
    final int wday = cl.get (Calendar.DAY_OF_WEEK);

    return !(isDayExcluded (wday));
  }

  /**
   * <p>
   * Determine the next time (in milliseconds) that is 'included' by the
   * Calendar after the given time. Return the original value if timeStamp is
   * included. Return 0 if all days are excluded.
   * </p>
   * <p>
   * Note that this Calendar is only has full-day precision.
   * </p>
   */
  @Override
  public long getNextIncludedTime (final long nTimeStamp)
  {
    if (excludeAll == true)
    {
      return 0;
    }

    // Call base calendar implementation first
    long timeStamp = nTimeStamp;
    final long baseTime = super.getNextIncludedTime (timeStamp);
    if ((baseTime > 0) && (baseTime > timeStamp))
    {
      timeStamp = baseTime;
    }

    // Get timestamp for 00:00:00
    final Calendar cl = getStartOfDayJavaCalendar (timeStamp);
    int wday = cl.get (Calendar.DAY_OF_WEEK);

    if (!isDayExcluded (wday))
    {
      return timeStamp; // return the original value
    }

    while (isDayExcluded (wday) == true)
    {
      cl.add (Calendar.DATE, 1);
      wday = cl.get (Calendar.DAY_OF_WEEK);
    }

    return cl.getTime ().getTime ();
  }
}
