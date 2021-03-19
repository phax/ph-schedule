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
package com.helger.quartz.impl.triggers;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.datetime.PDTFactory;
import com.helger.datetime.util.PDTHelper;
import com.helger.quartz.CQuartz;
import com.helger.quartz.DailyTimeIntervalScheduleBuilder;
import com.helger.quartz.EIntervalUnit;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IDailyTimeIntervalTrigger;
import com.helger.quartz.IScheduleBuilder;
import com.helger.quartz.QCloneUtils;
import com.helger.quartz.SchedulerException;

/**
 * A concrete implementation of DailyTimeIntervalTrigger that is used to fire a
 * <code>{@link com.helger.quartz.IJobDetail}</code> based upon daily repeating
 * time intervals.
 * <p>
 * The trigger will fire every N (see {@link #setRepeatInterval(int)} ) seconds,
 * minutes or hours (see {@link #setRepeatIntervalUnit(EIntervalUnit)}) during a
 * given time window on specified days of the week.
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
 * those week days period. Again, remember this trigger will reset fire time
 * each day with startTimeOfDay, regardless of your interval or endTimeOfDay!
 * </p>
 * <p>
 * The default values for fields if not set are: startTimeOfDay defaults to
 * 00:00:00, the endTimeOfDay default to 23:59:59, and daysOfWeek is default to
 * every day. The startTime default to current time-stamp now, while endTime has
 * not value.
 * </p>
 * <p>
 * If startTime is before startTimeOfDay, then startTimeOfDay will be used and
 * startTime has no affect other than to specify the first day of firing. Else
 * if startTime is after startTimeOfDay, then the first fire time for that day
 * will be the next interval after the startTime. For example, if you set
 * startingTimeOfDay=9am, endingTimeOfDay=11am, interval=15 mins, and
 * startTime=9:33am, then the next fire time will be 9:45pm. Note also that if
 * you do not set startTime value, the trigger builder will default to current
 * time, and current time maybe before or after the startTimeOfDay! So be aware
 * how you set your startTime.
 * </p>
 * <p>
 * This trigger also supports "repeatCount" feature to end the trigger fire time
 * after a certain number of count is reached. Just as the SimpleTrigger,
 * setting repeatCount=0 means trigger will fire once only! Setting any positive
 * count then the trigger will repeat count + 1 times. Unlike SimpleTrigger, the
 * default value of repeatCount of this trigger is set to REPEAT_INDEFINITELY
 * instead of 0 though.
 *
 * @see IDailyTimeIntervalTrigger
 * @see DailyTimeIntervalScheduleBuilder
 * @since 2.1.0
 * @author James House
 * @author Zemian Deng saltnlight5@gmail.com
 */
