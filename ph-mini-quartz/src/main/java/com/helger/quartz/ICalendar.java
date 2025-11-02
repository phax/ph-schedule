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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An interface to be implemented by objects that define spaces of time during
 * which an associated <code>{@link ITrigger}</code> may (not) fire. Calendars
 * do not define actual fire times, but rather are used to limit a
 * <code>Trigger</code> from firing on its normal schedule if necessary. Most
 * Calendars include all times by default and allow the user to specify times to
 * exclude.
 * <p>
 * As such, it is often useful to think of Calendars as being used to
 * <I>exclude</I> a block of time - as opposed to <I>include</I> a block of
 * time. (i.e. the schedule &quot;fire every five minutes except on
 * Sundays&quot; could be implemented with a <code>SimpleTrigger</code> and a
 * <code>WeeklyCalendar</code> which excludes Sundays)
 * </p>
 *
 * @author James House
 * @author Juergen Donnerstag
 */
public interface ICalendar
{
  int MONTH = 0;

  /**
   * Get the base calendar. Will be <code>null</code>, if not set.
   */
  @Nullable
  ICalendar getBaseCalendar ();

  /**
   * Set a new base calendar or remove the existing one.
   *
   * @param baseCalendar
   *        The new base calendar. May be <code>null</code>.
   */
  void setBaseCalendar (@Nullable ICalendar baseCalendar);

  /**
   * Return the description given to the <code>Calendar</code> instance by its
   * creator (if any).
   *
   * @return <code>null</code> if no description was set.
   */
  @Nullable
  String getDescription ();

  /**
   * Set a description for the <code>Calendar</code> instance - may be useful
   * for remembering/displaying the purpose of the calendar, though the
   * description has no meaning to Quartz.
   *
   * @param description
   *        The new description. May be <code>null</code>.
   */
  void setDescription (@Nullable String description);

  /**
   * Determine whether the given time (in milliseconds) is 'included' by the
   * Calendar.
   */
  boolean isTimeIncluded (long timeStamp);

  /**
   * Determine the next time (in milliseconds) that is 'included' by the
   * Calendar after the given time.
   */
  long getNextIncludedTime (long timeStamp);

  @NonNull
  ICalendar getClone ();
}
