/*
 * Copyright 2007-2009 Terracotta, Inc.
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
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.junit.Test;
import org.quartz.impl.triggers.CronTriggerImpl;

/**
 * Unit test for CronTrigger.
 */
public class CronTriggerTest
{
  @Test
  public void testClone () throws ParseException
  {
    final CronTriggerImpl trigger = new CronTriggerImpl ();
    trigger.setName ("test");
    trigger.setGroup ("testGroup");
    trigger.setCronExpression ("0 0 12 * * ?");
    final CronTrigger trigger2 = (CronTrigger) trigger.clone ();

    assertEquals ("Cloning failed", trigger, trigger2);

    // equals() doesn't test the cron expression
    assertEquals ("Cloning failed for the cron expression", "0 0 12 * * ?", trigger2.getCronExpression ());
  }

  // http://jira.opensymphony.com/browse/QUARTZ-558
  @Test
  public void testQuartz558 ()
  {
    final CronTriggerImpl trigger = new CronTriggerImpl ();
    trigger.setName ("test");
    trigger.setGroup ("testGroup");
    final CronTrigger trigger2 = (CronTrigger) trigger.clone ();

    assertEquals ("Cloning failed", trigger, trigger2);
  }

  @Test
  public void testMisfireInstructionValidity ()
  {
    final CronTriggerImpl trigger = new CronTriggerImpl ();

    try
    {
      trigger.setMisfireInstruction (Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      trigger.setMisfireInstruction (Trigger.MISFIRE_INSTRUCTION_SMART_POLICY);
      trigger.setMisfireInstruction (CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
      trigger.setMisfireInstruction (CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }
    catch (final Exception e)
    {
      fail ("Unexpected exception while setting misfire instruction.");
    }

    try
    {
      trigger.setMisfireInstruction (CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING + 1);

      fail ("Expected exception while setting invalid misfire instruction but did not get it.");
    }
    catch (final Exception e)
    {}
  }
}