public class DailyTimeIntervalTrigger extends AbstractTrigger <DailyTimeIntervalTrigger> implements
                                      IDailyTimeIntervalTrigger
{
  private Date m_aStartTime;
  private Date m_aEndTime;
  private Date m_aNextFireTime;
  private Date m_aPreviousFireTime;
  private int m_nRepeatCount = REPEAT_INDEFINITELY;
  private int m_nRepeatInterval = 1;
  private EIntervalUnit m_eRepeatIntervalUnit = EIntervalUnit.MINUTE;
  private EnumSet <DayOfWeek> m_aDaysOfWeek;
  private LocalTime m_aStartTimeOfDay;
  private LocalTime m_aEndTimeOfDay;
  private int m_nTimesTriggered = 0;
  private boolean m_bComplete = false;

  public DailyTimeIntervalTrigger (@Nonnull final DailyTimeIntervalTrigger aOther)
  {
    super (aOther);
    m_aStartTime = QCloneUtils.getClone (aOther.m_aStartTime);
    m_aEndTime = QCloneUtils.getClone (aOther.m_aEndTime);
    m_aNextFireTime = QCloneUtils.getClone (aOther.m_aNextFireTime);
    m_aPreviousFireTime = QCloneUtils.getClone (aOther.m_aPreviousFireTime);
    m_nRepeatCount = aOther.m_nRepeatCount;
    m_nRepeatInterval = aOther.m_nRepeatInterval;
    m_eRepeatIntervalUnit = aOther.m_eRepeatIntervalUnit;
    m_aDaysOfWeek = QCloneUtils.getClone (aOther.m_aDaysOfWeek);
    m_aStartTimeOfDay = aOther.m_aStartTimeOfDay;
    m_aEndTimeOfDay = aOther.m_aEndTimeOfDay;
    m_nTimesTriggered = aOther.m_nTimesTriggered;
    m_bComplete = aOther.m_bComplete;
  }

  /**
   * Create a <code>DailyTimeIntervalTrigger</code> with no settings.
   */
  public DailyTimeIntervalTrigger ()
  {}

  /**
   * <p>
   * Create a <code>DailyTimeIntervalTrigger</code> that will occur immediately,
   * and repeat at the the given interval.
   * </p>
   *
   * @param startTimeOfDay
   *        The <code>LocalTime</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>LocalTime</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link EIntervalUnit#SECOND},
   *        {@link EIntervalUnit#MINUTE}, and {@link EIntervalUnit#HOUR}.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final LocalTime startTimeOfDay,
                                   final LocalTime endTimeOfDay,
                                   final EIntervalUnit intervalUnit,
                                   final int repeatInterval)
  {
    this (name, null, startTimeOfDay, endTimeOfDay, intervalUnit, repeatInterval);
  }

  /**
   * <p>
   * Create a <code>DailyTimeIntervalTrigger</code> that will occur immediately,
   * and repeat at the the given interval.
   * </p>
   *
   * @param startTimeOfDay
   *        The <code>LocalTime</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>LocalTime</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link EIntervalUnit#SECOND},
   *        {@link EIntervalUnit#MINUTE}, and {@link EIntervalUnit#HOUR}.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final String group,
                                   final LocalTime startTimeOfDay,
                                   final LocalTime endTimeOfDay,
                                   final EIntervalUnit intervalUnit,
                                   final int repeatInterval)
  {
    this (name, group, new Date (), null, startTimeOfDay, endTimeOfDay, intervalUnit, repeatInterval);
  }

  /**
   * <p>
   * Create a <code>DailyTimeIntervalTrigger</code> that will occur at the given
   * time, and repeat at the the given interval until the given end time.
   * </p>
   *
   * @param startTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        fire.
   * @param endTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        quit repeat firing.
   * @param startTimeOfDay
   *        The <code>LocalTime</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>LocalTime</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link EIntervalUnit#SECOND},
   *        {@link EIntervalUnit#MINUTE}, and {@link EIntervalUnit#HOUR}.
   * @param repeatInterval
   *        The number of milliseconds to pause between the repeat firing.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final Date startTime,
                                   final Date endTime,
                                   final LocalTime startTimeOfDay,
                                   final LocalTime endTimeOfDay,
                                   final EIntervalUnit intervalUnit,
                                   final int repeatInterval)
  {
    this (name, null, startTime, endTime, startTimeOfDay, endTimeOfDay, intervalUnit, repeatInterval);
  }

  /**
   * <p>
   * Create a <code>DailyTimeIntervalTrigger</code> that will occur at the given
   * time, and repeat at the the given interval until the given end time.
   * </p>
   *
   * @param startTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        fire.
   * @param endTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        quit repeat firing.
   * @param startTimeOfDay
   *        The <code>LocalTime</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>LocalTime</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link EIntervalUnit#SECOND},
   *        {@link EIntervalUnit#MINUTE}, and {@link EIntervalUnit#HOUR}.
   * @param repeatInterval
   *        The number of milliseconds to pause between the repeat firing.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final String group,
                                   final Date startTime,
                                   final Date endTime,
                                   final LocalTime startTimeOfDay,
                                   final LocalTime endTimeOfDay,
                                   final EIntervalUnit intervalUnit,
                                   final int repeatInterval)
  {
    super (name, group);

    setStartTime (startTime);
    setEndTime (endTime);
    setRepeatIntervalUnit (intervalUnit);
    setRepeatInterval (repeatInterval);
    setStartTimeOfDay (startTimeOfDay);
    setEndTimeOfDay (endTimeOfDay);
  }

  /**
   * <p>
   * Create a <code>DailyTimeIntervalTrigger</code> that will occur at the given
   * time, fire the identified <code>Job</code> and repeat at the the given
   * interval until the given end time.
   * </p>
   *
   * @param startTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        fire.
   * @param endTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        quit repeat firing.
   * @param startTimeOfDay
   *        The <code>LocalTime</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>LocalTime</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link EIntervalUnit#SECOND},
   *        {@link EIntervalUnit#MINUTE}, and {@link EIntervalUnit#HOUR}.
   * @param repeatInterval
   *        The number of milliseconds to pause between the repeat firing.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final String group,
                                   final String jobName,
                                   final String jobGroup,
                                   final Date startTime,
                                   final Date endTime,
                                   final LocalTime startTimeOfDay,
                                   final LocalTime endTimeOfDay,
                                   final EIntervalUnit intervalUnit,
                                   final int repeatInterval)
  {
    super (name, group, jobName, jobGroup);

    setStartTime (startTime);
    setEndTime (endTime);
    setRepeatIntervalUnit (intervalUnit);
    setRepeatInterval (repeatInterval);
    setStartTimeOfDay (startTimeOfDay);
    setEndTimeOfDay (endTimeOfDay);
  }

  @Nonnull
  public final Date getStartTime ()
  {
    if (m_aStartTime == null)
      m_aStartTime = new Date ();
    return m_aStartTime;
  }

  public final void setStartTime (@Nonnull final Date startTime)
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

  public EIntervalUnit getRepeatIntervalUnit ()
  {
    return m_eRepeatIntervalUnit;
  }

  /**
   * <p>
   * Set the interval unit - the time unit on with the interval applies.
   * </p>
   *
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link EIntervalUnit#SECOND},
   *        {@link EIntervalUnit#MINUTE}, and {@link EIntervalUnit#HOUR}.
   */
  public void setRepeatIntervalUnit (final EIntervalUnit intervalUnit)
  {
    if (m_eRepeatIntervalUnit == null ||
        !(m_eRepeatIntervalUnit.equals (EIntervalUnit.SECOND) ||
          m_eRepeatIntervalUnit.equals (EIntervalUnit.MINUTE) ||
          m_eRepeatIntervalUnit.equals (EIntervalUnit.HOUR)))
      throw new IllegalArgumentException ("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
    m_eRepeatIntervalUnit = intervalUnit;
  }

  public int getRepeatInterval ()
  {
    return m_nRepeatInterval;
  }

  /**
   * <p>
   * set the the time interval that will be added to the
   * <code>DailyTimeIntervalTrigger</code>'s fire time (in the set repeat
   * interval unit) in order to calculate the time of the next trigger repeat.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if repeatInterval is &lt; 0
   */
  public void setRepeatInterval (final int repeatInterval)
  {
    ValueEnforcer.isGE0 (repeatInterval, "RepeatInterval");
    m_nRepeatInterval = repeatInterval;
  }

  public int getTimesTriggered ()
  {
    return m_nTimesTriggered;
  }

  /**
   * <p>
   * Set the number of times the <code>DailyTimeIntervalTrigger</code> has
   * already fired.
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
      case MISFIRE_INSTRUCTION_DO_NOTHING:
        return true;
      default:
        return false;
    }
  }

  /**
   * <p>
   * Updates the <code>DailyTimeIntervalTrigger</code>'s state based on the
   * MISFIRE_INSTRUCTION_XXX that was selected when the
   * <code>DailyTimeIntervalTrigger</code> was created.
   * </p>
   * <p>
   * If the misfire instruction is set to MISFIRE_INSTRUCTION_SMART_POLICY, then
   * the following scheme will be used: <br>
   * </p>
   * <ul>
   * <li>The instruction will be interpreted as
   * <code>MISFIRE_INSTRUCTION_FIRE_ONCE_NOW</code></li>
   * </ul>
   */
  public void updateAfterMisfire (final ICalendar cal)
  {
    EMisfireInstruction instr = getMisfireInstruction ();
    if (instr == EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY)
    {
      // What's smart here
      instr = EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    }

    switch (instr)
    {
      case MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY:
        return;
      case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
      {
        // fire once now...
        setNextFireTime (new Date ());
        // the new fire time afterward will magically preserve the original
        // time of day for firing for day/week/month interval triggers,
        // because of the way getFireTimeAfter() works - in its always
        // restarting
        // computation from the start time.
        break;
      }
      case MISFIRE_INSTRUCTION_DO_NOTHING:
      {
        Date newFireTime = getFireTimeAfter (new Date ());
        while (newFireTime != null && cal != null && !cal.isTimeIncluded (newFireTime.getTime ()))
        {
          newFireTime = getFireTimeAfter (newFireTime);
        }
        setNextFireTime (newFireTime);
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

    if (m_aNextFireTime == null)
      m_bComplete = true;
  }

  /**
   * @see com.helger.quartz.impl.triggers.AbstractTrigger#updateWithNewCalendar(com.helger.quartz.ICalendar,
   *      long)
   */
  public void updateWithNewCalendar (final ICalendar calendar, final long misfireThreshold)
  {
    m_aNextFireTime = getFireTimeAfter (m_aPreviousFireTime);
    if (m_aNextFireTime == null || calendar == null)
      return;

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
    m_aNextFireTime = getFireTimeAfter (new Date (getStartTime ().getTime () - 1000L));

    // Check calendar for date-time exclusion
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

  @Nonnull
  private static Calendar _createCalendarTime (final Date dateTime)
  {
    final Calendar cal = PDTFactory.createCalendar ();
    cal.setTime (dateTime);
    return cal;
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
   */
  public Date getNextFireTime ()
  {
    return m_aNextFireTime;
  }

  /**
   * <p>
   * Returns the previous time at which the
   * <code>DailyTimeIntervalTrigger</code> fired. If the trigger has not yet
   * fired, <code>null</code> will be returned.
   */
  public Date getPreviousFireTime ()
  {
    return m_aPreviousFireTime;
  }

  /**
   * <p>
   * Set the next time at which the <code>DailyTimeIntervalTrigger</code> should
   * fire.
   * </p>
   * <p>
   * <b>This method should not be invoked by client code.</b>
   * </p>
   */
  public void setNextFireTime (final Date nextFireTime)
  {
    m_aNextFireTime = nextFireTime;
  }

  /**
   * <p>
   * Set the previous time at which the <code>DailyTimeIntervalTrigger</code>
   * fired.
   * </p>
   * <p>
   * <b>This method should not be invoked by client code.</b>
   * </p>
   */
  public void setPreviousFireTime (final Date previousFireTime)
  {
    m_aPreviousFireTime = previousFireTime;
  }

  /**
   * <p>
   * Returns the next time at which the <code>DailyTimeIntervalTrigger</code>
   * will fire, after the given time. If the trigger will not fire after the
   * given time, <code>null</code> will be returned.
   * </p>
   *
   * @param aAfterTime
   *        after time
   */
  public Date getFireTimeAfter (@Nullable final Date aAfterTime)
  {
    // Check if trigger has completed or not.
    if (m_bComplete)
      return null;

    // Check repeatCount limit
    if (m_nRepeatCount != REPEAT_INDEFINITELY && m_nTimesTriggered > m_nRepeatCount)
      return null;

    // a. Increment afterTime by a second, so that we are comparing against a
    // time after it!
    Date afterTime = aAfterTime;
    if (afterTime == null)
      afterTime = new Date (System.currentTimeMillis () + CGlobal.MILLISECONDS_PER_SECOND);
    else
      afterTime = new Date (afterTime.getTime () + CGlobal.MILLISECONDS_PER_SECOND);

    // make sure afterTime is at least startTime
    if (afterTime.before (m_aStartTime))
      afterTime = m_aStartTime;

    // b.Check to see if afterTime is after endTimeOfDay or not. If yes, then we
    // need to advance to next day as well.
    boolean afterTimePastEndTimeOfDay = false;
    if (m_aEndTimeOfDay != null)
    {
      afterTimePastEndTimeOfDay = afterTime.getTime () > CQuartz.onDate (m_aEndTimeOfDay, afterTime).getTime ();
    }
    // c. now we need to move move to the next valid day of week if either:
    // the given time is past the end time of day, or given time is not on a
    // valid day of week
    Date fireTime = _advanceToNextDayOfWeekIfNecessary (afterTime, afterTimePastEndTimeOfDay);
    if (fireTime == null)
      return null;

    // d. Calculate and save fireTimeEndDate variable for later use
    final Date fireTimeEndDate;
    if (m_aEndTimeOfDay == null)
      fireTimeEndDate = CQuartz.onDate (PDTFactory.createLocalTime (23, 59, 59), fireTime);
    else
      fireTimeEndDate = CQuartz.onDate (m_aEndTimeOfDay, fireTime);

    // e. Check fireTime against startTime or startTimeOfDay to see which go
    // first.
    final Date fireTimeStartDate = CQuartz.onDate (m_aStartTimeOfDay, fireTime);
    if (fireTime.before (fireTimeStartDate))
    {
      return fireTimeStartDate;
    }

    // f. Continue to calculate the fireTime by incremental unit of intervals.
    // recall that if fireTime was less that fireTimeStartDate, we didn't get
    // this far
    final long fireMillis = fireTime.getTime ();
    final long startMillis = fireTimeStartDate.getTime ();
    final long secondsAfterStart = (fireMillis - startMillis) / 1000L;
    final long repeatLong = getRepeatInterval ();
    final Calendar sTime = _createCalendarTime (fireTimeStartDate);
    final EIntervalUnit repeatUnit = getRepeatIntervalUnit ();
    if (repeatUnit.equals (EIntervalUnit.SECOND))
    {
      long jumpCount = secondsAfterStart / repeatLong;
      if (secondsAfterStart % repeatLong != 0)
        jumpCount++;
      sTime.add (Calendar.SECOND, getRepeatInterval () * (int) jumpCount);
      fireTime = sTime.getTime ();
    }
    else
      if (repeatUnit.equals (EIntervalUnit.MINUTE))
      {
        long jumpCount = secondsAfterStart / (repeatLong * 60L);
        if (secondsAfterStart % (repeatLong * 60L) != 0)
          jumpCount++;
        sTime.add (Calendar.MINUTE, getRepeatInterval () * (int) jumpCount);
        fireTime = sTime.getTime ();
      }
      else
        if (repeatUnit.equals (EIntervalUnit.HOUR))
        {
          long jumpCount = secondsAfterStart / (repeatLong * 60L * 60L);
          if (secondsAfterStart % (repeatLong * 60L * 60L) != 0)
            jumpCount++;
          sTime.add (Calendar.HOUR_OF_DAY, getRepeatInterval () * (int) jumpCount);
          fireTime = sTime.getTime ();
        }

    // g. Ensure this new fireTime is within the day, or else we need to advance
    // to next day.
    if (fireTime.after (fireTimeEndDate))
    {
      fireTime = _advanceToNextDayOfWeekIfNecessary (fireTime, _isSameDay (fireTime, fireTimeEndDate));
      // make sure we hit the startTimeOfDay on the new day
      fireTime = CQuartz.onDate (m_aStartTimeOfDay, fireTime);
    }

    // i. Return calculated fireTime.
    return fireTime;
  }

  private static boolean _isSameDay (final Date d1, final Date d2)
  {
    final Calendar c1 = _createCalendarTime (d1);
    final Calendar c2 = _createCalendarTime (d2);
    return c1.get (Calendar.YEAR) == c2.get (Calendar.YEAR) &&
           c1.get (Calendar.DAY_OF_YEAR) == c2.get (Calendar.DAY_OF_YEAR);
  }

  /**
   * Given fireTime time determine if it is on a valid day of week. If so,
   * simply return it unaltered, if not, advance to the next valid week day, and
   * set the time of day to the start time of day
   *
   * @param aFireTime
   *        - given next fireTime.
   * @param forceToAdvanceNextDay
   *        - flag to whether to advance day without check existing week day.
   *        This scenario can happen when a caller determine fireTime has passed
   *        the endTimeOfDay that fireTime should move to next day anyway.
   * @return a next day fireTime.
   */
  private Date _advanceToNextDayOfWeekIfNecessary (final Date aFireTime, final boolean forceToAdvanceNextDay)
  {
    // a. Advance or adjust to next dayOfWeek if need to first, starting next
    // day with startTimeOfDay.
    Date fireTime = aFireTime;
    final LocalTime aimeOfDay = getStartTimeOfDay ();
    final Date fireTimeStartDate = CQuartz.onDate (aimeOfDay, fireTime);
    final Calendar fireTimeStartDateCal = _createCalendarTime (fireTimeStartDate);
    int nDayOfWeekOfFireTime = fireTimeStartDateCal.get (Calendar.DAY_OF_WEEK);
    final int nCalDay = nDayOfWeekOfFireTime;
    DayOfWeek eDayOfWeekOfFireTime = PDTHelper.getAsDayOfWeek (nCalDay);

    // b2. We need to advance to another day if isAfterTimePassEndTimeOfDay is
    // true, or dayOfWeek is not set.
    final Set <DayOfWeek> daysOfWeekToFire = getDaysOfWeek ();
    if (forceToAdvanceNextDay || !daysOfWeekToFire.contains (eDayOfWeekOfFireTime))
    {
      // Advance one day at a time until next available date.
      for (int i = 1; i <= 7; i++)
      {
        fireTimeStartDateCal.add (Calendar.DATE, 1);
        nDayOfWeekOfFireTime = fireTimeStartDateCal.get (Calendar.DAY_OF_WEEK);
        final int nCalDay1 = nDayOfWeekOfFireTime;
        eDayOfWeekOfFireTime = PDTHelper.getAsDayOfWeek (nCalDay1);
        if (daysOfWeekToFire.contains (eDayOfWeekOfFireTime))
        {
          fireTime = fireTimeStartDateCal.getTime ();
          break;
        }
      }
    }

    // Check fireTime not pass the endTime
    final Date eTime = getEndTime ();
    if (eTime != null && fireTime.getTime () > eTime.getTime ())
    {
      return null;
    }

    return fireTime;
  }

  /**
   * <p>
   * Returns the final time at which the <code>DailyTimeIntervalTrigger</code>
   * will fire, if there is no end time set, null will be returned.
   * </p>
   * <p>
   * Note that the return time may be in the past.
   * </p>
   */
  public Date getFinalFireTime ()
  {
    if (m_bComplete || getEndTime () == null)
    {
      return null;
    }

    // We have an endTime, we still need to check to see if there is a
    // endTimeOfDay if that's applicable.
    Date eTime = getEndTime ();
    if (m_aEndTimeOfDay != null)
    {
      final Date endTimeOfDayDate = CQuartz.onDate (m_aEndTimeOfDay, eTime);
      if (eTime.getTime () < endTimeOfDayDate.getTime ())
      {
        eTime = endTimeOfDayDate;
      }
    }
    return eTime;
  }

  /**
   * <p>
   * Determines whether or not the <code>DailyTimeIntervalTrigger</code> will
   * occur again.
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

    if (m_eRepeatIntervalUnit == null ||
        !(m_eRepeatIntervalUnit.equals (EIntervalUnit.SECOND) ||
          m_eRepeatIntervalUnit.equals (EIntervalUnit.MINUTE) ||
          m_eRepeatIntervalUnit.equals (EIntervalUnit.HOUR)))
      throw new SchedulerException ("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
    if (m_nRepeatInterval < 1)
    {
      throw new SchedulerException ("Repeat Interval cannot be zero.");
    }

    // Ensure interval does not exceed 24 hours
    final long secondsInHour = 24 * 60 * 60L;
    if (m_eRepeatIntervalUnit == EIntervalUnit.SECOND && m_nRepeatInterval > secondsInHour)
    {
      throw new SchedulerException ("repeatInterval can not exceed 24 hours (" +
                                    secondsInHour +
                                    " seconds). Given " +
                                    m_nRepeatInterval);
    }
    if (m_eRepeatIntervalUnit == EIntervalUnit.MINUTE && m_nRepeatInterval > secondsInHour / 60L)
    {
      throw new SchedulerException ("repeatInterval can not exceed 24 hours (" +
                                    secondsInHour / 60L +
                                    " minutes). Given " +
                                    m_nRepeatInterval);
    }
    if (m_eRepeatIntervalUnit == EIntervalUnit.HOUR && m_nRepeatInterval > 24)
    {
      throw new SchedulerException ("repeatInterval can not exceed 24 hours. Given " + m_nRepeatInterval + " hours.");
    }

    // Ensure timeOfDay is in order.
    if (getEndTimeOfDay () != null && !getStartTimeOfDay ().isBefore (getEndTimeOfDay ()))
    {
      throw new SchedulerException ("StartTimeOfDay " +
                                    m_aStartTimeOfDay +
                                    " should not come after endTimeOfDay " +
                                    m_aEndTimeOfDay);
    }
  }

  public Set <DayOfWeek> getDaysOfWeek ()
  {
    if (m_aDaysOfWeek == null)
      m_aDaysOfWeek = EnumSet.allOf (DayOfWeek.class);
    return m_aDaysOfWeek;
  }

  public void setDaysOfWeek (final Set <DayOfWeek> daysOfWeek)
  {
    ValueEnforcer.notEmpty (daysOfWeek, "DaysOfWeek");
    m_aDaysOfWeek = EnumSet.copyOf (daysOfWeek);
  }

  public LocalTime getStartTimeOfDay ()
  {
    if (m_aStartTimeOfDay == null)
      m_aStartTimeOfDay = LocalTime.MIDNIGHT;
    return m_aStartTimeOfDay;
  }

  public void setStartTimeOfDay (final LocalTime startTimeOfDay)
  {
    ValueEnforcer.notNull (startTimeOfDay, "StartTimeOfDay");

    final LocalTime aEndTimeOfDay = getEndTimeOfDay ();
    if (aEndTimeOfDay != null && aEndTimeOfDay.isBefore (startTimeOfDay))
      throw new IllegalArgumentException ("End time of day cannot be before start time of day");

    m_aStartTimeOfDay = startTimeOfDay;
  }

  /**
   * {@inheritDoc}
   */
  public LocalTime getEndTimeOfDay ()
  {
    return m_aEndTimeOfDay;
  }

  public void setEndTimeOfDay (final LocalTime aEndTimeOfDay)
  {
    ValueEnforcer.notNull (aEndTimeOfDay, "EndTimeOfDay");
    final LocalTime aStartTimeOfDay = getStartTimeOfDay ();
    if (aStartTimeOfDay != null && aEndTimeOfDay.isBefore (aEndTimeOfDay))
      throw new IllegalArgumentException ("End time of day cannot be before start time of day");
    m_aEndTimeOfDay = aEndTimeOfDay;
  }

  /**
   * Get a {@link IScheduleBuilder} that is configured to produce a schedule
   * identical to this trigger's schedule.
   *
   * @see #getTriggerBuilder()
   */
  @Override
  public IScheduleBuilder <DailyTimeIntervalTrigger> getScheduleBuilder ()
  {
    final DailyTimeIntervalScheduleBuilder cb = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule ()
                                                                                .withInterval (getRepeatInterval (),
                                                                                               getRepeatIntervalUnit ())
                                                                                .onDaysOfTheWeek (getDaysOfWeek ())
                                                                                .startingDailyAt (getStartTimeOfDay ())
                                                                                .endingDailyAt (getEndTimeOfDay ());

    switch (getMisfireInstruction ())
    {
      case MISFIRE_INSTRUCTION_DO_NOTHING:
        cb.withMisfireHandlingInstructionDoNothing ();
        break;
      case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
        cb.withMisfireHandlingInstructionFireAndProceed ();
        break;
    }
    return cb;
  }

  public int getRepeatCount ()
  {
    return m_nRepeatCount;
  }

  public void setRepeatCount (final int repeatCount)
  {
    if (repeatCount < 0 && repeatCount != REPEAT_INDEFINITELY)
    {
      throw new IllegalArgumentException ("Repeat count must be >= 0, use the " +
                                          "constant REPEAT_INDEFINITELY for infinite.");
    }

    m_nRepeatCount = repeatCount;
  }

  @Nonnull
  public DailyTimeIntervalTrigger getClone ()
  {
    return new DailyTimeIntervalTrigger (this);
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
}
