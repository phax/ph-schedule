/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
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
public abstract class AbstractCalendar implements ICalendar
{
  // A optional base calendar
  private ICalendar m_aBaseCalendar;
  private String m_sDescription;
  private TimeZone m_aTimeZone;

  /**
   * Copy constructor
   *
   * @param aOther
   *        Calendar to copy from. May not be <code>null</code>.
   */
  protected AbstractCalendar (@Nonnull final AbstractCalendar aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    m_aBaseCalendar = aOther.m_aBaseCalendar;
    m_sDescription = aOther.m_sDescription;
    m_aTimeZone = aOther.m_aTimeZone;
  }

  /**
   * @param timeZone
   *        The time zone to use for this Calendar, <code>null</code> if
   *        <code>{@link TimeZone#getDefault()}</code> should be used
   */
  public AbstractCalendar (@Nullable final ICalendar baseCalendar, @Nullable final TimeZone timeZone)
  {
    setBaseCalendar (baseCalendar);
    m_aTimeZone = timeZone;
  }

  public final ICalendar getBaseCalendar ()
  {
    return m_aBaseCalendar;
  }

  public final void setBaseCalendar (@Nullable final ICalendar baseCalendar)
  {
    m_aBaseCalendar = baseCalendar;
  }

  public final String getDescription ()
  {
    return m_sDescription;
  }

  public final void setDescription (@Nullable final String description)
  {
    m_sDescription = description;
  }

  @Nullable
  public TimeZone getTimeZone ()
  {
    return m_aTimeZone;
  }

  public void setTimeZone (@Nullable final TimeZone timeZone)
  {
    m_aTimeZone = timeZone;
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
      throw new IllegalArgumentException ("timeStamp must be greater 0");

    return m_aBaseCalendar == null || m_aBaseCalendar.isTimeIncluded (timeStamp);
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

    if (m_aBaseCalendar != null)
      return m_aBaseCalendar.getNextIncludedTime (timeStamp);

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
