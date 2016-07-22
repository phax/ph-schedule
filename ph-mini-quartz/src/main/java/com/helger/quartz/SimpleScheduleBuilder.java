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

import com.helger.quartz.impl.triggers.SimpleTrigger;

/**
 * <code>SimpleScheduleBuilder</code> is a {@link ScheduleBuilder} that defines
 * strict/literal interval-based schedules for <code>Trigger</code>s.
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
 *                                .withSchedule (simpleSchedule ().withIntervalInHours (1).repeatForever ())
 *                                .startAt (futureDate (10, MINUTES))
 *                                .build ();
 * scheduler.scheduleJob (job, trigger);
 * </pre>
 *
 * @see ISimpleTrigger
 * @see CalendarIntervalScheduleBuilder
 * @see CronScheduleBuilder
 * @see ScheduleBuilder
 * @see TriggerBuilder
 */
public class SimpleScheduleBuilder extends ScheduleBuilder <ISimpleTrigger>
{

  private long interval = 0;
  private int repeatCount = 0;
  private int misfireInstruction = ITrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

  protected SimpleScheduleBuilder ()
  {}

  /**
   * Create a SimpleScheduleBuilder.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder simpleSchedule ()
  {
    return new SimpleScheduleBuilder ();
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat forever with a 1 minute
   * interval.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatMinutelyForever ()
  {

    return simpleSchedule ().withIntervalInMinutes (1).repeatForever ();
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat forever with an interval of
   * the given number of minutes.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatMinutelyForever (final int minutes)
  {

    return simpleSchedule ().withIntervalInMinutes (minutes).repeatForever ();
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat forever with a 1 second
   * interval.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatSecondlyForever ()
  {

    return simpleSchedule ().withIntervalInSeconds (1).repeatForever ();
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat forever with an interval of
   * the given number of seconds.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatSecondlyForever (final int seconds)
  {

    return simpleSchedule ().withIntervalInSeconds (seconds).repeatForever ();
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat forever with a 1 hour
   * interval.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatHourlyForever ()
  {

    return simpleSchedule ().withIntervalInHours (1).repeatForever ();
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat forever with an interval of
   * the given number of hours.
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatHourlyForever (final int hours)
  {

    return simpleSchedule ().withIntervalInHours (hours).repeatForever ();
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat the given number of times - 1
   * with a 1 minute interval.
   * <p>
   * Note: Total count = 1 (at start time) + repeat count
   * </p>
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatMinutelyForTotalCount (final int count)
  {
    if (count < 1)
      throw new IllegalArgumentException ("Total count of firings must be at least one! Given count: " + count);

    return simpleSchedule ().withIntervalInMinutes (1).withRepeatCount (count - 1);
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat the given number of times - 1
   * with an interval of the given number of minutes.
   * <p>
   * Note: Total count = 1 (at start time) + repeat count
   * </p>
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatMinutelyForTotalCount (final int count, final int minutes)
  {
    if (count < 1)
      throw new IllegalArgumentException ("Total count of firings must be at least one! Given count: " + count);

    return simpleSchedule ().withIntervalInMinutes (minutes).withRepeatCount (count - 1);
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat the given number of times - 1
   * with a 1 second interval.
   * <p>
   * Note: Total count = 1 (at start time) + repeat count
   * </p>
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatSecondlyForTotalCount (final int count)
  {
    if (count < 1)
      throw new IllegalArgumentException ("Total count of firings must be at least one! Given count: " + count);

    return simpleSchedule ().withIntervalInSeconds (1).withRepeatCount (count - 1);
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat the given number of times - 1
   * with an interval of the given number of seconds.
   * <p>
   * Note: Total count = 1 (at start time) + repeat count
   * </p>
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatSecondlyForTotalCount (final int count, final int seconds)
  {
    if (count < 1)
      throw new IllegalArgumentException ("Total count of firings must be at least one! Given count: " + count);

    return simpleSchedule ().withIntervalInSeconds (seconds).withRepeatCount (count - 1);
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat the given number of times - 1
   * with a 1 hour interval.
   * <p>
   * Note: Total count = 1 (at start time) + repeat count
   * </p>
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatHourlyForTotalCount (final int count)
  {
    if (count < 1)
      throw new IllegalArgumentException ("Total count of firings must be at least one! Given count: " + count);

    return simpleSchedule ().withIntervalInHours (1).withRepeatCount (count - 1);
  }

  /**
   * Create a SimpleScheduleBuilder set to repeat the given number of times - 1
   * with an interval of the given number of hours.
   * <p>
   * Note: Total count = 1 (at start time) + repeat count
   * </p>
   *
   * @return the new SimpleScheduleBuilder
   */
  public static SimpleScheduleBuilder repeatHourlyForTotalCount (final int count, final int hours)
  {
    if (count < 1)
      throw new IllegalArgumentException ("Total count of firings must be at least one! Given count: " + count);

    return simpleSchedule ().withIntervalInHours (hours).withRepeatCount (count - 1);
  }

