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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.impl.triggers.SimpleTrigger;

/**
 * Unit test for {@link JobExecutionException}.
 */
public final class JobExecutionExceptionTest
{
  private static SimpleTrigger _createTrigger ()
  {
    final SimpleTrigger ret = new SimpleTrigger ();
    ret.setName ("test");
    ret.setJobName ("job");
    ret.setStartTime (new Date ());
    return ret;
  }

  @Test
  public void testDefaults ()
  {
    final JobExecutionException aEx = new JobExecutionException ("Test");
    assertFalse (aEx.refireImmediately ());
    assertFalse (aEx.unscheduleFiringTrigger ());
    assertFalse (aEx.unscheduleAllTriggers ());
  }

  @Test
  public void testUnscheduleFiringTrigger ()
  {
    final JobExecutionException aEx = new JobExecutionException ("Test");
    aEx.setUnscheduleFiringTrigger (true);
    assertTrue (aEx.unscheduleFiringTrigger ());
    assertFalse (aEx.unscheduleAllTriggers ());

    assertEquals (ECompletedExecutionInstruction.SET_TRIGGER_COMPLETE,
                  _createTrigger ().executionComplete (null, aEx));
  }

  @Test
  public void testUnscheduleAllTriggers ()
  {
    final JobExecutionException aEx = new JobExecutionException ("Test");
    aEx.setUnscheduleAllTriggers (true);
    assertFalse (aEx.unscheduleFiringTrigger ());
    assertTrue (aEx.unscheduleAllTriggers ());

    assertEquals (ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE,
                  _createTrigger ().executionComplete (null, aEx));
  }

  @Test
  public void testRefireImmediatelyWins ()
  {
    // "re-fire immediately" is evaluated before the unschedule flags
    final JobExecutionException aEx = new JobExecutionException ("Test", true);
    aEx.setUnscheduleAllTriggers (true);

    assertEquals (ECompletedExecutionInstruction.RE_EXECUTE_JOB, _createTrigger ().executionComplete (null, aEx));
  }
}
