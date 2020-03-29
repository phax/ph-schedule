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
package com.helger.quartz.impl.triggers;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.quartz.CQuartz;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IScheduleBuilder;
import com.helger.quartz.ISimpleTrigger;
import com.helger.quartz.QCloneUtils;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SimpleScheduleBuilder;

/**
 * <p>
 * A concrete <code>{@link ITrigger}</code> that is used to fire a
 * <code>{@link com.helger.quartz.IJobDetail}</code> at a given moment in time,
 * and optionally repeated at a specified interval.
 * </p>
 *
 * @see ITrigger
 * @see com.helger.quartz.ICronTrigger
 * @see com.helger.quartz.TriggerUtils
 * @author James House
 * @author contributions by Lieven Govaerts of Ebitec Nv, Belgium.
 */
public class SimpleTrigger extends AbstractTrigger <SimpleTrigger> implements ISimpleTrigger, ICoreTrigger
{
  private Date m_aStartTime;
  private Date m_aEndTime;
  private Date m_aNextFireTime;
  private Date m_aPreviousFireTime;
  private int m_nRepeatCount = 0;
  private long m_nRepeatInterval = 0;
  private int m_nTimesTriggered = 0;

  public SimpleTrigger (@Nonnull final SimpleTrigger aOther)
  {
    super (aOther);
    m_aStartTime = QCloneUtils.getClone (aOther.m_aStartTime);
    m_aEndTime = QCloneUtils.getClone (aOther.m_aEndTime);
    m_aNextFireTime = QCloneUtils.getClone (aOther.m_aNextFireTime);
    m_aPreviousFireTime = QCloneUtils.getClone (aOther.m_aPreviousFireTime);
    m_nRepeatCount = aOther.m_nRepeatCount;
    m_nRepeatInterval = aOther.m_nRepeatInterval;
    m_nTimesTriggered = aOther.m_nTimesTriggered;
  }

  /**
   * Create a <code>SimpleTrigger</code> with no settings.
   */
  public SimpleTrigger ()
  {}

  @Nullable
  public final Date getStartTime ()
  {
    return m_aStartTime;
  }

  public void setStartTime (@Nonnull final Date startTime)
  {
    ValueEnforcer.notNull (startTime, "StartTime");

    final Date eTime = getEndTime ();
    if (eTime != null && eTime.before (startTime))
      throw new IllegalArgumentException ("End time cannot be before start time");

    m_aStartTime = startTime;
  }

  @Nullable
  public final Date getEndTime ()
  {
    return m_aEndTime;
  }

  public final void setEndTime (@Nullable final Date endTime)
  {
    final Date sTime = getStartTime ();
    if (sTime != null && endTime != null && sTime.after (endTime))
      throw new IllegalArgumentException ("End time cannot be before start time");

    m_aEndTime = endTime;
  }

  public int getRepeatCount ()
  {
    return m_nRepeatCount;
  }

  /**
   * <p>
   * Set the the number of time the <code>SimpleTrigger</code> should repeat,
   * after which it will be automatically deleted.
   * </p>
   *
   * @see #REPEAT_INDEFINITELY
   * @exception IllegalArgumentException
   *            if repeatCount is &lt; 0
   */
  public void setRepeatCount (final int repeatCount)
  {
    if (repeatCount < 0 && repeatCount != REPEAT_INDEFINITELY)
    {
      throw new IllegalArgumentException ("Repeat count must be >= 0, use the " +
                                          "constant REPEAT_INDEFINITELY for infinite.");
    }

    m_nRepeatCount = repeatCount;
  }

  public long getRepeatInterval ()
  {
    return m_nRepeatInterval;
  }

  /**
   * <p>
   * Set the the time interval (in milliseconds) at which the
   * <code>SimpleTrigger</code> should repeat.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if repeatInterval is &lt; 0
   */
  public void setRepeatInterval (final long repeatInterval)
  {
    if (repeatInterval < 0)
    {
      throw new IllegalArgumentException ("Repeat interval must be >= 0");
    }

    m_nRepeatInterval = repeatInterval;
  }

  /**
   * <p>
   * Get the number of times the <code>SimpleTrigger</code> has already fired.
   * </p>
   */
  public int getTimesTriggered ()
  {
    return m_nTimesTriggered;
  }

