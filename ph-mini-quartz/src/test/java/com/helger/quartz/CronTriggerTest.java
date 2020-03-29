/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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
import static org.junit.Assert.fail;

import java.text.ParseException;

import org.junit.Test;

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
      trigger.setMisfireInstruction (ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      trigger.setMisfireInstruction (ITrigger.MISFIRE_INSTRUCTION_SMART_POLICY);
      trigger.setMisfireInstruction (ICronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
      trigger.setMisfireInstruction (ICronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
    }
    catch (final Exception e)
    {
      fail ("Unexpected exception while setting misfire instruction.");
    }

    try
    {
      trigger.setMisfireInstruction (ICronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING + 1);

      fail ("Expected exception while setting invalid misfire instruction but did not get it.");
    }
    catch (final Exception e)
    {}
  }
}
