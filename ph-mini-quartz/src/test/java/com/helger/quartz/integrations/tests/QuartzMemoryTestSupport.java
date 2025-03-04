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
package com.helger.quartz.integrations.tests;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.lang.NonBlockingProperties;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.simpl.SimpleThreadPool;

/**
 * A base class to support in-memory scheduler integration testing. Each test
 * will have a fresh scheduler created and started, and it will auto shutdown
 * upon each test run.
 *
 * @author Zemian Deng
 */
public class QuartzMemoryTestSupport
{
  protected static final Logger LOG = LoggerFactory.getLogger (QuartzMemoryTestSupport.class);
  protected IScheduler scheduler;

  @Before
  public void initSchedulerBeforeTest () throws Exception
  {
    final NonBlockingProperties properties = createSchedulerProperties ();
    final ISchedulerFactory sf = new StdSchedulerFactory ().initialize (properties);
    scheduler = sf.getScheduler ();
    afterSchedulerInit ();
  }

  protected void afterSchedulerInit () throws Exception
  {
    LOG.info ("Scheduler starting.");
    scheduler.start ();
    LOG.info ("Scheduler started.");
  }

  protected NonBlockingProperties createSchedulerProperties ()
  {
    final NonBlockingProperties properties = new NonBlockingProperties ();
    properties.put (StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "TestScheduler");
    properties.put (StdSchedulerFactory.PROP_SCHED_INSTANCE_ID, "AUTO");
    properties.put (StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName ());
    properties.put ("org.quartz.threadPool.threadCount", "12");
    properties.put ("org.quartz.threadPool.threadPriority", "5");
    properties.put ("org.quartz.jobStore.misfireThreshold", "10000");
    return properties;
  }

  @After
  public void initSchedulerAfterTest () throws Exception
  {
    LOG.info ("Scheduler shutting down.");
    scheduler.shutdown (true);
    LOG.info ("Scheduler shutdown complete.");
  }
}