  /**
   * <p>
   * Set the number of times the <code>SimpleTrigger</code> has already fired.
   * </p>
   */
  public void setTimesTriggered (final int timesTriggered)
  {
    m_nTimesTriggered = timesTriggered;
  }

  @Override
  protected boolean validateMisfireInstruction (final EMisfireInstruction misfireInstruction)
  {
    switch (misfireInstruction)
    {
      case MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY:
      case MISFIRE_INSTRUCTION_SMART_POLICY:
      case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
      case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT:
      case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT:
      case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT:
      case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT:
        return true;
      default:
        return false;
    }
  }

  /**
   * <p>
   * Updates the <code>SimpleTrigger</code>'s state based on the
   * MISFIRE_INSTRUCTION_XXX that was selected when the
   * <code>SimpleTrigger</code> was created.
   * </p>
   * <p>
   * If the misfire instruction is set to MISFIRE_INSTRUCTION_SMART_POLICY, then
   * the following scheme will be used: <br>
   * </p>
   * <ul>
   * <li>If the Repeat Count is <code>0</code>, then the instruction will be
   * interpreted as <code>MISFIRE_INSTRUCTION_FIRE_NOW</code>.</li>
   * <li>If the Repeat Count is <code>REPEAT_INDEFINITELY</code>, then the
   * instruction will be interpreted as
   * <code>MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT</code>.
   * <b>WARNING:</b> using
   * MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT with a trigger
   * that has a non-null end-time may cause the trigger to never fire again if
   * the end-time arrived during the misfire time span.</li>
   * <li>If the Repeat Count is <code>&gt; 0</code>, then the instruction will
   * be interpreted as
   * <code>MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT</code>.
   * </li>
   * </ul>
   */
  public void updateAfterMisfire (final ICalendar cal)
  {
    EMisfireInstruction instr = getMisfireInstruction ();
    if (instr == EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY)
    {
      // What is smart
      if (getRepeatCount () == 0)
        instr = EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
      else
        if (getRepeatCount () == REPEAT_INDEFINITELY)
          instr = EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT;
        else
        {
          // if (getRepeatCount() > 0)
          instr = EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT;
        }
    }
    else
      if (instr == EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW && getRepeatCount () != 0)
      {
        instr = EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT;
      }

    switch (instr)
    {
      case MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY:
        return;
      case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
      {
        setNextFireTime (new Date ());
        break;
      }
      case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT:
      {
        Date newFireTime = getFireTimeAfter (new Date ());
        while (newFireTime != null && cal != null && !cal.isTimeIncluded (newFireTime.getTime ()))
        {
          newFireTime = getFireTimeAfter (newFireTime);

          if (newFireTime == null)
            break;

          // avoid infinite loop
          final Calendar c = PDTFactory.createCalendar ();
          c.setTime (newFireTime);
          if (c.get (Calendar.YEAR) > CQuartz.MAX_YEAR)
          {
            newFireTime = null;
          }
        }
        setNextFireTime (newFireTime);
        break;
      }
      case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT:
      {
        Date newFireTime = getFireTimeAfter (new Date ());
        while (newFireTime != null && cal != null && !cal.isTimeIncluded (newFireTime.getTime ()))
        {
          newFireTime = getFireTimeAfter (newFireTime);

          if (newFireTime == null)
            break;

          // avoid infinite loop
          final Calendar c = PDTFactory.createCalendar ();
          c.setTime (newFireTime);
          if (c.get (Calendar.YEAR) > CQuartz.MAX_YEAR)
          {
            newFireTime = null;
          }
        }
        if (newFireTime != null)
        {
          final int timesMissed = computeNumTimesFiredBetween (m_aNextFireTime, newFireTime);
          setTimesTriggered (getTimesTriggered () + timesMissed);
        }

        setNextFireTime (newFireTime);
        break;
      }
      case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT:
      {
        final Date newFireTime = new Date ();
        if (m_nRepeatCount != 0 && m_nRepeatCount != REPEAT_INDEFINITELY)
        {
          setRepeatCount (getRepeatCount () - getTimesTriggered ());
          setTimesTriggered (0);
        }

        if (getEndTime () != null && getEndTime ().before (newFireTime))
        {
          setNextFireTime (null); // We are past the end time
        }
        else
        {
          setStartTime (newFireTime);
          setNextFireTime (newFireTime);
        }
        break;
      }
      case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT:
      {
        final Date newFireTime = new Date ();

        final int timesMissed = computeNumTimesFiredBetween (m_aNextFireTime, newFireTime);

        if (m_nRepeatCount != 0 && m_nRepeatCount != REPEAT_INDEFINITELY)
        {
          int remainingCount = getRepeatCount () - (getTimesTriggered () + timesMissed);
          if (remainingCount <= 0)
          {
            remainingCount = 0;
          }
          setRepeatCount (remainingCount);
          setTimesTriggered (0);
        }

        if (getEndTime () != null && getEndTime ().before (newFireTime))
        {
          setNextFireTime (null); // We are past the end time
        }
        else
        {
          setStartTime (newFireTime);
          setNextFireTime (newFireTime);
        }
        break;
      }
    }
  }

