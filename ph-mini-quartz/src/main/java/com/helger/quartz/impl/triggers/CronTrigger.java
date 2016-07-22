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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.helger.quartz.CronExpression;
import com.helger.quartz.CronScheduleBuilder;
import com.helger.quartz.ICalendar;
import com.helger.quartz.ICronTrigger;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.ScheduleBuilder;
import com.helger.quartz.TriggerUtils;

/**
 * <p>
 * A concrete <code>{@link ITrigger}</code> that is used to fire a
 * <code>{@link com.helger.quartz.IJobDetail}</code> at given moments in time,
 * defined with Unix 'cron-like' definitions.
 * </p>
 *
 * @author Sharada Jambula, James House
 * @author Contributions from Mads Henderson
 */
public class CronTrigger extends AbstractTrigger <ICronTrigger> implements ICronTrigger, ICoreTrigger
{
  protected static final int YEAR_TO_GIVEUP_SCHEDULING_AT = CronExpression.MAX_YEAR;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private CronExpression cronEx = null;
  private Date startTime = null;
  private Date endTime = null;
  private Date nextFireTime = null;
  private Date previousFireTime = null;
  private transient TimeZone timeZone = null;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Create a <code>CronTrigger</code> with no settings.
   * </p>
   * <p>
   * The start-time will also be set to the current time, and the time zone will
   * be set the the system's default time zone.
   * </p>
   */
  public CronTrigger ()
  {
    super ();
    setStartTime (new Date ());
    setTimeZone (TimeZone.getDefault ());
  }

  @Override
  public Object clone ()
  {
    final CronTrigger copy = (CronTrigger) super.clone ();
    if (cronEx != null)
    {
      copy.setCronExpression (new CronExpression (cronEx));
    }
    return copy;
  }

  public void setCronExpression (final String cronExpression) throws ParseException
  {
    final TimeZone origTz = getTimeZone ();
    this.cronEx = new CronExpression (cronExpression);
    this.cronEx.setTimeZone (origTz);
  }

  public String getCronExpression ()
  {
    return cronEx == null ? null : cronEx.getCronExpression ();
  }

  /**
   * Set the CronExpression to the given one. The TimeZone on the passed-in
   * CronExpression over-rides any that was already set on the Trigger.
   */
  public void setCronExpression (final CronExpression cronExpression)
  {
    this.cronEx = cronExpression;
    this.timeZone = cronExpression.getTimeZone ();
  }

  /**
   * <p>
   * Get the time at which the <code>CronTrigger</code> should occur.
   * </p>
   */
  @Override
  public Date getStartTime ()
  {
    return this.startTime;
  }

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

    // round off millisecond...
    // Note timeZone is not needed here as parameter for
    // Calendar.getInstance(),
    // since time zone is implicit when using a Date in the setTime method.
    final Calendar cl = Calendar.getInstance ();
    cl.setTime (startTime);
    cl.set (Calendar.MILLISECOND, 0);

