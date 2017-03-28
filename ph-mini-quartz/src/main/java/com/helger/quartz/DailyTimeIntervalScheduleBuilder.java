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
package com.helger.quartz;

import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.annotation.Nonnull;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.ext.CommonsHashSet;
import com.helger.commons.collection.ext.ICommonsSet;
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
public class DailyTimeIntervalScheduleBuilder implements IScheduleBuilder <IDailyTimeIntervalTrigger>
{
  private int m_nInterval = 1;
  private EIntervalUnit m_eIntervalUnit = EIntervalUnit.MINUTE;
  private Set <DayOfWeek> m_aDaysOfWeek;
  private TimeOfDay m_aStartTimeOfDay;
  private TimeOfDay m_aEndTimeOfDay;
  private int m_nRepeatCount = IDailyTimeIntervalTrigger.REPEAT_INDEFINITELY;
  private int m_nMisfireInstruction = ITrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

  /**
   * A set of all days of the week. The set contains all values between
   * {@link Calendar#SUNDAY} and {@link Calendar#SATURDAY}.
   */
  public static final Set <DayOfWeek> ALL_DAYS_OF_THE_WEEK;

  /**
   * A set of the business days of the week (for locales similar to the USA).
   * The set contains all values between {@link Calendar#MONDAY} and
   * {@link Calendar#FRIDAY}.
   */
  public static final Set <DayOfWeek> MONDAY_THROUGH_FRIDAY;

  /**
   * A set of the weekend days of the week (for locales similar to the USA). The
   * set contains {@link Calendar#SATURDAY} and {@link Calendar#SUNDAY}
   */
  public static final Set <DayOfWeek> SATURDAY_AND_SUNDAY;

