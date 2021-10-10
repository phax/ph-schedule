/*
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
package com.helger.quartz;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.datetime.PDTFactory;

/**
 * Quartz constants
 *
 * @author Philip Helger
 */
@Immutable
public final class CQuartz
{
  public static final int MAX_YEAR = PDTFactory.getCurrentYear () + 100;

  private CQuartz ()
  {}

  /**
   * Return a date with time of day reset to this object values. The millisecond
   * value will be zero.
   */
  @Nullable
  public static Date onDate (@Nonnull final LocalTime aLT, @Nullable final Date dateTime)
  {
    if (dateTime == null)
      return null;
    final Calendar cal = PDTFactory.createCalendar ();
    cal.setTime (dateTime);
    cal.set (Calendar.HOUR_OF_DAY, aLT.getHour ());
    cal.set (Calendar.MINUTE, aLT.getMinute ());
    cal.set (Calendar.SECOND, aLT.getSecond ());
    cal.clear (Calendar.MILLISECOND);
    return cal.getTime ();
  }
}
