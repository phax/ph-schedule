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
package com.helger.quartz;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.lang.ICloneable;
import com.helger.commons.regex.RegExHelper;

/**
 * <p>
 * Provides a parser and evaluator for unix-like cron expressions. Cron
 * expressions provide the ability to specify complex time combinations such as
 * &quot;At 8:00am every Monday through Friday&quot; or &quot;At 1:30am every
 * last Friday of the month&quot;.
 * </p>
 * <p>
 * Cron expressions are comprised of 6 required fields and one optional field
 * separated by white space. The fields respectively are described as follows:
 * </p>
 * <table>
 * <tr>
 * <th>Field Name</th>
 * <th>&nbsp;</th>
 * <th>Allowed Values</th>
 * <th>&nbsp;</th>
 * <th>Allowed Special Characters</th>
 * </tr>
 * <tr>
 * <td><code>Seconds</code></td>
 * <td>&nbsp;</td>
 * <td><code>0-59</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Minutes</code></td>
 * <td>&nbsp;</td>
 * <td><code>0-59</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Hours</code></td>
 * <td>&nbsp;</td>
 * <td><code>0-23</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Day-of-month</code></td>
 * <td>&nbsp;</td>
 * <td><code>1-31</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * ? / L W</code></td>
 * </tr>
 * <tr>
 * <td><code>Month</code></td>
 * <td>&nbsp;</td>
 * <td><code>0-11 or JAN-DEC</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td><code>Day-of-Week</code></td>
 * <td>&nbsp;</td>
 * <td><code>1-7 or SUN-SAT</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * ? / L #</code></td>
 * </tr>
 * <tr>
 * <td><code>Year (Optional)</code></td>
 * <td>&nbsp;</td>
 * <td><code>empty, 1970-2199</code></td>
 * <td>&nbsp;</td>
 * <td><code>, - * /</code></td>
 * </tr>
 * </table>
 * <p>
 * The '*' character is used to specify all values. For example, &quot;*&quot;
 * in the minute field means &quot;every minute&quot;.
 * </p>
 * <p>
 * The '?' character is allowed for the day-of-month and day-of-week fields. It
 * is used to specify 'no specific value'. This is useful when you need to
 * specify something in one of the two fields, but not the other.
 * </p>
 * <p>
 * The '-' character is used to specify ranges For example &quot;10-12&quot; in
 * the hour field means &quot;the hours 10, 11 and 12&quot;.
 * </p>
 * <p>
 * The ',' character is used to specify additional values. For example
 * &quot;MON,WED,FRI&quot; in the day-of-week field means &quot;the days Monday,
 * Wednesday, and Friday&quot;.
 * </p>
 * <p>
 * The '/' character is used to specify increments. For example &quot;0/15&quot;
 * in the seconds field means &quot;the seconds 0, 15, 30, and 45&quot;. And
 * &quot;5/15&quot; in the seconds field means &quot;the seconds 5, 20, 35, and
 * 50&quot;. Specifying '*' before the '/' is equivalent to specifying 0 is the
 * value to start with. Essentially, for each field in the expression, there is
 * a set of numbers that can be turned on or off. For seconds and minutes, the
 * numbers range from 0 to 59. For hours 0 to 23, for days of the month 0 to 31,
 * and for months 0 to 11 (JAN to DEC). The &quot;/&quot; character simply helps
 * you turn on every &quot;nth&quot; value in the given set. Thus
 * &quot;7/6&quot; in the month field only turns on month &quot;7&quot;, it does
 * NOT mean every 6th month, please note that subtlety.
 * </p>
 * <p>
 * The 'L' character is allowed for the day-of-month and day-of-week fields.
 * This character is short-hand for &quot;last&quot;, but it has different
 * meaning in each of the two fields. For example, the value &quot;L&quot; in
 * the day-of-month field means &quot;the last day of the month&quot; - day 31
 * for January, day 28 for February on non-leap years. If used in the
 * day-of-week field by itself, it simply means &quot;7&quot; or
 * &quot;SAT&quot;. But if used in the day-of-week field after another value, it
 * means &quot;the last xxx day of the month&quot; - for example &quot;6L&quot;
 * means &quot;the last friday of the month&quot;. You can also specify an
 * offset from the last day of the month, such as "L-3" which would mean the
 * third-to-last day of the calendar month. <i>When using the 'L' option, it is
 * important not to specify lists, or ranges of values, as you'll get
 * confusing/unexpected results.</i>
 * </p>
 * <p>
 * The 'W' character is allowed for the day-of-month field. This character is
 * used to specify the weekday (Monday-Friday) nearest the given day. As an
 * example, if you were to specify &quot;15W&quot; as the value for the
 * day-of-month field, the meaning is: &quot;the nearest weekday to the 15th of
 * the month&quot;. So if the 15th is a Saturday, the trigger will fire on
 * Friday the 14th. If the 15th is a Sunday, the trigger will fire on Monday the
 * 16th. If the 15th is a Tuesday, then it will fire on Tuesday the 15th.
 * However if you specify &quot;1W&quot; as the value for day-of-month, and the
 * 1st is a Saturday, the trigger will fire on Monday the 3rd, as it will not
 * 'jump' over the boundary of a month's days. The 'W' character can only be
 * specified when the day-of-month is a single day, not a range or list of days.
 * </p>
 * <p>
 * The 'L' and 'W' characters can also be combined for the day-of-month
 * expression to yield 'LW', which translates to &quot;last weekday of the
 * month&quot;.
 * </p>
 * <p>
 * The '#' character is allowed for the day-of-week field. This character is
 * used to specify &quot;the nth&quot; XXX day of the month. For example, the
 * value of &quot;6#3&quot; in the day-of-week field means the third Friday of
 * the month (day 6 = Friday and &quot;#3&quot; = the 3rd one in the month).
 * Other examples: &quot;2#1&quot; = the first Monday of the month and
 * &quot;4#5&quot; = the fifth Wednesday of the month. Note that if you specify
 * &quot;#5&quot; and there is not 5 of the given day-of-week in the month, then
 * no firing will occur that month. If the '#' character is used, there can only
 * be one expression in the day-of-week field (&quot;3#1,6#3&quot; is not valid,
 * since there are two expressions).
 * </p>
 * <!--
 * <p>
 * The 'C' character is allowed for the day-of-month and day-of-week fields.
 * This character is short-hand for "calendar". This means values are calculated
 * against the associated calendar, if any. If no calendar is associated, then
 * it is equivalent to having an all-inclusive calendar. A value of "5C" in the
 * day-of-month field means "the first day included by the calendar on or after
 * the 5th". A value of "1C" in the day-of-week field means "the first day
 * included by the calendar on or after Sunday".
 * </p>
 * -->
 * <p>
 * The legal characters and the names of months and days of the week are not
 * case sensitive.
 * </p>
 * <b>NOTES:</b>
 * <ul>
 * <li>Support for specifying both a day-of-week and a day-of-month value is not
 * complete (you'll need to use the '?' character in one of these fields).</li>
 * <li>Overflowing ranges is supported - that is, having a larger number on the
 * left hand side than the right. You might do 22-2 to catch 10 o'clock at night
 * until 2 o'clock in the morning, or you might have NOV-FEB. It is very
 * important to note that overuse of overflowing ranges creates ranges that
 * don't make sense and no effort has been made to determine which
 * interpretation CronExpression chooses. An example would be "0 0 14-6 ? *
 * FRI-MON".</li>
 * </ul>
 *
 * @author Sharada Jambula, James House
 * @author Contributions from Mads Henderson
 * @author Refactoring from CronTrigger to CronExpression by Aaron Craven
 */
