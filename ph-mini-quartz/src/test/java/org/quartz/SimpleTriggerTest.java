/*
 * Copyright 2001-2009 Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.quartz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;
import org.quartz.impl.triggers.SimpleTriggerImpl;

/**
 * Unit test for SimpleTrigger serialization backwards compatibility.
 */
public class SimpleTriggerTest
{
  private static final TimeZone EST_TIME_ZONE = TimeZone.getTimeZone ("US/Eastern");
  private static final Calendar START_TIME = Calendar.getInstance ();
  private static final Calendar END_TIME = Calendar.getInstance ();

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

    final Calendar startTime = Calendar.getInstance ();
    startTime.set (2005, Calendar.JULY, 5, 9, 0, 0);

    final Calendar endTime = Calendar.getInstance ();
    endTime.set (2005, Calendar.JULY, 5, 10, 0, 0);

    final SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl ();
    simpleTrigger.setMisfireInstruction (SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT);
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
    final SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl ();

    simpleTrigger.setStartTime (new Date (0));
    simpleTrigger.setRepeatInterval (10);
    simpleTrigger.setRepeatCount (4);

    final Date fireTimeAfter = simpleTrigger.getFireTimeAfter (new Date (34));
    assertEquals (40, fireTimeAfter.getTime ());
  }

  @Test
  public void testClone ()
  {
    final SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl ();

    // Make sure empty sub-objects are cloned okay
    Trigger clone = (Trigger) simpleTrigger.clone ();
    assertEquals (0, clone.getJobDataMap ().size ());

    // Make sure non-empty sub-objects are cloned okay
    simpleTrigger.getJobDataMap ().put ("K1", "V1");
    simpleTrigger.getJobDataMap ().put ("K2", "V2");
    clone = (Trigger) simpleTrigger.clone ();
    assertEquals (2, clone.getJobDataMap ().size ());
    assertEquals ("V1", clone.getJobDataMap ().get ("K1"));
    assertEquals ("V2", clone.getJobDataMap ().get ("K2"));

    // Make sure sub-object collections have really been cloned by ensuring
    // their modification does not change the source Trigger
    clone.getJobDataMap ().remove ("K1");
    assertEquals (1, clone.getJobDataMap ().size ());

    assertEquals (2, simpleTrigger.getJobDataMap ().size ());
    assertEquals ("V1", simpleTrigger.getJobDataMap ().get ("K1"));
    assertEquals ("V2", simpleTrigger.getJobDataMap ().get ("K2"));
  }

  // NPE in equals()
  public void testQuartz665 ()
  {
    new SimpleTriggerImpl ().equals (new SimpleTriggerImpl ());
  }

  @Test
  public void testMisfireInstructionValidity ()
  {
    final SimpleTriggerImpl trigger = new SimpleTriggerImpl ();

    try
    {
      trigger.setMisfireInstruction (Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      trigger.setMisfireInstruction (Trigger.MISFIRE_INSTRUCTION_SMART_POLICY);
      trigger.setMisfireInstruction (SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
      trigger.setMisfireInstruction (SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT);
      trigger.setMisfireInstruction (SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
      trigger.setMisfireInstruction (SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT);
      trigger.setMisfireInstruction (SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT);
    }
    catch (final Exception e)
    {
      fail ("Unexpected exception while setting misfire instruction: " + e.getMessage ());
    }

    try
    {
      trigger.setMisfireInstruction (SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT + 1);

      fail ("Expected exception while setting invalid misfire instruction but did not get it.");
    }
    catch (final Exception e)
    {}
  }
}
