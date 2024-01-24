/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2024 Philip Helger (www.helger.com)
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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.quartz.ITrigger.EMisfireInstruction;
import com.helger.quartz.impl.triggers.SimpleTrigger;

/**
 * Unit test for SimpleTrigger serialization backwards compatibility.
 */
public class SimpleTriggerTest
{
  private static final TimeZone EST_TIME_ZONE = TimeZone.getTimeZone ("US/Eastern");
  private static final Calendar START_TIME = PDTFactory.createCalendar ();
  private static final Calendar END_TIME = PDTFactory.createCalendar ();

  static
  {
    START_TIME.clear ();
    START_TIME.set (2006, Calendar.JUNE, 1, 10, 5, 15);
    START_TIME.setTimeZone (EST_TIME_ZONE);
    END_TIME.clear ();
    END_TIME.set (2008, Calendar.MAY, 2, 20, 15, 30);
    END_TIME.setTimeZone (EST_TIME_ZONE);
  }

  @Test
  public void testUpdateAfterMisfire ()
  {

    final Calendar startTime = PDTFactory.createCalendar ();
    startTime.set (2005, Calendar.JULY, 5, 9, 0, 0);

    final Calendar endTime = PDTFactory.createCalendar ();
    endTime.set (2005, Calendar.JULY, 5, 10, 0, 0);

    final SimpleTrigger simpleTrigger = new SimpleTrigger ();
    simpleTrigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT);
    simpleTrigger.setRepeatCount (5);
    simpleTrigger.setStartTime (startTime.getTime ());
    simpleTrigger.setEndTime (endTime.getTime ());

    simpleTrigger.updateAfterMisfire (null);
    assertEquals (startTime.getTime (), simpleTrigger.getStartTime ());
    assertEquals (endTime.getTime (), simpleTrigger.getEndTime ());
    assertNull (simpleTrigger.getNextFireTime ());
  }

  @Test
  public void testGetFireTimeAfter ()
  {
    final SimpleTrigger simpleTrigger = new SimpleTrigger ();

    simpleTrigger.setStartTime (new Date (0));
    simpleTrigger.setRepeatInterval (10);
    simpleTrigger.setRepeatCount (4);

    final Date fireTimeAfter = simpleTrigger.getFireTimeAfter (new Date (34));
    assertEquals (40, fireTimeAfter.getTime ());
  }

  @Test
  public void testClone ()
  {
    final SimpleTrigger simpleTrigger = new SimpleTrigger ();

    // Make sure empty sub-objects are cloned okay
    SimpleTrigger aClone = simpleTrigger.getClone ();
    assertEquals (0, aClone.getJobDataMap ().size ());

    // Make sure non-empty sub-objects are cloned okay
    simpleTrigger.getJobDataMap ().put ("K1", "V1");
    simpleTrigger.getJobDataMap ().put ("K2", "V2");
    aClone = simpleTrigger.getClone ();
    assertEquals (2, aClone.getJobDataMap ().size ());
    assertEquals ("V1", aClone.getJobDataMap ().get ("K1"));
    assertEquals ("V2", aClone.getJobDataMap ().get ("K2"));

    // Make sure sub-object collections have really been cloned by ensuring
    // their modification does not change the source Trigger
    aClone.getJobDataMap ().remove ("K1");
    assertEquals (1, aClone.getJobDataMap ().size ());

    assertEquals (2, simpleTrigger.getJobDataMap ().size ());
    assertEquals ("V1", simpleTrigger.getJobDataMap ().get ("K1"));
    assertEquals ("V2", simpleTrigger.getJobDataMap ().get ("K2"));
  }

  // NPE in equals()
  public void testQuartz665 ()
  {
    new SimpleTrigger ().equals (new SimpleTrigger ());
  }

  @Test
  public void testMisfireInstructionValidity ()
  {
    final SimpleTrigger trigger = new SimpleTrigger ();

    try
    {
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT);
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT);
    }
    catch (final Exception e)
    {
      fail ("Unexpected exception while setting misfire instruction: " + e.getMessage ());
    }

    try
    {
      trigger.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_DO_NOTHING);

      fail ("Expected exception while setting invalid misfire instruction but did not get it.");
    }
    catch (final Exception e)
    {}
  }
}
