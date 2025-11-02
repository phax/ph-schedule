/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2025 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.quartz.CronExpression;
import com.helger.quartz.ICalendar;

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
public class CronCalendar extends AbstractCalendar <CronCalendar>
{
  private CronExpression m_aCronExpression;

  public CronCalendar (@NonNull final CronCalendar aOther)
  {
    super (aOther);
    m_aCronExpression = aOther.m_aCronExpression.getClone ();
  }

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
   *        {@link AbstractCalendar} for more information on base calendar
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
   *        {@link AbstractCalendar} for more information on base calendar
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
    super (baseCalendar, null);
    m_aCronExpression = new CronExpression (expression);
    m_aCronExpression.setTimeZone (timeZone);
  }

  /**
   * Returns the time zone for which the <code>CronExpression</code> of this
   * <code>CronCalendar</code> will be resolved.
   * <p>
   * Overrides <code>{@link AbstractCalendar#getTimeZone()}</code> to defer to
   * its <code>CronExpression</code>.
   * </p>
   */
  @Override
  public TimeZone getTimeZone ()
  {
    return m_aCronExpression.getTimeZone ();
  }

  /**
   * Sets the time zone for which the <code>CronExpression</code> of this
   * <code>CronCalendar</code> will be resolved. If <code>timeZone</code> is
   * <code>null</code> then <code>TimeZone.getDefault()</code> will be used.
   * <p>
   * Overrides <code>{@link AbstractCalendar#setTimeZone(TimeZone)}</code> to
   * defer to its <code>CronExpression</code>.
   * </p>
   */
  @Override
  public void setTimeZone (final TimeZone timeZone)
  {
    m_aCronExpression.setTimeZone (timeZone);
  }

  /**
   * Determines whether the given time (in milliseconds) is 'included' by the
   * <CODE>AbstractCalendar</CODE>
   *
   * @param timeInMillis
   *        the date/time to test
   * @return a boolean indicating whether the specified time is 'included' by
   *         the <CODE>CronCalendar</CODE>
   */
  @Override
  public boolean isTimeIncluded (final long timeInMillis)
  {
    if (getBaseCalendar () != null && !getBaseCalendar ().isTimeIncluded (timeInMillis))
    {
      return false;
    }

    return !m_aCronExpression.isSatisfiedBy (new Date (timeInMillis));
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
      if (m_aCronExpression.isSatisfiedBy (new Date (nextIncludedTime)))
      {
        nextIncludedTime = m_aCronExpression.getNextInvalidTimeAfter (new Date (nextIncludedTime)).getTime ();
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
   * @return the properties of the CronCalendar in a String format
   */
  @Override
  public String toString ()
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("base calendar: [");
    if (getBaseCalendar () != null)
    {
      aSB.append (getBaseCalendar ().toString ());
    }
    else
    {
      aSB.append ("null");
    }
    aSB.append ("], excluded cron expression: '");
    aSB.append (m_aCronExpression);
    aSB.append ("'");
    return aSB.toString ();
  }

  /**
   * Returns the object representation of the cron expression that defines the
   * dates and times this calendar excludes.
   *
   * @return the cron expression
   * @see com.helger.quartz.CronExpression
   */
  @NonNull
  public CronExpression getCronExpression ()
  {
    return m_aCronExpression;
  }

  /**
   * Sets the cron expression for the calendar to a new value
   *
   * @param expression
   *        the new string value to build a cron expression from
   * @throws ParseException
   *         if the string expression cannot be parsed
   */
  public void setCronExpression (@NonNull final String expression) throws ParseException
  {
    final CronExpression newExp = new CronExpression (expression);
    setCronExpression (newExp);
  }

  /**
   * Sets the cron expression for the calendar to a new value
   *
   * @param expression
   *        the new cron expression
   */
  public void setCronExpression (@NonNull final CronExpression expression)
  {
    ValueEnforcer.notNull (expression, "Expression");

    m_aCronExpression = expression;
  }

  @Override
  @NonNull
  public CronCalendar getClone ()
  {
    return new CronCalendar (this);
  }
}
