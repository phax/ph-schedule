/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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

import java.util.List;

import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;

/**
 * @author cdennis
 */
public class TrackingJob implements IJob
{
  public static String SCHEDULED_TIMES_KEY = "TrackingJob.ScheduledTimes";

  @SuppressWarnings ("unchecked")
  @Override
  public void execute (final IJobExecutionContext context) throws JobExecutionException
  {
    try
    {
      final IScheduler scheduler = context.getScheduler ();
      final List <Long> scheduledFires = (List <Long>) scheduler.getContext ().get (SCHEDULED_TIMES_KEY);
      scheduledFires.add (context.getScheduledFireTime ().getTime ());
    }
    catch (final SchedulerException e)
    {
      throw new JobExecutionException (e);
    }
  }
}