public final class CronExpression implements ICloneable <CronExpression>
{
  private static final class ValueSet
  {
    final int m_nValue;
    final int m_nPos;

    public ValueSet (final int nValue, final int nPos)
    {
      m_nValue = nValue;
      m_nPos = nPos;
    }
  }

  @VisibleForTesting
  enum EType
  {
    SECOND,
    MINUTE,
    HOUR,
    DAY_OF_MONTH,
    MONTH,
    DAY_OF_WEEK,
    YEAR
  }

  // '*'
  private static final int ALL_SPEC_INT = 99;
  // '?'
  private static final int NO_SPEC_INT = 98;
  private static final Integer ALL_SPEC = Integer.valueOf (ALL_SPEC_INT);
  private static final Integer NO_SPEC = Integer.valueOf (NO_SPEC_INT);

  private static final ICommonsMap <String, Integer> MONTH_MAP = new CommonsHashMap <> (12);
  private static final ICommonsMap <String, Integer> DAY_OF_WEEK_MAP = new CommonsHashMap <> (7);
  static
  {
    for (final Month e : Month.values ())
      MONTH_MAP.put (e.getDisplayName (TextStyle.SHORT, Locale.US).toUpperCase (Locale.US),
                     Integer.valueOf (e.getValue () - 1));

    for (final DayOfWeek e : DayOfWeek.values ())
      DAY_OF_WEEK_MAP.put (e.getDisplayName (TextStyle.SHORT, Locale.US).toUpperCase (Locale.US),
                           Integer.valueOf (e.getValue ()));
  }

  private final String m_sCronExpression;
  private TimeZone m_aTimeZone;

  private TreeSet <Integer> m_aSeconds;
  private TreeSet <Integer> m_aMinutes;
  private TreeSet <Integer> m_aHours;
  private TreeSet <Integer> m_aDaysOfMonth;
  private TreeSet <Integer> m_aMonths;
  private TreeSet <Integer> m_aDaysOfWeek;
  private TreeSet <Integer> m_aYears;

  private boolean m_bLastdayOfWeek = false;
  private int m_nNthdayOfWeek = 0;
  private boolean m_bLastdayOfMonth = false;
  private boolean m_bNearestWeekday = false;
  private int m_nLastdayOffset = 0;

  /**
   * Constructs a new <CODE>CronExpression</CODE> based on the specified
   * parameter.
   *
   * @param cronExpression
   *        String representation of the cron expression the new object should
   *        represent
   * @throws java.text.ParseException
   *         if the string expression cannot be parsed into a valid
   *         <CODE>CronExpression</CODE>
   */
  public CronExpression (@Nonnull final String cronExpression) throws ParseException
  {
    ValueEnforcer.notNull (cronExpression, "CronExpression");

    m_sCronExpression = cronExpression.toUpperCase (Locale.US);
    _buildExpression (m_sCronExpression);
  }

  /**
   * Constructs a new {@code CronExpression} as a copy of an existing instance.
   *
   * @param expression
   *        The existing cron expression to be copied
   */
  public CronExpression (final CronExpression expression)
  {
    /*
     * We don't call the other constructor here since we need to swallow the
     * ParseException. We also elide some of the sanity checking as it is not
     * logically trippable.
     */
    m_sCronExpression = expression.getCronExpression ();
    try
    {
      _buildExpression (m_sCronExpression);
    }
    catch (final ParseException ex)
    {
      throw new AssertionError ();
    }
    setTimeZone (QCloneUtils.getClone (expression.getTimeZone ()));
  }

  /**
   * Indicates whether the given date satisfies the cron expression. Note that
   * milliseconds are ignored, so two Dates falling on different milliseconds of
   * the same second will always have the same result here.
   *
   * @param date
   *        the date to evaluate
   * @return a boolean indicating whether the given date satisfies the cron
   *         expression
   */
  public boolean isSatisfiedBy (final Date date)
  {
    final Calendar testDateCal = Calendar.getInstance (getTimeZone (), Locale.getDefault (Locale.Category.FORMAT));
    testDateCal.setTime (date);
    testDateCal.set (Calendar.MILLISECOND, 0);
    final Date originalDate = testDateCal.getTime ();

    testDateCal.add (Calendar.SECOND, -1);

    final Date timeAfter = getTimeAfter (testDateCal.getTime ());

    return ((timeAfter != null) && (timeAfter.equals (originalDate)));
  }

  /**
   * Returns the next date/time <I>after</I> the given date/time which satisfies
   * the cron expression.
   *
   * @param date
   *        the date/time at which to begin the search for the next valid
   *        date/time
   * @return the next valid date/time
   */
  public Date getNextValidTimeAfter (final Date date)
  {
    return getTimeAfter (date);
  }

  /**
   * Returns the next date/time <I>after</I> the given date/time which does
   * <I>not</I> satisfy the expression
   *
   * @param date
   *        the date/time at which to begin the search for the next invalid
   *        date/time
   * @return the next valid date/time
   */
  public Date getNextInvalidTimeAfter (final Date date)
  {
    long difference = 1000;

    // move back to the nearest second so differences will be accurate
    final Calendar adjustCal = Calendar.getInstance (getTimeZone (), Locale.getDefault (Locale.Category.FORMAT));
    adjustCal.setTime (date);
    adjustCal.set (Calendar.MILLISECOND, 0);
    Date lastDate = adjustCal.getTime ();

    Date newDate;

    // FUTURE_TODO: (QUARTZ-481) IMPROVE THIS! The following is a BAD solution
    // to this problem. Performance will be very bad here, depending on the cron
    // expression. It is, however A solution.

    // keep getting the next included time until it's farther than one second
    // apart. At that point, lastDate is the last valid fire time. We return
    // the second immediately following it.
    while (difference == 1000)
    {
      newDate = getTimeAfter (lastDate);
      if (newDate == null)
        break;

      difference = newDate.getTime () - lastDate.getTime ();
      if (difference == 1000)
        lastDate = newDate;
    }

    return new Date (lastDate.getTime () + 1000);
  }

  /**
   * @return the time zone for which this <code>CronExpression</code> will be
   *         resolved.
   */
  @Nonnull
  public TimeZone getTimeZone ()
  {
    if (m_aTimeZone == null)
      m_aTimeZone = TimeZone.getDefault ();

    return m_aTimeZone;
  }

