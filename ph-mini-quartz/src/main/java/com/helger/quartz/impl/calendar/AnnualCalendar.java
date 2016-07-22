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
package com.helger.quartz.impl.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.helger.quartz.ICalendar;

/**
 * <p>
 * This implementation of the Calendar excludes a set of days of the year. You
 * may use it to exclude bank holidays which are on the same date every year.
 * </p>
 *
 * @see com.helger.quartz.ICalendar
 * @see com.helger.quartz.impl.calendar.BaseCalendar
 * @author Juergen Donnerstag
 */
public class AnnualCalendar extends BaseCalendar
{
  private List <Calendar> excludeDays = new ArrayList<> ();

  // true, if excludeDays is sorted
  private boolean dataSorted = false;

  public AnnualCalendar ()
  {}

  public AnnualCalendar (final ICalendar baseCalendar)
  {
    super (baseCalendar);
  }

  public AnnualCalendar (final TimeZone timeZone)
  {
    super (timeZone);
  }

  public AnnualCalendar (final ICalendar baseCalendar, final TimeZone timeZone)
  {
    super (baseCalendar, timeZone);
  }

  @Override
  public Object clone ()
  {
    final AnnualCalendar clone = (AnnualCalendar) super.clone ();
    clone.excludeDays = new ArrayList<> (excludeDays);
    return clone;
  }

  /**
   * <p>
   * Get the array which defines the exclude-value of each day of month
   * </p>
   */
  public List <Calendar> getDaysExcluded ()
  {
    return excludeDays;
  }

  /**
   * <p>
   * Return true, if day is defined to be exluded.
   * </p>
   */
  public boolean isDayExcluded (final Calendar day)
  {

    if (day == null)
    {
      throw new IllegalArgumentException ("Parameter day must not be null");
    }

    // Check baseCalendar first
    if (!super.isTimeIncluded (day.getTime ().getTime ()))
    {
      return true;
    }

    final int dmonth = day.get (Calendar.MONTH);
    final int dday = day.get (Calendar.DAY_OF_MONTH);

    if (dataSorted == false)
    {
      Collections.sort (excludeDays, new CalendarComparator ());
      dataSorted = true;
    }

    final Iterator <Calendar> iter = excludeDays.iterator ();
    while (iter.hasNext ())
    {
      final Calendar cl = iter.next ();

      // remember, the list is sorted
      if (dmonth < cl.get (Calendar.MONTH))
      {
        return false;
      }

      if (dday != cl.get (Calendar.DAY_OF_MONTH))
      {
        continue;
      }

      if (dmonth != cl.get (Calendar.MONTH))
      {
        continue;
      }

      return true;
    }

    return false;
  }

  /**
   * <p>
   * Redefine the list of days excluded. The ArrayList should contain
   * <code>Calendar</code> objects.
   * </p>
   */
  public void setDaysExcluded (final ArrayList <Calendar> days)
  {
    if (days == null)
    {
      excludeDays = new ArrayList<> ();
    }
    else
    {
      excludeDays = days;
    }

    dataSorted = false;
  }

  /**
   * <p>
   * Redefine a certain day to be excluded (true) or included (false).
   * </p>
   */
  public void setDayExcluded (final Calendar day, final boolean exclude)
  {
    if (exclude)
    {
      if (isDayExcluded (day))
      {
        return;
      }

      excludeDays.add (day);
      dataSorted = false;
    }
    else
    {
      if (!isDayExcluded (day))
      {
        return;
      }

      removeExcludedDay (day, true);
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
    removeExcludedDay (day, false);
  }

  private void removeExcludedDay (final Calendar aDay, final boolean isChecked)
  {
    Calendar day = aDay;
    if (!isChecked && !isDayExcluded (day))
      return;

    // Fast way, see if exact day object was already in list
    if (this.excludeDays.remove (day))
      return;

    final int dmonth = day.get (Calendar.MONTH);
    final int dday = day.get (Calendar.DAY_OF_MONTH);

    // Since there is no guarantee that the given day is in the arraylist with
    // the exact same year
    // search for the object based on month and day of month in the list and
    // remove it
    final Iterator <Calendar> iter = excludeDays.iterator ();
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

    this.excludeDays.remove (day);
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
    if (super.isTimeIncluded (timeStamp) == false)
    {
      return false;
    }

    final Calendar day = createJavaCalendar (timeStamp);

    return !(isDayExcluded (day));
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
    if (isDayExcluded (day) == false)
    {
      return timeStamp; // return the original value
    }

    while (isDayExcluded (day) == true)
    {
      day.add (Calendar.DATE, 1);
    }

    return day.getTime ().getTime ();
  }
}

class CalendarComparator implements Comparator <Calendar>, Serializable
{
  public CalendarComparator ()
  {}

  public int compare (final Calendar c1, final Calendar c2)
  {

    final int month1 = c1.get (Calendar.MONTH);
    final int month2 = c2.get (Calendar.MONTH);

    final int day1 = c1.get (Calendar.DAY_OF_MONTH);
    final int day2 = c2.get (Calendar.DAY_OF_MONTH);

    if (month1 < month2)
    {
      return -1;
    }
    if (month1 > month2)
    {
      return 1;
    }
    if (day1 < day2)
    {
      return -1;
    }
    if (day1 > day2)
    {
      return 1;
    }
    return 0;
  }
}