  /**
   * Called when the <code>{@link com.helger.quartz.IScheduler}</code> has
   * decided to 'fire' the trigger (execute the associated <code>Job</code>), in
   * order to give the <code>Trigger</code> a chance to update itself for its
   * next triggering (if any).
   *
   * @see #executionComplete(com.helger.quartz.IJobExecutionContext,
   *      com.helger.quartz.JobExecutionException)
   */
  @Override
  public void triggered (final ICalendar calendar)
  {
    m_nTimesTriggered++;
    m_aPreviousFireTime = m_aNextFireTime;
    m_aNextFireTime = getFireTimeAfter (m_aNextFireTime);

    while (m_aNextFireTime != null && calendar != null && !calendar.isTimeIncluded (m_aNextFireTime.getTime ()))
    {

      m_aNextFireTime = getFireTimeAfter (m_aNextFireTime);

      if (m_aNextFireTime == null)
        break;

      // avoid infinite loop
      final Calendar c = PDTFactory.createCalendar ();
      c.setTime (m_aNextFireTime);
      if (c.get (Calendar.YEAR) > CQuartz.MAX_YEAR)
      {
        m_aNextFireTime = null;
      }
    }
  }

  /**
   * @see com.helger.quartz.impl.triggers.AbstractTrigger#updateWithNewCalendar(com.helger.quartz.ICalendar,
   *      long)
   */
  public void updateWithNewCalendar (final ICalendar calendar, final long misfireThreshold)
  {
    m_aNextFireTime = getFireTimeAfter (m_aPreviousFireTime);

    if (m_aNextFireTime == null || calendar == null)
    {
      return;
    }

    final Date now = new Date ();
    while (m_aNextFireTime != null && !calendar.isTimeIncluded (m_aNextFireTime.getTime ()))
    {

      m_aNextFireTime = getFireTimeAfter (m_aNextFireTime);

      if (m_aNextFireTime == null)
        break;

      // avoid infinite loop
      final Calendar c = PDTFactory.createCalendar ();
      c.setTime (m_aNextFireTime);
      if (c.get (Calendar.YEAR) > CQuartz.MAX_YEAR)
      {
        m_aNextFireTime = null;
      }

      if (m_aNextFireTime != null && m_aNextFireTime.before (now))
      {
        final long diff = now.getTime () - m_aNextFireTime.getTime ();
        if (diff >= misfireThreshold)
        {
          m_aNextFireTime = getFireTimeAfter (m_aNextFireTime);
        }
      }
    }
  }

  /**
   * <p>
   * Called by the scheduler at the time a <code>Trigger</code> is first added
   * to the scheduler, in order to have the <code>Trigger</code> compute its
   * first fire time, based on any associated calendar.
   * </p>
   * <p>
   * After this method has been called, <code>getNextFireTime()</code> should
   * return a valid answer.
   * </p>
   *
   * @return the first time at which the <code>Trigger</code> will be fired by
   *         the scheduler, which is also the same value
   *         <code>getNextFireTime()</code> will return (until after the first
   *         firing of the <code>Trigger</code>).
   */
  @Override
  public Date computeFirstFireTime (final ICalendar calendar)
  {
    m_aNextFireTime = getStartTime ();

    while (m_aNextFireTime != null && calendar != null && !calendar.isTimeIncluded (m_aNextFireTime.getTime ()))
    {
      m_aNextFireTime = getFireTimeAfter (m_aNextFireTime);

      if (m_aNextFireTime == null)
        break;

      // avoid infinite loop
      final Calendar c = PDTFactory.createCalendar ();
      c.setTime (m_aNextFireTime);
      if (c.get (Calendar.YEAR) > CQuartz.MAX_YEAR)
      {
        return null;
      }
    }

    return m_aNextFireTime;
  }

