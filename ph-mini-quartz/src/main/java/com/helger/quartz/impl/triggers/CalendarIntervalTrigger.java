/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2022 Philip Helger (www.helger.com)
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
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.quartz.CQuartz;
import com.helger.quartz.CalendarIntervalScheduleBuilder;
import com.helger.quartz.EIntervalUnit;
import com.helger.quartz.ICalendar;
import com.helger.quartz.ICalendarIntervalTrigger;
import com.helger.quartz.QCloneUtils;
import com.helger.quartz.SchedulerException;

/**
 * <p>
 * A concrete <code>{@link com.helger.quartz.ITrigger}</code> that is used to
 * fire a <code>{@link com.helger.quartz.IJobDetail}</code> based upon repeating
 * calendar time intervals.
 * </p>
 * <p>
 * The trigger will fire every N (see {@link #setRepeatInterval(int)} ) units of
 * calendar time (see {@link #setRepeatIntervalUnit(EIntervalUnit)}) as
 * specified in the trigger's definition. This trigger can achieve schedules
 * that are not possible with {@link com.helger.quartz.ISimpleTrigger} (e.g
 * because months are not a fixed number of seconds) or
 * {@link com.helger.quartz.ICronTrigger} (e.g. because "every 5 months" is not
 * an even divisor of 12).
 * </p>
 * <p>
 * If you use an interval unit of <code>MONTH</code> then care should be taken
 * when setting a <code>startTime</code> value that is on a day near the end of
 * the month. For example, if you choose a start time that occurs on January
 * 31st, and have a trigger with unit <code>MONTH</code> and interval
 * <code>1</code>, then the next fire time will be February 28th, and the next
 * time after that will be March 28th - and essentially each subsequent firing
 * will occur on the 28th of the month, even if a 31st day exists. If you want a
 * trigger that always fires on the last day of the month - regardless of the
 * number of days in the month, you should use <code>CronTrigger</code>.
 * </p>
 *
 * @see com.helger.quartz.ITrigger
 * @see com.helger.quartz.ICronTrigger
 * @see com.helger.quartz.ISimpleTrigger
 * @see com.helger.quartz.TriggerUtils
 * @since 1.7
 * @author James House
 */
