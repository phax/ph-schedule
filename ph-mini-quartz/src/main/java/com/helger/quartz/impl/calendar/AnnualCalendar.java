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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.compare.IComparator;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.quartz.ICalendar;

/**
 * <p>
 * This implementation of the Calendar excludes a set of days of the year. You
 * may use it to exclude bank holidays which are on the same date every year.
 * </p>
 *
 * @see com.helger.quartz.ICalendar
 * @see com.helger.quartz.impl.calendar.AbstractCalendar
 * @author Juergen Donnerstag
 */
public class AnnualCalendar extends AbstractCalendar <AnnualCalendar>
{
  private final ICommonsList <Calendar> m_aExcludeDays = new CommonsArrayList <> ();

  // true, if excludeDays is sorted
  private boolean m_bDataSorted = false;

  public AnnualCalendar (@NonNull final AnnualCalendar aRhs)
  {
    super (aRhs);
    m_aExcludeDays.addAll (aRhs.m_aExcludeDays);
  }

  public AnnualCalendar ()
  {
    this (null, null);
  }

  public AnnualCalendar (final ICalendar baseCalendar)
  {
    this (baseCalendar, null);
  }

  public AnnualCalendar (final TimeZone timeZone)
  {
    this (null, timeZone);
  }

  public AnnualCalendar (final ICalendar baseCalendar, final TimeZone timeZone)
  {
    super (baseCalendar, timeZone);
  }

  /**
   * @return Get the list which defines the exclude-value of each day of month
   */
  @ReturnsMutableObject
  public ICommonsList <Calendar> getDaysExcluded ()
  {
    return m_aExcludeDays;
  }

  /**
   * @param day
   *        day to check. May not be <code>null</code>.
   * @return <code>true</code> if day is defined to be excluded.
   */
  public boolean isDayExcluded (@NonNull final Calendar day)
  {
    ValueEnforcer.notNull (day, "Day");

    // Check baseCalendar first
    if (!super.isTimeIncluded (day.getTime ().getTime ()))
      return true;

    final int dmonth = day.get (Calendar.MONTH);
    final int dday = day.get (Calendar.DAY_OF_MONTH);

    if (!m_bDataSorted)
    {
      Collections.sort (m_aExcludeDays, new CalendarComparator ());
      m_bDataSorted = true;
    }

    final Iterator <Calendar> iter = m_aExcludeDays.iterator ();
    while (iter.hasNext ())
    {
      final Calendar cl = iter.next ();

      // remember, the list is sorted
      if (dmonth < cl.get (Calendar.MONTH))
        return false;

      if (dday != cl.get (Calendar.DAY_OF_MONTH))
        continue;

      if (dmonth != cl.get (Calendar.MONTH))
        continue;

      return true;
    }

    return false;
  }

  /**
   * Redefine the list of days excluded. The ArrayList should contain
   * <code>Calendar</code> objects.
   *
   * @param days
   *        The days to be excluded
   */
  public void setDaysExcluded (@Nullable final List <Calendar> days)
  {
    if (days == null)
      m_aExcludeDays.clear ();
    else
      m_aExcludeDays.setAll (days);

    m_bDataSorted = false;
  }

  /**
   * Redefine a certain day to be excluded (true) or included (false).
   */
  public void setDayExcluded (final Calendar day, final boolean exclude)
  {
    if (exclude)
    {
      if (!isDayExcluded (day))
      {
        m_aExcludeDays.add (day);
        m_bDataSorted = false;
      }
    }
    else
    {
      if (isDayExcluded (day))
        _removeExcludedDay (day, true);
    }
  }

  /**
   * Remove the given day from the list of excluded days
   *
   * @param day
   *        the day to exclude
   */
  public void removeExcludedDay (final Calendar day)
  {
    _removeExcludedDay (day, false);
  }

  private void _removeExcludedDay (final Calendar aDay, final boolean isChecked)
  {
    Calendar day = aDay;
    if (!isChecked && !isDayExcluded (day))
      return;

    // Fast way, see if exact day object was already in list
    if (m_aExcludeDays.remove (day))
      return;

    final int dmonth = day.get (Calendar.MONTH);
    final int dday = day.get (Calendar.DAY_OF_MONTH);

    // Since there is no guarantee that the given day is in the arraylist with
    // the exact same year
    // search for the object based on month and day of month in the list and
    // remove it
    final Iterator <Calendar> iter = m_aExcludeDays.iterator ();
    while (iter.hasNext ())
    {
      final Calendar cl = iter.next ();

      if (dmonth != cl.get (Calendar.MONTH))
        continue;

      if (dday != cl.get (Calendar.DAY_OF_MONTH))
        continue;

      day = cl;
      break;
    }

    m_aExcludeDays.remove (day);
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
    // Test the base calendar first. Only if the base calendar not already
    // excludes the time/date, continue evaluating this calendar instance.
    if (!super.isTimeIncluded (timeStamp))
      return false;

    final Calendar day = createJavaCalendar (timeStamp);
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
    // Call base calendar implementation first
    long timeStamp = nTimeStamp;
    final long baseTime = super.getNextIncludedTime (timeStamp);
    if ((baseTime > 0) && (baseTime > timeStamp))
    {
      timeStamp = baseTime;
    }

    // Get timestamp for 00:00:00
    final Calendar day = getStartOfDayJavaCalendar (timeStamp);
    if (!isDayExcluded (day))
    {
      // return the original value
      return timeStamp;
    }

    while (isDayExcluded (day))
    {
      day.add (Calendar.DATE, 1);
    }

    return day.getTime ().getTime ();
  }

  @Override
  @NonNull
  @ReturnsMutableCopy
  public AnnualCalendar getClone ()
  {
    return new AnnualCalendar (this);
  }
}

class CalendarComparator implements IComparator <Calendar>
{
  public int compare (final Calendar c1, final Calendar c2)
  {
    final int month1 = c1.get (Calendar.MONTH);
    final int month2 = c2.get (Calendar.MONTH);
    if (month1 < month2)
      return -1;
    if (month1 > month2)
      return 1;

    final int day1 = c1.get (Calendar.DAY_OF_MONTH);
    final int day2 = c2.get (Calendar.DAY_OF_MONTH);
    if (day1 < day2)
      return -1;
    if (day1 > day2)
      return 1;

    return 0;
  }
}
