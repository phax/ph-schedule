/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2026 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import com.helger.quartz.ITrigger.EMisfireInstruction;
import com.helger.quartz.impl.triggers.CronTrigger;

/**
 * Unit test for CronTrigger.
 */
public class CronTriggerTest
{
  @Test
  public void testClone () throws ParseException
  {
    final CronTrigger trigger = new CronTrigger ();
    trigger.setName ("test");
    trigger.setGroup ("testGroup");
    trigger.setCronExpression ("0 0 12 * * ?");
    final CronTrigger trigger2 = trigger.getClone ();

    assertEquals ("Cloning failed", trigger, trigger2);

    // equals() doesn't test the cron expression
    assertEquals ("Cloning failed for the cron expression", "0 0 12 * * ?", trigger2.getCronExpression ());
  }

  @Test
  public void testGetFinalFireTime () throws ParseException
  {
    final TimeZone aUTC = TimeZone.getTimeZone ("UTC");
    final Calendar aCal = Calendar.getInstance (aUTC, Locale.getDefault (Locale.Category.FORMAT));

    final CronTrigger aTrigger = new CronTrigger ();
    aTrigger.setName ("test");
    aTrigger.setJobName ("job");
    aTrigger.setCronExpression ("0 0 12 * * ?");
    aTrigger.setTimeZone (aUTC);

    aCal.clear ();
    aCal.set (2026, Calendar.JANUARY, 1, 0, 0, 0);
    aTrigger.setStartTime (aCal.getTime ());

    // Without an end time there is no final fire time
    assertNull (aTrigger.getFinalFireTime ());

    aCal.clear ();
    aCal.set (2026, Calendar.MARCH, 10, 18, 0, 0);
    aTrigger.setEndTime (aCal.getTime ());

    // The last firing before the end time
    aCal.clear ();
    aCal.set (2026, Calendar.MARCH, 10, 12, 0, 0);
    assertEquals (aCal.getTime (), aTrigger.getFinalFireTime ());
  }

  // http://jira.opensymphony.com/browse/QUARTZ-558
  @Test
  public void testQuartz558 ()
  {
    final CronTrigger trigger = new CronTrigger ();
    trigger.setName ("test");
    trigger.setGroup ("testGroup");
    final ICronTrigger trigger2 = trigger.getClone ();

    assertEquals ("Cloning failed", trigger, trigger2);
  }

  @Test
  public void testMisfireInstructionValidity ()
  {
    final CronTrigger trigger = new CronTrigger ();

    try
    {
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_DO_NOTHING);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }
    catch (final Exception e)
    {
      fail ("Unexpected exception while setting misfire instruction.");
    }

    try
    {
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT);

      fail ("Expected exception while setting invalid misfire instruction but did not get it.");
    }
    catch (final Exception e)
    {}
  }
}
