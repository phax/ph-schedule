/*
 * Copyright 2001-2009 Terracotta, Inc.
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
package com.helger.quartz.impl.triggers;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import com.helger.quartz.DailyTimeIntervalScheduleBuilder;
import com.helger.quartz.DateBuilder.IntervalUnit;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IDailyTimeIntervalTrigger;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.ScheduleBuilder;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.TimeOfDay;

/**
 * A concrete implementation of DailyTimeIntervalTrigger that is used to fire a
 * <code>{@link com.helger.quartz.IJobDetail}</code> based upon daily repeating
 * time intervals.
 * <p>
 * The trigger will fire every N (see {@link #setRepeatInterval(int)} ) seconds,
 * minutes or hours (see
 * {@link #setRepeatIntervalUnit(com.helger.quartz.DateBuilder.IntervalUnit)})
 * during a given time window on specified days of the week.
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
public class DailyTimeIntervalTrigger extends AbstractTrigger <IDailyTimeIntervalTrigger>
                                      implements IDailyTimeIntervalTrigger, ICoreTrigger
{
  private static final int YEAR_TO_GIVEUP_SCHEDULING_AT = Calendar.getInstance ().get (Calendar.YEAR) + 100;

  private Date startTime = null;
  private Date endTime = null;
  private Date nextFireTime = null;
  private Date previousFireTime = null;
  private int repeatCount = REPEAT_INDEFINITELY;
  private int repeatInterval = 1;
  private IntervalUnit repeatIntervalUnit = IntervalUnit.MINUTE;
  private Set <Integer> daysOfWeek;
  private TimeOfDay startTimeOfDay;
  private TimeOfDay endTimeOfDay;
  private int timesTriggered = 0;
  private boolean complete = false;

  /**
   * <p>
   * Create a <code>DailyTimeIntervalTrigger</code> with no settings.
   * </p>
   */
  public DailyTimeIntervalTrigger ()
  {
    super ();
  }

  /**
   * <p>
   * Create a <code>DailyTimeIntervalTrigger</code> that will occur immediately,
   * and repeat at the the given interval.
   * </p>
   *
   * @param startTimeOfDay
   *        The <code>TimeOfDay</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>TimeOfDay</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link IntervalUnit#SECOND},
   *        {@link IntervalUnit#MINUTE}, and {@link IntervalUnit#HOUR}.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final TimeOfDay startTimeOfDay,
                                   final TimeOfDay endTimeOfDay,
                                   final IntervalUnit intervalUnit,
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
   *        The <code>TimeOfDay</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>TimeOfDay</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link IntervalUnit#SECOND},
   *        {@link IntervalUnit#MINUTE}, and {@link IntervalUnit#HOUR}.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final String group,
                                   final TimeOfDay startTimeOfDay,
                                   final TimeOfDay endTimeOfDay,
                                   final IntervalUnit intervalUnit,
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
   *        The <code>TimeOfDay</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>TimeOfDay</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link IntervalUnit#SECOND},
   *        {@link IntervalUnit#MINUTE}, and {@link IntervalUnit#HOUR}.
   * @param repeatInterval
   *        The number of milliseconds to pause between the repeat firing.
   * @throws IllegalArgumentException
   *         if an invalid IntervalUnit is given, or the repeat interval is zero
   *         or less.
   */
  public DailyTimeIntervalTrigger (final String name,
                                   final Date startTime,
                                   final Date endTime,
                                   final TimeOfDay startTimeOfDay,
                                   final TimeOfDay endTimeOfDay,
                                   final IntervalUnit intervalUnit,
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
   *        The <code>TimeOfDay</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>TimeOfDay</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link IntervalUnit#SECOND},
   *        {@link IntervalUnit#MINUTE}, and {@link IntervalUnit#HOUR}.
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
                                   final TimeOfDay startTimeOfDay,
                                   final TimeOfDay endTimeOfDay,
                                   final IntervalUnit intervalUnit,
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
   *        The <code>TimeOfDay</code> that the repeating should begin
   *        occurring.
   * @param endTimeOfDay
   *        The <code>TimeOfDay</code> that the repeating should stop occurring.
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link IntervalUnit#SECOND},
   *        {@link IntervalUnit#MINUTE}, and {@link IntervalUnit#HOUR}.
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
                                   final TimeOfDay startTimeOfDay,
                                   final TimeOfDay endTimeOfDay,
                                   final IntervalUnit intervalUnit,
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

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Get the time at which the <code>DailyTimeIntervalTrigger</code> should
   * occur. It defaults to the getStartTimeOfDay of current day.
   * </p>
   */
  @Override
  public Date getStartTime ()
  {
    if (startTime == null)
    {
      startTime = new Date ();
    }
    return startTime;
  }

  /**
   * <p>
   * Set the time at which the <code>DailyTimeIntervalTrigger</code> should
   * occur.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if startTime is <code>null</code>.
   */
  @Override
  public void setStartTime (final Date startTime)
  {
    if (startTime == null)
    {
      throw new IllegalArgumentException ("Start time cannot be null");
    }

    final Date eTime = getEndTime ();
    if (eTime != null && eTime.before (startTime))
    {
      throw new IllegalArgumentException ("End time cannot be before start time");
    }

    this.startTime = startTime;
  }

  /**
   * <p>
   * Get the time at which the <code>DailyTimeIntervalTrigger</code> should quit
   * repeating.
   * </p>
   *
   * @see #getFinalFireTime()
   */
  @Override
  public Date getEndTime ()
  {
    return endTime;
  }

  /**
   * <p>
   * Set the time at which the <code>DailyTimeIntervalTrigger</code> should quit
   * repeating (and be automatically deleted).
   * </p>
   *
   * @exception IllegalArgumentException
   *            if endTime is before start time.
   */
  @Override
  public void setEndTime (final Date endTime)
  {
    final Date sTime = getStartTime ();
    if (sTime != null && endTime != null && sTime.after (endTime))
    {
      throw new IllegalArgumentException ("End time cannot be before start time");
    }

    this.endTime = endTime;
  }

  public IntervalUnit getRepeatIntervalUnit ()
  {
    return repeatIntervalUnit;
  }

  /**
   * <p>
   * Set the interval unit - the time unit on with the interval applies.
   * </p>
   *
   * @param intervalUnit
   *        The repeat interval unit. The only intervals that are valid for this
   *        type of trigger are {@link IntervalUnit#SECOND},
   *        {@link IntervalUnit#MINUTE}, and {@link IntervalUnit#HOUR}.
   */
  public void setRepeatIntervalUnit (final IntervalUnit intervalUnit)
  {
    if (repeatIntervalUnit == null ||
        !((repeatIntervalUnit.equals (IntervalUnit.SECOND) ||
           repeatIntervalUnit.equals (IntervalUnit.MINUTE) ||
           repeatIntervalUnit.equals (IntervalUnit.HOUR))))
      throw new IllegalArgumentException ("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
    this.repeatIntervalUnit = intervalUnit;
  }

  public int getRepeatInterval ()
  {
    return repeatInterval;
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
    if (repeatInterval < 0)
      throw new IllegalArgumentException ("Repeat interval must be >= 0");

    this.repeatInterval = repeatInterval;
  }

  public int getTimesTriggered ()
  {
    return timesTriggered;
  }

  /**
   * <p>
   * Set the number of times the <code>DailyTimeIntervalTrigger</code> has
   * already fired.
   * </p>
   */
  public void setTimesTriggered (final int timesTriggered)
  {
    this.timesTriggered = timesTriggered;
  }

  @Override
  protected boolean validateMisfireInstruction (final int misfireInstruction)
  {
    return misfireInstruction >= MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY &&
           misfireInstruction <= MISFIRE_INSTRUCTION_DO_NOTHING;

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
  @Override
  public void updateAfterMisfire (final ICalendar cal)
  {
    int instr = getMisfireInstruction ();

    if (instr == ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY)
      return;

    if (instr == MISFIRE_INSTRUCTION_SMART_POLICY)
    {
      instr = MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    }

    if (instr == MISFIRE_INSTRUCTION_DO_NOTHING)
    {
      Date newFireTime = getFireTimeAfter (new Date ());
      while (newFireTime != null && cal != null && !cal.isTimeIncluded (newFireTime.getTime ()))
      {
        newFireTime = getFireTimeAfter (newFireTime);
      }
      setNextFireTime (newFireTime);
    }
    else
      if (instr == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW)
      {
        // fire once now...
        setNextFireTime (new Date ());
        // the new fire time afterward will magically preserve the original
        // time of day for firing for day/week/month interval triggers,
        // because of the way getFireTimeAfter() works - in its always
        // restarting
        // computation from the start time.
      }
  }

  /**
   * <p>
   * Called when the <code>{@link IScheduler}</code> has decided to 'fire' the
   * trigger (execute the associated <code>Job</code>), in order to give the
   * <code>Trigger</code> a chance to update itself for its next triggering (if
   * any).
   * </p>
   *
   * @see #executionComplete(IJobExecutionContext, JobExecutionException)
   */
  @Override
  public void triggered (final com.helger.quartz.ICalendar calendar)
  {
    timesTriggered++;
    previousFireTime = nextFireTime;
    nextFireTime = getFireTimeAfter (nextFireTime);

    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded (nextFireTime.getTime ()))
    {

      nextFireTime = getFireTimeAfter (nextFireTime);

      if (nextFireTime == null)
        break;

      // avoid infinite loop
      final Calendar c = Calendar.getInstance ();
      c.setTime (nextFireTime);
      if (c.get (Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT)
      {
        nextFireTime = null;
      }
    }

    if (nextFireTime == null)
    {
      complete = true;
    }
  }

  /**
   * @see com.helger.quartz.impl.triggers.AbstractTrigger#updateWithNewCalendar(com.helger.quartz.ICalendar,
   *      long)
   */
  @Override
  public void updateWithNewCalendar (final com.helger.quartz.ICalendar calendar, final long misfireThreshold)
  {
    nextFireTime = getFireTimeAfter (previousFireTime);

    if (nextFireTime == null || calendar == null)
    {
      return;
    }

    final Date now = new Date ();
    while (nextFireTime != null && !calendar.isTimeIncluded (nextFireTime.getTime ()))
    {

      nextFireTime = getFireTimeAfter (nextFireTime);

      if (nextFireTime == null)
        break;

      // avoid infinite loop
      final Calendar c = Calendar.getInstance ();
      c.setTime (nextFireTime);
      if (c.get (Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT)
      {
        nextFireTime = null;
      }

      if (nextFireTime != null && nextFireTime.before (now))
      {
        final long diff = now.getTime () - nextFireTime.getTime ();
        if (diff >= misfireThreshold)
        {
          nextFireTime = getFireTimeAfter (nextFireTime);
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
    nextFireTime = getFireTimeAfter (new Date (getStartTime ().getTime () - 1000L));

    // Check calendar for date-time exclusion
    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded (nextFireTime.getTime ()))
    {

      nextFireTime = getFireTimeAfter (nextFireTime);

      if (nextFireTime == null)
        break;

      // avoid infinite loop
      final Calendar c = Calendar.getInstance ();
      c.setTime (nextFireTime);
      if (c.get (Calendar.YEAR) > YEAR_TO_GIVEUP_SCHEDULING_AT)
      {
        return null;
      }
    }

    return nextFireTime;
  }

  private Calendar createCalendarTime (final Date dateTime)
  {
    final Calendar cal = Calendar.getInstance ();
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
  @Override
  public Date getNextFireTime ()
  {
    return nextFireTime;
  }

  /**
   * <p>
   * Returns the previous time at which the
   * <code>DailyTimeIntervalTrigger</code> fired. If the trigger has not yet
   * fired, <code>null</code> will be returned.
   */
  @Override
  public Date getPreviousFireTime ()
  {
    return previousFireTime;
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
    this.nextFireTime = nextFireTime;
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
    this.previousFireTime = previousFireTime;
  }

  /**
   * <p>
   * Returns the next time at which the <code>DailyTimeIntervalTrigger</code>
   * will fire, after the given time. If the trigger will not fire after the
   * given time, <code>null</code> will be returned.
   * </p>
   *
   * @param aAfterTime
   */
  @Override
  public Date getFireTimeAfter (final Date aAfterTime)
  {
    // Check if trigger has completed or not.
    if (complete)
    {
      return null;
    }

    // Check repeatCount limit
    if (repeatCount != REPEAT_INDEFINITELY && timesTriggered > repeatCount)
    {
      return null;
    }

    // a. Increment afterTime by a second, so that we are comparing against a
    // time after it!
    Date afterTime = aAfterTime;
    if (afterTime == null)
      afterTime = new Date (System.currentTimeMillis () + 1000L);
    else
      afterTime = new Date (afterTime.getTime () + 1000L);

    // make sure afterTime is at least startTime
    if (afterTime.before (startTime))
      afterTime = startTime;

    // b.Check to see if afterTime is after endTimeOfDay or not. If yes, then we
    // need to advance to next day as well.
    boolean afterTimePastEndTimeOfDay = false;
    if (endTimeOfDay != null)
    {
      afterTimePastEndTimeOfDay = afterTime.getTime () > endTimeOfDay.getTimeOfDayForDate (afterTime).getTime ();
    }
    // c. now we need to move move to the next valid day of week if either:
    // the given time is past the end time of day, or given time is not on a
    // valid day of week
    Date fireTime = advanceToNextDayOfWeekIfNecessary (afterTime, afterTimePastEndTimeOfDay);
    if (fireTime == null)
      return null;

    // d. Calculate and save fireTimeEndDate variable for later use
    Date fireTimeEndDate = null;
    if (endTimeOfDay == null)
      fireTimeEndDate = new TimeOfDay (23, 59, 59).getTimeOfDayForDate (fireTime);
    else
      fireTimeEndDate = endTimeOfDay.getTimeOfDayForDate (fireTime);

    // e. Check fireTime against startTime or startTimeOfDay to see which go
    // first.
    final Date fireTimeStartDate = startTimeOfDay.getTimeOfDayForDate (fireTime);
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
    final Calendar sTime = createCalendarTime (fireTimeStartDate);
    final IntervalUnit repeatUnit = getRepeatIntervalUnit ();
    if (repeatUnit.equals (IntervalUnit.SECOND))
    {
      long jumpCount = secondsAfterStart / repeatLong;
      if (secondsAfterStart % repeatLong != 0)
        jumpCount++;
      sTime.add (Calendar.SECOND, getRepeatInterval () * (int) jumpCount);
      fireTime = sTime.getTime ();
    }
    else
      if (repeatUnit.equals (IntervalUnit.MINUTE))
      {
        long jumpCount = secondsAfterStart / (repeatLong * 60L);
        if (secondsAfterStart % (repeatLong * 60L) != 0)
          jumpCount++;
        sTime.add (Calendar.MINUTE, getRepeatInterval () * (int) jumpCount);
        fireTime = sTime.getTime ();
      }
      else
        if (repeatUnit.equals (IntervalUnit.HOUR))
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
      fireTime = advanceToNextDayOfWeekIfNecessary (fireTime, isSameDay (fireTime, fireTimeEndDate));
      // make sure we hit the startTimeOfDay on the new day
      fireTime = startTimeOfDay.getTimeOfDayForDate (fireTime);
    }

    // i. Return calculated fireTime.
    return fireTime;
  }

  private boolean isSameDay (final Date d1, final Date d2)
  {

    final Calendar c1 = createCalendarTime (d1);
    final Calendar c2 = createCalendarTime (d2);

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
  private Date advanceToNextDayOfWeekIfNecessary (final Date aFireTime, final boolean forceToAdvanceNextDay)
  {
    // a. Advance or adjust to next dayOfWeek if need to first, starting next
    // day with startTimeOfDay.
    Date fireTime = aFireTime;
    final TimeOfDay sTimeOfDay = getStartTimeOfDay ();
    final Date fireTimeStartDate = sTimeOfDay.getTimeOfDayForDate (fireTime);
    final Calendar fireTimeStartDateCal = createCalendarTime (fireTimeStartDate);
    int dayOfWeekOfFireTime = fireTimeStartDateCal.get (Calendar.DAY_OF_WEEK);

    // b2. We need to advance to another day if isAfterTimePassEndTimeOfDay is
    // true, or dayOfWeek is not set.
    final Set <Integer> daysOfWeekToFire = getDaysOfWeek ();
    if (forceToAdvanceNextDay || !daysOfWeekToFire.contains (dayOfWeekOfFireTime))
    {
      // Advance one day at a time until next available date.
      for (int i = 1; i <= 7; i++)
      {
        fireTimeStartDateCal.add (Calendar.DATE, 1);
        dayOfWeekOfFireTime = fireTimeStartDateCal.get (Calendar.DAY_OF_WEEK);
        if (daysOfWeekToFire.contains (dayOfWeekOfFireTime))
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
  @Override
  public Date getFinalFireTime ()
  {
    if (complete || getEndTime () == null)
    {
      return null;
    }

    // We have an endTime, we still need to check to see if there is a
    // endTimeOfDay if that's applicable.
    Date eTime = getEndTime ();
    if (endTimeOfDay != null)
    {
      final Date endTimeOfDayDate = endTimeOfDay.getTimeOfDayForDate (eTime);
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
  @Override
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

    if (repeatIntervalUnit == null ||
        !(repeatIntervalUnit.equals (IntervalUnit.SECOND) ||
          repeatIntervalUnit.equals (IntervalUnit.MINUTE) ||
          repeatIntervalUnit.equals (IntervalUnit.HOUR)))
      throw new SchedulerException ("Invalid repeat IntervalUnit (must be SECOND, MINUTE or HOUR).");
    if (repeatInterval < 1)
    {
      throw new SchedulerException ("Repeat Interval cannot be zero.");
    }

    // Ensure interval does not exceed 24 hours
    final long secondsInHour = 24 * 60 * 60L;
    if (repeatIntervalUnit == IntervalUnit.SECOND && repeatInterval > secondsInHour)
    {
      throw new SchedulerException ("repeatInterval can not exceed 24 hours (" +
                                    secondsInHour +
                                    " seconds). Given " +
                                    repeatInterval);
    }
    if (repeatIntervalUnit == IntervalUnit.MINUTE && repeatInterval > secondsInHour / 60L)
    {
      throw new SchedulerException ("repeatInterval can not exceed 24 hours (" +
                                    secondsInHour / 60L +
                                    " minutes). Given " +
                                    repeatInterval);
    }
    if (repeatIntervalUnit == IntervalUnit.HOUR && repeatInterval > 24)
    {
      throw new SchedulerException ("repeatInterval can not exceed 24 hours. Given " + repeatInterval + " hours.");
    }

    // Ensure timeOfDay is in order.
    if (getEndTimeOfDay () != null && !getStartTimeOfDay ().before (getEndTimeOfDay ()))
    {
      throw new SchedulerException ("StartTimeOfDay " +
                                    startTimeOfDay +
                                    " should not come after endTimeOfDay " +
                                    endTimeOfDay);
    }
  }

  /**
   * {@inheritDoc}
   */
  public Set <Integer> getDaysOfWeek ()
  {
    if (daysOfWeek == null)
    {
      daysOfWeek = DailyTimeIntervalScheduleBuilder.ALL_DAYS_OF_THE_WEEK;
    }
    return daysOfWeek;
  }

  public void setDaysOfWeek (final Set <Integer> daysOfWeek)
  {
    if (daysOfWeek == null || daysOfWeek.size () == 0)
      throw new IllegalArgumentException ("DaysOfWeek set must be a set that contains at least one day.");
    else
      if (daysOfWeek.size () == 0)
        throw new IllegalArgumentException ("DaysOfWeek set must contain at least one day.");

    this.daysOfWeek = daysOfWeek;
  }

  /**
   * {@inheritDoc}
   */
  public TimeOfDay getStartTimeOfDay ()
  {
    if (startTimeOfDay == null)
    {
      startTimeOfDay = new TimeOfDay (0, 0, 0);
    }
    return startTimeOfDay;
  }

  public void setStartTimeOfDay (final TimeOfDay startTimeOfDay)
  {
    if (startTimeOfDay == null)
    {
      throw new IllegalArgumentException ("Start time of day cannot be null");
    }

    final TimeOfDay eTime = getEndTimeOfDay ();
    if (eTime != null && eTime.before (startTimeOfDay))
    {
      throw new IllegalArgumentException ("End time of day cannot be before start time of day");
    }

    this.startTimeOfDay = startTimeOfDay;
  }

  /**
   * {@inheritDoc}
   */
  public TimeOfDay getEndTimeOfDay ()
  {
    return endTimeOfDay;
  }

  public void setEndTimeOfDay (final TimeOfDay endTimeOfDay)
  {
    if (endTimeOfDay == null)
      throw new IllegalArgumentException ("End time of day cannot be null");

    final TimeOfDay sTime = getStartTimeOfDay ();
    if (sTime != null && endTimeOfDay.before (endTimeOfDay))
    {
      throw new IllegalArgumentException ("End time of day cannot be before start time of day");
    }
    this.endTimeOfDay = endTimeOfDay;
  }

  /**
   * Get a {@link ScheduleBuilder} that is configured to produce a schedule
   * identical to this trigger's schedule.
   *
   * @see #getTriggerBuilder()
   */
  @Override
  public ScheduleBuilder <IDailyTimeIntervalTrigger> getScheduleBuilder ()
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

  /**
   * This trigger has no additional properties besides what's defined in this
   * class.
   */
  public boolean hasAdditionalProperties ()
  {
    return false;
  }

  public int getRepeatCount ()
  {
    return repeatCount;
  }

  public void setRepeatCount (final int repeatCount)
  {
    if (repeatCount < 0 && repeatCount != REPEAT_INDEFINITELY)
    {
      throw new IllegalArgumentException ("Repeat count must be >= 0, use the " +
                                          "constant REPEAT_INDEFINITELY for infinite.");
    }

    this.repeatCount = repeatCount;
  }
}
