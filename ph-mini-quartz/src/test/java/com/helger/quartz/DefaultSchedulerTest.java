/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.helger.quartz.impl.JobDetail;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * DefaultSchedulerTest
 */
public class DefaultSchedulerTest
{
  @Test
  public void testAddJobNoTrigger () throws Exception
  {
    final IScheduler scheduler = StdSchedulerFactory.getDefaultScheduler ();
    final JobDetail jobDetail = new JobDetail ();
    jobDetail.setName ("testjob");

    try
    {
      scheduler.addJob (jobDetail, false);
    }
    catch (final SchedulerException e)
    {
      assertThat (e.getMessage (), containsString ("durable"));
    }

    jobDetail.setDurability (true);
    scheduler.addJob (jobDetail, false);
  }
}
