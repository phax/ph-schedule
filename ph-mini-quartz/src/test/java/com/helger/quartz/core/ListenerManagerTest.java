/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2022 Philip Helger (www.helger.com)
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
package com.helger.quartz.core;

import static com.helger.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static com.helger.quartz.impl.matchers.GroupMatcher.triggerGroupEquals;
import static com.helger.quartz.impl.matchers.NameMatcher.jobNameContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.helger.quartz.IJobListener;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.ITriggerListener;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.NameMatcher;

/**
 * Test ListenerManagerImpl functionality
 */
public final class ListenerManagerTest
{
  public static class TestJobListener implements IJobListener
  {
    private final String m_sName;

    public TestJobListener (final String name)
    {
      m_sName = name;
    }

    public String getName ()
    {
      return m_sName;
    }
  }

  public static class TestTriggerListener implements ITriggerListener
  {
    private final String m_sName;

    public TestTriggerListener (final String name)
    {
      m_sName = name;
    }

    public String getName ()
    {
      return m_sName;
    }
  }

  public static class TestSchedulerListener implements ISchedulerListener
  {}

  @Test
  public void testManagementOfJobListeners () throws Exception
  {

    final IJobListener tl1 = new TestJobListener ("tl1");
    final IJobListener tl2 = new TestJobListener ("tl2");

    ListenerManager manager = new ListenerManager ();

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
    manager = new ListenerManager ();
    final IJobListener [] lstners = new IJobListener [numListenersToTestOrderOf];
    for (int i = 0; i < numListenersToTestOrderOf; i++)
    {
      // use random name, to help test that order isn't based on naming or
      // coincidental hashing
      lstners[i] = new TestJobListener (UUID.randomUUID ().toString ());
      manager.addJobListener (lstners[i]);
    }
    final List <IJobListener> mls = manager.getJobListeners ();
    int i = 0;
    for (final IJobListener lsnr : mls)
    {
      assertSame ("Unexpected order of listeners", lstners[i], lsnr);
      i++;
    }
  }

  @Test
  public void testManagementOfTriggerListeners () throws Exception
  {

    final ITriggerListener tl1 = new TestTriggerListener ("tl1");
    final ITriggerListener tl2 = new TestTriggerListener ("tl2");

    ListenerManager manager = new ListenerManager ();

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
    manager = new ListenerManager ();
    final ITriggerListener [] lstners = new ITriggerListener [numListenersToTestOrderOf];
    for (int i = 0; i < numListenersToTestOrderOf; i++)
    {
      // use random name, to help test that order isn't based on naming or
      // coincidental hashing
      lstners[i] = new TestTriggerListener (UUID.randomUUID ().toString ());
      manager.addTriggerListener (lstners[i]);
    }
    final List <ITriggerListener> mls = manager.getTriggerListeners ();
    int i = 0;
    for (final ITriggerListener lsnr : mls)
    {
      assertSame ("Unexpected order of listeners", lstners[i], lsnr);
      i++;
    }
  }

  @Test
  public void testManagementOfSchedulerListeners () throws Exception
  {

    final ISchedulerListener tl1 = new TestSchedulerListener ();
    final ISchedulerListener tl2 = new TestSchedulerListener ();

    ListenerManager manager = new ListenerManager ();

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
    manager = new ListenerManager ();
    final ISchedulerListener [] lstners = new ISchedulerListener [numListenersToTestOrderOf];
    for (int i = 0; i < numListenersToTestOrderOf; i++)
    {
      lstners[i] = new TestSchedulerListener ();
      manager.addSchedulerListener (lstners[i]);
    }
    final List <ISchedulerListener> mls = manager.getSchedulerListeners ();
    int i = 0;
    for (final ISchedulerListener lsnr : mls)
    {
      assertSame ("Unexpected order of listeners", lstners[i], lsnr);
      i++;
    }
  }
}
