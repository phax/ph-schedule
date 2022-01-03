/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2022 Philip Helger (www.helger.com)
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
import java.time.LocalTime;
import java.util.Set;

/**
 * A <code>{@link ITrigger}</code> that is used to fire a
 * <code>{@link com.helger.quartz.IJobDetail}</code> based upon daily repeating
 * time intervals.
 * <p>
 * The trigger will fire every N (see {@link #getRepeatInterval()} ) seconds,
 * minutes or hours (see {@link #getRepeatIntervalUnit()}) during a given time
 * window on specified days of the week.
 * </p>
 * <p>
 * For example#1, a trigger can be set to fire every 72 minutes between 8:00 and
 * 11:00 everyday. It's fire times would be 8:00, 9:12, 10:24, then next day
 * would repeat: 8:00, 9:12, 10:24 again.
 * </p>
 * <p>
 * For example#2, a trigger can be set to fire every 23 minutes between 9:20 and
 * 16:47 Monday through Friday.
 * </p>
 * <p>
 * On each day, the starting fire time is reset to startTimeOfDay value, and
 * then it will add repeatInterval value to it until the endTimeOfDay is
 * reached. If you set daysOfWeek values, then fire time will only occur during
 * those week days period.
 * </p>
 * <p>
 * The default values for fields if not set are: startTimeOfDay defaults to
 * 00:00:00, the endTimeOfDay default to 23:59:59, and daysOfWeek is default to
 * every day. The startTime default to current time-stamp now, while endTime has
 * not value.
 * </p>
 * <p>
 * If startTime is before startTimeOfDay, then it has no affect. Else if
 * startTime after startTimeOfDay, then the first fire time for that day will be
 * normal startTimeOfDay incremental values after startTime value. Same reversal
 * logic is applied to endTime with endTimeOfDay.
 * </p>
 *
 * @see DailyTimeIntervalScheduleBuilder
 * @since 2.1.0
 * @author James House
 * @author Zemian Deng saltnlight5@gmail.com
 */
public interface IDailyTimeIntervalTrigger extends ITrigger
{
  /**
   * Used to indicate the 'repeat count' of the trigger is indefinite. Or in
   * other words, the trigger should repeat continually until the trigger's
   * ending timestamp.
   */
  int REPEAT_INDEFINITELY = -1;

  /**
   * Get the interval unit - the time unit on with the interval applies. <br>
   * The only intervals that are valid for this type of trigger are
   * {@link EIntervalUnit#SECOND}, {@link EIntervalUnit#MINUTE}, and
   * {@link EIntervalUnit#HOUR}.
   */
  EIntervalUnit getRepeatIntervalUnit ();

  /**
   * Get the the number of times for interval this trigger should repeat, after
   * which it will be automatically deleted.
   *
   * @see #REPEAT_INDEFINITELY
   */
  int getRepeatCount ();

  /**
   * Get the the time interval that will be added to the
   * <code>DateIntervalTrigger</code>'s fire time (in the set repeat interval
   * unit) in order to calculate the time of the next trigger repeat.
   */
  int getRepeatInterval ();

  /**
   * The time of day to start firing at the given interval.
   */
  LocalTime getStartTimeOfDay ();

  /**
   * The time of day to complete firing at the given interval.
   */
  LocalTime getEndTimeOfDay ();

  /**
   * The days of the week upon which to fire.
   *
   * @return a Set containing the enums representing the days of the week.
   */
  Set <DayOfWeek> getDaysOfWeek ();

  /**
   * Get the number of times the <code>DateIntervalTrigger</code> has already
   * fired.
   */
  int getTimesTriggered ();

  TriggerBuilder <? extends IDailyTimeIntervalTrigger> getTriggerBuilder ();
}