  /**
   * Sets the time zone for which this <code>CronExpression</code> will be
   * resolved.
   *
   * @param timeZone
   *        Time zone to use
   */
  public void setTimeZone (@Nullable final TimeZone timeZone)
  {
    m_aTimeZone = timeZone;
  }

  /**
   * Returns the string representation of the <CODE>CronExpression</CODE>
   *
   * @return a string representation of the <CODE>CronExpression</CODE>
   */
  @Override
  public String toString ()
  {
    return m_sCronExpression;
  }

  /**
   * Indicates whether the specified cron expression can be parsed into a valid
   * cron expression
   *
   * @param cronExpression
   *        the expression to evaluate
   * @return a boolean indicating whether the given expression is a valid cron
   *         expression
   */
  public static boolean isValidExpression (final String cronExpression)
  {
    try
    {
      validateExpression (cronExpression);
      return true;
    }
    catch (final ParseException pe)
    {
      return false;
    }
  }

  public static void validateExpression (final String cronExpression) throws ParseException
  {
    // Parse the exception - throws an exception in case of error
    new CronExpression (cronExpression);
  }

  private void _buildExpression (final String expression) throws ParseException
  {
    try
    {
      if (m_aSeconds == null)
        m_aSeconds = new TreeSet <> ();
      if (m_aMinutes == null)
        m_aMinutes = new TreeSet <> ();
      if (m_aHours == null)
        m_aHours = new TreeSet <> ();
      if (m_aDaysOfMonth == null)
        m_aDaysOfMonth = new TreeSet <> ();
      if (m_aMonths == null)
        m_aMonths = new TreeSet <> ();
      if (m_aDaysOfWeek == null)
        m_aDaysOfWeek = new TreeSet <> ();
      if (m_aYears == null)
        m_aYears = new TreeSet <> ();

      final StringTokenizer exprsTok = new StringTokenizer (expression, " \t", false);

      EType eLastType = null;
      for (final EType exprOn : EType.values ())
      {
        if (!exprsTok.hasMoreTokens ())
          break;

        final String expr = exprsTok.nextToken ().trim ();

        // throw an exception if L is used with other days of the month
        if (exprOn == EType.DAY_OF_MONTH)
        {
          if (expr.indexOf ('L') != -1 && expr.length () > 1 && expr.indexOf (',') >= 0)
            throw new ParseException ("Support for specifying 'L' and 'LW' with other days of the month is not implemented",
                                      -1);
        }
        if (exprOn == EType.DAY_OF_WEEK)
        {
          // throw an exception if L is used with other days of the week
          if (expr.indexOf ('L') >= 0 && expr.length () > 1 && expr.indexOf (',') >= 0)
          {
            throw new ParseException ("Support for specifying 'L' with other days of the week is not implemented", -1);
          }
          if (expr.indexOf ('#') >= 0 && expr.indexOf ('#', expr.indexOf ('#') + 1) != -1)
          {
            throw new ParseException ("Support for specifying multiple \"nth\" days is not implemented.", -1);
          }
        }

        final StringTokenizer vTok = new StringTokenizer (expr, ",");
        while (vTok.hasMoreTokens ())
        {
          final String v = vTok.nextToken ();
          _storeExpressionVals (0, v, exprOn);
        }
        eLastType = exprOn;
      }

      if (eLastType.ordinal () < EType.DAY_OF_WEEK.ordinal ())
        throw new ParseException ("Unexpected end of expression.", expression.length ());
      if (eLastType.ordinal () < EType.YEAR.ordinal ())
        _storeExpressionVals (0, "*", EType.YEAR);

      final Set <Integer> dow = getSet (EType.DAY_OF_WEEK);
      final Set <Integer> dom = getSet (EType.DAY_OF_MONTH);

      // Copying the logic from the UnsupportedOperationException below
      final boolean dayOfMSpec = !dom.contains (NO_SPEC);
      final boolean dayOfWSpec = !dow.contains (NO_SPEC);

      if (dayOfMSpec && dayOfWSpec)
      {
        throw new ParseException ("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.",
                                  0);
      }
    }
    catch (final ParseException pe)
    {
      throw pe;
    }
    catch (final Exception e)
    {
      throw new ParseException ("Illegal cron expression format (" + e.toString () + ")", 0);
    }
  }