  /**
   * <p>
   * Returns the next time at which the <code>Trigger</code> is scheduled to
   * fire. If the trigger will not fire again, <code>null</code> will be
   * returned. Note that the time returned can possibly be in the past, if the
   * time that was computed for the trigger to next fire has already arrived,
   * but the scheduler has not yet been able to fire the trigger (which would
   * likely be due to lack of resources e.g. threads).
   * </p>
   * <p>
   * The value returned is not guaranteed to be valid until after the
   * <code>Trigger</code> has been added to the scheduler.
   * </p>
   *
   * @see com.helger.quartz.TriggerUtils#computeFireTimesBetween(com.helger.quartz.spi.IOperableTrigger,
   *      ICalendar, Date, Date)
   */
  public Date getNextFireTime ()
  {
    return m_aNextFireTime;
  }

  /**
   * Returns the previous time at which the <code>SimpleTrigger</code> fired. If
   * the trigger has not yet fired, <code>null</code> will be returned.
   */
  public Date getPreviousFireTime ()
  {
    return m_aPreviousFireTime;
  }

  /**
   * Set the next time at which the <code>SimpleTrigger</code> should fire.<br>
   * <b>This method should not be invoked by client code.</b>
   */
  public void setNextFireTime (final Date nextFireTime)
  {
    m_aNextFireTime = nextFireTime;
  }

  /**
   * Set the previous time at which the <code>SimpleTrigger</code> fired.<br>
   * <b>This method should not be invoked by client code.</b>
   */
  public void setPreviousFireTime (final Date previousFireTime)
  {
    m_aPreviousFireTime = previousFireTime;
  }

  /**
   * <p>
   * Returns the next time at which the <code>SimpleTrigger</code> will fire,
   * after the given time. If the trigger will not fire after the given time,
   * <code>null</code> will be returned.
   * </p>
   *
   * @param aAfterTime
   *        After time. May be <code>null</code>
   */
  public Date getFireTimeAfter (@Nullable final Date aAfterTime)
  {
    if ((m_nTimesTriggered > m_nRepeatCount) && (m_nRepeatCount != REPEAT_INDEFINITELY))
      return null;

    Date afterTime = aAfterTime;
    if (afterTime == null)
      afterTime = new Date ();

    if (m_nRepeatCount == 0 && afterTime.compareTo (getStartTime ()) >= 0)
      return null;

    final long startMillis = getStartTime ().getTime ();
    final long afterMillis = afterTime.getTime ();
    final long endMillis = (getEndTime () == null) ? Long.MAX_VALUE : getEndTime ().getTime ();

    if (endMillis <= afterMillis)
      return null;

    if (afterMillis < startMillis)
      return new Date (startMillis);

    final long numberOfTimesExecuted = ((afterMillis - startMillis) / m_nRepeatInterval) + 1;

    if ((numberOfTimesExecuted > m_nRepeatCount) && (m_nRepeatCount != REPEAT_INDEFINITELY))
      return null;

    final Date time = new Date (startMillis + (numberOfTimesExecuted * m_nRepeatInterval));
    if (endMillis <= time.getTime ())
      return null;
    return time;
  }

  /**
   * <p>
   * Returns the last time at which the <code>SimpleTrigger</code> will fire,
   * before the given time. If the trigger will not fire before the given time,
   * <code>null</code> will be returned.
   * </p>
   */
  public Date getFireTimeBefore (final Date end)
  {
    if (end.getTime () < getStartTime ().getTime ())
    {
      return null;
    }

    final int numFires = computeNumTimesFiredBetween (getStartTime (), end);

    return new Date (getStartTime ().getTime () + (numFires * m_nRepeatInterval));
  }

