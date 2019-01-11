/**
 * Copyright (C) 2014-2019 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.mutable.MutableBoolean;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for class {@link GlobalQuartzScheduler}.
 *
 * @author Philip Helger
 */
public final class GlobalQuartzSchedulerTest
{
  static final MutableBoolean EXEC_LOG = new MutableBoolean (false);

  @Rule
  public TestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testGetScheduler () throws Exception
  {
    final GlobalQuartzScheduler aDS = GlobalQuartzScheduler.getInstance ();
    aDS.scheduleJob ("test",
                     JDK8TriggerBuilder.newTrigger ()
                                       .startNow ()
                                       .withSchedule (SimpleScheduleBuilder.repeatMinutelyForTotalCount (1)),
                     MockJob.class,
                     null);
    Thread.sleep (100);
    assertTrue (EXEC_LOG.booleanValue ());
  }
}