  static
  {
    ALL_DAYS_OF_THE_WEEK = new CommonsHashSet <> (DayOfWeek.values ()).getAsUnmodifiable ();
    final ICommonsSet <DayOfWeek> t = new CommonsHashSet <> (5);
    for (final DayOfWeek e : DayOfWeek.values ())
      if (e != DayOfWeek.SATURDAY && e != DayOfWeek.SUNDAY)
        t.add (e);
    MONDAY_THROUGH_FRIDAY = t.getAsUnmodifiable ();
    SATURDAY_AND_SUNDAY = new CommonsHashSet <> (DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).getAsUnmodifiable ();
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
  @Override
  public DailyTimeIntervalTrigger build ()
  {
    final DailyTimeIntervalTrigger st = new DailyTimeIntervalTrigger ();
    st.setRepeatInterval (m_nInterval);
    st.setRepeatIntervalUnit (m_eIntervalUnit);
    st.setMisfireInstruction (m_nMisfireInstruction);
    st.setRepeatCount (m_nRepeatCount);

    if (m_aDaysOfWeek != null)
      st.setDaysOfWeek (m_aDaysOfWeek);
    else
      st.setDaysOfWeek (ALL_DAYS_OF_THE_WEEK);

    if (m_aStartTimeOfDay != null)
      st.setStartTimeOfDay (m_aStartTimeOfDay);
    else
      st.setStartTimeOfDay (TimeOfDay.hourAndMinuteOfDay (0, 0));

    if (m_aEndTimeOfDay != null)
      st.setEndTimeOfDay (m_aEndTimeOfDay);
    else
      st.setEndTimeOfDay (TimeOfDay.hourMinuteAndSecondOfDay (23, 59, 59));

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
  public DailyTimeIntervalScheduleBuilder withInterval (final int timeInterval, final EIntervalUnit unit)
  {
    if (unit == null ||
        !(unit.equals (EIntervalUnit.SECOND) || unit.equals (EIntervalUnit.MINUTE) || unit.equals (EIntervalUnit.HOUR)))
      throw new IllegalArgumentException ("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
    _validateInterval (timeInterval);
    this.m_nInterval = timeInterval;
    this.m_eIntervalUnit = unit;
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
  public DailyTimeIntervalScheduleBuilder withIntervalInSeconds (final int intervalInSeconds)
  {
    withInterval (intervalInSeconds, EIntervalUnit.SECOND);
    return this;
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
  public DailyTimeIntervalScheduleBuilder withIntervalInMinutes (final int intervalInMinutes)
  {
    withInterval (intervalInMinutes, EIntervalUnit.MINUTE);
    return this;
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
  public DailyTimeIntervalScheduleBuilder onDaysOfTheWeek (final Set <DayOfWeek> onDaysOfWeek)
  {
    ValueEnforcer.notEmpty (onDaysOfWeek, "OnDaysOfWeek");

    this.m_aDaysOfWeek = onDaysOfWeek;
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
  public DailyTimeIntervalScheduleBuilder onDaysOfTheWeek (final DayOfWeek... onDaysOfWeek)
  {
    return onDaysOfTheWeek (new CommonsHashSet <> (onDaysOfWeek));
  }

  /**
   * Set the trigger to fire on the days from Monday through Friday.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  public DailyTimeIntervalScheduleBuilder onMondayThroughFriday ()
  {
    this.m_aDaysOfWeek = MONDAY_THROUGH_FRIDAY;
    return this;
  }

  /**
   * Set the trigger to fire on the days Saturday and Sunday.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  public DailyTimeIntervalScheduleBuilder onSaturdayAndSunday ()
  {
    this.m_aDaysOfWeek = SATURDAY_AND_SUNDAY;
    return this;
  }

  /**
   * Set the trigger to fire on all days of the week.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  public DailyTimeIntervalScheduleBuilder onEveryDay ()
  {
    this.m_aDaysOfWeek = ALL_DAYS_OF_THE_WEEK;
    return this;
  }

  /**
   * Set the trigger to begin firing each day at the given time.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  public DailyTimeIntervalScheduleBuilder startingDailyAt (final TimeOfDay timeOfDay)
  {
    if (timeOfDay == null)
      throw new IllegalArgumentException ("Start time of day cannot be null!");

    this.m_aStartTimeOfDay = timeOfDay;
    return this;
  }

  /**
   * Set the startTimeOfDay for this trigger to end firing each day at the given
   * time.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  public DailyTimeIntervalScheduleBuilder endingDailyAt (final TimeOfDay timeOfDay)
  {
    this.m_aEndTimeOfDay = timeOfDay;
    return this;
  }

  /**
   * Calculate and set the endTimeOfDay using count, interval and starTimeOfDay.
   * This means that these must be set before this method is call.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   */
  public DailyTimeIntervalScheduleBuilder endingDailyAfterCount (final int count)
  {
    ValueEnforcer.isGT0 (count, "Count");

    if (m_aStartTimeOfDay == null)
      throw new IllegalArgumentException ("You must set the startDailyAt() before calling this endingDailyAfterCount()!");

    final Date today = new Date ();
    final Date startTimeOfDayDate = m_aStartTimeOfDay.getTimeOfDayForDate (today);
    final Date maxEndTimeOfDayDate = TimeOfDay.hourMinuteAndSecondOfDay (23, 59, 59).getTimeOfDayForDate (today);
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

    final Calendar cal = Calendar.getInstance ();
    cal.setTime (endTimeOfDayDate);
    final int hour = cal.get (Calendar.HOUR_OF_DAY);
    final int minute = cal.get (Calendar.MINUTE);
    final int second = cal.get (Calendar.SECOND);

    m_aEndTimeOfDay = TimeOfDay.hourMinuteAndSecondOfDay (hour, minute, second);
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
   */
  public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires ()
  {
    m_nMisfireInstruction = ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link IDailyTimeIntervalTrigger#MISFIRE_INSTRUCTION_DO_NOTHING}
   * instruction.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see IDailyTimeIntervalTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
   */
  public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionDoNothing ()
  {
    m_nMisfireInstruction = IDailyTimeIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link IDailyTimeIntervalTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW}
   * instruction.
   *
   * @return the updated DailyTimeIntervalScheduleBuilder
   * @see IDailyTimeIntervalTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
   */
  public DailyTimeIntervalScheduleBuilder withMisfireHandlingInstructionFireAndProceed ()
  {
    m_nMisfireInstruction = ICalendarIntervalTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
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
  public DailyTimeIntervalScheduleBuilder withRepeatCount (final int repeatCount)
  {
    this.m_nRepeatCount = repeatCount;
    return this;
  }

  private static void _validateInterval (final int timeInterval)
  {
    if (timeInterval <= 0)
      throw new IllegalArgumentException ("Interval must be a positive value.");
  }
}
