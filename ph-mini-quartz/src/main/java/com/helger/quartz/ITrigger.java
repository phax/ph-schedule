/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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

import java.io.Serializable;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.quartz.spi.IMutableTrigger;

/**
 * The base interface with properties common to all <code>Trigger</code>s - use
 * {@link TriggerBuilder} to instantiate an actual Trigger.
 * <p>
 * <code>Triggers</code>s have a {@link TriggerKey} associated with them, which
 * should uniquely identify them within a single
 * <code>{@link IScheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s are
 * scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 * <p>
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents
 * into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * </p>
 *
 * @see TriggerBuilder
 * @see JobDataMap
 * @see IJobExecutionContext
 * @see TriggerUtils
 * @see ISimpleTrigger
 * @see ICronTrigger
 * @see ICalendarIntervalTrigger
 * @author James House
 */
@MustImplementEqualsAndHashcode
public interface ITrigger extends Serializable, Comparable <ITrigger>
{
  public enum ETriggerState
  {
    NONE,
    NORMAL,
    PAUSED,
    COMPLETE,
    ERROR,
    BLOCKED
  }

  /**
   * <p>
   * <code>NOOP</code> Instructs the <code>{@link IScheduler}</code> that the
   * <code>{@link ITrigger}</code> has no further instructions.
   * </p>
   * <p>
   * <code>RE_EXECUTE_JOB</code> Instructs the <code>{@link IScheduler}</code>
   * that the <code>{@link ITrigger}</code> wants the
   * <code>{@link com.helger.quartz.IJobDetail}</code> to re-execute
   * immediately. If not in a 'RECOVERING' or 'FAILED_OVER' situation, the
   * execution context will be re-used (giving the <code>Job</code> the ability
   * to 'see' anything placed in the context by its last execution).
   * </p>
   * <p>
   * <code>SET_TRIGGER_COMPLETE</code> Instructs the
   * <code>{@link IScheduler}</code> that the <code>{@link ITrigger}</code>
   * should be put in the <code>COMPLETE</code> state.
   * </p>
   * <p>
   * <code>DELETE_TRIGGER</code> Instructs the <code>{@link IScheduler}</code>
   * that the <code>{@link ITrigger}</code> wants itself deleted.
   * </p>
   * <p>
   * <code>SET_ALL_JOB_TRIGGERS_COMPLETE</code> Instructs the
   * <code>{@link IScheduler}</code> that all <code>Trigger</code>s referencing
   * the same <code>{@link com.helger.quartz.IJobDetail}</code> as this one
   * should be put in the <code>COMPLETE</code> state.
   * </p>
   * <p>
   * <code>SET_TRIGGER_ERROR</code> Instructs the
   * <code>{@link IScheduler}</code> that all <code>Trigger</code>s referencing
   * the same <code>{@link com.helger.quartz.IJobDetail}</code> as this one
   * should be put in the <code>ERROR</code> state.
   * </p>
   * <p>
   * <code>SET_ALL_JOB_TRIGGERS_ERROR</code> Instructs the
   * <code>{@link IScheduler}</code> that the <code>Trigger</code> should be put
   * in the <code>ERROR</code> state.
   * </p>
   */
  public enum ECompletedExecutionInstruction
  {
    NOOP,
    RE_EXECUTE_JOB,
    SET_TRIGGER_COMPLETE,
    DELETE_TRIGGER,
    SET_ALL_JOB_TRIGGERS_COMPLETE,
    SET_TRIGGER_ERROR,
    SET_ALL_JOB_TRIGGERS_ERROR
  }

  public enum EMisfireInstruction
  {
    /**
     * Instructs the <code>{@link IScheduler}</code> that the
     * <code>Trigger</code> will never be evaluated for a misfire situation, and
     * that the scheduler will simply try to fire it as soon as it can, and then
     * update the Trigger as if it had fired at the proper time.<br>
     * NOTE: if a trigger uses this instruction, and it has missed several of
     * its scheduled firings, then several rapid firings may occur as the
     * trigger attempt to catch back up to where it would have been. For
     * example, a SimpleTrigger that fires every 15 seconds which has misfired
     * for 5 minutes will fire 20 times once it gets the chance to fire.
     */
    MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY, // -1

    /**
     * Instructs the <code>{@link IScheduler}</code> that upon a mis-fire
     * situation, the <code>updateAfterMisfire()</code> method will be called on
     * the <code>Trigger</code> to determine the mis-fire instruction, which
     * logic will be trigger-implementation-dependent.<br>
     * In order to see if this instruction fits your needs, you should look at
     * the documentation for the <code>getSmartMisfirePolicy()</code> method on
     * the particular <code>Trigger</code> implementation you are using.
     */
    MISFIRE_INSTRUCTION_SMART_POLICY, // 0

