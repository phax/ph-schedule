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
package com.helger.schedule.quartz.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.MockFailingJob;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.scope.mock.ScopeTestRule;

/**
 * Test class for class {@link ErrorHistoryJobListener}.
 *
 * @author Philip Helger
 */
public final class ErrorHistoryJobListenerTest
{
  @Rule
  public TestRule m_aRule = new ScopeTestRule ();

  @Test
  public void testFailingJobIsRemembered () throws Exception
  {
    JobExecutionErrorRegistry.removeAllErrors ();

    final GlobalQuartzScheduler aScheduler = GlobalQuartzScheduler.getInstance ();
    // The listener is registered by default
    assertNotNull (aScheduler.getJobListenerOfName ("ErrorHistoryJobListener"));

    final TriggerKey aTriggerKey = aScheduler.scheduleJob ("failing-job",
                                                           JDK8TriggerBuilder.newTrigger ()
                                                                             .startNow ()
                                                                             .withSchedule (SimpleScheduleBuilder.repeatMinutelyForTotalCount (1)),
                                                           MockFailingJob.class,
                                                           null);
    assertNotNull (aTriggerKey);

    // Wait for the job to run and fail
    int nWait = 0;
    while (JobExecutionErrorRegistry.getTotalErrorCount () == 0 && nWait < 50)
    {
      ThreadHelper.sleep (100);
      nWait++;
    }

    assertEquals (1, JobExecutionErrorRegistry.getTotalErrorCount ());

    final JobExecutionError aError = JobExecutionErrorRegistry.getAllErrors ().values ().iterator ().next ().getLastOrNull ();
    assertNotNull (aError);
    assertEquals (MockFailingJob.class.getName (), aError.getJobClassName ());
    assertNotNull (aError.getErrorDateTime ());
    assertTrue (aError.getThrowableMessage ().contains (MockFailingJob.FAILURE_MESSAGE));
    assertTrue (aError.getStackTrace ().length () > 0);

    JobExecutionErrorRegistry.removeAllErrors ();
  }
}
