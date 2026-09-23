/*
 * Copyright (C) 2014-2026 Philip Helger (www.helger.com)
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
package com.helger.schedule.quartz;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;

/**
 * Test class for class {@link QuartzSchedulerHelper}.
 * 
 * @author Philip Helger
 */
public final class QuartzSchedulerHelperTest
{
  @Test
  public void testGetScheduler () throws SchedulerException
  {
    assertNotNull (QuartzSchedulerHelper.getScheduler (false));
    // Was never started so far
    assertSame (ESchedulerState.NOT_STARTED, QuartzSchedulerHelper.getSchedulerState ());
    assertNotNull (QuartzSchedulerHelper.getScheduler ());
    assertSame (ESchedulerState.STARTED, QuartzSchedulerHelper.getSchedulerState ());
    assertNotNull (QuartzSchedulerHelper.getSchedulerMetaData ());
    QuartzSchedulerHelper.getScheduler ().shutdown (true);
    // After a shutdown the factory hands out a brand new scheduler, so the shut down one can no
    // longer be observed through the helper
    assertSame (ESchedulerState.NOT_STARTED, QuartzSchedulerHelper.getSchedulerState ());
    assertNotNull (QuartzSchedulerHelper.getScheduler ());
    assertSame (ESchedulerState.STARTED, QuartzSchedulerHelper.getSchedulerState ());
    QuartzSchedulerHelper.getScheduler ().shutdown (true);
  }

  @Test
  public void testStandbyIsNotNotStarted () throws SchedulerException
  {
    final IScheduler aScheduler = QuartzSchedulerHelper.getScheduler ();
    assertSame (ESchedulerState.STARTED, QuartzSchedulerHelper.getSchedulerState ());

    // Standby after a start is a different state than "never started"
    aScheduler.standby ();
    assertSame (ESchedulerState.STANDBY, QuartzSchedulerHelper.getSchedulerState ());

    aScheduler.start ();
    assertSame (ESchedulerState.STARTED, QuartzSchedulerHelper.getSchedulerState ());
    aScheduler.shutdown (true);
  }
}
