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
package com.helger.quartz.impl.calendar;

import java.util.Calendar;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import com.helger.commons.collection.ArrayHelper;
import com.helger.quartz.ICalendar;

/**
 * <p>
 * This implementation of the Calendar excludes a set of days of the month. You
 * may use it to exclude every first day of each month for example. But you may
 * define any day of a month.
 * </p>
 *
 * @see com.helger.quartz.ICalendar
 * @see com.helger.quartz.impl.calendar.AbstractCalendar
 * @author Juergen Donnerstag
 */
public class MonthlyCalendar extends AbstractCalendar <MonthlyCalendar>
{
  private static final int MAX_DAYS_IN_MONTH = 31;

  // An array to store a months days which are to be excluded.
  // Calendar.get( ) as index.
  private boolean [] m_aExcludeDays = new boolean [MAX_DAYS_IN_MONTH];

  // Will be set to true, if all week days are excluded
  private boolean m_bExcludeAll = false;

  public MonthlyCalendar (@Nonnull final MonthlyCalendar aOther)
  {
    super (aOther);
    m_aExcludeDays = ArrayHelper.getCopy (aOther.m_aExcludeDays);
    m_bExcludeAll = aOther.m_bExcludeAll;
  }

  public MonthlyCalendar ()
  {
    this (null, null);
  }

  public MonthlyCalendar (final ICalendar baseCalendar)
  {
    this (baseCalendar, null);
  }

  public MonthlyCalendar (final TimeZone timeZone)
  {
    this (null, timeZone);
  }

  public MonthlyCalendar (final ICalendar baseCalendar, final TimeZone timeZone)
  {
    super (baseCalendar, timeZone);

    // all days are included by default
    m_bExcludeAll = areAllDaysExcluded ();
  }

  /**
   * <p>
   * Get the array which defines the exclude-value of each day of month. Only
   * the first 31 elements of the array are relevant, with the 0 index element
   * representing the first day of the month.
   * </p>
   */
  public boolean [] getDaysExcluded ()
  {
    return m_aExcludeDays;
  }

  /**
   * <p>
   * Return true, if day is defined to be excluded.
   * </p>
   *
   * @param day
   *        The day of the month (from 1 to 31) to check.
   */
  public boolean isDayExcluded (final int day)
  {
    if ((day < 1) || (day > MAX_DAYS_IN_MONTH))
    {
      throw new IllegalArgumentException ("The day parameter must be in the range of 1 to " + MAX_DAYS_IN_MONTH);
    }

    return m_aExcludeDays[day - 1];
  }

  /**
   * <p>
   * Redefine the array of days excluded. The array must non-null and of size
   * greater or equal to 31. The 0 index element represents the first day of the
   * month.
   * </p>
   */
  public void setDaysExcluded (final boolean [] days)
  {
    if (days == null)
    {
      throw new IllegalArgumentException ("The days parameter cannot be null.");
    }

    if (days.length < MAX_DAYS_IN_MONTH)
    {
      throw new IllegalArgumentException ("The days parameter must have a length of at least " +
                                          MAX_DAYS_IN_MONTH +
                                          " elements.");
    }

    m_aExcludeDays = days;
    m_bExcludeAll = areAllDaysExcluded ();
  }

  /**
   * <p>
   * Redefine a certain day of the month to be excluded (true) or included
   * (false).
   * </p>
   *
   * @param day
   *        The day of the month (from 1 to 31) to set.
   */
  public void setDayExcluded (final int day, final boolean exclude)
  {
    if ((day < 1) || (day > MAX_DAYS_IN_MONTH))
    {
      throw new IllegalArgumentException ("The day parameter must be in the range of 1 to " + MAX_DAYS_IN_MONTH);
    }

    m_aExcludeDays[day - 1] = exclude;
    m_bExcludeAll = areAllDaysExcluded ();
  }

  /**
   * <p>
   * Check if all days are excluded. That is no day is included.
   * </p>
   */
  public boolean areAllDaysExcluded ()
  {
    for (int i = 1; i <= MAX_DAYS_IN_MONTH; i++)
      if (!isDayExcluded (i))
        return false;
    return true;
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
    if (m_bExcludeAll)
    {
      return false;
    }

    // Test the base calendar first. Only if the base calendar not already
    // excludes the time/date, continue evaluating this calendar instance.
    if (!super.isTimeIncluded (timeStamp))
      return false;

    final Calendar cl = createJavaCalendar (timeStamp);
    final int day = cl.get (Calendar.DAY_OF_MONTH);
    return !isDayExcluded (day);
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
    if (m_bExcludeAll)
      return 0;

    // Call base calendar implementation first
    long timeStamp = nTimeStamp;
    final long baseTime = super.getNextIncludedTime (timeStamp);
    if (baseTime > 0 && baseTime > timeStamp)
    {
      timeStamp = baseTime;
    }

    // Get timestamp for 00:00:00
    final Calendar cl = getStartOfDayJavaCalendar (timeStamp);
    int day = cl.get (Calendar.DAY_OF_MONTH);

    if (!isDayExcluded (day))
    {
      // return the original value
      return timeStamp;
    }

    while (isDayExcluded (day))
    {
      cl.add (Calendar.DATE, 1);
      day = cl.get (Calendar.DAY_OF_MONTH);
    }

    return cl.getTime ().getTime ();
  }

  @Override
  @Nonnull
  public MonthlyCalendar getClone ()
  {
    return new MonthlyCalendar (this);
  }
}
