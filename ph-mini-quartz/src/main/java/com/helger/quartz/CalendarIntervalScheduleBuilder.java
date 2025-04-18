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

import java.util.TimeZone;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.quartz.ITrigger.EMisfireInstruction;
import com.helger.quartz.impl.triggers.CalendarIntervalTrigger;

/**
 * <code>CalendarIntervalScheduleBuilder</code> is a {@link IScheduleBuilder}
 * that defines calendar time (day, week, month, year) interval-based schedules
 * for <code>Trigger</code>s.
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
 *                                .withSchedule (withIntervalInDays (3))
 *                                .startAt (futureDate (10, MINUTES))
 *                                .build ();
 * scheduler.scheduleJob (job, trigger);
 * </pre>
 *
 * @see DailyTimeIntervalScheduleBuilder
 * @see CronScheduleBuilder
 * @see IScheduleBuilder
 * @see SimpleScheduleBuilder
 * @see TriggerBuilder
 */
public class CalendarIntervalScheduleBuilder implements IScheduleBuilder <CalendarIntervalTrigger>
{
  private int m_nInterval = 1;
  private EIntervalUnit m_eIntervalUnit = EIntervalUnit.DAY;

  private EMisfireInstruction m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY;
  private TimeZone m_aTimeZone;
  private boolean m_bPreserveHourOfDayAcrossDaylightSavings;
  private boolean m_bSkipDayIfHourDoesNotExist;

  protected CalendarIntervalScheduleBuilder ()
  {}

