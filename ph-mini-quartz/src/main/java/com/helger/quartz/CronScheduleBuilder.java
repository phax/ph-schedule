/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.helger.quartz;

import java.text.ParseException;
import java.util.TimeZone;

import com.helger.quartz.impl.triggers.CronTriggerImpl;

/**
 * <code>CronScheduleBuilder</code> is a {@link ScheduleBuilder} that defines
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
 * JobDetail job = newJob(MyJob.class).withIdentity(&quot;myJob&quot;).build();
 * Trigger trigger = newTrigger()
 * .withIdentity(triggerKey(&quot;myTrigger&quot;, &quot;myTriggerGroup&quot;))
 * .withSchedule(dailyAtHourAndMinute(10, 0)) .startAt(futureDate(10,
 * MINUTES)).build(); scheduler.scheduleJob(job, trigger);
 *
 * <pre>
 *
 * @see CronExpression
 * @see ICronTrigger
 * @see ScheduleBuilder
 * @see SimpleScheduleBuilder
 * @see CalendarIntervalScheduleBuilder
 * @see TriggerBuilder
 */
public class CronScheduleBuilder extends ScheduleBuilder <ICronTrigger>
{

  private final CronExpression cronExpression;
  private int misfireInstruction = ITrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

  protected CronScheduleBuilder (final CronExpression cronExpression)
  {
    if (cronExpression == null)
    {
      throw new NullPointerException ("cronExpression cannot be null");
    }
    this.cronExpression = cronExpression;
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but
   * will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   *
   * @see TriggerBuilder#withSchedule(ScheduleBuilder)
   */
  @Override
  public CronTriggerImpl build ()
  {

    final CronTriggerImpl ct = new CronTriggerImpl ();

    ct.setCronExpression (cronExpression);
    ct.setTimeZone (cronExpression.getTimeZone ());
    ct.setMisfireInstruction (misfireInstruction);

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
  public static CronScheduleBuilder cronScheduleNonvalidatedExpression (final String cronExpression) throws ParseException
  {
    return cronSchedule (new CronExpression (cronExpression));
  }

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
  public static CronScheduleBuilder dailyAtHourAndMinute (final int hour, final int minute)
  {
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    final String cronExpression = String.format ("0 %d %d ? * *", minute, hour);

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

  public static CronScheduleBuilder atHourAndMinuteOnGivenDaysOfWeek (final int hour,
                                                                      final int minute,
                                                                      final Integer... daysOfWeek)
  {
    if (daysOfWeek == null || daysOfWeek.length == 0)
      throw new IllegalArgumentException ("You must specify at least one day of week.");
    for (final int dayOfWeek : daysOfWeek)
      DateBuilder.validateDayOfWeek (dayOfWeek);
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    String cronExpression = String.format ("0 %d %d ? * %d", minute, hour, daysOfWeek[0]);

    for (int i = 1; i < daysOfWeek.length; i++)
      cronExpression = cronExpression + "," + daysOfWeek[i];

    return _cronScheduleNoParseException (cronExpression);
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
  public static CronScheduleBuilder weeklyOnDayAndHourAndMinute (final int dayOfWeek, final int hour, final int minute)
  {
    DateBuilder.validateDayOfWeek (dayOfWeek);
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    final String cronExpression = String.format ("0 %d %d ? * %d", minute, hour, dayOfWeek);

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
  public static CronScheduleBuilder monthlyOnDayAndHourAndMinute (final int dayOfMonth,
                                                                  final int hour,
                                                                  final int minute)
  {
    DateBuilder.validateDayOfMonth (dayOfMonth);
    DateBuilder.validateHour (hour);
    DateBuilder.validateMinute (minute);

    final String cronExpression = String.format ("0 %d %d %d * ?", minute, hour, dayOfMonth);

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
  public CronScheduleBuilder inTimeZone (final TimeZone timezone)
  {
    cronExpression.setTimeZone (timezone);
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
   */
  public CronScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires ()
  {
    misfireInstruction = ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ICronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see ICronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
   */
  public CronScheduleBuilder withMisfireHandlingInstructionDoNothing ()
  {
    misfireInstruction = ICronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ICronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see ICronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
   */
  public CronScheduleBuilder withMisfireHandlingInstructionFireAndProceed ()
  {
    misfireInstruction = ICronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    return this;
  }
}
