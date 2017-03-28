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

import java.text.ParseException;
import java.time.DayOfWeek;
import java.util.TimeZone;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.datetime.util.PDTHelper;
import com.helger.quartz.impl.triggers.CronTrigger;

/**
 * <code>CronScheduleBuilder</code> is a {@link IScheduleBuilder} that defines
 * {@link CronExpression}-based schedules for <code>Trigger</code>s.
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
 * JobDetail job = newJob (MyJob.class).withIdentity (&quot;myJob&quot;).build ();
 * Trigger trigger = newTrigger ().withIdentity (triggerKey (&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 *                                .withSchedule (dailyAtHourAndMinute (10, 0))
 *                                .startAt (futureDate (10, MINUTES))
 *                                .build ();
 * scheduler.scheduleJob (job, trigger);
 * </pre>
 *
 * @see CronExpression
 * @see ICronTrigger
 * @see IScheduleBuilder
 * @see SimpleScheduleBuilder
 * @see CalendarIntervalScheduleBuilder
 * @see TriggerBuilder
 */
public class CronScheduleBuilder implements IScheduleBuilder <ICronTrigger>
{
  private final CronExpression m_aCronExpression;
  private int m_nMisfireInstruction = ITrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

  protected CronScheduleBuilder (@Nonnull final CronExpression aCronExpression)
  {
    ValueEnforcer.notNull (aCronExpression, "CronExpression");
    m_aCronExpression = aCronExpression;
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but
   * will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   *
   * @see TriggerBuilder#withSchedule(IScheduleBuilder)
   */
  @Nonnull
  public CronTrigger build ()
  {
    final CronTrigger ct = new CronTrigger ();
    ct.setCronExpression (m_aCronExpression);
    ct.setTimeZone (m_aCronExpression.getTimeZone ());
    ct.setMisfireInstruction (m_nMisfireInstruction);
    return ct;
  }

  /**
   * Create a CronScheduleBuilder with the given cron-expression string - which
   * is presumed to b e valid cron expression (and hence only a RuntimeException
   * will be thrown if it is not).
   *
   * @param cronExpression
   *        the cron expression string to base the schedule on.
   * @return the new CronScheduleBuilder
   * @throws RuntimeException
   *         wrapping a ParseException if the expression is invalid
   * @see CronExpression
   */
  @Nonnull
  public static CronScheduleBuilder cronSchedule (final String cronExpression)
  {
    try
    {
      return cronSchedule (new CronExpression (cronExpression));
    }
    catch (final ParseException e)
    {
      // all methods of construction ensure the expression is valid by
      // this point...
      throw new RuntimeException ("CronExpression '" + cronExpression + "' is invalid.", e);
    }
  }

  /**
   * Create a CronScheduleBuilder with the given cron-expression string - which
   * may not be a valid cron expression (and hence a ParseException will be
   * thrown if it is not).
   *
   * @param cronExpression
   *        the cron expression string to base the schedule on.
   * @return the new CronScheduleBuilder
   * @throws ParseException
   *         if the expression is invalid
   * @see CronExpression
   */
  @Nonnull
  public static CronScheduleBuilder cronScheduleNonvalidatedExpression (final String cronExpression) throws ParseException
  {
    return cronSchedule (new CronExpression (cronExpression));
  }

  @Nonnull
  private static CronScheduleBuilder _cronScheduleNoParseException (final String presumedValidCronExpression)
  {
    try
    {
      return cronSchedule (new CronExpression (presumedValidCronExpression));
    }
    catch (final ParseException e)
    {
      // all methods of construction ensure the expression is valid by
      // this point...
      throw new RuntimeException ("CronExpression '" +
                                  presumedValidCronExpression +
                                  "' is invalid, which should not be possible, please report bug to Quartz developers.",
                                  e);
    }
  }

  /**
   * Create a CronScheduleBuilder with the given cron-expression.
   *
   * @param cronExpression
   *        the cron expression to base the schedule on.
   * @return the new CronScheduleBuilder
   * @see CronExpression
   */
  @Nonnull
  public static CronScheduleBuilder cronSchedule (final CronExpression cronExpression)
  {
    return new CronScheduleBuilder (cronExpression);
  }

  /**
   * Create a CronScheduleBuilder with a cron-expression that sets the schedule
   * to fire every day at the given time (hour and minute).
   *
   * @param hour
   *        the hour of day to fire
   * @param minute
   *        the minute of the given hour to fire
   * @return the new CronScheduleBuilder
   * @see CronExpression
   */
  @Nonnull
  public static CronScheduleBuilder dailyAtHourAndMinute (final int hour, final int minute)
  {
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    final String cronExpression = "0 " + minute + " " + hour + " ? * *";

    return _cronScheduleNoParseException (cronExpression);
  }

  /**
   * Create a CronScheduleBuilder with a cron-expression that sets the schedule
   * to fire at the given day at the given time (hour and minute) on the given
   * days of the week.
   *
   * @param daysOfWeek
   *        the dasy of the week to fire
   * @param hour
   *        the hour of day to fire
   * @param minute
   *        the minute of the given hour to fire
   * @return the new CronScheduleBuilder
   * @see CronExpression
   * @see DateBuilder#MONDAY
   * @see DateBuilder#TUESDAY
   * @see DateBuilder#WEDNESDAY
   * @see DateBuilder#THURSDAY
   * @see DateBuilder#FRIDAY
   * @see DateBuilder#SATURDAY
   * @see DateBuilder#SUNDAY
   */
  @Nonnull
  public static CronScheduleBuilder atHourAndMinuteOnGivenDaysOfWeek (final int hour,
                                                                      final int minute,
                                                                      final DayOfWeek... daysOfWeek)
  {
    ValueEnforcer.notEmptyNoNullValue (daysOfWeek, "DaysOfWeek");
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("0 ")
       .append (minute)
       .append (' ')
       .append (hour)
       .append (" ? * ")
       .append (PDTHelper.getCalendarDayOfWeek (daysOfWeek[0]));
    for (int i = 1; i < daysOfWeek.length; i++)
      aSB.append (',').append (PDTHelper.getCalendarDayOfWeek (daysOfWeek[i]));

    return _cronScheduleNoParseException (aSB.toString ());
  }

  /**
   * Create a CronScheduleBuilder with a cron-expression that sets the schedule
   * to fire one per week on the given day at the given time (hour and minute).
   *
   * @param dayOfWeek
   *        the day of the week to fire
   * @param hour
   *        the hour of day to fire
   * @param minute
   *        the minute of the given hour to fire
   * @return the new CronScheduleBuilder
   * @see CronExpression
   * @see DateBuilder#MONDAY
   * @see DateBuilder#TUESDAY
   * @see DateBuilder#WEDNESDAY
   * @see DateBuilder#THURSDAY
   * @see DateBuilder#FRIDAY
   * @see DateBuilder#SATURDAY
   * @see DateBuilder#SUNDAY
   */
  @Nonnull
  public static CronScheduleBuilder weeklyOnDayAndHourAndMinute (final DayOfWeek dayOfWeek,
                                                                 final int hour,
                                                                 final int minute)
  {
    DateBuilder.validateDayOfWeek (dayOfWeek);
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    final String cronExpression = "0 " + minute + " " + hour + " ? * " + PDTHelper.getCalendarDayOfWeek (dayOfWeek);

    return _cronScheduleNoParseException (cronExpression);
  }

  /**
   * Create a CronScheduleBuilder with a cron-expression that sets the schedule
   * to fire one per month on the given day of month at the given time (hour and
   * minute).
   *
   * @param dayOfMonth
   *        the day of the month to fire
   * @param hour
   *        the hour of day to fire
   * @param minute
   *        the minute of the given hour to fire
   * @return the new CronScheduleBuilder
   * @see CronExpression
   */
  @Nonnull
  public static CronScheduleBuilder monthlyOnDayAndHourAndMinute (final int dayOfMonth,
                                                                  final int hour,
                                                                  final int minute)
  {
    DateBuilder.validateDayOfMonth (dayOfMonth);
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    final String cronExpression = "0 " + minute + " " + hour + " " + dayOfMonth + " * ?";
    return _cronScheduleNoParseException (cronExpression);
  }

  /**
   * The <code>TimeZone</code> in which to base the schedule.
   *
   * @param timezone
   *        the time-zone for the schedule.
   * @return the updated CronScheduleBuilder
   * @see CronExpression#getTimeZone()
   */
  @Nonnull
  public CronScheduleBuilder inTimeZone (final TimeZone timezone)
  {
    m_aCronExpression.setTimeZone (timezone);
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
   */
  @Nonnull
  public CronScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires ()
  {
    m_nMisfireInstruction = ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ICronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see ICronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
   */
  @Nonnull
  public CronScheduleBuilder withMisfireHandlingInstructionDoNothing ()
  {
    m_nMisfireInstruction = ICronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ICronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see ICronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
   */
  @Nonnull
  public CronScheduleBuilder withMisfireHandlingInstructionFireAndProceed ()
  {
    m_nMisfireInstruction = ICronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    return this;
  }
}