  private int _storeExpressionVals (final int pos, final String s, final EType type) throws ParseException
  {
    int i = _skipWhiteSpace (pos, s);
    if (i >= s.length ())
      return i;

    int incr = 0;
    char c = s.charAt (i);
    if (c >= 'A' &&
        c <= 'Z' &&
        !s.equals ("L") &&
        !s.equals ("LW") &&
        !RegExHelper.stringMatchesPattern ("^L-[0-9]*[W]?", s))
    {
      String sub = s.substring (i, i + 3);
      int sval = -1;
      int eval = -1;
      if (type == EType.MONTH)
      {
        sval = _getMonthNumber (sub) + 1;
        if (sval <= 0)
        {
          throw new ParseException ("Invalid Month value: '" + sub + "'", i);
        }
        if (s.length () > i + 3)
        {
          c = s.charAt (i + 3);
          if (c == '-')
          {
            i += 4;
            sub = s.substring (i, i + 3);
            eval = _getMonthNumber (sub) + 1;
            if (eval <= 0)
            {
              throw new ParseException ("Invalid Month value: '" + sub + "'", i);
            }
          }
        }
      }
      else
        if (type == EType.DAY_OF_WEEK)
        {
          sval = _getDayOfWeekNumber (sub);
          if (sval < 0)
            throw new ParseException ("Invalid Day-of-Week value: '" + sub + "'", i);
          if (s.length () > i + 3)
          {
            c = s.charAt (i + 3);
            if (c == '-')
            {
              i += 4;
              sub = s.substring (i, i + 3);
              eval = _getDayOfWeekNumber (sub);
              if (eval < 0)
              {
                throw new ParseException ("Invalid Day-of-Week value: '" + sub + "'", i);
              }
            }
            else
              if (c == '#')
              {
                try
                {
                  i += 4;
                  m_nNthdayOfWeek = Integer.parseInt (s.substring (i));
                  if (m_nNthdayOfWeek < 1 || m_nNthdayOfWeek > 5)
                    throw new Exception ();
                }
                catch (final Exception e)
                {
                  throw new ParseException ("A numeric value between 1 and 5 must follow the '#' option", i);
                }
              }
              else
                if (c == 'L')
                {
                  m_bLastdayOfWeek = true;
                  i++;
                }
          }

        }
        else
        {
          throw new ParseException ("Illegal characters for this position: '" + sub + "'", i);
        }
      if (eval != -1)
        incr = 1;
      _addToSet (sval, eval, incr, type);
      return i + 3;
    }

    if (c == '?')
    {
      i++;
      if ((i + 1) < s.length () && (s.charAt (i) != ' ' && s.charAt (i + 1) != '\t'))
        throw new ParseException ("Illegal character after '?': " + s.charAt (i), i);
      if (type != EType.DAY_OF_WEEK && type != EType.DAY_OF_MONTH)
        throw new ParseException ("'?' can only be specfied for Day-of-Month or Day-of-Week.", i);
      if (type == EType.DAY_OF_WEEK && !m_bLastdayOfMonth)
      {
        final int val = m_aDaysOfMonth.last ().intValue ();
        if (val == NO_SPEC_INT)
          throw new ParseException ("'?' can only be specfied for Day-of-Month -OR- Day-of-Week.", i);
      }

      _addToSet (NO_SPEC_INT, -1, 0, type);
      return i;
    }

    if (c == '*' || c == '/')
    {
      if (c == '*' && (i + 1) >= s.length ())
      {
        _addToSet (ALL_SPEC_INT, -1, incr, type);
        return i + 1;
      }
      else
        if (c == '/' && ((i + 1) >= s.length () || s.charAt (i + 1) == ' ' || s.charAt (i + 1) == '\t'))
        {
          throw new ParseException ("'/' must be followed by an integer.", i);
        }
        else
          if (c == '*')
          {
            i++;
          }
      c = s.charAt (i);
      if (c == '/')
      {
        // is an increment specified?
        i++;
        if (i >= s.length ())
          throw new ParseException ("Unexpected end of string.", i);

        incr = _getNumericValue (s, i);

        i++;
        if (incr > 10)
          i++;

        if (incr > 59 && (type == EType.SECOND || type == EType.MINUTE))
          throw new ParseException ("Increment > 60 : " + incr, i);
        else
          if (incr > 23 && type == EType.HOUR)
            throw new ParseException ("Increment > 24 : " + incr, i);
          else
            if (incr > 31 && type == EType.DAY_OF_MONTH)
              throw new ParseException ("Increment > 31 : " + incr, i);
            else
              if (incr > 7 && type == EType.DAY_OF_WEEK)
                throw new ParseException ("Increment > 7 : " + incr, i);
              else
                if (incr > 12 && type == EType.MONTH)
                  throw new ParseException ("Increment > 12 : " + incr, i);
      }
      else
      {
        incr = 1;
      }

      _addToSet (ALL_SPEC_INT, -1, incr, type);
      return i;
    }

    if (c == 'L')
    {
      // "last"
      i++;
      if (type == EType.DAY_OF_MONTH)
      {
        m_bLastdayOfMonth = true;
      }
      if (type == EType.DAY_OF_WEEK)
      {
        _addToSet (7, 7, 0, type);
      }
      if (type == EType.DAY_OF_MONTH && s.length () > i)
      {
        c = s.charAt (i);
        if (c == '-')
        {
          final ValueSet vs = _getValue (0, s, i + 1);
          m_nLastdayOffset = vs.m_nValue;
          if (m_nLastdayOffset > 30)
            throw new ParseException ("Offset from last day must be <= 30", i + 1);
          i = vs.m_nPos;
        }
        if (s.length () > i)
        {
          c = s.charAt (i);
          if (c == 'W')
          {
            m_bNearestWeekday = true;
            i++;
          }
        }
      }
      return i;
    }

    if (c >= '0' && c <= '9')
    {
      int val = Integer.parseInt (String.valueOf (c));
      i++;
      if (i >= s.length ())
      {
        _addToSet (val, -1, -1, type);
      }
      else
      {
        c = s.charAt (i);
        if (c >= '0' && c <= '9')
        {
          final ValueSet vs = _getValue (val, s, i);
          val = vs.m_nValue;
          i = vs.m_nPos;
        }
        i = _checkNext (i, s, val, type);
      }
      return i;
    }

    throw new ParseException ("Unexpected character: " + c, i);
  }

  private int _checkNext (final int pos, final String s, final int val, final EType type) throws ParseException
  {
    int end = -1;
    int i = pos;

    if (i >= s.length ())
    {
      _addToSet (val, end, -1, type);
      return i;
    }

    char c = s.charAt (pos);
    if (c == 'L')
    {
      if (type == EType.DAY_OF_WEEK)
      {
        if (val < 1 || val > 7)
          throw new ParseException ("Day-of-Week values must be between 1 and 7", -1);
        m_bLastdayOfWeek = true;
      }
      else
      {
        throw new ParseException ("'L' option is not valid here. (pos=" + i + ")", i);
      }
      final Set <Integer> set = getSet (type);
      set.add (Integer.valueOf (val));
      i++;
      return i;
    }

    if (c == 'W')
    {
      if (type == EType.DAY_OF_MONTH)
      {
        m_bNearestWeekday = true;
      }
      else
      {
        throw new ParseException ("'W' option is not valid here. (pos=" + i + ")", i);
      }
      if (val > 31)
        throw new ParseException ("The 'W' option does not make sense with values larger than 31 (max number of days in a month)",
                                  i);
      final Set <Integer> set = getSet (type);
      set.add (Integer.valueOf (val));
      i++;
      return i;
    }

    if (c == '#')
    {
      if (type != EType.DAY_OF_WEEK)
      {
        throw new ParseException ("'#' option is not valid here. (pos=" + i + ")", i);
      }
      i++;
      try
      {
        m_nNthdayOfWeek = Integer.parseInt (s.substring (i));
        if (m_nNthdayOfWeek < 1 || m_nNthdayOfWeek > 5)
        {
          throw new Exception ();
        }
      }
      catch (final Exception e)
      {
        throw new ParseException ("A numeric value between 1 and 5 must follow the '#' option", i);
      }

      final Set <Integer> set = getSet (type);
      set.add (Integer.valueOf (val));
      i++;
      return i;
    }

    if (c == '-')
    {
      i++;
      c = s.charAt (i);
      final int v = Integer.parseInt (String.valueOf (c));
      end = v;
      i++;
      if (i >= s.length ())
      {
        _addToSet (val, end, 1, type);
        return i;
      }
      c = s.charAt (i);
      if (c >= '0' && c <= '9')
      {
        final ValueSet vs = _getValue (v, s, i);
        end = vs.m_nValue;
        i = vs.m_nPos;
      }
      if (i < s.length () && s.charAt (i) == '/')
      {
        i++;
        c = s.charAt (i);
        final int v2 = Integer.parseInt (String.valueOf (c));
        i++;
        if (i >= s.length ())
        {
          _addToSet (val, end, v2, type);
          return i;
        }
        c = s.charAt (i);
        if (c >= '0' && c <= '9')
        {
          final ValueSet vs = _getValue (v2, s, i);
          final int v3 = vs.m_nValue;
          _addToSet (val, end, v3, type);
          i = vs.m_nPos;
          return i;
        }
        _addToSet (val, end, v2, type);
        return i;
      }
      _addToSet (val, end, 1, type);
      return i;
    }

    if (c == '/')
    {
      i++;
      c = s.charAt (i);
      final int v2 = Integer.parseInt (String.valueOf (c));
      i++;
      if (i >= s.length ())
      {
        _addToSet (val, end, v2, type);
        return i;
      }
      c = s.charAt (i);
      if (c >= '0' && c <= '9')
      {
        final ValueSet vs = _getValue (v2, s, i);
        final int v3 = vs.m_nValue;
        _addToSet (val, end, v3, type);
        i = vs.m_nPos;
        return i;
      }
      throw new ParseException ("Unexpected character '" + c + "' after '/'", i);
    }

    _addToSet (val, end, 0, type);
    i++;
    return i;
  }

