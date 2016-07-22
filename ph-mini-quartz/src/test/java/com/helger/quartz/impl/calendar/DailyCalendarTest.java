/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.quartz.impl.calendar.DailyCalendar;

/**
 * Unit test for DailyCalendar.
 */
public class DailyCalendarTest
{
  @Test
  public void testStringStartEndTimes ()
  {
    DailyCalendar dailyCalendar = new DailyCalendar ("1:20", "14:50");
    assertTrue (dailyCalendar.toString ().indexOf ("01:20:00:000 - 14:50:00:000") > 0);

    dailyCalendar = new DailyCalendar ("1:20:1:456", "14:50:15:2");
    assertTrue (dailyCalendar.toString ().indexOf ("01:20:01:456 - 14:50:15:002") > 0);
  }

  @Test
  public void testStringInvertTimeRange ()
  {
    final DailyCalendar dailyCalendar = new DailyCalendar ("1:20", "14:50");
    dailyCalendar.setInvertTimeRange (true);
    assertTrue (dailyCalendar.toString ().indexOf ("inverted: true") > 0);

    dailyCalendar.setInvertTimeRange (false);
    assertTrue (dailyCalendar.toString ().indexOf ("inverted: false") > 0);
  }
}
