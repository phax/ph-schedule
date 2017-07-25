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
package com.helger.quartz;

import java.util.Date;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.spi.IOperableTrigger;

/**
 * Convenience and utility methods for working with
 * <code>{@link ITrigger}s</code>.
 *
 * @see ICronTrigger
 * @see ISimpleTrigger
 * @see DateBuilder
 * @author James House
 */
public final class TriggerUtils
{
  /**
   * Private constructor because this is a pure utility class.
   */
  private TriggerUtils ()
  {}

  /**
   * Returns a list of Dates that are the next fire times of a
   * <code>Trigger</code>. The input trigger will be cloned before any work is
   * done, so you need not worry about its state being altered by this method.
   *
   * @param trigg
   *        The trigger upon which to do the work
   * @param cal
   *        The calendar to apply to the trigger's schedule
   * @param numTimes
   *        The number of next fire times to produce
   * @return List of java.util.Date objects
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <Date> computeFireTimes (final IOperableTrigger trigg,
                                                      final ICalendar cal,
                                                      final int numTimes)
  {
    final ICommonsList <Date> lst = new CommonsArrayList <> ();

    final IOperableTrigger t = (IOperableTrigger) trigg.clone ();
    if (t.getNextFireTime () == null)
      t.computeFirstFireTime (cal);

    for (int i = 0; i < numTimes; i++)
    {
      final Date d = t.getNextFireTime ();
      if (d != null)
      {
        lst.add (d);
        t.triggered (cal);
      }
      else
      {
        break;
      }
    }

    return lst;
  }

  /**
   * Compute the <code>Date</code> that is 1 second after the Nth firing of the
   * given <code>Trigger</code>, taking the triger's associated
   * <code>Calendar</code> into consideration. The input trigger will be cloned
   * before any work is done, so you need not worry about its state being
   * altered by this method.
   *
   * @param trigg
   *        The trigger upon which to do the work
   * @param cal
   *        The calendar to apply to the trigger's schedule
   * @param numTimes
   *        The number of next fire times to produce
   * @return the computed Date, or null if the trigger (as configured) will not
   *         fire that many times.
   */
  public static Date computeEndTimeToAllowParticularNumberOfFirings (final IOperableTrigger trigg,
                                                                     final ICalendar cal,
                                                                     final int numTimes)
  {
    final IOperableTrigger t = (IOperableTrigger) trigg.clone ();
    if (t.getNextFireTime () == null)
      t.computeFirstFireTime (cal);

    int c = 0;
    Date endTime = null;
    for (int i = 0; i < numTimes; i++)
    {
      final Date d = t.getNextFireTime ();
      if (d != null)
      {
        c++;
        t.triggered (cal);
        if (c == numTimes)
          endTime = d;
      }
      else
      {
        break;
      }
    }

    if (endTime == null)
      return null;

    endTime = new Date (endTime.getTime () + 1000L);
    return endTime;
  }

  /**
   * Returns a list of Dates that are the next fire times of a
   * <code>Trigger</code> that fall within the given date range. The input
   * trigger will be cloned before any work is done, so you need not worry about
   * its state being altered by this method.
   * <p>
   * NOTE: if this is a trigger that has previously fired within the given date
   * range, then firings which have already occurred will not be listed in the
   * output List.
   * </p>
   *
   * @param trigg
   *        The trigger upon which to do the work
   * @param cal
   *        The calendar to apply to the trigger's schedule
   * @param from
   *        The starting date at which to find fire times
   * @param to
   *        The ending date at which to stop finding fire times
   * @return List of java.util.Date objects
   */
  public static ICommonsList <Date> computeFireTimesBetween (final IOperableTrigger trigg,
                                                             final ICalendar cal,
                                                             final Date from,
                                                             final Date to)
  {
    final ICommonsList <Date> lst = new CommonsArrayList <> ();
    final IOperableTrigger t = (IOperableTrigger) trigg.clone ();
    if (t.getNextFireTime () == null)
    {
      t.setStartTime (from);
      t.setEndTime (to);
      t.computeFirstFireTime (cal);
    }

    while (true)
    {
      final Date d = t.getNextFireTime ();
      if (d != null)
      {
        if (d.before (from))
        {
          t.triggered (cal);
          continue;
        }
        if (d.after (to))
        {
          break;
        }
        lst.add (d);
        t.triggered (cal);
      }
      else
      {
        break;
      }
    }

    return lst;
  }
}