  public String getCronExpression ()
  {
    return m_sCronExpression;
  }

  public String getExpressionSummary ()
  {
    final StringBuilder buf = new StringBuilder ();

    buf.append ("seconds: ");
    buf.append (_getExpressionSetSummary (m_aSeconds));
    buf.append ("\n");
    buf.append ("minutes: ");
    buf.append (_getExpressionSetSummary (m_aMinutes));
    buf.append ("\n");
    buf.append ("hours: ");
    buf.append (_getExpressionSetSummary (m_aHours));
    buf.append ("\n");
    buf.append ("daysOfMonth: ");
    buf.append (_getExpressionSetSummary (m_aDaysOfMonth));
    buf.append ("\n");
    buf.append ("months: ");
    buf.append (_getExpressionSetSummary (m_aMonths));
    buf.append ("\n");
    buf.append ("daysOfWeek: ");
    buf.append (_getExpressionSetSummary (m_aDaysOfWeek));
    buf.append ("\n");
    buf.append ("lastdayOfWeek: ");
    buf.append (m_bLastdayOfWeek);
    buf.append ("\n");
    buf.append ("nearestWeekday: ");
    buf.append (m_bNearestWeekday);
    buf.append ("\n");
    buf.append ("NthDayOfWeek: ");
    buf.append (m_nNthdayOfWeek);
    buf.append ("\n");
    buf.append ("lastdayOfMonth: ");
    buf.append (m_bLastdayOfMonth);
    buf.append ("\n");
    buf.append ("years: ");
    buf.append (_getExpressionSetSummary (m_aYears));
    buf.append ("\n");

    return buf.toString ();
  }

  private static String _getExpressionSetSummary (final Set <Integer> set)
  {
    if (set.contains (NO_SPEC))
      return "?";
    if (set.contains (ALL_SPEC))
      return "*";

    final StringBuilder buf = new StringBuilder ();
    final Iterator <Integer> itr = set.iterator ();
    boolean first = true;
    while (itr.hasNext ())
    {
      final Integer iVal = itr.next ();
      final String val = iVal.toString ();
      if (first)
        first = false;
      else
        buf.append (',');
      buf.append (val);
    }
    return buf.toString ();
  }

  private static int _skipWhiteSpace (final int i, final String s)
  {
    int nIndex = i;
    final int nMax = s.length ();
    while (nIndex < nMax)
    {
      if (!Character.isWhitespace (s.charAt (i)))
        break;
      nIndex++;
    }

    return nIndex;
  }

  private static int _findNextWhiteSpace (final int i, final String s)
  {
    int nIndex = i;
    final int nMax = s.length ();
    while (nIndex < nMax)
    {
      if (Character.isWhitespace (s.charAt (i)))
        break;
      nIndex++;
    }

    return nIndex;
  }

  private void _addToSet (final int val, final int end, final int nIncr, final EType type) throws ParseException
  {
    int incr = nIncr;
    final Set <Integer> set = getSet (type);

    if (type == EType.SECOND || type == EType.MINUTE)
    {
      if ((val < 0 || val > 59 || end > 59) && (val != ALL_SPEC_INT))
        throw new ParseException ("Minute and Second values must be between 0 and 59", -1);
    }
    else
      if (type == EType.HOUR)
      {
        if ((val < 0 || val > 23 || end > 23) && (val != ALL_SPEC_INT))
          throw new ParseException ("Hour values must be between 0 and 23", -1);
      }
      else
        if (type == EType.DAY_OF_MONTH)
        {
          if ((val < 1 || val > 31 || end > 31) && (val != ALL_SPEC_INT) && (val != NO_SPEC_INT))
            throw new ParseException ("Day of month values must be between 1 and 31", -1);
        }
        else
          if (type == EType.MONTH)
          {
            if ((val < 1 || val > 12 || end > 12) && (val != ALL_SPEC_INT))
              throw new ParseException ("Month values must be between 1 and 12", -1);
          }
          else
            if (type == EType.DAY_OF_WEEK)
            {
              if ((val < 1 || val > 7 || end > 7) && (val != ALL_SPEC_INT) && (val != NO_SPEC_INT))
                throw new ParseException ("Day-of-Week values must be between 1 and 7", -1);
            }

    if ((incr == 0 || incr == -1) && val != ALL_SPEC_INT)
    {
      if (val != -1)
        set.add (Integer.valueOf (val));
      else
        set.add (NO_SPEC);
      return;
    }

    int startAt = val;
    int stopAt = end;

    if (val == ALL_SPEC_INT && incr <= 0)
    {
      incr = 1;
      set.add (ALL_SPEC); // put in a marker, but also fill values
    }

    if (type == EType.SECOND || type == EType.MINUTE)
    {
      if (stopAt == -1)
        stopAt = 59;
      if (startAt == -1 || startAt == ALL_SPEC_INT)
        startAt = 0;
    }
    else
      if (type == EType.HOUR)
      {
        if (stopAt == -1)
          stopAt = 23;
        if (startAt == -1 || startAt == ALL_SPEC_INT)
          startAt = 0;
      }
      else
        if (type == EType.DAY_OF_MONTH)
        {
          if (stopAt == -1)
            stopAt = 31;
          if (startAt == -1 || startAt == ALL_SPEC_INT)
            startAt = 1;
        }
        else
          if (type == EType.MONTH)
          {
            if (stopAt == -1)
              stopAt = 12;
            if (startAt == -1 || startAt == ALL_SPEC_INT)
              startAt = 1;
          }
          else
            if (type == EType.DAY_OF_WEEK)
            {
              if (stopAt == -1)
                stopAt = 7;
              if (startAt == -1 || startAt == ALL_SPEC_INT)
                startAt = 1;
            }
            else
              if (type == EType.YEAR)
              {
                if (stopAt == -1)
                  stopAt = CQuartz.MAX_YEAR;
                if (startAt == -1 || startAt == ALL_SPEC_INT)
                  startAt = 1970;
              }

    // if the end of the range is before the start, then we need to overflow
    // into the next day, month etc. This is done by adding the maximum amount
    // for
    // that type, and using modulus max to determine the value being added.
    int max = -1;
    if (stopAt < startAt)
    {
      switch (type)
      {
        case SECOND:
          max = 60;
          break;
        case MINUTE:
          max = 60;
          break;
        case HOUR:
          max = 24;
          break;
        case MONTH:
          max = 12;
          break;
        case DAY_OF_WEEK:
          max = 7;
          break;
        case DAY_OF_MONTH:
          max = 31;
          break;
        case YEAR:
          throw new IllegalArgumentException ("Start year must be less than stop year");
        default:
          throw new IllegalArgumentException ("Unexpected type encountered");
      }
      stopAt += max;
    }

    for (int i = startAt; i <= stopAt; i += incr)
    {
      if (max == -1)
      {
        // ie: there's no max to overflow over
        set.add (Integer.valueOf (i));
      }
      else
      {
        // take the modulus to get the real value
        int i2 = i % max;

        // 1-indexed ranges should not include 0, and should include their max
        if (i2 == 0 && (type == EType.MONTH || type == EType.DAY_OF_WEEK || type == EType.DAY_OF_MONTH))
        {
          i2 = max;
        }

        set.add (Integer.valueOf (i2));
      }
    }
  }