  /**
   * Build the actual Trigger -- NOT intended to be invoked by end users, but
   * will rather be invoked by a TriggerBuilder which this ScheduleBuilder is
   * given to.
   *
   * @see TriggerBuilder#withSchedule(ScheduleBuilder)
   */
  @Override
  public SimpleTrigger build ()
  {

    final SimpleTrigger st = new SimpleTrigger ();
    st.setRepeatInterval (interval);
    st.setRepeatCount (repeatCount);
    st.setMisfireInstruction (misfireInstruction);

    return st;
  }

  /**
   * Specify a repeat interval in milliseconds.
   *
   * @param intervalInMillis
   *        the number of seconds at which the trigger should repeat.
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#getRepeatInterval()
   * @see #withRepeatCount(int)
   */
  public SimpleScheduleBuilder withIntervalInMilliseconds (final long intervalInMillis)
  {
    this.interval = intervalInMillis;
    return this;
  }

  /**
   * Specify a repeat interval in seconds - which will then be multiplied by
   * 1000 to produce milliseconds.
   *
   * @param intervalInSeconds
   *        the number of seconds at which the trigger should repeat.
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#getRepeatInterval()
   * @see #withRepeatCount(int)
   */
  public SimpleScheduleBuilder withIntervalInSeconds (final int intervalInSeconds)
  {
    this.interval = intervalInSeconds * 1000L;
    return this;
  }

  /**
   * Specify a repeat interval in minutes - which will then be multiplied by 60
   * * 1000 to produce milliseconds.
   *
   * @param intervalInMinutes
   *        the number of seconds at which the trigger should repeat.
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#getRepeatInterval()
   * @see #withRepeatCount(int)
   */
  public SimpleScheduleBuilder withIntervalInMinutes (final int intervalInMinutes)
  {
    this.interval = intervalInMinutes * DateBuilder.MILLISECONDS_IN_MINUTE;
    return this;
  }

  /**
   * Specify a repeat interval in minutes - which will then be multiplied by 60
   * * 60 * 1000 to produce milliseconds.
   *
   * @param intervalInHours
   *        the number of seconds at which the trigger should repeat.
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#getRepeatInterval()
   * @see #withRepeatCount(int)
   */
  public SimpleScheduleBuilder withIntervalInHours (final int intervalInHours)
  {
    this.interval = intervalInHours * DateBuilder.MILLISECONDS_IN_HOUR;
    return this;
  }

  /**
   * Specify a the number of time the trigger will repeat - total number of
   * firings will be this number + 1.
   *
   * @param triggerRepeatCount
   *        the number of seconds at which the trigger should repeat.
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#getRepeatCount()
   * @see #repeatForever()
   */
  public SimpleScheduleBuilder withRepeatCount (final int triggerRepeatCount)
  {
    this.repeatCount = triggerRepeatCount;
    return this;
  }

  /**
   * Specify that the trigger will repeat indefinitely.
   *
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#getRepeatCount()
   * @see ISimpleTrigger#REPEAT_INDEFINITELY
   * @see #withIntervalInMilliseconds(long)
   * @see #withIntervalInSeconds(int)
   * @see #withIntervalInMinutes(int)
   * @see #withIntervalInHours(int)
   */
  public SimpleScheduleBuilder repeatForever ()
  {
    this.repeatCount = ISimpleTrigger.REPEAT_INDEFINITELY;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY} instruction.
   *
   * @return the updated CronScheduleBuilder
   * @see ITrigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
   */
  public SimpleScheduleBuilder withMisfireHandlingInstructionIgnoreMisfires ()
  {
    misfireInstruction = ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ISimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW} instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW
   */

  public SimpleScheduleBuilder withMisfireHandlingInstructionFireNow ()
  {
    misfireInstruction = ISimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT}
   * instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
   */
  public SimpleScheduleBuilder withMisfireHandlingInstructionNextWithExistingCount ()
  {
    misfireInstruction = ISimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT}
   * instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT
   */
  public SimpleScheduleBuilder withMisfireHandlingInstructionNextWithRemainingCount ()
  {
    misfireInstruction = ISimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT}
   * instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
   */
  public SimpleScheduleBuilder withMisfireHandlingInstructionNowWithExistingCount ()
  {
    misfireInstruction = ISimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT;
    return this;
  }

  /**
   * If the Trigger misfires, use the
   * {@link ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT}
   * instruction.
   *
   * @return the updated SimpleScheduleBuilder
   * @see ISimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
   */
  public SimpleScheduleBuilder withMisfireHandlingInstructionNowWithRemainingCount ()
  {
    misfireInstruction = ISimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT;
    return this;
  }

}
