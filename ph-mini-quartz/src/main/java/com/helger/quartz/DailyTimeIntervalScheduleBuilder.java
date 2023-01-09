/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2023 Philip Helger (www.helger.com)
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.datetime.PDTFactory;
import com.helger.quartz.ITrigger.EMisfireInstruction;
import com.helger.quartz.impl.triggers.DailyTimeIntervalTrigger;

/**
 * A {@link IScheduleBuilder} implementation that build schedule for
 * DailyTimeIntervalTrigger.
 * <p>
 * This builder provide an extra convenient method for you to set the trigger's
 * endTimeOfDay. You may use either endingDailyAt() or endingDailyAfterCount()
 * to set the value. The later will auto calculate your endTimeOfDay by using
 * the interval, intervalUnit and startTimeOfDay to perform the calculation.
 * <p>
 * When using endingDailyAfterCount(), you should note that it is used to
 * calculating endTimeOfDay. So if your startTime on the first day is already
 * pass by a time that would not add up to the count you expected, until the
 * next day comes. Remember that DailyTimeIntervalTrigger will use
 * startTimeOfDay and endTimeOfDay as fresh per each day!
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related
 * entities via a Domain-Specific Language (DSL). The DSL can best be utilized
 * through the usage of static imports of the methods on the classes
 * <code>TriggerBuilder</code>, <code>JobBuilder</code>,
 * <code>DateBuilder</code>, <code>JobKey</code>, <code>TriggerKey</code> and
 * the various <code>ScheduleBuilder</code> implementations.
 * </p>
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 *
 * <pre>
 * JobDetail job = newJob (MyJob.class).withIdentity ("myJob").build ();
 * Trigger trigger = newTrigger ().withIdentity (triggerKey ("myTrigger", "myTriggerGroup"))
 *                                .withSchedule (onDaysOfTheWeek (MONDAY, THURSDAY))
 *                                .startAt (futureDate (10, MINUTES))
 *                                .build ();
 * scheduler.scheduleJob (job, trigger);
 * </pre>
 *
 * @since 2.1.0
 * @author James House
 * @author Zemian Deng saltnlight5@gmail.com
 */
public class DailyTimeIntervalScheduleBuilder implements IScheduleBuilder <DailyTimeIntervalTrigger>
{
  private int m_nInterval = 1;
  private EIntervalUnit m_eIntervalUnit = EIntervalUnit.MINUTE;
  private EnumSet <DayOfWeek> m_aDaysOfWeek;
  private LocalTime m_aStartTimeOfDay;
  private LocalTime m_aEndTimeOfDay;
  private int m_nRepeatCount = IDailyTimeIntervalTrigger.REPEAT_INDEFINITELY;
  private EMisfireInstruction m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY;

  /**
   * A set of all days of the week. The set contains all values between
   * {@link Calendar#SUNDAY} and {@link Calendar#SATURDAY}.
   */
  private static final EnumSet <DayOfWeek> ALL_DAYS_OF_THE_WEEK;

  /**
   * A set of the business days of the week (for locales similar to the USA).
   * The set contains all values between {@link Calendar#MONDAY} and
   * {@link Calendar#FRIDAY}.
   */
  private static final EnumSet <DayOfWeek> MONDAY_THROUGH_FRIDAY;

  /**
   * A set of the weekend days of the week (for locales similar to the USA). The
   * set contains {@link Calendar#SATURDAY} and {@link Calendar#SUNDAY}
   */
  private static final EnumSet <DayOfWeek> SATURDAY_AND_SUNDAY;

  static
  {
    ALL_DAYS_OF_THE_WEEK = EnumSet.allOf (DayOfWeek.class);
    MONDAY_THROUGH_FRIDAY = EnumSet.of (DayOfWeek.MONDAY,
                                        DayOfWeek.TUESDAY,
                                        DayOfWeek.WEDNESDAY,
                                        DayOfWeek.THURSDAY,
                                        DayOfWeek.FRIDAY);
    SATURDAY_AND_SUNDAY = EnumSet.of (DayOfWeek.SUNDAY, DayOfWeek.SATURDAY);
  }

  protected DailyTimeIntervalScheduleBuilder ()
  {}