    /**
     * Instructs the <code>{@link IScheduler}</code> that upon a mis-fire
     * situation, the <code>Trigger</code> wants to be fired now by
     * <code>Scheduler</code>.
     */
    MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, // 1
    /**
     * Instructs the <code>{@link IScheduler}</code> that upon a mis-fire
     * situation, the <code>Trigger</code> wants to have it's next-fire-time
     * updated to the next time in the schedule after the current time (taking
     * into account any associated <code>Calendar</code>, but it does not want
     * to be fired now.<br>
     * Not applicable to ISimpleTrigger
     */
    MISFIRE_INSTRUCTION_DO_NOTHING, // 2

    /**
     * Instructs the <code>{@link IScheduler}</code> that upon a mis-fire
     * situation, the <code>{@link ISimpleTrigger}</code> wants to be
     * re-scheduled to 'now' (even if the associated
     * <code>{@link ICalendar}</code> excludes 'now') with the repeat count left
     * as-is. This does obey the <code>Trigger</code> end-time however, so if
     * 'now' is after the end-time the <code>Trigger</code> will not fire
     * again.<br>
     * <i>NOTE:</i> Use of this instruction causes the trigger to 'forget' the
     * start-time and repeat-count that it was originally setup with (this is
     * only an issue if you for some reason wanted to be able to tell what the
     * original values were at some later time).<br>
     * Only applicable to ISimpleTrigger
     */
    MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT, // 2

    /**
     * Instructs the <code>{@link IScheduler}</code> that upon a mis-fire
     * situation, the <code>{@link ISimpleTrigger}</code> wants to be
     * re-scheduled to 'now' (even if the associated
     * <code>{@link ICalendar}</code> excludes 'now') with the repeat count set
     * to what it would be, if it had not missed any firings. This does obey the
     * <code>Trigger</code> end-time however, so if 'now' is after the end-time
     * the <code>Trigger</code> will not fire again.<br>
     * <i>NOTE:</i> Use of this instruction causes the trigger to 'forget' the
     * start-time and repeat-count that it was originally setup with. Instead,
     * the repeat count on the trigger will be changed to whatever the remaining
     * repeat count is (this is only an issue if you for some reason wanted to
     * be able to tell what the original values were at some later time).<br>
     * <i>NOTE:</i> This instruction could cause the <code>Trigger</code> to go
     * to the 'COMPLETE' state after firing 'now', if all the repeat-fire-times
     * where missed.<br>
     * Only applicable to ISimpleTrigger
     */
    MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT, // 3

    /**
     * Instructs the <code>{@link IScheduler}</code> that upon a mis-fire
     * situation, the <code>{@link ISimpleTrigger}</code> wants to be
     * re-scheduled to the next scheduled time after 'now' - taking into account
     * any associated <code>{@link ICalendar}</code>, and with the repeat count
     * set to what it would be, if it had not missed any firings.<br>
     * <i>NOTE/WARNING:</i> This instruction could cause the
     * <code>Trigger</code> to go directly to the 'COMPLETE' state if all
     * fire-times where missed.<br>
     * Only applicable to ISimpleTrigger
     */
    MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, // 4

    /**
     * Instructs the <code>{@link IScheduler}</code> that upon a mis-fire
     * situation, the <code>{@link ISimpleTrigger}</code> wants to be
     * re-scheduled to the next scheduled time after 'now' - taking into account
     * any associated <code>{@link ICalendar}</code>, and with the repeat count
     * left unchanged.<br>
     * <i>NOTE/WARNING:</i> This instruction could cause the
     * <code>Trigger</code> to go directly to the 'COMPLETE' state if the
     * end-time of the trigger has arrived.<br>
     * Only applicable to ISimpleTrigger
     */
    MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT // 5
  }

  /**
   * The default value for priority.
   */
  int DEFAULT_PRIORITY = 5;

  @Nullable
  TriggerKey getKey ();

  @Nullable
  JobKey getJobKey ();

  /**
   * Return the description given to the <code>Trigger</code> instance by its
   * creator (if any).
   *
   * @return <code>null</code> if no description was set.
   */
  @Nullable
  String getDescription ();

  /**
   * Get the name of the <code>{@link ICalendar}</code> associated with this
   * Trigger.
   *
   * @return <code>null</code> if there is no associated Calendar.
   */
  @Nullable
  String getCalendarName ();