public class CalendarIntervalTrigger extends AbstractTrigger <CalendarIntervalTrigger> implements
                                     ICalendarIntervalTrigger
{
  private Date m_aStartTime;
  private Date m_aEndTime;
  private Date m_aNextFireTime;
  private Date m_aPreviousFireTime;
  private int m_nRepeatInterval = 0;
  private EIntervalUnit m_eRepeatIntervalUnit = EIntervalUnit.DAY;
  private TimeZone m_aTimeZone;
  // false is backward-compatible with behavior
  private boolean m_bPreserveHourOfDayAcrossDaylightSavings = false;
  private boolean m_bSkipDayIfHourDoesNotExist = false;
  private int m_nTimesTriggered = 0;

  public CalendarIntervalTrigger (@Nonnull final CalendarIntervalTrigger aOther)
  {
    super (aOther);
    m_aStartTime = QCloneUtils.getClone (aOther.m_aStartTime);
    m_aEndTime = QCloneUtils.getClone (aOther.m_aEndTime);
    m_aNextFireTime = QCloneUtils.getClone (aOther.m_aNextFireTime);
    m_aPreviousFireTime = QCloneUtils.getClone (aOther.m_aPreviousFireTime);
    m_nRepeatInterval = aOther.m_nRepeatInterval;
    m_eRepeatIntervalUnit = aOther.m_eRepeatIntervalUnit;
    m_aTimeZone = QCloneUtils.getClone (aOther.m_aTimeZone);
    m_bPreserveHourOfDayAcrossDaylightSavings = aOther.m_bPreserveHourOfDayAcrossDaylightSavings;
    m_bSkipDayIfHourDoesNotExist = aOther.m_bSkipDayIfHourDoesNotExist;
    m_nTimesTriggered = aOther.m_nTimesTriggered;
  }

  /**
   * Create a <code>DateIntervalTrigger</code> with no settings.
   */
  public CalendarIntervalTrigger ()
  {
    super ();
  }

  /**
   * Create a <code>DateIntervalTrigger</code> that will occur immediately, and
   * repeat at the the given interval.
   */
  public CalendarIntervalTrigger (final String name, final EIntervalUnit intervalUnit, final int repeatInterval)
  {
    this (name, null, intervalUnit, repeatInterval);
  }

  /**
   * <p>
   * Create a <code>DateIntervalTrigger</code> that will occur immediately, and
   * repeat at the the given interval.
   * </p>
   */
  public CalendarIntervalTrigger (final String name,
                                  final String group,
                                  final EIntervalUnit intervalUnit,
                                  final int repeatInterval)
  {
    this (name, group, new Date (), null, intervalUnit, repeatInterval);
  }

  /**
   * <p>
   * Create a <code>DateIntervalTrigger</code> that will occur at the given
   * time, and repeat at the the given interval until the given end time.
   * </p>
   *
   * @param startTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        fire.
   * @param endTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        quit repeat firing.
   * @param intervalUnit
   *        The repeat interval unit (minutes, days, months, etc).
   * @param repeatInterval
   *        The number of milliseconds to pause between the repeat firing.
   */
  public CalendarIntervalTrigger (final String name,
                                  final Date startTime,
                                  final Date endTime,
                                  final EIntervalUnit intervalUnit,
                                  final int repeatInterval)
  {
    this (name, null, startTime, endTime, intervalUnit, repeatInterval);
  }

  /**
   * <p>
   * Create a <code>DateIntervalTrigger</code> that will occur at the given
   * time, and repeat at the the given interval until the given end time.
   * </p>
   *
   * @param startTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        fire.
   * @param endTime
   *        A <code>Date</code> set to the time for the <code>Trigger</code> to
   *        quit repeat firing.
   * @param intervalUnit
   *        The repeat interval unit (minutes, days, months, etc).
   * @param repeatInterval
   *        The number of milliseconds to pause between the repeat firing.
   */
  public CalendarIntervalTrigger (final String name,
                                  final String group,
                                  final Date startTime,
                                  final Date endTime,
                                  final EIntervalUnit intervalUnit,
                                  final int repeatInterval)
  {
    super (name, group);

    setStartTime (startTime);
    setEndTime (endTime);
    setRepeatIntervalUnit (intervalUnit);
    setRepeatInterval (repeatInterval);
  }

  /**
   * <p>
   * Create a <code>DateIntervalTrigger</code> that will occur at the given
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
   * @param intervalUnit
   *        The repeat interval unit (minutes, days, months, etc).
   * @param repeatInterval
   *        The number of milliseconds to pause between the repeat firing.
   */
  public CalendarIntervalTrigger (final String name,
                                  final String group,
                                  final String jobName,
                                  final String jobGroup,
                                  final Date startTime,
                                  final Date endTime,
                                  final EIntervalUnit intervalUnit,
                                  final int repeatInterval)
  {
    super (name, group, jobName, jobGroup);

    setStartTime (startTime);
    setEndTime (endTime);
    setRepeatIntervalUnit (intervalUnit);
    setRepeatInterval (repeatInterval);
  }

  public final Date getStartTime ()
  {
    if (m_aStartTime == null)
      m_aStartTime = new Date ();
    return m_aStartTime;
  }

  public final void setStartTime (final Date startTime)
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
   */
  public void setRepeatIntervalUnit (final EIntervalUnit intervalUnit)
  {
    m_eRepeatIntervalUnit = intervalUnit;
  }

  public int getRepeatInterval ()
  {
    return m_nRepeatInterval;
  }

  /**
   * <p>
   * set the the time interval that will be added to the
   * <code>DateIntervalTrigger</code>'s fire time (in the set repeat interval
   * unit) in order to calculate the time of the next trigger repeat.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if repeatInterval is &lt; 0
   */
  public void setRepeatInterval (final int repeatInterval)
  {
    if (repeatInterval < 0)
      throw new IllegalArgumentException ("Repeat interval must be >= 0");

    m_nRepeatInterval = repeatInterval;
  }

  public TimeZone getTimeZone ()
  {

    if (m_aTimeZone == null)
    {
      m_aTimeZone = TimeZone.getDefault ();
    }
    return m_aTimeZone;
  }

  /**
   * <p>
   * Sets the time zone within which time calculations related to this trigger
   * will be performed.
   * </p>
   *
   * @param timeZone
   *        the desired TimeZone, or null for the system default.
   */
  public void setTimeZone (final TimeZone timeZone)
  {
    m_aTimeZone = timeZone;
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
   * @see #isSkipDayIfHourDoesNotExist()
   * @see #getStartTime()
   * @see #getTimeZone()
   */
  public boolean isPreserveHourOfDayAcrossDaylightSavings ()
  {
    return m_bPreserveHourOfDayAcrossDaylightSavings;
  }

  public void setPreserveHourOfDayAcrossDaylightSavings (final boolean preserveHourOfDayAcrossDaylightSavings)
  {
    m_bPreserveHourOfDayAcrossDaylightSavings = preserveHourOfDayAcrossDaylightSavings;
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
   * @see #isPreserveHourOfDayAcrossDaylightSavings()
   */
  public boolean isSkipDayIfHourDoesNotExist ()
  {
    return m_bSkipDayIfHourDoesNotExist;
  }

  public void setSkipDayIfHourDoesNotExist (final boolean skipDayIfHourDoesNotExist)
  {
    m_bSkipDayIfHourDoesNotExist = skipDayIfHourDoesNotExist;
  }

  public int getTimesTriggered ()
  {
    return m_nTimesTriggered;
  }

  /**
   * <p>
   * Set the number of times the <code>DateIntervalTrigger</code> has already
   * fired.
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
   * Updates the <code>DateIntervalTrigger</code>'s state based on the
   * MISFIRE_INSTRUCTION_XXX that was selected when the
   * <code>DateIntervalTrigger</code> was created.
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
   * Called when the <code>Scheduler</code> has decided to 'fire' the trigger
   * (execute the associated <code>Job</code>), in order to give the
   * <code>Trigger</code> a chance to update itself for its next triggering (if
   * any).
   *
   * @see #executionComplete(IJobExecutionContext, JobExecutionException)
   */
  @Override
  public void triggered (final com.helger.quartz.ICalendar calendar)
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
   * @see com.helger.quartz.spi.IOperableTrigger#updateWithNewCalendar(com.helger.quartz.ICalendar,
   *      long)
   */
  public void updateWithNewCalendar (final com.helger.quartz.ICalendar calendar, final long misfireThreshold)
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
  public Date computeFirstFireTime (final com.helger.quartz.ICalendar calendar)
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
   */
  public Date getNextFireTime ()
  {
    return m_aNextFireTime;
  }

  /**
   * <p>
   * Returns the previous time at which the <code>DateIntervalTrigger</code>
   * fired. If the trigger has not yet fired, <code>null</code> will be
   * returned.
   */
  public Date getPreviousFireTime ()
  {
    return m_aPreviousFireTime;
  }

  /**
   * <p>
   * Set the next time at which the <code>DateIntervalTrigger</code> should
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
   * Set the previous time at which the <code>DateIntervalTrigger</code> fired.
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
   * Returns the next time at which the <code>DateIntervalTrigger</code> will
   * fire, after the given time. If the trigger will not fire after the given
   * time, <code>null</code> will be returned.
   * </p>
   */
  public Date getFireTimeAfter (final Date afterTime)
  {
    return getFireTimeAfter (afterTime, false);
  }

  protected Date getFireTimeAfter (final Date aAfterTime, final boolean ignoreEndTime)
  {
    // increment afterTme by a second, so that we are
    // comparing against a time after it!
    Date afterTime = aAfterTime;
    if (afterTime == null)
      afterTime = new Date ();

    final long startMillis = getStartTime ().getTime ();
    final long afterMillis = afterTime.getTime ();
    final long endMillis = (getEndTime () == null) ? Long.MAX_VALUE : getEndTime ().getTime ();

    if (!ignoreEndTime && (endMillis <= afterMillis))
      return null;

    if (afterMillis < startMillis)
      return new Date (startMillis);

    final long secondsAfterStart = 1 + (afterMillis - startMillis) / 1000L;

    Date time = null;
    final long repeatLong = getRepeatInterval ();

    final Calendar aTime = PDTFactory.createCalendar ();
    aTime.setTime (afterTime);

    final Calendar sTime = PDTFactory.createCalendar ();
    if (m_aTimeZone != null)
      sTime.setTimeZone (m_aTimeZone);
    sTime.setTime (getStartTime ());
    sTime.setLenient (true);

    if (getRepeatIntervalUnit ().equals (EIntervalUnit.SECOND))
    {
      long jumpCount = secondsAfterStart / repeatLong;
      if (secondsAfterStart % repeatLong != 0)
        jumpCount++;
      sTime.add (Calendar.SECOND, getRepeatInterval () * (int) jumpCount);
      time = sTime.getTime ();
    }
    else
      if (getRepeatIntervalUnit ().equals (EIntervalUnit.MINUTE))
      {
        long jumpCount = secondsAfterStart / (repeatLong * 60L);
        if (secondsAfterStart % (repeatLong * 60L) != 0)
          jumpCount++;
        sTime.add (Calendar.MINUTE, getRepeatInterval () * (int) jumpCount);
        time = sTime.getTime ();
      }
      else
        if (getRepeatIntervalUnit ().equals (EIntervalUnit.HOUR))
        {
          long jumpCount = secondsAfterStart / (repeatLong * 60L * 60L);
          if (secondsAfterStart % (repeatLong * 60L * 60L) != 0)
            jumpCount++;
          sTime.add (Calendar.HOUR_OF_DAY, getRepeatInterval () * (int) jumpCount);
          time = sTime.getTime ();
        }
        else
        { // intervals a day or greater ...

          final int initialHourOfDay = sTime.get (Calendar.HOUR_OF_DAY);

          if (getRepeatIntervalUnit ().equals (EIntervalUnit.DAY))
          {
            sTime.setLenient (true);

            // Because intervals greater than an hour have an non-fixed number
            // of seconds in them (due to daylight savings, variation number of
            // days in each month, leap year, etc. ) we can't jump forward an
            // exact number of seconds to calculate the fire time as we can
            // with the second, minute and hour intervals. But, rather
            // than slowly crawling our way there by iteratively adding the
            // increment to the start time until we reach the "after time",
            // we can first make a big leap most of the way there...

            long jumpCount = secondsAfterStart / (repeatLong * 24L * 60L * 60L);
            // if we need to make a big jump, jump most of the way there,
            // but not all the way because in some cases we may over-shoot or
            // under-shoot
            if (jumpCount > 20)
            {
              if (jumpCount < 50)
                jumpCount = (long) (jumpCount * 0.80);
              else
                if (jumpCount < 500)
                  jumpCount = (long) (jumpCount * 0.90);
                else
                  jumpCount = (long) (jumpCount * 0.95);
              sTime.add (Calendar.DAY_OF_YEAR, (int) (getRepeatInterval () * jumpCount));
            }

            // now baby-step the rest of the way there...
            while (!sTime.getTime ().after (afterTime) && (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
            {
              sTime.add (Calendar.DAY_OF_YEAR, getRepeatInterval ());
            }
            while (daylightSavingHourShiftOccurredAndAdvanceNeeded (sTime, initialHourOfDay, afterTime) &&
                   (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
            {
              sTime.add (Calendar.DAY_OF_YEAR, getRepeatInterval ());
            }
            time = sTime.getTime ();
          }
          else
            if (getRepeatIntervalUnit ().equals (EIntervalUnit.WEEK))
            {
              sTime.setLenient (true);

              // Because intervals greater than an hour have an non-fixed number
              // of seconds in them (due to daylight savings, variation number
              // of
              // days in each month, leap year, etc. ) we can't jump forward an
              // exact number of seconds to calculate the fire time as we can
              // with the second, minute and hour intervals. But, rather
              // than slowly crawling our way there by iteratively adding the
              // increment to the start time until we reach the "after time",
              // we can first make a big leap most of the way there...

              long jumpCount = secondsAfterStart / (repeatLong * 7L * 24L * 60L * 60L);
              // if we need to make a big jump, jump most of the way there,
              // but not all the way because in some cases we may over-shoot or
              // under-shoot
              if (jumpCount > 20)
              {
                if (jumpCount < 50)
                  jumpCount = (long) (jumpCount * 0.80);
                else
                  if (jumpCount < 500)
                    jumpCount = (long) (jumpCount * 0.90);
                  else
                    jumpCount = (long) (jumpCount * 0.95);
                sTime.add (Calendar.WEEK_OF_YEAR, (int) (getRepeatInterval () * jumpCount));
              }

              while (!sTime.getTime ().after (afterTime) && (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
              {
                sTime.add (Calendar.WEEK_OF_YEAR, getRepeatInterval ());
              }
              while (daylightSavingHourShiftOccurredAndAdvanceNeeded (sTime, initialHourOfDay, afterTime) &&
                     (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
              {
                sTime.add (Calendar.WEEK_OF_YEAR, getRepeatInterval ());
              }
              time = sTime.getTime ();
            }
            else
              if (getRepeatIntervalUnit ().equals (EIntervalUnit.MONTH))
              {
                sTime.setLenient (true);

                // because of the large variation in size of months, and
                // because months are already large blocks of time, we will
                // just advance via brute-force iteration.

                while (!sTime.getTime ().after (afterTime) && (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
                {
                  sTime.add (Calendar.MONTH, getRepeatInterval ());
                }
                while (daylightSavingHourShiftOccurredAndAdvanceNeeded (sTime, initialHourOfDay, afterTime) &&
                       (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
                {
                  sTime.add (Calendar.MONTH, getRepeatInterval ());
                }
                time = sTime.getTime ();
              }
              else
                if (getRepeatIntervalUnit ().equals (EIntervalUnit.YEAR))
                {

                  while (!sTime.getTime ().after (afterTime) && (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
                  {
                    sTime.add (Calendar.YEAR, getRepeatInterval ());
                  }
                  while (daylightSavingHourShiftOccurredAndAdvanceNeeded (sTime, initialHourOfDay, afterTime) &&
                         (sTime.get (Calendar.YEAR) < CQuartz.MAX_YEAR))
                  {
                    sTime.add (Calendar.YEAR, getRepeatInterval ());
                  }
                  time = sTime.getTime ();
                }
        } // case of interval of a day or greater

    if (!ignoreEndTime && (endMillis <= time.getTime ()))
    {
      return null;
    }

    return time;
  }

  private boolean daylightSavingHourShiftOccurredAndAdvanceNeeded (final Calendar newTime,
                                                                   final int initialHourOfDay,
                                                                   final Date afterTime)
  {
    if (isPreserveHourOfDayAcrossDaylightSavings () && newTime.get (Calendar.HOUR_OF_DAY) != initialHourOfDay)
    {
      newTime.set (Calendar.HOUR_OF_DAY, initialHourOfDay);
      if (newTime.get (Calendar.HOUR_OF_DAY) != initialHourOfDay)
      {
        return isSkipDayIfHourDoesNotExist ();
      }
      return !newTime.getTime ().after (afterTime);
    }
    return false;
  }

  /**
   * <p>
   * Returns the final time at which the <code>DateIntervalTrigger</code> will
   * fire, if there is no end time set, null will be returned.
   * </p>
   * <p>
   * Note that the return time may be in the past.
   * </p>
   */
  public Date getFinalFireTime ()
  {
    if (getEndTime () == null)
    {
      return null;
    }

    // back up a second from end time
    Date fTime = new Date (getEndTime ().getTime () - 1000L);
    // find the next fire time after that
    fTime = getFireTimeAfter (fTime, true);

    // the the trigger fires at the end time, that's it!
    if (fTime.equals (getEndTime ()))
      return fTime;

    // otherwise we have to back up one interval from the fire time after the
    // end time

    final Calendar lTime = PDTFactory.createCalendar ();
    if (m_aTimeZone != null)
      lTime.setTimeZone (m_aTimeZone);
    lTime.setTime (fTime);
    lTime.setLenient (true);

    if (getRepeatIntervalUnit ().equals (EIntervalUnit.SECOND))
      lTime.add (Calendar.SECOND, -1 * getRepeatInterval ());
    else
      if (getRepeatIntervalUnit ().equals (EIntervalUnit.MINUTE))
        lTime.add (Calendar.MINUTE, -1 * getRepeatInterval ());
      else
        if (getRepeatIntervalUnit ().equals (EIntervalUnit.HOUR))
          lTime.add (Calendar.HOUR_OF_DAY, -1 * getRepeatInterval ());
        else
          if (getRepeatIntervalUnit ().equals (EIntervalUnit.DAY))
            lTime.add (Calendar.DAY_OF_YEAR, -1 * getRepeatInterval ());
          else
            if (getRepeatIntervalUnit ().equals (EIntervalUnit.WEEK))
              lTime.add (Calendar.WEEK_OF_YEAR, -1 * getRepeatInterval ());
            else
              if (getRepeatIntervalUnit ().equals (EIntervalUnit.MONTH))
                lTime.add (Calendar.MONTH, -1 * getRepeatInterval ());
              else
                if (getRepeatIntervalUnit ().equals (EIntervalUnit.YEAR))
                  lTime.add (Calendar.YEAR, -1 * getRepeatInterval ());

    return lTime.getTime ();
  }

  /**
   * <p>
   * Determines whether or not the <code>DateIntervalTrigger</code> will occur
   * again.
   * </p>
   */
  public boolean mayFireAgain ()
  {
    return getNextFireTime () != null;
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

    if (m_nRepeatInterval < 1)
      throw new SchedulerException ("Repeat Interval cannot be zero.");
  }

  /**
   * Get a {@link com.helger.quartz.IScheduleBuilder} that is configured to
   * produce a schedule identical to this trigger's schedule.
   *
   * @see #getTriggerBuilder()
   */
  @Override
  public CalendarIntervalScheduleBuilder getScheduleBuilder ()
  {
    final CalendarIntervalScheduleBuilder cb = CalendarIntervalScheduleBuilder.calendarIntervalSchedule ()
                                                                              .withInterval (getRepeatInterval (),
                                                                                             getRepeatIntervalUnit ());
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

  @Nonnull
  @ReturnsMutableCopy
  public CalendarIntervalTrigger getClone ()
  {
    return new CalendarIntervalTrigger (this);
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
