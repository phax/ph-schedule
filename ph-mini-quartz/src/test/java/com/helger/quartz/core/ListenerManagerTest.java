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
package com.helger.quartz.core;

import static com.helger.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static com.helger.quartz.impl.matchers.GroupMatcher.triggerGroupEquals;
import static com.helger.quartz.impl.matchers.NameMatcher.jobNameContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.helger.quartz.JobListener;
import com.helger.quartz.SchedulerListener;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.TriggerListener;
import com.helger.quartz.core.ListenerManagerImpl;
import com.helger.quartz.impl.matchers.NameMatcher;
import com.helger.quartz.listeners.JobListenerSupport;
import com.helger.quartz.listeners.SchedulerListenerSupport;
import com.helger.quartz.listeners.TriggerListenerSupport;

/**
 * Test ListenerManagerImpl functionality
 */
public class ListenerManagerTest
{

  public static class TestJobListener extends JobListenerSupport
  {

    private final String name;

    public TestJobListener (final String name)
    {
      this.name = name;
    }

    public String getName ()
    {
      return name;
    }
  }

  public static class TestTriggerListener extends TriggerListenerSupport
  {

    private final String name;

    public TestTriggerListener (final String name)
    {
      this.name = name;
    }

    public String getName ()
    {
      return name;
    }
  }

  public static class TestSchedulerListener extends SchedulerListenerSupport
  {

  }

  @Test
  public void testManagementOfJobListeners () throws Exception
  {

    final JobListener tl1 = new TestJobListener ("tl1");
    final JobListener tl2 = new TestJobListener ("tl2");

    ListenerManagerImpl manager = new ListenerManagerImpl ();

    // test adding listener without matcher
    manager.addJobListener (tl1);
    assertEquals ("Unexpected size of listener list", 1, manager.getJobListeners ().size ());

    // test adding listener with matcher
    manager.addJobListener (tl2, jobGroupEquals ("foo"));
    assertEquals ("Unexpected size of listener list", 2, manager.getJobListeners ().size ());

    // test removing a listener
    manager.removeJobListener ("tl1");
    assertEquals ("Unexpected size of listener list", 1, manager.getJobListeners ().size ());

    // test adding a matcher
    manager.addJobListenerMatcher ("tl2", jobNameContains ("foo"));
    assertEquals ("Unexpected size of listener's matcher list", 2, manager.getJobListenerMatchers ("tl2").size ());

    // Test ordering of registration is preserved.
    final int numListenersToTestOrderOf = 15;
    manager = new ListenerManagerImpl ();
    final JobListener [] lstners = new JobListener [numListenersToTestOrderOf];
    for (int i = 0; i < numListenersToTestOrderOf; i++)
    {
      // use random name, to help test that order isn't based on naming or
      // coincidental hashing
      lstners[i] = new TestJobListener (UUID.randomUUID ().toString ());
      manager.addJobListener (lstners[i]);
    }
    final List <JobListener> mls = manager.getJobListeners ();
    int i = 0;
    for (final JobListener lsnr : mls)
    {
      assertSame ("Unexpected order of listeners", lstners[i], lsnr);
      i++;
    }
  }

  @Test
  public void testManagementOfTriggerListeners () throws Exception
  {

    final TriggerListener tl1 = new TestTriggerListener ("tl1");
    final TriggerListener tl2 = new TestTriggerListener ("tl2");

    ListenerManagerImpl manager = new ListenerManagerImpl ();

    // test adding listener without matcher
    manager.addTriggerListener (tl1);
    assertEquals ("Unexpected size of listener list", 1, manager.getTriggerListeners ().size ());

    // test adding listener with matcher
    manager.addTriggerListener (tl2, triggerGroupEquals ("foo"));
    assertEquals ("Unexpected size of listener list", 2, manager.getTriggerListeners ().size ());

    // test removing a listener
    manager.removeTriggerListener ("tl1");
    assertEquals ("Unexpected size of listener list", 1, manager.getTriggerListeners ().size ());

    // test adding a matcher
    manager.addTriggerListenerMatcher ("tl2", NameMatcher.<TriggerKey> nameContains ("foo"));
    assertEquals ("Unexpected size of listener's matcher list", 2, manager.getTriggerListenerMatchers ("tl2").size ());

    // Test ordering of registration is preserved.
    final int numListenersToTestOrderOf = 15;
    manager = new ListenerManagerImpl ();
    final TriggerListener [] lstners = new TriggerListener [numListenersToTestOrderOf];
    for (int i = 0; i < numListenersToTestOrderOf; i++)
    {
      // use random name, to help test that order isn't based on naming or
      // coincidental hashing
      lstners[i] = new TestTriggerListener (UUID.randomUUID ().toString ());
      manager.addTriggerListener (lstners[i]);
    }
    final List <TriggerListener> mls = manager.getTriggerListeners ();
    int i = 0;
    for (final TriggerListener lsnr : mls)
    {
      assertSame ("Unexpected order of listeners", lstners[i], lsnr);
      i++;
    }
  }

  @Test
  public void testManagementOfSchedulerListeners () throws Exception
  {

    final SchedulerListener tl1 = new TestSchedulerListener ();
    final SchedulerListener tl2 = new TestSchedulerListener ();

    ListenerManagerImpl manager = new ListenerManagerImpl ();

    // test adding listener without matcher
    manager.addSchedulerListener (tl1);
    assertEquals ("Unexpected size of listener list", 1, manager.getSchedulerListeners ().size ());

    // test adding listener with matcher
    manager.addSchedulerListener (tl2);
    assertEquals ("Unexpected size of listener list", 2, manager.getSchedulerListeners ().size ());

    // test removing a listener
    manager.removeSchedulerListener (tl1);
    assertEquals ("Unexpected size of listener list", 1, manager.getSchedulerListeners ().size ());

    // Test ordering of registration is preserved.
    final int numListenersToTestOrderOf = 15;
    manager = new ListenerManagerImpl ();
    final SchedulerListener [] lstners = new SchedulerListener [numListenersToTestOrderOf];
    for (int i = 0; i < numListenersToTestOrderOf; i++)
    {
      lstners[i] = new TestSchedulerListener ();
      manager.addSchedulerListener (lstners[i]);
    }
    final List <SchedulerListener> mls = manager.getSchedulerListeners ();
    int i = 0;
    for (final SchedulerListener lsnr : mls)
    {
      assertSame ("Unexpected order of listeners", lstners[i], lsnr);
      i++;
    }
  }
}
