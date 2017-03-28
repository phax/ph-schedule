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
package com.helger.quartz.impl.calendar;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.helger.quartz.ICalendar;

/**
 * <p>
 * This implementation of the Calendar may be used (you don't have to) as a base
 * class for more sophisticated one's. It merely implements the base
 * functionality required by each Calendar.
 * </p>
 * <p>
 * Regarded as base functionality is the treatment of base calendars. Base
 * calendar allow you to chain (stack) as much calendars as you may need. For
 * example to exclude weekends you may use WeeklyCalendar. In order to exclude
 * holidays as well you may define a WeeklyCalendar instance to be the base
 * calendar for HolidayCalendar instance.
 * </p>
 *
 * @see com.helger.quartz.ICalendar
 * @author Juergen Donnerstag
 * @author James House
 */
public class BaseCalendar implements ICalendar
{
  // <p>A optional base calendar.</p>
  private ICalendar baseCalendar;

  private String description;

  private TimeZone timeZone;

  public BaseCalendar ()
  {}

  public BaseCalendar (final ICalendar baseCalendar)
  {
    setBaseCalendar (baseCalendar);
  }

  /**
   * @param timeZone
   *        The time zone to use for this Calendar, <code>null</code> if
   *        <code>{@link TimeZone#getDefault()}</code> should be used
   */
  public BaseCalendar (final TimeZone timeZone)
  {
    setTimeZone (timeZone);
  }

  /**
   * @param timeZone
   *        The time zone to use for this Calendar, <code>null</code> if
   *        <code>{@link TimeZone#getDefault()}</code> should be used
   */
  public BaseCalendar (final ICalendar baseCalendar, final TimeZone timeZone)
  {
    setBaseCalendar (baseCalendar);
    setTimeZone (timeZone);
  }

  @Override
  public Object clone ()
  {
    try
    {
      final BaseCalendar clone = (BaseCalendar) super.clone ();
      if (getBaseCalendar () != null)
      {
        clone.baseCalendar = (ICalendar) getBaseCalendar ().clone ();
      }
      if (getTimeZone () != null)
        clone.timeZone = (TimeZone) getTimeZone ().clone ();
      return clone;
    }
    catch (final CloneNotSupportedException ex)
    {
      throw new IncompatibleClassChangeError ("Not Cloneable.");
    }
  }

  /**
   * <p>
   * Set a new base calendar or remove the existing one
   * </p>
   */
  public void setBaseCalendar (final ICalendar baseCalendar)
  {
    this.baseCalendar = baseCalendar;
  }

  /**
   * <p>
   * Get the base calendar. Will be null, if not set.
   * </p>
   */
  public ICalendar getBaseCalendar ()
  {
    return this.baseCalendar;
  }

  /**
   * <p>
   * Return the description given to the <code>Calendar</code> instance by its
   * creator (if any).
   * </p>
   *
   * @return null if no description was set.
   */
  public String getDescription ()
  {
    return description;
  }

  /**
   * <p>
   * Set a description for the <code>Calendar</code> instance - may be useful
   * for remembering/displaying the purpose of the calendar, though the
   * description has no meaning to Quartz.
   * </p>
   */
  public void setDescription (final String description)
  {
    this.description = description;
  }

  /**
   * Returns the time zone for which this <code>Calendar</code> will be
   * resolved.
   *
   * @return This Calendar's timezone, <code>null</code> if Calendar should use
   *         the <code>{@link TimeZone#getDefault()}</code>
   */
  public TimeZone getTimeZone ()
  {
    return timeZone;
  }

  /**
   * Sets the time zone for which this <code>Calendar</code> will be resolved.
   *
   * @param timeZone
   *        The time zone to use for this Calendar, <code>null</code> if
   *        <code>{@link TimeZone#getDefault()}</code> should be used
   */
  public void setTimeZone (final TimeZone timeZone)
  {
    this.timeZone = timeZone;
  }

  /**
   * <p>
   * Check if date/time represented by timeStamp is included. If included return
   * true. The implementation of BaseCalendar simply calls the base calendars
   * isTimeIncluded() method if base calendar is set.
   * </p>
   *
   * @see com.helger.quartz.ICalendar#isTimeIncluded(long)
   */
  public boolean isTimeIncluded (final long timeStamp)
  {

    if (timeStamp <= 0)
    {
      throw new IllegalArgumentException ("timeStamp must be greater 0");
    }

    if (baseCalendar != null)
    {
      if (baseCalendar.isTimeIncluded (timeStamp) == false)
      {
        return false;
      }
    }

    return true;
  }

  /**
   * <p>
   * Determine the next time (in milliseconds) that is 'included' by the
   * Calendar after the given time. Return the original value if timeStamp is
   * included. Return 0 if all days are excluded.
   * </p>
   *
   * @see com.helger.quartz.ICalendar#getNextIncludedTime(long)
   */
  public long getNextIncludedTime (final long timeStamp)
  {
    if (timeStamp <= 0)
      throw new IllegalArgumentException ("timeStamp must be greater 0");

    if (baseCalendar != null)
    {
      return baseCalendar.getNextIncludedTime (timeStamp);
    }

    return timeStamp;
  }

  /**
   * Build a <code>{@link Calendar}</code> for the given timeStamp. The new
   * Calendar will use the <code>BaseCalendar</code> time zone if it is not
   * <code>null</code>.
   */
  protected Calendar createJavaCalendar (final long timeStamp)
  {
    final Calendar calendar = createJavaCalendar ();
    calendar.setTime (new Date (timeStamp));
    return calendar;
  }

  /**
   * Build a <code>{@link Calendar}</code> with the current time. The new
   * Calendar will use the <code>BaseCalendar</code> time zone if it is not
   * <code>null</code>.
   */
  protected Calendar createJavaCalendar ()
  {
    return Calendar.getInstance (getTimeZone () != null ? getTimeZone () : TimeZone.getDefault (),
                                 Locale.getDefault (Locale.Category.FORMAT));
  }

  /**
   * Returns the start of the given day as a <code>{@link Calendar}</code>. This
   * calculation will take the <code>BaseCalendar</code> time zone into account
   * if it is not <code>null</code>.
   *
   * @param timeInMillis
   *        A time containing the desired date for the start-of-day time
   * @return A <code>{@link Calendar}</code> set to the start of the given day.
   */
  protected Calendar getStartOfDayJavaCalendar (final long timeInMillis)
  {
    final Calendar startOfDay = createJavaCalendar (timeInMillis);
    startOfDay.set (Calendar.HOUR_OF_DAY, 0);
    startOfDay.set (Calendar.MINUTE, 0);
    startOfDay.set (Calendar.SECOND, 0);
    startOfDay.set (Calendar.MILLISECOND, 0);
    return startOfDay;
  }

  /**
   * Returns the end of the given day <code>{@link Calendar}</code>. This
   * calculation will take the <code>BaseCalendar</code> time zone into account
   * if it is not <code>null</code>.
   *
   * @param timeInMillis
   *        a time containing the desired date for the end-of-day time.
   * @return A <code>{@link Calendar}</code> set to the end of the given day.
   */
  protected Calendar getEndOfDayJavaCalendar (final long timeInMillis)
  {
    final Calendar endOfDay = createJavaCalendar (timeInMillis);
    endOfDay.set (Calendar.HOUR_OF_DAY, 23);
    endOfDay.set (Calendar.MINUTE, 59);
    endOfDay.set (Calendar.SECOND, 59);
    endOfDay.set (Calendar.MILLISECOND, 999);
    return endOfDay;
  }
}
