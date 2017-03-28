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
package com.helger.quartz.impl.calendar;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import com.helger.quartz.ICalendar;
import com.helger.quartz.CronExpression;

/**
 * This implementation of the Calendar excludes the set of times expressed by a
 * given {@link com.helger.quartz.CronExpression CronExpression}. For example,
 * you could use this calendar to exclude all but business hours (8AM - 5PM)
 * every day using the expression &quot;* * 0-7,18-23 ? * *&quot;.
 * <P>
 * It is important to remember that the cron expression here describes a set of
 * times to be <I>excluded</I> from firing. Whereas the cron expression in
 * {@link com.helger.quartz.ICronTrigger CronTrigger} describes a set of times
 * that can be <I>included</I> for firing. Thus, if a <CODE>CronTrigger</CODE>
 * has a given cron expression and is associated with a
 * <CODE>CronCalendar</CODE> with the <I>same</I> expression, the calendar will
 * exclude all the times the trigger includes, and they will cancel each other
 * out.
 *
 * @author Aaron Craven
 */
public class CronCalendar extends BaseCalendar
{
  CronExpression cronExpression;

  /**
   * Create a <CODE>CronCalendar</CODE> with the given cron expression and no
   * <CODE>baseCalendar</CODE>.
   *
   * @param expression
   *        a String representation of the desired cron expression
   */
  public CronCalendar (final String expression) throws ParseException
  {
    this (null, expression, null);
  }

  /**
   * Create a <CODE>CronCalendar</CODE> with the given cron expression and
   * <CODE>baseCalendar</CODE>.
   *
   * @param baseCalendar
   *        the base calendar for this calendar instance &ndash; see
   *        {@link BaseCalendar} for more information on base calendar
   *        functionality
   * @param expression
   *        a String representation of the desired cron expression
   */
  public CronCalendar (final ICalendar baseCalendar, final String expression) throws ParseException
  {
    this (baseCalendar, expression, null);
  }

  /**
   * Create a <CODE>CronCalendar</CODE> with the given cron exprssion,
   * <CODE>baseCalendar</CODE>, and <code>TimeZone</code>.
   *
   * @param baseCalendar
   *        the base calendar for this calendar instance &ndash; see
   *        {@link BaseCalendar} for more information on base calendar
   *        functionality
   * @param expression
   *        a String representation of the desired cron expression
   * @param timeZone
   *        Specifies for which time zone the <code>expression</code> should be
   *        interpreted, i.e. the expression 0 0 10 * * ?, is resolved to 10:00
   *        am in this time zone. If <code>timeZone</code> is <code>null</code>
   *        then <code>TimeZone.getDefault()</code> will be used.
   */
  public CronCalendar (final ICalendar baseCalendar,
                       final String expression,
                       final TimeZone timeZone) throws ParseException
  {
    super (baseCalendar);
    this.cronExpression = new CronExpression (expression);
    this.cronExpression.setTimeZone (timeZone);
  }

  @Override
  public Object clone ()
  {
    final CronCalendar clone = (CronCalendar) super.clone ();
    clone.cronExpression = new CronExpression (cronExpression);
    return clone;
  }

  /**
   * Returns the time zone for which the <code>CronExpression</code> of this
   * <code>CronCalendar</code> will be resolved.
   * <p>
   * Overrides <code>{@link BaseCalendar#getTimeZone()}</code> to defer to its
   * <code>CronExpression</code>.
   * </p>
   */
  @Override
  public TimeZone getTimeZone ()
  {
    return cronExpression.getTimeZone ();
  }

  /**
   * Sets the time zone for which the <code>CronExpression</code> of this
   * <code>CronCalendar</code> will be resolved. If <code>timeZone</code> is
   * <code>null</code> then <code>TimeZone.getDefault()</code> will be used.
   * <p>
   * Overrides <code>{@link BaseCalendar#setTimeZone(TimeZone)}</code> to defer
   * to its <code>CronExpression</code>.
   * </p>
   */
  @Override
  public void setTimeZone (final TimeZone timeZone)
  {
    cronExpression.setTimeZone (timeZone);
  }

  /**
   * Determines whether the given time (in milliseconds) is 'included' by the
   * <CODE>BaseCalendar</CODE>
   *
   * @param timeInMillis
   *        the date/time to test
   * @return a boolean indicating whether the specified time is 'included' by
   *         the <CODE>CronCalendar</CODE>
   */
  @Override
  public boolean isTimeIncluded (final long timeInMillis)
  {
    if ((getBaseCalendar () != null) && (getBaseCalendar ().isTimeIncluded (timeInMillis) == false))
    {
      return false;
    }

    return (!(cronExpression.isSatisfiedBy (new Date (timeInMillis))));
  }

  /**
   * Determines the next time included by the <CODE>CronCalendar</CODE> after
   * the specified time.
   *
   * @param timeInMillis
   *        the initial date/time after which to find an included time
   * @return the time in milliseconds representing the next time included after
   *         the specified time.
   */
  @Override
  public long getNextIncludedTime (final long timeInMillis)
  {
    long nextIncludedTime = timeInMillis + 1; // plus on millisecond

    while (!isTimeIncluded (nextIncludedTime))
    {

      // If the time is in a range excluded by this calendar, we can
      // move to the end of the excluded time range and continue testing
      // from there. Otherwise, if nextIncludedTime is excluded by the
      // baseCalendar, ask it the next time it includes and begin testing
      // from there. Failing this, add one millisecond and continue
      // testing.
      if (cronExpression.isSatisfiedBy (new Date (nextIncludedTime)))
      {
        nextIncludedTime = cronExpression.getNextInvalidTimeAfter (new Date (nextIncludedTime)).getTime ();
      }
      else
        if ((getBaseCalendar () != null) && (!getBaseCalendar ().isTimeIncluded (nextIncludedTime)))
        {
          nextIncludedTime = getBaseCalendar ().getNextIncludedTime (nextIncludedTime);
        }
        else
        {
          nextIncludedTime++;
        }
    }

    return nextIncludedTime;
  }

  /**
   * Returns a string representing the properties of the
   * <CODE>CronCalendar</CODE>
   *
   * @return the properteis of the CronCalendar in a String format
   */
  @Override
  public String toString ()
  {
    final StringBuffer buffer = new StringBuffer ();
    buffer.append ("base calendar: [");
    if (getBaseCalendar () != null)
    {
      buffer.append (getBaseCalendar ().toString ());
    }
    else
    {
      buffer.append ("null");
    }
    buffer.append ("], excluded cron expression: '");
    buffer.append (cronExpression);
    buffer.append ("'");
    return buffer.toString ();
  }

  /**
   * Returns the object representation of the cron expression that defines the
   * dates and times this calendar excludes.
   *
   * @return the cron expression
   * @see com.helger.quartz.CronExpression
   */
  public CronExpression getCronExpression ()
  {
    return cronExpression;
  }

  /**
   * Sets the cron expression for the calendar to a new value
   *
   * @param expression
   *        the new string value to build a cron expression from
   * @throws ParseException
   *         if the string expression cannot be parsed
   */
  public void setCronExpression (final String expression) throws ParseException
  {
    final CronExpression newExp = new CronExpression (expression);

    this.cronExpression = newExp;
  }

  /**
   * Sets the cron expression for the calendar to a new value
   *
   * @param expression
   *        the new cron expression
   */
  public void setCronExpression (final CronExpression expression)
  {
    if (expression == null)
    {
      throw new IllegalArgumentException ("expression cannot be null");
    }

    this.cronExpression = expression;
  }
}
