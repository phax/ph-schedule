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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Set;

import org.junit.Test;

public final class Quartz601Test
{
  private static void _assertParsesForField (final String expression, final CronExpression.EType constant)
  {
    try
    {
      final CronExpression cronExpression = new CronExpression (expression);
      final Set <Integer> set = cronExpression.getSet (constant);
      assertFalse ("Empty field [" + constant + "] returned for " + expression, set.isEmpty ());
    }
    catch (final ParseException pe)
    {
      fail ("Exception thrown during parsing: " + pe);
    }
  }

  @Test
  public void testNormal ()
  {
    for (final CronExpression.EType e : CronExpression.EType.values ())
      _assertParsesForField ("0 15 10 * * ? 2005", e);
  }

  @Test
  public void testSecond ()
  {
    _assertParsesForField ("58-4 5 21 ? * MON-FRI", CronExpression.EType.SECOND);
  }

  @Test
  public void testMinute ()
  {
    _assertParsesForField ("0 58-4 21 ? * MON-FRI", CronExpression.EType.MINUTE);
  }

  @Test
  public void testHour ()
  {
    _assertParsesForField ("0 0/5 21-3 ? * MON-FRI", CronExpression.EType.HOUR);
  }

  @Test
  public void testDayOfMonth ()
  {
    _assertParsesForField ("58 5 21 28-5 1 ?", CronExpression.EType.DAY_OF_MONTH);
  }

  @Test
  public void testMonth ()
  {
    _assertParsesForField ("58 5 21 ? 11-2 FRI", CronExpression.EType.MONTH);
  }

  @Test
  public void testDayOfWeekNumber ()
  {
    _assertParsesForField ("58 5 21 ? * 6-2", CronExpression.EType.DAY_OF_WEEK);
  }

  @Test
  public void testDayOfWeek ()
  {
    _assertParsesForField ("58 5 21 ? * FRI-TUE", CronExpression.EType.DAY_OF_WEEK);
  }

  @Test
  public void testAmbiguous ()
  {
    _assertParsesForField ("55-3 56-2 6 ? * FRI", CronExpression.EType.SECOND);
    _assertParsesForField ("55-3 56-2 6 ? * FRI", CronExpression.EType.MINUTE);

    _assertParsesForField ("0 0 14-6 ? * FRI-MON", CronExpression.EType.HOUR);
    _assertParsesForField ("0 0 14-6 ? * FRI-MON", CronExpression.EType.DAY_OF_WEEK);
  }
}