  /**
   * Create a DailyTimeIntervalScheduleBuilder.
   *
   * @return the new DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public static DailyTimeIntervalScheduleBuilder dailyTimeIntervalSchedule ()
  {
    return new DailyTimeIntervalScheduleBuilder ();
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but
   * will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   *
   * @see TriggerBuilder#withSchedule(IScheduleBuilder)
   */
  @Nonnull
  public DailyTimeIntervalTrigger build ()
  {
    final DailyTimeIntervalTrigger st = new DailyTimeIntervalTrigger ();
    st.setRepeatInterval (m_nInterval);
    st.setRepeatIntervalUnit (m_eIntervalUnit);
    st.setMisfireInstruction (m_eMisfireInstruction);
    st.setRepeatCount (m_nRepeatCount);

    if (m_aDaysOfWeek != null)
      st.setDaysOfWeek (m_aDaysOfWeek);
    else
      st.setDaysOfWeek (EnumSet.copyOf (ALL_DAYS_OF_THE_WEEK));

    if (m_aStartTimeOfDay != null)
      st.setStartTimeOfDay (m_aStartTimeOfDay);
    else
      st.setStartTimeOfDay (LocalTime.MIDNIGHT);

    if (m_aEndTimeOfDay != null)
      st.setEndTimeOfDay (m_aEndTimeOfDay);
    else
      st.setEndTimeOfDay (PDTFactory.createLocalTime (23, 59, 59));

    return st;
  }