  @Nullable
  @VisibleForTesting
  TreeSet <Integer> getSet (@Nonnull final EType type)
  {
    switch (type)
    {
      case SECOND:
        return m_aSeconds;
      case MINUTE:
        return m_aMinutes;
      case HOUR:
        return m_aHours;
      case DAY_OF_MONTH:
        return m_aDaysOfMonth;
      case MONTH:
        return m_aMonths;
      case DAY_OF_WEEK:
        return m_aDaysOfWeek;
      case YEAR:
        return m_aYears;
    }
    throw new IllegalStateException ("oops");
  }

  @Nonnull
  private static ValueSet _getValue (final int v, final String s, final int nI)
  {
    int nIndex = nI;
    char c = s.charAt (nIndex);
    final StringBuilder aNums = new StringBuilder ();
    aNums.append (Integer.toString (v));
    while (c >= '0' && c <= '9')
    {
      aNums.append (c);
      nIndex++;
      if (nIndex >= s.length ())
        break;
      c = s.charAt (nIndex);
    }
    return new ValueSet (Integer.parseInt (aNums.toString ()), nIndex < s.length () ? nIndex : nIndex + 1);
  }

  private static int _getNumericValue (final String s, final int i)
  {
    final int endOfVal = _findNextWhiteSpace (i, s);
    final String val = s.substring (i, endOfVal);
    return Integer.parseInt (val);
  }

  private static int _getMonthNumber (final String s)
  {
    final Integer integer = MONTH_MAP.get (s);

    if (integer == null)
      return -1;

    return integer.intValue ();
  }

