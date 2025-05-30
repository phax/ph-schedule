/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2025 Philip Helger (www.helger.com)
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.ITrigger.EMisfireInstruction;
import com.helger.quartz.ITrigger.ETriggerState;
import com.helger.quartz.impl.JobDetail;
import com.helger.quartz.impl.matchers.GroupMatcher;
import com.helger.quartz.impl.triggers.SimpleTrigger;
import com.helger.quartz.simpl.CascadingClassLoadHelper;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.IJobStore;
import com.helger.quartz.spi.IOperableTrigger;
import com.helger.quartz.spi.ISchedulerSignaler;

/**
 * Unit test for JobStores. These tests were submitted by Johannes Zillmann as
 * part of issue QUARTZ-306.
 */
public abstract class AbstractJobStoreTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractJobStoreTest.class);

  private IJobStore m_aJobStore;
  private JobDetail m_aJobDetail;
  private SampleSignaler m_aSignaler;

  @Before
  public void setUp () throws Exception
  {
    m_aSignaler = new SampleSignaler ();
    final IClassLoadHelper loadHelper = new CascadingClassLoadHelper ();
    loadHelper.initialize ();
    m_aJobStore = createJobStore ("AbstractJobStoreTest");
    m_aJobStore.initialize (loadHelper, m_aSignaler);
    m_aJobStore.schedulerStarted ();

    m_aJobDetail = JobDetail.create ("job1", "jobGroup1", MyJob.class);
    m_aJobDetail.setDurability (true);
    m_aJobStore.storeJob (m_aJobDetail, false);
  }

  @After
  public void tearDown ()
  {
    destroyJobStore ("AbstractJobStoreTest");
  }

  protected abstract IJobStore createJobStore (String name);

  protected abstract void destroyJobStore (String name);

  @Test
  public void testAcquireNextTrigger () throws Exception
  {

    final Date baseFireTimeDate = DateBuilder.evenMinuteDateAfterNow ();
    final long baseFireTime = baseFireTimeDate.getTime ();

    final IOperableTrigger trigger1 = SimpleTrigger.create ("trigger1",
                                                            "triggerGroup1",
                                                            m_aJobDetail.getName (),
                                                            m_aJobDetail.getGroup (),
                                                            new Date (baseFireTime + 200000),
                                                            new Date (baseFireTime + 200000),
                                                            2,
                                                            2000);
    final IOperableTrigger trigger2 = SimpleTrigger.create ("trigger2",
                                                            "triggerGroup1",
                                                            m_aJobDetail.getName (),
                                                            m_aJobDetail.getGroup (),
                                                            new Date (baseFireTime + 50000),
                                                            new Date (baseFireTime + 200000),
                                                            2,
                                                            2000);
    final IOperableTrigger trigger3 = SimpleTrigger.create ("trigger1",
                                                            "triggerGroup2",
                                                            m_aJobDetail.getName (),
                                                            m_aJobDetail.getGroup (),
                                                            new Date (baseFireTime + 100000),
                                                            new Date (baseFireTime + 200000),
                                                            2,
                                                            2000);

    trigger1.computeFirstFireTime (null);
    trigger2.computeFirstFireTime (null);
    trigger3.computeFirstFireTime (null);
    m_aJobStore.storeTrigger (trigger1, false);
    m_aJobStore.storeTrigger (trigger2, false);
    m_aJobStore.storeTrigger (trigger3, false);

    final long firstFireTime = new Date (trigger1.getNextFireTime ().getTime ()).getTime ();

    assertTrue (m_aJobStore.acquireNextTriggers (10, 1, 0L).isEmpty ());
    assertEquals (trigger2.getKey (), m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 1, 0L).get (0).getKey ());
    assertEquals (trigger3.getKey (), m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 1, 0L).get (0).getKey ());
    assertEquals (trigger1.getKey (), m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 1, 0L).get (0).getKey ());
    assertTrue (m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 1, 0L).isEmpty ());

    // release trigger3
    m_aJobStore.releaseAcquiredTrigger (trigger3);
    assertEquals (trigger3,
                  m_aJobStore.acquireNextTriggers (new Date (trigger1.getNextFireTime ().getTime ()).getTime () + 10000,
                                                   1,
                                                   1L)
                             .get (0));
  }

  @Test
  public void testAcquireNextTriggerBatch () throws Exception
  {

    final long baseFireTime = System.currentTimeMillis () - 1000;

    final IOperableTrigger early = SimpleTrigger.create ("early",
                                                         "triggerGroup1",
                                                         m_aJobDetail.getName (),
                                                         m_aJobDetail.getGroup (),
                                                         new Date (baseFireTime),
                                                         new Date (baseFireTime + 5),
                                                         2,
                                                         2000);
    final IOperableTrigger trigger1 = SimpleTrigger.create ("trigger1",
                                                            "triggerGroup1",
                                                            m_aJobDetail.getName (),
                                                            m_aJobDetail.getGroup (),
                                                            new Date (baseFireTime + 200000),
                                                            new Date (baseFireTime + 200005),
                                                            2,
                                                            2000);
    final IOperableTrigger trigger2 = SimpleTrigger.create ("trigger2",
                                                            "triggerGroup1",
                                                            m_aJobDetail.getName (),
                                                            m_aJobDetail.getGroup (),
                                                            new Date (baseFireTime + 210000),
                                                            new Date (baseFireTime + 210005),
                                                            2,
                                                            2000);
    final IOperableTrigger trigger3 = SimpleTrigger.create ("trigger3",
                                                            "triggerGroup1",
                                                            m_aJobDetail.getName (),
                                                            m_aJobDetail.getGroup (),
                                                            new Date (baseFireTime + 220000),
                                                            new Date (baseFireTime + 220005),
                                                            2,
                                                            2000);
    final IOperableTrigger trigger4 = SimpleTrigger.create ("trigger4",
                                                            "triggerGroup1",
                                                            m_aJobDetail.getName (),
                                                            m_aJobDetail.getGroup (),
                                                            new Date (baseFireTime + 230000),
                                                            new Date (baseFireTime + 230005),
                                                            2,
                                                            2000);

    final IOperableTrigger trigger10 = SimpleTrigger.create ("trigger10",
                                                             "triggerGroup2",
                                                             m_aJobDetail.getName (),
                                                             m_aJobDetail.getGroup (),
                                                             new Date (baseFireTime + 500000),
                                                             new Date (baseFireTime + 700000),
                                                             2,
                                                             2000);

    early.computeFirstFireTime (null);
    early.setMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
    trigger1.computeFirstFireTime (null);
    trigger2.computeFirstFireTime (null);
    trigger3.computeFirstFireTime (null);
    trigger4.computeFirstFireTime (null);
    trigger10.computeFirstFireTime (null);
    m_aJobStore.storeTrigger (early, false);
    m_aJobStore.storeTrigger (trigger1, false);
    m_aJobStore.storeTrigger (trigger2, false);
    m_aJobStore.storeTrigger (trigger3, false);
    m_aJobStore.storeTrigger (trigger4, false);
    m_aJobStore.storeTrigger (trigger10, false);

    final long firstFireTime = new Date (trigger1.getNextFireTime ().getTime ()).getTime ();

    List <IOperableTrigger> acquiredTriggers = m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 4, 1000L);
    assertEquals (1, acquiredTriggers.size ());
    assertEquals (early.getKey (), acquiredTriggers.get (0).getKey ());
    m_aJobStore.releaseAcquiredTrigger (early);

    acquiredTriggers = m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 4, 205000);
    assertEquals (2, acquiredTriggers.size ());
    assertEquals (early.getKey (), acquiredTriggers.get (0).getKey ());
    assertEquals (trigger1.getKey (), acquiredTriggers.get (1).getKey ());
    m_aJobStore.releaseAcquiredTrigger (early);
    m_aJobStore.releaseAcquiredTrigger (trigger1);

    m_aJobStore.removeTrigger (early.getKey ());

    acquiredTriggers = m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 5, 100000L);
    assertEquals (4, acquiredTriggers.size ());
    assertEquals (trigger1.getKey (), acquiredTriggers.get (0).getKey ());
    assertEquals (trigger2.getKey (), acquiredTriggers.get (1).getKey ());
    assertEquals (trigger3.getKey (), acquiredTriggers.get (2).getKey ());
    assertEquals (trigger4.getKey (), acquiredTriggers.get (3).getKey ());
    m_aJobStore.releaseAcquiredTrigger (trigger1);
    m_aJobStore.releaseAcquiredTrigger (trigger2);
    m_aJobStore.releaseAcquiredTrigger (trigger3);
    m_aJobStore.releaseAcquiredTrigger (trigger4);

    acquiredTriggers = m_aJobStore.acquireNextTriggers (firstFireTime + 10000, 6, 100000L);
    assertEquals (4, acquiredTriggers.size ());
    assertEquals (trigger1.getKey (), acquiredTriggers.get (0).getKey ());
    assertEquals (trigger2.getKey (), acquiredTriggers.get (1).getKey ());
    assertEquals (trigger3.getKey (), acquiredTriggers.get (2).getKey ());
    assertEquals (trigger4.getKey (), acquiredTriggers.get (3).getKey ());
    m_aJobStore.releaseAcquiredTrigger (trigger1);
    m_aJobStore.releaseAcquiredTrigger (trigger2);
    m_aJobStore.releaseAcquiredTrigger (trigger3);
    m_aJobStore.releaseAcquiredTrigger (trigger4);

    acquiredTriggers = m_aJobStore.acquireNextTriggers (firstFireTime + 1, 5, 0L);
    assertEquals (1, acquiredTriggers.size ());
    assertEquals (trigger1.getKey (), acquiredTriggers.get (0).getKey ());
    m_aJobStore.releaseAcquiredTrigger (trigger1);

    acquiredTriggers = m_aJobStore.acquireNextTriggers (firstFireTime + 250, 5, 19999L);
    assertEquals (2, acquiredTriggers.size ());
    assertEquals (trigger1.getKey (), acquiredTriggers.get (0).getKey ());
    assertEquals (trigger2.getKey (), acquiredTriggers.get (1).getKey ());
    m_aJobStore.releaseAcquiredTrigger (trigger1);
    m_aJobStore.releaseAcquiredTrigger (trigger2);
    m_aJobStore.releaseAcquiredTrigger (trigger3);

    acquiredTriggers = m_aJobStore.acquireNextTriggers (firstFireTime + 150, 5, 5000L);
    assertEquals (1, acquiredTriggers.size ());
    assertEquals (trigger1.getKey (), acquiredTriggers.get (0).getKey ());
    m_aJobStore.releaseAcquiredTrigger (trigger1);
  }

  @Test
  public void testTriggerStates () throws Exception
  {
    IOperableTrigger trigger = SimpleTrigger.create ("trigger1",
                                                     "triggerGroup1",
                                                     m_aJobDetail.getName (),
                                                     m_aJobDetail.getGroup (),
                                                     new Date (System.currentTimeMillis () + 100000),
                                                     new Date (System.currentTimeMillis () + 200000),
                                                     2,
                                                     2000);
    trigger.computeFirstFireTime (null);
    assertEquals (ETriggerState.NONE, m_aJobStore.getTriggerState (trigger.getKey ()));
    m_aJobStore.storeTrigger (trigger, false);
    assertEquals (ETriggerState.NORMAL, m_aJobStore.getTriggerState (trigger.getKey ()));

    m_aJobStore.pauseTrigger (trigger.getKey ());
    assertEquals (ETriggerState.PAUSED, m_aJobStore.getTriggerState (trigger.getKey ()));

    m_aJobStore.resumeTrigger (trigger.getKey ());
    assertEquals (ETriggerState.NORMAL, m_aJobStore.getTriggerState (trigger.getKey ()));

    trigger = m_aJobStore.acquireNextTriggers (new Date (trigger.getNextFireTime ().getTime ()).getTime () + 10000,
                                               1,
                                               1L)
                         .get (0);
    assertNotNull (trigger);
    m_aJobStore.releaseAcquiredTrigger (trigger);
    trigger = m_aJobStore.acquireNextTriggers (new Date (trigger.getNextFireTime ().getTime ()).getTime () + 10000,
                                               1,
                                               1L)
                         .get (0);
    assertNotNull (trigger);
    assertTrue (m_aJobStore.acquireNextTriggers (new Date (trigger.getNextFireTime ().getTime ()).getTime () + 10000,
                                                 1,
                                                 1L)
                           .isEmpty ());
  }

  // See: http://jira.opensymphony.com/browse/QUARTZ-606
  @Test
  public void testStoreTriggerReplacesTrigger () throws Exception
  {

    final String jobName = "StoreTriggerReplacesTrigger";
    final String jobGroup = "StoreTriggerReplacesTriggerGroup";
    final JobDetail detail = JobDetail.create (jobName, jobGroup, MyJob.class);
    m_aJobStore.storeJob (detail, false);

    final String trName = "StoreTriggerReplacesTrigger";
    final String trGroup = "StoreTriggerReplacesTriggerGroup";
    final SimpleTrigger tr = new SimpleTrigger ();
    tr.setName (trName);
    tr.setGroup (trGroup);
    tr.setStartTime (new Date ());
    tr.setJobKey (new JobKey (jobName, jobGroup));
    tr.setCalendarName (null);

    m_aJobStore.storeTrigger (tr, false);
    assertEquals (tr, m_aJobStore.retrieveTrigger (tr.getKey ()));

    try
    {
      m_aJobStore.storeTrigger (tr, false);
      fail ("an attempt to store duplicate trigger succeeded");
    }
    catch (final ObjectAlreadyExistsException oaee)
    {
      // expected
    }

    tr.setCalendarName ("QQ");
    m_aJobStore.storeTrigger (tr, true); // fails here
    assertEquals (tr, m_aJobStore.retrieveTrigger (tr.getKey ()));
    assertEquals ("StoreJob doesn't replace triggers",
                  "QQ",
                  m_aJobStore.retrieveTrigger (tr.getKey ()).getCalendarName ());
  }

  @Test
  public void testPauseJobGroupPausesNewJob () throws Exception
  {
    final String jobName1 = "PauseJobGroupPausesNewJob";
    final String jobName2 = "PauseJobGroupPausesNewJob2";
    final String jobGroup = "PauseJobGroupPausesNewJobGroup";

    JobDetail detail = JobDetail.create (jobName1, jobGroup, MyJob.class);
    detail.setDurability (true);
    m_aJobStore.storeJob (detail, false);
    m_aJobStore.pauseJobs (GroupMatcher.jobGroupEquals (jobGroup));

    detail = JobDetail.create (jobName2, jobGroup, MyJob.class);
    detail.setDurability (true);
    m_aJobStore.storeJob (detail, false);

    final String trName = "PauseJobGroupPausesNewJobTrigger";
    final String trGroup = "PauseJobGroupPausesNewJobTriggerGroup";
    final SimpleTrigger tr = new SimpleTrigger ();
    tr.setName (trName);
    tr.setGroup (trGroup);
    tr.setStartTime (new Date ());
    tr.setJobKey (new JobKey (jobName2, jobGroup));
    m_aJobStore.storeTrigger (tr, false);
    assertEquals (ETriggerState.PAUSED, m_aJobStore.getTriggerState (tr.getKey ()));
  }

  @Test
  public void testStoreAndRetrieveJobs () throws Exception
  {
    final ISchedulerSignaler schedSignaler = new SampleSignaler ();
    final IClassLoadHelper loadHelper = new CascadingClassLoadHelper ();
    loadHelper.initialize ();

    final IJobStore store = createJobStore ("testStoreAndRetrieveJobs");
    store.initialize (loadHelper, schedSignaler);

    // Store jobs.
    for (int i = 0; i < 10; i++)
    {
      final String group = i < 5 ? "a" : "b";
      final IJobDetail job = JobBuilder.newJob (MyJob.class).withIdentity ("job" + i, group).build ();
      store.storeJob (job, false);
    }
    // Retrieve jobs.
    for (int i = 0; i < 10; i++)
    {
      final String group = i < 5 ? "a" : "b";
      final JobKey jobKey = JobKey.jobKey ("job" + i, group);
      final IJobDetail storedJob = store.retrieveJob (jobKey);
      assertEquals (jobKey, storedJob.getKey ());
    }
    // Retrieve by group
    assertEquals ("Wrong number of jobs in group 'a'", 5, store.getJobKeys (GroupMatcher.jobGroupEquals ("a")).size ());
    assertEquals ("Wrong number of jobs in group 'b'", 5, store.getJobKeys (GroupMatcher.jobGroupEquals ("b")).size ());
  }

  @Test
  public void testStoreAndRetriveTriggers () throws Exception
  {
    final ISchedulerSignaler schedSignaler = new SampleSignaler ();
    final IClassLoadHelper loadHelper = new CascadingClassLoadHelper ();
    loadHelper.initialize ();

    final IJobStore store = createJobStore ("testStoreAndRetriveTriggers");
    store.initialize (loadHelper, schedSignaler);

    // Store jobs and triggers.
    for (int i = 0; i < 10; i++)
    {
      final String group = i < 5 ? "a" : "b";
      final IJobDetail job = JobBuilder.newJob (MyJob.class).withIdentity ("job" + i, group).build ();
      store.storeJob (job, true);
      final SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule ();
      final ITrigger trigger = TriggerBuilder.newTrigger ()
                                             .withIdentity ("job" + i, group)
                                             .withSchedule (schedule)
                                             .forJob (job)
                                             .build ();
      store.storeTrigger ((IOperableTrigger) trigger, true);
    }
    // Retrieve job and trigger.
    for (int i = 0; i < 10; i++)
    {
      final String group = i < 5 ? "a" : "b";
      final JobKey jobKey = JobKey.jobKey ("job" + i, group);
      final IJobDetail storedJob = store.retrieveJob (jobKey);
      assertEquals (jobKey, storedJob.getKey ());

      final TriggerKey triggerKey = TriggerKey.triggerKey ("job" + i, group);
      final ITrigger storedTrigger = store.retrieveTrigger (triggerKey);
      assertEquals (triggerKey, storedTrigger.getKey ());
    }
    // Retrieve by group
    assertEquals ("Wrong number of triggers in group 'a'",
                  5,
                  store.getTriggerKeys (GroupMatcher.triggerGroupEquals ("a")).size ());
    assertEquals ("Wrong number of triggers in group 'b'",
                  5,
                  store.getTriggerKeys (GroupMatcher.triggerGroupEquals ("b")).size ());
  }

  @Test
  public void testMatchers () throws Exception
  {
    final ISchedulerSignaler schedSignaler = new SampleSignaler ();
    final IClassLoadHelper loadHelper = new CascadingClassLoadHelper ();
    loadHelper.initialize ();

    final IJobStore store = createJobStore ("testMatchers");
    store.initialize (loadHelper, schedSignaler);

    IJobDetail job = JobBuilder.newJob (MyJob.class).withIdentity ("job1", "aaabbbccc").build ();
    store.storeJob (job, true);
    SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule ();
    ITrigger trigger = TriggerBuilder.newTrigger ()
                                     .withIdentity ("trig1", "aaabbbccc")
                                     .withSchedule (schedule)
                                     .forJob (job)
                                     .build ();
    store.storeTrigger ((IOperableTrigger) trigger, true);

    job = JobBuilder.newJob (MyJob.class).withIdentity ("job1", "xxxyyyzzz").build ();
    store.storeJob (job, true);
    schedule = SimpleScheduleBuilder.simpleSchedule ();
    trigger = TriggerBuilder.newTrigger ()
                            .withIdentity ("trig1", "xxxyyyzzz")
                            .withSchedule (schedule)
                            .forJob (job)
                            .build ();
    store.storeTrigger ((IOperableTrigger) trigger, true);

    job = JobBuilder.newJob (MyJob.class).withIdentity ("job2", "xxxyyyzzz").build ();
    store.storeJob (job, true);
    schedule = SimpleScheduleBuilder.simpleSchedule ();
    trigger = TriggerBuilder.newTrigger ()
                            .withIdentity ("trig2", "xxxyyyzzz")
                            .withSchedule (schedule)
                            .forJob (job)
                            .build ();
    store.storeTrigger ((IOperableTrigger) trigger, true);

    Set <JobKey> jkeys = store.getJobKeys (GroupMatcher.anyJobGroup ());
    assertEquals ("Wrong number of jobs found by anything matcher", 3, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupEquals ("xxxyyyzzz"));
    assertEquals ("Wrong number of jobs found by equals matcher", 2, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupEquals ("aaabbbccc"));
    assertEquals ("Wrong number of jobs found by equals matcher", 1, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupStartsWith ("aa"));
    assertEquals ("Wrong number of jobs found by starts with matcher", 1, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupStartsWith ("xx"));
    assertEquals ("Wrong number of jobs found by starts with matcher", 2, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupEndsWith ("cc"));
    assertEquals ("Wrong number of jobs found by ends with matcher", 1, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupEndsWith ("zzz"));
    assertEquals ("Wrong number of jobs found by ends with matcher", 2, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupContains ("bc"));
    assertEquals ("Wrong number of jobs found by contains with matcher", 1, jkeys.size ());

    jkeys = store.getJobKeys (GroupMatcher.jobGroupContains ("yz"));
    assertEquals ("Wrong number of jobs found by contains with matcher", 2, jkeys.size ());

    Set <TriggerKey> tkeys = store.getTriggerKeys (GroupMatcher.anyTriggerGroup ());
    assertEquals ("Wrong number of triggers found by anything matcher", 3, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupEquals ("xxxyyyzzz"));
    assertEquals ("Wrong number of triggers found by equals matcher", 2, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupEquals ("aaabbbccc"));
    assertEquals ("Wrong number of triggers found by equals matcher", 1, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupStartsWith ("aa"));
    assertEquals ("Wrong number of triggers found by starts with matcher", 1, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupStartsWith ("xx"));
    assertEquals ("Wrong number of triggers found by starts with matcher", 2, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupEndsWith ("cc"));
    assertEquals ("Wrong number of triggers found by ends with matcher", 1, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupEndsWith ("zzz"));
    assertEquals ("Wrong number of triggers found by ends with matcher", 2, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupContains ("bc"));
    assertEquals ("Wrong number of triggers found by contains with matcher", 1, tkeys.size ());

    tkeys = store.getTriggerKeys (GroupMatcher.triggerGroupContains ("yz"));
    assertEquals ("Wrong number of triggers found by contains with matcher", 2, tkeys.size ());

  }

  @Test
  public void testAcquireTriggers () throws Exception
  {
    final ISchedulerSignaler schedSignaler = new SampleSignaler ();
    final IClassLoadHelper loadHelper = new CascadingClassLoadHelper ();
    loadHelper.initialize ();

    final IJobStore store = createJobStore ("testAcquireTriggers");
    store.initialize (loadHelper, schedSignaler);

    // Setup: Store jobs and triggers.
    final long MIN = 60 * 1000L;
    // a min from now.
    final Date startTime0 = new Date (System.currentTimeMillis () + MIN);
    for (int i = 0; i < 10; i++)
    {
      // a min apart
      final Date startTime = new Date (startTime0.getTime () + i * MIN);
      final IJobDetail job = JobBuilder.newJob (MyJob.class).withIdentity ("job" + i).build ();
      final SimpleScheduleBuilder schedule = SimpleScheduleBuilder.repeatMinutelyForever (2);
      final IOperableTrigger trigger = TriggerBuilder.newTrigger ()
                                                     .withIdentity ("job" + i)
                                                     .withSchedule (schedule)
                                                     .forJob (job)
                                                     .startAt (startTime)
                                                     .build ();

      // Manually trigger the first fire time computation that scheduler would
      // do. Otherwise
      // the store.acquireNextTriggers() will not work properly.
      final Date fireTime = trigger.computeFirstFireTime (null);
      assertNotNull (fireTime);

      store.storeJobAndTrigger (job, trigger);
    }

    // Test acquire one trigger at a time
    for (int i = 0; i < 10; i++)
    {
      final long noLaterThan = (startTime0.getTime () + i * MIN);
      final int maxCount = 1;
      final long timeWindow = 0;
      final List <IOperableTrigger> triggers = store.acquireNextTriggers (noLaterThan, maxCount, timeWindow);
      assertEquals (1, triggers.size ());
      assertEquals ("job" + i, triggers.get (0).getKey ().getName ());

      // Let's remove the trigger now.
      store.removeJob (triggers.get (0).getJobKey ());
    }
  }

  @Test
  public void testAcquireTriggersInBatch () throws Exception
  {
    final ISchedulerSignaler schedSignaler = new SampleSignaler ();
    final IClassLoadHelper loadHelper = new CascadingClassLoadHelper ();
    loadHelper.initialize ();

    final IJobStore store = createJobStore ("testAcquireTriggersInBatch");
    store.initialize (loadHelper, schedSignaler);

    // Setup: Store jobs and triggers.
    final long MIN = 60 * 1000L;
    // a min from now.
    final Date startTime0 = new Date (System.currentTimeMillis () + MIN);
    for (int i = 0; i < 10; i++)
    {
      // a min apart
      final Date startTime = new Date (startTime0.getTime () + i * MIN);
      final IJobDetail job = JobBuilder.newJob (MyJob.class).withIdentity ("job" + i).build ();
      final SimpleScheduleBuilder schedule = SimpleScheduleBuilder.repeatMinutelyForever (2);
      final IOperableTrigger trigger = TriggerBuilder.newTrigger ()
                                                     .withIdentity ("job" + i)
                                                     .withSchedule (schedule)
                                                     .forJob (job)
                                                     .startAt (startTime)
                                                     .build ();

      // Manually trigger the first fire time computation that scheduler would
      // do. Otherwise
      // the store.acquireNextTriggers() will not work properly.
      final Date fireTime = trigger.computeFirstFireTime (null);
      assertNotNull (fireTime);

      store.storeJobAndTrigger (job, trigger);
    }

    // Test acquire batch of triggers at a time
    final long noLaterThan = startTime0.getTime () + 10 * MIN;
    final int maxCount = 7;
    // time window needs to be big to be able to pick up multiple triggers when
    // they are a minute apart
    final long timeWindow = 8 * MIN;
    final List <IOperableTrigger> triggers = store.acquireNextTriggers (noLaterThan, maxCount, timeWindow);
    assertEquals (7, triggers.size ());
    for (int i = 0; i < 7; i++)
    {
      assertEquals ("job" + i, triggers.get (i).getKey ().getName ());
    }
  }

  public static class SampleSignaler implements ISchedulerSignaler
  {
    volatile int fMisfireCount = 0;

    public void notifyTriggerListenersMisfired (final ITrigger trigger)
    {
      LOGGER.info ("Trigger misfired: " + trigger.getKey () + ", fire time: " + trigger.getNextFireTime ());
      fMisfireCount++;
    }

    public void signalSchedulingChange (final long candidateNewNextFireTime)
    {}

    public void notifySchedulerListenersFinalized (final ITrigger trigger)
    {}

    public void notifySchedulerListenersJobDeleted (final JobKey jobKey)
    {}

    public void notifySchedulerListenersError (final String string, final SchedulerException jpe)
    {}
  }

  /** An empty job for testing purpose. */
  public static class MyJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      //
    }
  }

}