  /**
   * Specify the time unit and interval for the Trigger to be produced.
   *
   * @param timeInterval
   *        the interval at which the trigger should repeat.
   * @param unit
   *        the time unit (IntervalUnit) of the interval. The only intervals
   *        that are valid for this type of trigger are
   *        {@link EIntervalUnit#SECOND}, {@link EIntervalUnit#MINUTE}, and
   *        {@link EIntervalUnit#HOUR}.
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see IDailyTimeIntervalTrigger#getRepeatInterval()
   * @see IDailyTimeIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withInterval (final int timeInterval, @Nonnull final EIntervalUnit unit)
  {
    if (unit == null ||
        !(unit.equals (EIntervalUnit.SECOND) || unit.equals (EIntervalUnit.MINUTE) || unit.equals (EIntervalUnit.HOUR)))
      throw new IllegalArgumentException ("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
    _validateInterval (timeInterval);
    m_nInterval = timeInterval;
    m_eIntervalUnit = unit;
    return this;
  }

  /**
   * Specify an interval in the IntervalUnit.SECOND that the produced Trigger
   * will repeat at.
   *
   * @param intervalInSeconds
   *        the number of seconds at which the trigger should repeat.
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see IDailyTimeIntervalTrigger#getRepeatInterval()
   * @see IDailyTimeIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withIntervalInSeconds (final int intervalInSeconds)
  {
    return withInterval (intervalInSeconds, EIntervalUnit.SECOND);
  }

  /**
   * Specify an interval in the IntervalUnit.MINUTE that the produced Trigger
   * will repeat at.
   *
   * @param intervalInMinutes
   *        the number of minutes at which the trigger should repeat.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see IDailyTimeIntervalTrigger#getRepeatInterval()
   * @see IDailyTimeIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withIntervalInMinutes (final int intervalInMinutes)
  {
    return withInterval (intervalInMinutes, EIntervalUnit.MINUTE);
  }

  /**
   * Specify an interval in the IntervalUnit.HOUR that the produced Trigger will
   * repeat at.
   *
   * @param intervalInHours
   *        the number of hours at which the trigger should repeat.
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see IDailyTimeIntervalTrigger#getRepeatInterval()
   * @see IDailyTimeIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withIntervalInHours (final int intervalInHours)
  {
    withInterval (intervalInHours, EIntervalUnit.HOUR);
    return this;
  }

  /**
   * Set the trigger to fire on the given days of the week.
   *
   * @param onDaysOfWeek
   *        a Set containing the integers representing the days of the week, per
   *        the values 1-7 as defined by {@link Calendar#SUNDAY} -
   *        {@link Calendar#SATURDAY}.
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder onDaysOfTheWeek (final Set <DayOfWeek> onDaysOfWeek)
  {
    ValueEnforcer.notEmpty (onDaysOfWeek, "OnDaysOfWeek");

    m_aDaysOfWeek = EnumSet.copyOf (onDaysOfWeek);
    return this;
  }

  /**
   * Set the trigger to fire on the given days of the week.
   *
   * @param onDaysOfWeek
   *        a variable length list of Integers representing the days of the
   *        week, per the values 1-7 as defined by {@link Calendar#SUNDAY} -
   *        {@link Calendar#SATURDAY}.
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder onDaysOfTheWeek (final DayOfWeek... onDaysOfWeek)
  {
    final EnumSet <DayOfWeek> aSet = EnumSet.noneOf (DayOfWeek.class);
    Collections.addAll (aSet, onDaysOfWeek);
    return onDaysOfTheWeek (aSet);
  }

  /**
   * Set the trigger to fire on the days from Monday through Friday.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder onMondayThroughFriday ()
  {
    m_aDaysOfWeek = EnumSet.copyOf (MONDAY_THROUGH_FRIDAY);
    return this;
  }

  /**
   * Set the trigger to fire on the days Saturday and Sunday.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder onSaturdayAndSunday ()
  {
    m_aDaysOfWeek = EnumSet.copyOf (SATURDAY_AND_SUNDAY);
    return this;
  }

  /**
   * Set the trigger to fire on all days of the week.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder onEveryDay ()
  {
    m_aDaysOfWeek = EnumSet.copyOf (ALL_DAYS_OF_THE_WEEK);
    return this;
  }

  /**
   * Set the trigger to begin firing each day at the given time.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder startingDailyAt (final LocalTime timeOfDay)
  {
    if (timeOfDay == null)
      throw new IllegalArgumentException ("Start time of day cannot be null!");

    m_aStartTimeOfDay = timeOfDay;
    return this;
  }

  /**
   * Set the startTimeOfDay for this trigger to end firing each day at the given
   * time.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder endingDailyAt (final LocalTime timeOfDay)
  {
    m_aEndTimeOfDay = timeOfDay;
    return this;
  }

  /**
   * Calculate and set the endTimeOfDay using count, interval and starTimeOfDay.
   * This means that these must be set before this method is call.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder endingDailyAfterCount (final int count)
  {
    ValueEnforcer.isGT0 (count, "Count");

    if (m_aStartTimeOfDay == null)
      throw new IllegalArgumentException ("You must set the startDailyAt() before calling this endingDailyAfterCount()!");

    final Date today = new Date ();
    final Date startTimeOfDayDate = CQuartz.onDate (m_aStartTimeOfDay, today);
    final Date maxEndTimeOfDayDate = CQuartz.onDate (PDTFactory.createLocalTime (23, 59, 59), today);
    final long remainingMillisInDay = maxEndTimeOfDayDate.getTime () - startTimeOfDayDate.getTime ();
    long intervalInMillis;
    if (m_eIntervalUnit == EIntervalUnit.SECOND)
      intervalInMillis = m_nInterval * CGlobal.MILLISECONDS_PER_SECOND;
    else
      if (m_eIntervalUnit == EIntervalUnit.MINUTE)
        intervalInMillis = m_nInterval * CGlobal.MILLISECONDS_PER_MINUTE;
      else
        if (m_eIntervalUnit == EIntervalUnit.HOUR)
          intervalInMillis = m_nInterval * DateBuilder.MILLISECONDS_IN_DAY;
        else
          throw new IllegalArgumentException ("The IntervalUnit: " + m_eIntervalUnit + " is invalid for this trigger.");

    if (remainingMillisInDay - intervalInMillis <= 0)
      throw new IllegalArgumentException ("The startTimeOfDay is too late with given Interval and IntervalUnit values.");

    final long maxNumOfCount = (remainingMillisInDay / intervalInMillis);
    if (count > maxNumOfCount)
      throw new IllegalArgumentException ("The given count " +
                                          count +
                                          " is too large! The max you can set is " +
                                          maxNumOfCount);

    final long incrementInMillis = (count - 1) * intervalInMillis;
    final Date endTimeOfDayDate = new Date (startTimeOfDayDate.getTime () + incrementInMillis);

    if (endTimeOfDayDate.getTime () > maxEndTimeOfDayDate.getTime ())
      throw new IllegalArgumentException ("The given count " +
                                          count +
                                          " is too large! The max you can set is " +
                                          maxNumOfCount);

    final Calendar cal = PDTFactory.createCalendar ();
    cal.setTime (endTimeOfDayDate);
    final int hour = cal.get (Calendar.HOUR_OF_DAY);
    final int minute = cal.get (Calendar.MINUTE);
    final int second = cal.get (Calendar.SECOND);

    m_aEndTimeOfDay = PDTFactory.createLocalTime (hour, minute, second);
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link EMisfireInstruction#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY}
   * instruction.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see EMisfireInstruction#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires ()
  {
    m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link EMisfireInstruction#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see EMisfireInstruction#MISFIRE_INSTRUCTION_DO_NOTHING
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionDoNothing ()
  {
    m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_DO_NOTHING;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link EMisfireInstruction#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see EMisfireInstruction#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionFireAndProceed ()
  {
    m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    return this;
  }

  /**
   * Set number of times for interval to repeat.
   * <p>
   * Note: if you want total count = 1 (at start time) + repeatCount
   * </p>
   *
   * @return the new DailyTimeIntervalScheduleBuilder
   */
  @Nonnull
  public DailyTimeIntervalScheduleBuilder withRepeatCount (final int nRepeatCount)
  {
    m_nRepeatCount = nRepeatCount;
    return this;
  }

  private static void _validateInterval (final int timeInterval)
  {
    if (timeInterval <= 0)
      throw new IllegalArgumentException ("Interval must be a positive value.");
  }
}
