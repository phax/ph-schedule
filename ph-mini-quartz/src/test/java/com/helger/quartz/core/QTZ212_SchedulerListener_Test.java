/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.listeners.BroadcastSchedulerListener;

/**
 * Test that verifies that schedulerStarting() is called before the
 * schedulerStarted()
 *
 * @author adahanne
 */
public class QTZ212_SchedulerListener_Test
{
  private static final Logger LOGGER = LoggerFactory.getLogger (QTZ212_SchedulerListener_Test.class);

  private static final String SCHEDULER_STARTED = "SCHEDULER_STARTED";
  private static final String SCHEDULER_STARTING = "SCHEDULER_STARTING";
  private static List <String> s_aMethodsCalledInSchedulerListener = new ArrayList <> ();

  @Test
  public void testStdSchedulerCallsStartingBeforeStartedTest () throws SchedulerException
  {
    final ISchedulerFactory sf = new StdSchedulerFactory ();
    final IScheduler sched = sf.getScheduler ();
    sched.getListenerManager ().addSchedulerListener (new TestSchedulerListener ());
    sched.start ();

    assertEquals (SCHEDULER_STARTING, s_aMethodsCalledInSchedulerListener.get (0));
    assertEquals (SCHEDULER_STARTED, s_aMethodsCalledInSchedulerListener.get (1));

    sched.shutdown ();
  }

  @Test
  public void testBroadcastSchedulerListenerCallsSchedulerStartingOnAllItsListeners () throws SchedulerException
  {
    s_aMethodsCalledInSchedulerListener = new ArrayList <> ();
    final ISchedulerFactory sf = new StdSchedulerFactory ();
    final IScheduler sched = sf.getScheduler ();
    final List <ISchedulerListener> listeners = new ArrayList <> ();
    listeners.add (new TestSchedulerListener ());

    sched.getListenerManager ().addSchedulerListener (new BroadcastSchedulerListener (listeners));
    sched.start ();

    assertEquals (SCHEDULER_STARTING, s_aMethodsCalledInSchedulerListener.get (0));
    assertEquals (SCHEDULER_STARTED, s_aMethodsCalledInSchedulerListener.get (1));

    sched.shutdown ();
  }

  public static class TestSchedulerListener implements ISchedulerListener
  {
    @Override
    public void schedulerStarted ()
    {
      s_aMethodsCalledInSchedulerListener.add (SCHEDULER_STARTED);
      LOGGER.info ("schedulerStarted was called");
    }

    @Override
    public void schedulerStarting ()
    {
      s_aMethodsCalledInSchedulerListener.add (SCHEDULER_STARTING);
      LOGGER.info ("schedulerStarting was called");
    }
  }
}