  private static int _getDayOfWeekNumber (final String s)
  {
    final Integer integer = DAY_OF_WEEK_MAP.get (s);

    if (integer == null)
      return -1;

    return integer.intValue ();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Computation Functions
  //
  ////////////////////////////////////////////////////////////////////////////

  public Date getTimeAfter (final Date aAfterTime)
  {
    // Computation is based on Gregorian year only.
    final Calendar cl = new GregorianCalendar (getTimeZone (), Locale.getDefault (Category.FORMAT));

    // move ahead one second, since we're computing the time *after* the
    // given time
    final Date afterTime = new Date (aAfterTime.getTime () + 1000);
    // CronTrigger does not deal with milliseconds
    cl.setTime (afterTime);
    cl.set (Calendar.MILLISECOND, 0);

    boolean bGotOne = false;
    // loop until we've computed the next time, or we've past the endTime
    while (!bGotOne)
    {
      // if (endTime != null && cl.getTime().after(endTime)) return null;
      if (cl.get (Calendar.YEAR) > CQuartz.MAX_YEAR)
      {
        // prevent endless loop...
        return null;
      }

      SortedSet <Integer> st = null;
      int t = 0;

      int sec = cl.get (Calendar.SECOND);
      int min = cl.get (Calendar.MINUTE);

      // get second.................................................
      st = m_aSeconds.tailSet (Integer.valueOf (sec));
      if (st != null && !st.isEmpty ())
      {
        sec = st.first ().intValue ();
      }
      else
      {
        sec = m_aSeconds.first ().intValue ();
        min++;
        cl.set (Calendar.MINUTE, min);
      }
      cl.set (Calendar.SECOND, sec);

      min = cl.get (Calendar.MINUTE);
      int hr = cl.get (Calendar.HOUR_OF_DAY);
      t = -1;

      // get minute.................................................
      st = m_aMinutes.tailSet (Integer.valueOf (min));
      if (st != null && !st.isEmpty ())
      {
        t = min;
        min = st.first ().intValue ();
      }
      else
      {
        min = m_aMinutes.first ().intValue ();
        hr++;
      }
      if (min != t)
      {
        cl.set (Calendar.SECOND, 0);
        cl.set (Calendar.MINUTE, min);
        _setCalendarHour (cl, hr);
        continue;
      }
      cl.set (Calendar.MINUTE, min);

      hr = cl.get (Calendar.HOUR_OF_DAY);
      int day = cl.get (Calendar.DAY_OF_MONTH);
      t = -1;

      // get hour...................................................
      st = m_aHours.tailSet (Integer.valueOf (hr));
      if (st != null && !st.isEmpty ())
      {
        t = hr;
        hr = st.first ().intValue ();
      }
      else
      {
        hr = m_aHours.first ().intValue ();
        day++;
      }
      if (hr != t)
      {
        cl.set (Calendar.SECOND, 0);
        cl.set (Calendar.MINUTE, 0);
        cl.set (Calendar.DAY_OF_MONTH, day);
        _setCalendarHour (cl, hr);
        continue;
      }
      cl.set (Calendar.HOUR_OF_DAY, hr);

      day = cl.get (Calendar.DAY_OF_MONTH);
      int mon = cl.get (Calendar.MONTH) + 1;
      // '+ 1' because calendar is 0-based for this field, and we are
      // 1-based
      t = -1;
      int tmon = mon;

      // get day...................................................
      final boolean dayOfMSpec = !m_aDaysOfMonth.contains (NO_SPEC);
      final boolean dayOfWSpec = !m_aDaysOfWeek.contains (NO_SPEC);
      if (dayOfMSpec && !dayOfWSpec)
      { // get day by day of month rule
        st = m_aDaysOfMonth.tailSet (Integer.valueOf (day));
        if (m_bLastdayOfMonth)
        {
          if (!m_bNearestWeekday)
          {
            t = day;
            day = _getLastDayOfMonth (mon, cl.get (Calendar.YEAR));
            day -= m_nLastdayOffset;
            if (t > day)
            {
              mon++;
              if (mon > 12)
              {
                mon = 1;
                tmon = 3333; // ensure test of mon != tmon further below fails
                cl.add (Calendar.YEAR, 1);
              }
              day = 1;
            }
          }
          else
          {
            t = day;
            day = _getLastDayOfMonth (mon, cl.get (Calendar.YEAR));
            day -= m_nLastdayOffset;

            final Calendar tcal = Calendar.getInstance (getTimeZone (), Locale.getDefault (Locale.Category.FORMAT));
            tcal.set (Calendar.SECOND, 0);
            tcal.set (Calendar.MINUTE, 0);
            tcal.set (Calendar.HOUR_OF_DAY, 0);
            tcal.set (Calendar.DAY_OF_MONTH, day);
            tcal.set (Calendar.MONTH, mon - 1);
            tcal.set (Calendar.YEAR, cl.get (Calendar.YEAR));

            final int ldom = _getLastDayOfMonth (mon, cl.get (Calendar.YEAR));
            final int dow = tcal.get (Calendar.DAY_OF_WEEK);

            if (dow == Calendar.SATURDAY && day == 1)
            {
              day += 2;
            }
            else
              if (dow == Calendar.SATURDAY)
              {
                day -= 1;
              }
              else
                if (dow == Calendar.SUNDAY && day == ldom)
                {
                  day -= 2;
                }
                else
                  if (dow == Calendar.SUNDAY)
                  {
                    day += 1;
                  }

            tcal.set (Calendar.SECOND, sec);
            tcal.set (Calendar.MINUTE, min);
            tcal.set (Calendar.HOUR_OF_DAY, hr);
            tcal.set (Calendar.DAY_OF_MONTH, day);
            tcal.set (Calendar.MONTH, mon - 1);
            final Date nTime = tcal.getTime ();
            if (nTime.before (afterTime))
            {
              day = 1;
              mon++;
            }
          }
        }
        else
          if (m_bNearestWeekday)
          {
            t = day;
            day = m_aDaysOfMonth.first ().intValue ();

            final Calendar tcal = Calendar.getInstance (getTimeZone (), Locale.getDefault (Locale.Category.FORMAT));
            tcal.set (Calendar.SECOND, 0);
            tcal.set (Calendar.MINUTE, 0);
            tcal.set (Calendar.HOUR_OF_DAY, 0);
            tcal.set (Calendar.DAY_OF_MONTH, day);
            tcal.set (Calendar.MONTH, mon - 1);
            tcal.set (Calendar.YEAR, cl.get (Calendar.YEAR));

            final int ldom = _getLastDayOfMonth (mon, cl.get (Calendar.YEAR));
            final int dow = tcal.get (Calendar.DAY_OF_WEEK);

            if (dow == Calendar.SATURDAY && day == 1)
            {
              day += 2;
            }
            else
              if (dow == Calendar.SATURDAY)
              {
                day -= 1;
              }
              else
                if (dow == Calendar.SUNDAY && day == ldom)
                {
                  day -= 2;
                }
                else
                  if (dow == Calendar.SUNDAY)
                  {
                    day += 1;
                  }

            tcal.set (Calendar.SECOND, sec);
            tcal.set (Calendar.MINUTE, min);
            tcal.set (Calendar.HOUR_OF_DAY, hr);
            tcal.set (Calendar.DAY_OF_MONTH, day);
            tcal.set (Calendar.MONTH, mon - 1);
            final Date nTime = tcal.getTime ();
            if (nTime.before (afterTime))
            {
              day = m_aDaysOfMonth.first ().intValue ();
              mon++;
            }
          }
          else
            if (st != null && !st.isEmpty ())
            {
              t = day;
              day = st.first ().intValue ();
              // make sure we don't over-run a short month, such as february
              final int lastDay = _getLastDayOfMonth (mon, cl.get (Calendar.YEAR));
              if (day > lastDay)
              {
                day = m_aDaysOfMonth.first ().intValue ();
                mon++;
              }
            }
            else
            {
              day = m_aDaysOfMonth.first ().intValue ();
              mon++;
            }

        if (day != t || mon != tmon)
        {
          cl.set (Calendar.SECOND, 0);
          cl.set (Calendar.MINUTE, 0);
          cl.set (Calendar.HOUR_OF_DAY, 0);
          cl.set (Calendar.DAY_OF_MONTH, day);
          cl.set (Calendar.MONTH, mon - 1);
          // '- 1' because calendar is 0-based for this field, and we
          // are 1-based
          continue;
        }
      }
      else
        if (dayOfWSpec && !dayOfMSpec)
        { // get day by day of week rule
          if (m_bLastdayOfWeek)
          { // are we looking for the last XXX day of
            // the month?
            // desired
            final int dow = m_aDaysOfWeek.first ().intValue ();
            // d-o-w
            // current d-o-w
            final int cDow = cl.get (Calendar.DAY_OF_WEEK);
            int daysToAdd = 0;
            if (cDow < dow)
            {
              daysToAdd = dow - cDow;
            }
            if (cDow > dow)
            {
              daysToAdd = dow + (7 - cDow);
            }

            final int lDay = _getLastDayOfMonth (mon, cl.get (Calendar.YEAR));

            if (day + daysToAdd > lDay)
            { // did we already miss the
              // last one?
              cl.set (Calendar.SECOND, 0);
              cl.set (Calendar.MINUTE, 0);
              cl.set (Calendar.HOUR_OF_DAY, 0);
              cl.set (Calendar.DAY_OF_MONTH, 1);
              cl.set (Calendar.MONTH, mon);
              // no '- 1' here because we are promoting the month
              continue;
            }

            // find date of last occurrence of this day in this month...
            while ((day + daysToAdd + 7) <= lDay)
            {
              daysToAdd += 7;
            }

            day += daysToAdd;

            if (daysToAdd > 0)
            {
              cl.set (Calendar.SECOND, 0);
              cl.set (Calendar.MINUTE, 0);
              cl.set (Calendar.HOUR_OF_DAY, 0);
              cl.set (Calendar.DAY_OF_MONTH, day);
              cl.set (Calendar.MONTH, mon - 1);
              // '- 1' here because we are not promoting the month
              continue;
            }

          }
          else
            if (m_nNthdayOfWeek != 0)
            {
              // are we looking for the Nth XXX day in the month?
              // desired
              final int dow = m_aDaysOfWeek.first ().intValue ();
              // d-o-w
              // current d-o-w
              final int cDow = cl.get (Calendar.DAY_OF_WEEK);
              int daysToAdd = 0;
              if (cDow < dow)
              {
                daysToAdd = dow - cDow;
              }
              else
                if (cDow > dow)
                {
                  daysToAdd = dow + (7 - cDow);
                }

              boolean dayShifted = false;
              if (daysToAdd > 0)
              {
                dayShifted = true;
              }

              day += daysToAdd;
              int weekOfMonth = day / 7;
              if (day % 7 > 0)
              {
                weekOfMonth++;
              }

              daysToAdd = (m_nNthdayOfWeek - weekOfMonth) * 7;
              day += daysToAdd;
              if (daysToAdd < 0 || day > _getLastDayOfMonth (mon, cl.get (Calendar.YEAR)))
              {
                cl.set (Calendar.SECOND, 0);
                cl.set (Calendar.MINUTE, 0);
                cl.set (Calendar.HOUR_OF_DAY, 0);
                cl.set (Calendar.DAY_OF_MONTH, 1);
                cl.set (Calendar.MONTH, mon);
                // no '- 1' here because we are promoting the month
                continue;
              }
              else
                if (daysToAdd > 0 || dayShifted)
                {
                  cl.set (Calendar.SECOND, 0);
                  cl.set (Calendar.MINUTE, 0);
                  cl.set (Calendar.HOUR_OF_DAY, 0);
                  cl.set (Calendar.DAY_OF_MONTH, day);
                  cl.set (Calendar.MONTH, mon - 1);
                  // '- 1' here because we are NOT promoting the month
                  continue;
                }
            }
            else
            {
              // current d-o-w
              final int cDow = cl.get (Calendar.DAY_OF_WEEK);
              // desired
              int dow = m_aDaysOfWeek.first ().intValue ();
              // d-o-w
              st = m_aDaysOfWeek.tailSet (Integer.valueOf (cDow));
              if (st != null && !st.isEmpty ())
              {
                dow = st.first ().intValue ();
              }

              int daysToAdd = 0;
              if (cDow < dow)
              {
                daysToAdd = dow - cDow;
              }
              if (cDow > dow)
              {
                daysToAdd = dow + (7 - cDow);
              }

              final int lDay = _getLastDayOfMonth (mon, cl.get (Calendar.YEAR));

              if (day + daysToAdd > lDay)
              { // will we pass the end of
                // the month?
                cl.set (Calendar.SECOND, 0);
                cl.set (Calendar.MINUTE, 0);
                cl.set (Calendar.HOUR_OF_DAY, 0);
                cl.set (Calendar.DAY_OF_MONTH, 1);
                cl.set (Calendar.MONTH, mon);
                // no '- 1' here because we are promoting the month
                continue;
              }
              else
                if (daysToAdd > 0)
                { // are we swithing days?
                  cl.set (Calendar.SECOND, 0);
                  cl.set (Calendar.MINUTE, 0);
                  cl.set (Calendar.HOUR_OF_DAY, 0);
                  cl.set (Calendar.DAY_OF_MONTH, day + daysToAdd);
                  cl.set (Calendar.MONTH, mon - 1);
                  // '- 1' because calendar is 0-based for this field,
                  // and we are 1-based
                  continue;
                }
            }
        }
        else
        { // dayOfWSpec && !dayOfMSpec
          throw new UnsupportedOperationException ("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.");
        }
      cl.set (Calendar.DAY_OF_MONTH, day);

      mon = cl.get (Calendar.MONTH) + 1;
      // '+ 1' because calendar is 0-based for this field, and we are
      // 1-based
      int year = cl.get (Calendar.YEAR);
      t = -1;

      // test for expressions that never generate a valid fire date,
      // but keep looping...
      if (year > CQuartz.MAX_YEAR)
      {
        return null;
      }

      // get month...................................................
      st = m_aMonths.tailSet (Integer.valueOf (mon));
      if (st != null && !st.isEmpty ())
      {
        t = mon;
        mon = st.first ().intValue ();
      }
      else
      {
        mon = m_aMonths.first ().intValue ();
        year++;
      }
      if (mon != t)
      {
        cl.set (Calendar.SECOND, 0);
        cl.set (Calendar.MINUTE, 0);
        cl.set (Calendar.HOUR_OF_DAY, 0);
        cl.set (Calendar.DAY_OF_MONTH, 1);
        cl.set (Calendar.MONTH, mon - 1);
        // '- 1' because calendar is 0-based for this field, and we are
        // 1-based
        cl.set (Calendar.YEAR, year);
        continue;
      }
      cl.set (Calendar.MONTH, mon - 1);
      // '- 1' because calendar is 0-based for this field, and we are
      // 1-based

      year = cl.get (Calendar.YEAR);

      // get year...................................................
      st = m_aYears.tailSet (Integer.valueOf (year));
      if (st == null || st.isEmpty ())
      {
        // ran out of years...
        return null;
      }

      t = year;
      year = st.first ().intValue ();

      if (year != t)
      {
        cl.set (Calendar.SECOND, 0);
        cl.set (Calendar.MINUTE, 0);
        cl.set (Calendar.HOUR_OF_DAY, 0);
        cl.set (Calendar.DAY_OF_MONTH, 1);
        cl.set (Calendar.MONTH, 0);
        // '- 1' because calendar is 0-based for this field, and we are
        // 1-based
        cl.set (Calendar.YEAR, year);
        continue;
      }
      cl.set (Calendar.YEAR, year);

      bGotOne = true;
    } // while( !done )

    return cl.getTime ();
  }

  /**
   * Advance the calendar to the particular hour paying particular attention to
   * daylight saving problems.
   *
   * @param cal
   *        the calendar to operate on
   * @param hour
   *        the hour to set
   */
  private static void _setCalendarHour (final Calendar cal, final int hour)
  {
    cal.set (Calendar.HOUR_OF_DAY, hour);
    if (cal.get (Calendar.HOUR_OF_DAY) != hour && hour != 24)
    {
      cal.set (Calendar.HOUR_OF_DAY, hour + 1);
    }
  }

  /**
   * NOT YET IMPLEMENTED: Returns the time before the given time that the
   * <code>CronExpression</code> matches.
   *
   * @param endTime
   *        end time
   * @return the time before the given time that the <code>CronExpression</code>
   *         matches. May be <code>null</code>.
   */
  @Nullable
  public Date getTimeBefore (final Date endTime)
  {
    // FUTURE_TODO: implement QUARTZ-423
    return null;
  }

  /**
   * NOT YET IMPLEMENTED: Returns the final time that the
   * <code>CronExpression</code> will match.
   *
   * @return the final time that the <code>CronExpression</code> will match.
   */
  @Nullable
  public Date getFinalFireTime ()
  {
    // FUTURE_TODO: implement QUARTZ-423
    return null;
  }

  private static boolean _isLeapYear (final int year)
  {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
  }

  private static int _getLastDayOfMonth (final int monthNum, final int year)
  {
    switch (monthNum)
    {
      case 1:
        return 31;
      case 2:
        return _isLeapYear (year) ? 29 : 28;
      case 3:
        return 31;
      case 4:
        return 30;
      case 5:
        return 31;
      case 6:
        return 30;
      case 7:
        return 31;
      case 8:
        return 31;
      case 9:
        return 30;
      case 10:
        return 31;
      case 11:
        return 30;
      case 12:
        return 31;
      default:
        throw new IllegalArgumentException ("Illegal month number: " + monthNum);
    }
  }

  private void readObject (final ObjectInputStream stream) throws IOException, ClassNotFoundException
  {
    stream.defaultReadObject ();
    try
    {
      _buildExpression (m_sCronExpression);
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException (ex);
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public CronExpression getClone ()
  {
    return new CronExpression (this);
  }
}