  public int computeNumTimesFiredBetween (final Date start, final Date end)
  {

    if (m_nRepeatInterval < 1)
    {
      return 0;
    }

    final long time = end.getTime () - start.getTime ();

    return (int) (time / m_nRepeatInterval);
  }

  /**
   * <p>
   * Returns the final time at which the <code>SimpleTrigger</code> will fire,
   * if repeatCount is REPEAT_INDEFINITELY, null will be returned.
   * </p>
   * <p>
   * Note that the return time may be in the past.
   * </p>
   */
  public Date getFinalFireTime ()
  {
    if (m_nRepeatCount == 0)
    {
      return m_aStartTime;
    }

    if (m_nRepeatCount == REPEAT_INDEFINITELY)
    {
      return (getEndTime () == null) ? null : getFireTimeBefore (getEndTime ());
    }

    final long lastTrigger = m_aStartTime.getTime () + (m_nRepeatCount * m_nRepeatInterval);

    if ((getEndTime () == null) || (lastTrigger < getEndTime ().getTime ()))
    {
      return new Date (lastTrigger);
    }
    return getFireTimeBefore (getEndTime ());
  }

  /**
   * <p>
   * Determines whether or not the <code>SimpleTrigger</code> will occur again.
   * </p>
   */
  public boolean mayFireAgain ()
  {
    return (getNextFireTime () != null);
  }

  /**
   * <p>
   * Validates whether the properties of the <code>JobDetail</code> are valid
   * for submission into a <code>Scheduler</code>.
   *
   * @throws IllegalStateException
   *         if a required property (such as Name, Group, Class) is not set.
   */
  @Override
  public void validate () throws SchedulerException
  {
    super.validate ();

    if (m_nRepeatCount != 0 && m_nRepeatInterval < 1)
    {
      throw new SchedulerException ("Repeat Interval cannot be zero.");
    }
  }

  /**
   * Used by extensions of SimpleTrigger to imply that there are additional
   * properties, specifically so that extensions can choose whether to be stored
   * as a serialized blob, or as a flattened SimpleTrigger table.
   */
  public boolean hasAdditionalProperties ()
  {
    return false;
  }

  /**
   * Get a {@link IScheduleBuilder} that is configured to produce a schedule
   * identical to this trigger's schedule.
   *
   * @see #getTriggerBuilder()
   */
  @Override
  public IScheduleBuilder <SimpleTrigger> getScheduleBuilder ()
  {
    final SimpleScheduleBuilder sb = SimpleScheduleBuilder.simpleSchedule ()
                                                          .withIntervalInMilliseconds (getRepeatInterval ())
                                                          .withRepeatCount (getRepeatCount ());
    switch (getMisfireInstruction ())
    {
      case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
        sb.withMisfireHandlingInstructionFireNow ();
        break;
      case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT:
        sb.withMisfireHandlingInstructionNextWithExistingCount ();
        break;
      case MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT:
        sb.withMisfireHandlingInstructionNextWithRemainingCount ();
        break;
      case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT:
        sb.withMisfireHandlingInstructionNowWithExistingCount ();
        break;
      case MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT:
        sb.withMisfireHandlingInstructionNowWithRemainingCount ();
        break;
    }
    return sb;
  }

  @Nonnull
  @ReturnsMutableCopy
  public SimpleTrigger getClone ()
  {
    return new SimpleTrigger (this);
  }

  @Override
  public boolean equals (final Object o)
  {
    // New field, no change
    return super.equals (o);
  }

  @Override
  public int hashCode ()
  {
    // New field, no change
    return super.hashCode ();
  }

  @Nonnull
  public static SimpleTrigger create (@Nonnull final String name,
                                      @Nullable final String group,
                                      @Nonnull final String jobName,
                                      @Nullable final String jobGroup,
                                      final Date startTime,
                                      final Date endTime,
                                      final int repeatCount,
                                      final long repeatInterval)
  {
    final SimpleTrigger ret = new SimpleTrigger ();
    ret.setName (name);
    ret.setGroup (group);
    ret.setJobName (jobName);
    ret.setJobGroup (jobGroup);
    ret.setStartTime (startTime);
    ret.setEndTime (endTime);
    ret.setRepeatCount (repeatCount);
    ret.setRepeatInterval (repeatInterval);
    return ret;
  }
}
