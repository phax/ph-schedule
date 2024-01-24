/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2024 Philip Helger (www.helger.com)
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
package com.helger.quartz.impl;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Test;

import com.helger.quartz.IScheduler;
import com.helger.quartz.simpl.RAMJobStore;
import com.helger.quartz.simpl.SimpleThreadPool;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.ISchedulerPlugin;
import com.helger.quartz.spi.IThreadPool;

public class DirectSchedulerFactoryTest
{
  @Test
  public void testPlugins () throws Exception
  {
    final StringBuffer result = new StringBuffer ();

    final ISchedulerPlugin testPlugin = new ISchedulerPlugin ()
    {
      public void initialize (final String name,
                              final com.helger.quartz.IScheduler scheduler,
                              final IClassLoadHelper classLoadHelper) throws com.helger.quartz.SchedulerException
      {
        result.append (name).append ("|").append (scheduler.getSchedulerName ());
      }

      public void start ()
      {
        result.append ("|start");
      }

      public void shutdown ()
      {
        result.append ("|shutdown");
      }
    };

    final IThreadPool threadPool = new SimpleThreadPool (1, 5);
    threadPool.initialize ();
    DirectSchedulerFactory.getInstance ()
                          .createScheduler ("MyScheduler",
                                            "Instance1",
                                            threadPool,
                                            new RAMJobStore (),
                                            Collections.singletonMap ("TestPlugin", testPlugin),
                                            -1);

    final IScheduler scheduler = DirectSchedulerFactory.getInstance ().getScheduler ("MyScheduler");
    scheduler.start ();
    scheduler.shutdown ();

    assertEquals ("TestPlugin|MyScheduler|start|shutdown", result.toString ());
  }
}
