/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2018 Philip Helger (www.helger.com)
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
import java.util.Date;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import com.helger.quartz.ICalendar;

/**
 * <p>
 * This implementation of the Calendar stores a list of holidays (full days that
 * are excluded from scheduling).
 * </p>
 * <p>
 * The implementation DOES take the year into consideration, so if you want to
 * exclude July 4th for the next 10 years, you need to add 10 entries to the
 * exclude list.
 * </p>
 *
 * @author Sharada Jambula
 * @author Juergen Donnerstag
 */
public class HolidayCalendar extends BaseCalendar
{
  // A sorted set to store the holidays
  private TreeSet <Date> dates = new TreeSet <> ();

  public HolidayCalendar ()
  {}

  public HolidayCalendar (final ICalendar baseCalendar)
  {
    super (baseCalendar);
  }

  public HolidayCalendar (final TimeZone timeZone)
  {
    super (timeZone);
  }

  public HolidayCalendar (final ICalendar baseCalendar, final TimeZone timeZone)
  {
    super (baseCalendar, timeZone);
  }

  @Override
  public Object clone ()
  {
    final HolidayCalendar clone = (HolidayCalendar) super.clone ();
    clone.dates = new TreeSet <> (dates);
    return clone;
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
    if (super.isTimeIncluded (timeStamp) == false)
    {
      return false;
    }

    final Date lookFor = getStartOfDayJavaCalendar (timeStamp).getTime ();

    return !(dates.contains (lookFor));
  }

  /**
   * <p>
   * Determine the next time (in milliseconds) that is 'included' by the
   * Calendar after the given time.
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
    while (isTimeIncluded (day.getTime ().getTime ()) == false)
    {
      day.add (Calendar.DATE, 1);
    }

    return day.getTime ().getTime ();
  }

  /**
   * <p>
   * Add the given Date to the list of excluded days. Only the month, day and
   * year of the returned dates are significant.
   * </p>
   */
  public void addExcludedDate (final Date excludedDate)
  {
    final Date date = getStartOfDayJavaCalendar (excludedDate.getTime ()).getTime ();
    /*
     * System.err.println( "HolidayCalendar.add(): date=" +
     * excludedDate.toLocaleString());
     */
    this.dates.add (date);
  }

  public void removeExcludedDate (final Date dateToRemove)
  {
    final Date date = getStartOfDayJavaCalendar (dateToRemove.getTime ()).getTime ();
    dates.remove (date);
  }

  /**
   * <p>
   * Returns a <code>SortedSet</code> of Dates representing the excluded days.
   * Only the month, day and year of the returned dates are significant.
   * </p>
   */
  public SortedSet <Date> getExcludedDates ()
  {
    return Collections.unmodifiableSortedSet (dates);
  }
}