    this.startTime = cl.getTime ();
  }

  /**
   * <p>
   * Get the time at which the <code>CronTrigger</code> should quit repeating -
   * even if repeastCount isn't yet satisfied.
   * </p>
   *
   * @see #getFinalFireTime()
   */
  @Override
  public Date getEndTime ()
  {
    return this.endTime;
  }

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
   * @see TriggerUtils#computeFireTimesBetween(com.helger.quartz.spi.IOperableTrigger,
   *      com.helger.quartz.ICalendar, Date, Date)
   */
  @Override
  public Date getNextFireTime ()
  {
    return this.nextFireTime;
  }

  /**
   * <p>
   * Returns the previous time at which the <code>CronTrigger</code> fired. If
   * the trigger has not yet fired, <code>null</code> will be returned.
   */
  @Override
  public Date getPreviousFireTime ()
  {
    return this.previousFireTime;
  }

  /**
   * <p>
   * Sets the next time at which the <code>CronTrigger</code> will fire. <b>This
   * method should not be invoked by client code.</b>
   * </p>
   */
  public void setNextFireTime (final Date nextFireTime)
  {
    this.nextFireTime = nextFireTime;
  }

  /**
   * <p>
   * Set the previous time at which the <code>CronTrigger</code> fired.
   * </p>
   * <p>
   * <b>This method should not be invoked by client code.</b>
   * </p>
   */
  public void setPreviousFireTime (final Date previousFireTime)
  {
    this.previousFireTime = previousFireTime;
  }

  public TimeZone getTimeZone ()
  {
    if (cronEx != null)
      return cronEx.getTimeZone ();

    if (timeZone == null)
    {
      timeZone = TimeZone.getDefault ();
    }
    return timeZone;
  }

  /**
   * <p>
   * Sets the time zone for which the <code>cronExpression</code> of this
   * <code>CronTrigger</code> will be resolved.
   * </p>
   * <p>
   * If {@link #setCronExpression(CronExpression)} is called after this method,
   * the TimeZon setting on the CronExpression will "win". However if
   * {@link #setCronExpression(String)} is called after this method, the time
   * zone applied by this method will remain in effect, since the String cron
   * expression does not carry a time zone!
   */
  public void setTimeZone (final TimeZone timeZone)
  {
    if (cronEx != null)
    {
      cronEx.setTimeZone (timeZone);
    }
    this.timeZone = timeZone;
  }

  /**
   * <p>
   * Returns the next time at which the <code>CronTrigger</code> will fire,
   * after the given time. If the trigger will not fire after the given time,
   * <code>null</code> will be returned.
   * </p>
   * <p>
   * Note that the date returned is NOT validated against the related
   * {@link com.helger.quartz.ICalendar} (if any)
   * </p>
   *
   * @param aAfterTime
   */
  @Override
  public Date getFireTimeAfter (final Date aAfterTime)
  {
    Date afterTime = aAfterTime;
    if (afterTime == null)
      afterTime = new Date ();

    if (getStartTime ().after (afterTime))
    {
      afterTime = new Date (getStartTime ().getTime () - 1000l);
    }

    if (getEndTime () != null && (afterTime.compareTo (getEndTime ()) >= 0))
    {
      return null;
    }

    final Date pot = getTimeAfter (afterTime);
    if (getEndTime () != null && pot != null && pot.after (getEndTime ()))
    {
      return null;
    }

    return pot;
  }

  /**
   * <p>
   * NOT YET IMPLEMENTED: Returns the final time at which the
   * <code>CronTrigger</code> will fire.
   * </p>
   * <p>
   * Note that the return time *may* be in the past. and the date returned is
   * not validated against {@link com.helger.quartz.ICalendar}
   * </p>
   */
  @Override
  public Date getFinalFireTime ()
  {
    Date resultTime;
    if (getEndTime () != null)
    {
      resultTime = getTimeBefore (new Date (getEndTime ().getTime () + 1000l));
    }
    else
    {
      resultTime = (cronEx == null) ? null : cronEx.getFinalFireTime ();
    }

    if ((resultTime != null) && (getStartTime () != null) && (resultTime.before (getStartTime ())))
    {
      return null;
    }

    return resultTime;
  }

  /**
   * <p>
   * Determines whether or not the <code>CronTrigger</code> will occur again.
   * </p>
   */
  @Override
  public boolean mayFireAgain ()
  {
    return (getNextFireTime () != null);
  }

  @Override
  protected boolean validateMisfireInstruction (final int misfireInstruction)
  {
    return misfireInstruction >= MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY &&
           misfireInstruction <= MISFIRE_INSTRUCTION_DO_NOTHING;
  }

  /**
   * <p>
   * Updates the <code>CronTrigger</code>'s state based on the
   * MISFIRE_INSTRUCTION_XXX that was selected when the <code>CronTrigger</code>
   * was created.
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
        setNextFireTime (new Date ());
      }
  }

  /**
   * <p>
   * Determines whether the date and (optionally) time of the given Calendar
   * instance falls on a scheduled fire-time of this trigger.
   * </p>
   * <p>
   * Equivalent to calling <code>willFireOn(cal, false)</code>.
   * </p>
   *
   * @param test
   *        the date to compare
   * @see #willFireOn(Calendar, boolean)
   */
  public boolean willFireOn (final Calendar test)
  {
    return willFireOn (test, false);
  }

  /**
   * <p>
   * Determines whether the date and (optionally) time of the given Calendar
   * instance falls on a scheduled fire-time of this trigger.
   * </p>
   * <p>
   * Note that the value returned is NOT validated against the related
   * {@link com.helger.quartz.ICalendar} (if any)
   * </p>
   *
   * @param aTest
   *        the date to compare
   * @param dayOnly
   *        if set to true, the method will only determine if the trigger will
   *        fire during the day represented by the given Calendar (hours,
   *        minutes and seconds will be ignored).
   * @see #willFireOn(Calendar)
   */
  public boolean willFireOn (final Calendar aTest, final boolean dayOnly)
  {
    final Calendar test = (Calendar) aTest.clone ();
    test.set (Calendar.MILLISECOND, 0); // don't compare millis.

    if (dayOnly)
    {
      test.set (Calendar.HOUR_OF_DAY, 0);
      test.set (Calendar.MINUTE, 0);
      test.set (Calendar.SECOND, 0);
    }

    final Date testTime = test.getTime ();

    Date fta = getFireTimeAfter (new Date (test.getTime ().getTime () - 1000));

    if (fta == null)
      return false;

    final Calendar p = Calendar.getInstance (test.getTimeZone ());
    p.setTime (fta);

    final int year = p.get (Calendar.YEAR);
    final int month = p.get (Calendar.MONTH);
    final int day = p.get (Calendar.DATE);

    if (dayOnly)
    {
      return (year == test.get (Calendar.YEAR) &&
              month == test.get (Calendar.MONTH) &&
              day == test.get (Calendar.DATE));
    }

    while (fta.before (testTime))
    {
      fta = getFireTimeAfter (fta);
    }

    return fta.equals (testTime);
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
    previousFireTime = nextFireTime;
    nextFireTime = getFireTimeAfter (nextFireTime);

    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded (nextFireTime.getTime ()))
    {
      nextFireTime = getFireTimeAfter (nextFireTime);
    }
  }

  /**
   * @see AbstractTrigger#updateWithNewCalendar(com.helger.quartz.ICalendar,
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
      // Use gregorian only because the constant is based on Gregorian
      final Calendar c = new java.util.GregorianCalendar ();
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
  public Date computeFirstFireTime (final com.helger.quartz.ICalendar calendar)
  {
    nextFireTime = getFireTimeAfter (new Date (getStartTime ().getTime () - 1000l));

    while (nextFireTime != null && calendar != null && !calendar.isTimeIncluded (nextFireTime.getTime ()))
    {
      nextFireTime = getFireTimeAfter (nextFireTime);
    }

    return nextFireTime;
  }

  public String getExpressionSummary ()
  {
    return cronEx == null ? null : cronEx.getExpressionSummary ();
  }

  /**
   * Used by extensions of CronTrigger to imply that there are additional
   * properties, specifically so that extensions can choose whether to be stored
   * as a serialized blob, or as a flattened CronTrigger table.
   */
  public boolean hasAdditionalProperties ()
  {
    return false;
  }

  /**
   * Get a {@link ScheduleBuilder} that is configured to produce a schedule
   * identical to this trigger's schedule.
   *
   * @see #getTriggerBuilder()
   */
  @Override
  public ScheduleBuilder <ICronTrigger> getScheduleBuilder ()
  {

    final CronScheduleBuilder cb = CronScheduleBuilder.cronSchedule (getCronExpression ()).inTimeZone (getTimeZone ());

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

  ////////////////////////////////////////////////////////////////////////////
  //
  // Computation Functions
  //
  ////////////////////////////////////////////////////////////////////////////

  protected Date getTimeAfter (final Date afterTime)
  {
    return (cronEx == null) ? null : cronEx.getTimeAfter (afterTime);
  }

  /**
   * NOT YET IMPLEMENTED: Returns the time before the given time that this
   * <code>CronTrigger</code> will fire.
   */
  protected Date getTimeBefore (final Date eTime)
  {
    return (cronEx == null) ? null : cronEx.getTimeBefore (eTime);
  }
}
