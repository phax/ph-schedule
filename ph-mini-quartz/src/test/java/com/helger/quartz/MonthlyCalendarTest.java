/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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

import java.util.Calendar;

import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.quartz.impl.calendar.MonthlyCalendar;

/**
 * Unit test for MonthlyCalendar
 */
public final class MonthlyCalendarTest
{
  /**
   * Tests whether greater than the 7th of the month causes infinite looping.
   * See: QUARTZ-636
   */
  @Test
  public void testForInfiniteLoop ()
  {
    final MonthlyCalendar monthlyCalendar = new MonthlyCalendar ();

    for (int i = 1; i < 9; i++)
    {
      monthlyCalendar.setDayExcluded (i, true);
    }
    final Calendar c = PDTFactory.createCalendar ();
    c.set (2007, 11, 8, 12, 0, 0);

    monthlyCalendar.getNextIncludedTime (c.getTime ().getTime ());
  }

}