  /**
   * Get the <code>JobDataMap</code> that is associated with the
   * <code>Trigger</code>.<br>
   * Changes made to this map during job execution are not re-persisted, and in
   * fact typically result in an <code>IllegalStateException</code>.
   */
  @Nonnull
  JobDataMap getJobDataMap ();

  /**
   * The priority of a <code>Trigger</code> acts as a tiebreaker such that if
   * two <code>Trigger</code>s have the same scheduled fire time, then the one
   * with the higher priority will get first access to a worker thread.<br>
   * If not explicitly set, the default value is <code>5</code>.
   *
   * @see #DEFAULT_PRIORITY
   */
  int getPriority ();

  /**
   * Used by the <code>{@link IScheduler}</code> to determine whether or not it
   * is possible for this <code>Trigger</code> to fire again.
   * <p>
   * If the returned value is <code>false</code> then the <code>Scheduler</code>
   * may remove the <code>Trigger</code> from the
   * <code>{@link com.helger.quartz.spi.IJobStore}</code>.
   * </p>
   */
  boolean mayFireAgain ();

  /**
   * Get the time at which the <code>Trigger</code> should occur.
   *
   * @return The start. May be <code>null</code> depending on the implementation
   */
  Date getStartTime ();

  /**
   * Get the time at which the <code>Trigger</code> should quit repeating -
   * regardless of any remaining repeats (based on the trigger's particular
   * repeat settings).
   *
   * @return the end time or <code>null</code>.
   * @see #getFinalFireTime()
   */
  @Nullable
  Date getEndTime ();

  /**
   * Returns the next time at which the <code>Trigger</code> is scheduled to
   * fire. If the trigger will not fire again, <code>null</code> will be
   * returned. Note that the time returned can possibly be in the past, if the
   * time that was computed for the trigger to next fire has already arrived,
   * but the scheduler has not yet been able to fire the trigger (which would
   * likely be due to lack of resources e.g. threads).
   * <p>
   * The value returned is not guaranteed to be valid until after the
   * <code>Trigger</code> has been added to the scheduler.
   * </p>
   *
   * @see TriggerUtils#computeFireTimesBetween(com.helger.quartz.spi.IOperableTrigger,
   *      ICalendar, Date, Date)
   */
  Date getNextFireTime ();

  /**
   * Returns the previous time at which the <code>Trigger</code> fired. If the
   * trigger has not yet fired, <code>null</code> will be returned.
   */
  Date getPreviousFireTime ();

  /**
   * Returns the next time at which the <code>Trigger</code> will fire, after
   * the given time. If the trigger will not fire after the given time,
   * <code>null</code> will be returned.
   */
  Date getFireTimeAfter (Date afterTime);

  /**
   * Returns the last time at which the <code>Trigger</code> will fire, if the
   * Trigger will repeat indefinitely, null will be returned.
   * <p>
   * Note that the return time *may* be in the past.
   * </p>
   */
  Date getFinalFireTime ();

  /**
   * Get the instruction the <code>Scheduler</code> should be given for handling
   * misfire situations for this <code>Trigger</code>- the concrete
   * <code>Trigger</code> type that you are using will have defined a set of
   * additional <code>MISFIRE_INSTRUCTION_XXX</code> constants that may be set
   * as this property's value.
   * <p>
   * If not explicitly set, the default value is
   * <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
   * </p>
   *
   * @see ISimpleTrigger
   * @see ICronTrigger
   */
  EMisfireInstruction getMisfireInstruction ();

  /**
   * Get a {@link TriggerBuilder} that is configured to produce a
   * <code>Trigger</code> identical to this one.
   *
   * @see #getScheduleBuilder()
   */
  TriggerBuilder <? extends ITrigger> getTriggerBuilder ();

  /**
   * Get a {@link IScheduleBuilder} that is configured to produce a schedule
   * identical to this trigger's schedule.
   *
   * @see #getTriggerBuilder()
   */
  IScheduleBuilder <? extends IMutableTrigger> getScheduleBuilder ();

  /**
   * Trigger equality is based upon the equality of the TriggerKey.
   *
   * @return <code>true</code> if the key of this Trigger equals that of the
   *         given Trigger.
   */
  boolean equals (Object other);

  /**
   * Compare the next fire time of this <code>Trigger</code> to that of another
   * by comparing their keys, or in other words, sorts them according to the
   * natural (i.e. alphabetical) order of their keys.
   */
  int compareTo (ITrigger other);

  @Nonnull
  @ReturnsMutableCopy
  ITrigger getClone ();
}