  /**
   * Create a CalendarIntervalScheduleBuilder.
   *
   * @return the new CalendarIntervalScheduleBuilder
   */
  @Nonnull
  public static CalendarIntervalScheduleBuilder calendarIntervalSchedule ()
  {
    return new CalendarIntervalScheduleBuilder ();
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but
   * will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   *
   * @see TriggerBuilder#withSchedule(IScheduleBuilder)
   */
  @Nonnull
  public CalendarIntervalTrigger build ()
  {
    final CalendarIntervalTrigger st = new CalendarIntervalTrigger ();
    st.setRepeatInterval (m_nInterval);
    st.setRepeatIntervalUnit (m_eIntervalUnit);
    st.setMisfireInstruction (m_eMisfireInstruction);
    st.setTimeZone (m_aTimeZone);
    st.setPreserveHourOfDayAcrossDaylightSavings (m_bPreserveHourOfDayAcrossDaylightSavings);
    st.setSkipDayIfHourDoesNotExist (m_bSkipDayIfHourDoesNotExist);
    return st;
  }

  /**
   * Specify the time unit and interval for the Trigger to be produced.
   *
   * @param timeInterval
   *        the interval at which the trigger should repeat.
   * @param unit
   *        the time unit (IntervalUnit) of the interval.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withInterval (final int timeInterval, @Nonnull final EIntervalUnit unit)
  {
    ValueEnforcer.notNull (unit, "Unit");
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
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withIntervalInSeconds (final int intervalInSeconds)
  {
    _validateInterval (intervalInSeconds);
    m_nInterval = intervalInSeconds;
    m_eIntervalUnit = EIntervalUnit.SECOND;
    return this;
  }

  /**
   * Specify an interval in the IntervalUnit.MINUTE that the produced Trigger
   * will repeat at.
   *
   * @param intervalInMinutes
   *        the number of minutes at which the trigger should repeat.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withIntervalInMinutes (final int intervalInMinutes)
  {
    _validateInterval (intervalInMinutes);
    m_nInterval = intervalInMinutes;
    m_eIntervalUnit = EIntervalUnit.MINUTE;
    return this;
  }

  /**
   * Specify an interval in the IntervalUnit.HOUR that the produced Trigger will
   * repeat at.
   *
   * @param intervalInHours
   *        the number of hours at which the trigger should repeat.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withIntervalInHours (final int intervalInHours)
  {
    _validateInterval (intervalInHours);
    m_nInterval = intervalInHours;
    m_eIntervalUnit = EIntervalUnit.HOUR;
    return this;
  }

  /**
   * Specify an interval in the IntervalUnit.DAY that the produced Trigger will
   * repeat at.
   *
   * @param intervalInDays
   *        the number of days at which the trigger should repeat.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withIntervalInDays (final int intervalInDays)
  {
    _validateInterval (intervalInDays);
    m_nInterval = intervalInDays;
    m_eIntervalUnit = EIntervalUnit.DAY;
    return this;
  }

  /**
   * Specify an interval in the IntervalUnit.WEEK that the produced Trigger will
   * repeat at.
   *
   * @param intervalInWeeks
   *        the number of weeks at which the trigger should repeat.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withIntervalInWeeks (final int intervalInWeeks)
  {
    _validateInterval (intervalInWeeks);
    m_nInterval = intervalInWeeks;
    m_eIntervalUnit = EIntervalUnit.WEEK;
    return this;
  }

  /**
   * Specify an interval in the IntervalUnit.MONTH that the produced Trigger
   * will repeat at.
   *
   * @param intervalInMonths
   *        the number of months at which the trigger should repeat.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withIntervalInMonths (final int intervalInMonths)
  {
    _validateInterval (intervalInMonths);
    m_nInterval = intervalInMonths;
    m_eIntervalUnit = EIntervalUnit.MONTH;
    return this;
  }

  /**
   * Specify an interval in the IntervalUnit.YEAR that the produced Trigger will
   * repeat at.
   *
   * @param intervalInYears
   *        the number of years at which the trigger should repeat.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getRepeatInterval()
   * @see ICalendarIntervalTrigger#getRepeatIntervalUnit()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withIntervalInYears (final int intervalInYears)
  {
    _validateInterval (intervalInYears);
    m_nInterval = intervalInYears;
    m_eIntervalUnit = EIntervalUnit.YEAR;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link EMisfireInstruction#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY}
   * instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see EMisfireInstruction#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires ()
  {
    m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link EMisfireInstruction#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
   *
   * @return the updated CalendarIntervalScheduleBuilder
   * @see EMisfireInstruction#MISFIRE_INSTRUCTION_DO_NOTHING
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionDoNothing ()
  {
    m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_DO_NOTHING;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link EMisfireInstruction#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
   *
   * @return the updated CalendarIntervalScheduleBuilder
   * @see EMisfireInstruction#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder withMisfireHandlingInstructionFireAndProceed ()
  {
    m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    return this;
  }

  /**
   * The <code>TimeZone</code> in which to base the schedule.
   *
   * @param timezone
   *        the time-zone for the schedule.
   * @return the updated CalendarIntervalScheduleBuilder
   * @see ICalendarIntervalTrigger#getTimeZone()
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder inTimeZone (final TimeZone timezone)
  {
    m_aTimeZone = timezone;
    return this;
  }

  /**
   * If intervals are a day or greater, this property (set to true) will cause
   * the firing of the trigger to always occur at the same time of day, (the
   * time of day of the startTime) regardless of daylight saving time
   * transitions. Default value is false.
   * <p>
   * For example, without the property set, your trigger may have a start time
   * of 9:00 am on March 1st, and a repeat interval of 2 days. But after the
   * daylight saving transition occurs, the trigger may start firing at 8:00 am
   * every other day.
   * </p>
   * <p>
   * If however, the time of day does not exist on a given day to fire (e.g.
   * 2:00 am in the United States on the days of daylight saving transition),
   * the trigger will go ahead and fire one hour off on that day, and then
   * resume the normal hour on other days. If you wish for the trigger to never
   * fire at the "wrong" hour, then you should set the property
   * skipDayIfHourDoesNotExist.
   * </p>
   *
   * @param bPreserveHourOfDay
   *        <code>true</code> to enable
   * @return this for chaining
   * @see #skipDayIfHourDoesNotExist(boolean)
   * @see #inTimeZone(TimeZone)
   * @see TriggerBuilder#startAt(java.util.Date)
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder preserveHourOfDayAcrossDaylightSavings (final boolean bPreserveHourOfDay)
  {
    m_bPreserveHourOfDayAcrossDaylightSavings = bPreserveHourOfDay;
    return this;
  }

  /**
   * If intervals are a day or greater, and
   * preserveHourOfDayAcrossDaylightSavings property is set to true, and the
   * hour of the day does not exist on a given day for which the trigger would
   * fire, the day will be skipped and the trigger advanced a second interval if
   * this property is set to true. Defaults to false.
   * <p>
   * <b>CAUTION!</b> If you enable this property, and your hour of day happens
   * to be that of daylight savings transition (e.g. 2:00 am in the United
   * States) and the trigger's interval would have had the trigger fire on that
   * day, then you may actually completely miss a firing on the day of
   * transition if that hour of day does not exist on that day! In such a case
   * the next fire time of the trigger will be computed as double (if the
   * interval is 2 days, then a span of 4 days between firings will occur).
   * </p>
   *
   * @param bSkipDay
   *        <code>true</code> to enable
   * @return this for chaining
   * @see #preserveHourOfDayAcrossDaylightSavings(boolean)
   */
  @Nonnull
  public CalendarIntervalScheduleBuilder skipDayIfHourDoesNotExist (final boolean bSkipDay)
  {
    m_bSkipDayIfHourDoesNotExist = bSkipDay;
    return this;
  }

  private static void _validateInterval (final int timeInterval)
  {
    ValueEnforcer.isGT0 (timeInterval, "TimeInterval");
  }
}
