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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.datetime.helper.PDTFactory;
import com.helger.quartz.JobKey;

/**
 * Test class for class {@link JobExecutionErrorRegistry}.
 *
 * @author Philip Helger
 */
public final class JobExecutionErrorRegistryTest
{
  private static final JobKey JOB_KEY = new JobKey ("job1", "group1");
  private static final JobKey JOB_KEY2 = new JobKey ("job2", "group1");

  @Before
  @After
  public void cleanup ()
  {
    JobExecutionErrorRegistry.removeAllErrors ();
    JobExecutionErrorRegistry.setMaxErrorsPerJob (JobExecutionErrorRegistry.DEFAULT_MAX_ERRORS_PER_JOB);
  }

  private static void _addError (final JobKey aJobKey, final String sMsg)
  {
    JobExecutionErrorRegistry.addError (new JobExecutionError (PDTFactory.getCurrentLocalDateTime (),
                                                               aJobKey,
                                                               "com.example.MyJob",
                                                               "fire-1",
                                                               new IllegalStateException (sMsg)));
  }

  @Test
  public void testEmpty ()
  {
    assertEquals (0, JobExecutionErrorRegistry.getTotalErrorCount ());
    assertTrue (JobExecutionErrorRegistry.getAllErrorsOfJob (JOB_KEY).isEmpty ());
    assertNull (JobExecutionErrorRegistry.getLastErrorOfJob (JOB_KEY));
    assertNull (JobExecutionErrorRegistry.getLastErrorOfJob (null));
    assertTrue (JobExecutionErrorRegistry.getAllErrorsOfJob (null).isEmpty ());
  }

  @Test
  public void testAddAndRead ()
  {
    _addError (JOB_KEY, "first");
    _addError (JOB_KEY, "second");

    assertEquals (2, JobExecutionErrorRegistry.getTotalErrorCount ());
    assertEquals (2, JobExecutionErrorRegistry.getAllErrorsOfJob (JOB_KEY).size ());

    // Oldest first - so the last one is the most recent one
    final JobExecutionError aLast = JobExecutionErrorRegistry.getLastErrorOfJob (JOB_KEY);
    assertNotNull (aLast);
    assertEquals ("second", aLast.getThrowableMessage ());
    assertEquals (IllegalStateException.class.getName (), aLast.getThrowableClassName ());
    assertEquals (JOB_KEY, aLast.getJobKey ());
    assertEquals ("com.example.MyJob", aLast.getJobClassName ());
    assertEquals ("fire-1", aLast.getFireInstanceID ());
    // The stack trace is extracted eagerly, the Throwable itself is not retained
    assertTrue (aLast.getStackTrace ().contains ("IllegalStateException"));
  }

  @Test
  public void testBounded ()
  {
    JobExecutionErrorRegistry.setMaxErrorsPerJob (2);
    _addError (JOB_KEY, "1");
    _addError (JOB_KEY, "2");
    _addError (JOB_KEY, "3");

    assertEquals (2, JobExecutionErrorRegistry.getAllErrorsOfJob (JOB_KEY).size ());
    // The oldest one was dropped
    assertEquals ("2", JobExecutionErrorRegistry.getAllErrorsOfJob (JOB_KEY).getFirstOrNull ().getThrowableMessage ());
    assertEquals ("3", JobExecutionErrorRegistry.getLastErrorOfJob (JOB_KEY).getThrowableMessage ());
  }

  @Test
  public void testLoweringTheMaximumDropsExistingEntries ()
  {
    _addError (JOB_KEY, "1");
    _addError (JOB_KEY, "2");
    _addError (JOB_KEY, "3");
    assertEquals (3, JobExecutionErrorRegistry.getAllErrorsOfJob (JOB_KEY).size ());

    JobExecutionErrorRegistry.setMaxErrorsPerJob (1);
    assertEquals (1, JobExecutionErrorRegistry.getAllErrorsOfJob (JOB_KEY).size ());
    assertEquals ("3", JobExecutionErrorRegistry.getLastErrorOfJob (JOB_KEY).getThrowableMessage ());
  }

  @Test
  public void testDisabled ()
  {
    JobExecutionErrorRegistry.setMaxErrorsPerJob (0);
    _addError (JOB_KEY, "1");
    assertEquals (0, JobExecutionErrorRegistry.getTotalErrorCount ());
  }

  @Test
  public void testMultipleJobs ()
  {
    _addError (JOB_KEY, "a");
    _addError (JOB_KEY2, "b");
    assertEquals (2, JobExecutionErrorRegistry.getTotalErrorCount ());
    assertEquals (2, JobExecutionErrorRegistry.getAllErrors ().size ());

    assertTrue (JobExecutionErrorRegistry.removeAllErrorsOfJob (JOB_KEY).isChanged ());
    assertEquals (1, JobExecutionErrorRegistry.getTotalErrorCount ());
    assertTrue (JobExecutionErrorRegistry.removeAllErrorsOfJob (JOB_KEY).isUnchanged ());
    assertTrue (JobExecutionErrorRegistry.removeAllErrorsOfJob (null).isUnchanged ());

    assertTrue (JobExecutionErrorRegistry.removeAllErrors ().isChanged ());
    assertEquals (0, JobExecutionErrorRegistry.getTotalErrorCount ());
    assertTrue (JobExecutionErrorRegistry.removeAllErrors ().isUnchanged ());
  }
}
