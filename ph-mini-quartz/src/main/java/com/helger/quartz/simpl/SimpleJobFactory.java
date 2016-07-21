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
 *
 */
package com.helger.quartz.simpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.Job;
import com.helger.quartz.JobDetail;
import com.helger.quartz.Scheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.spi.JobFactory;
import com.helger.quartz.spi.TriggerFiredBundle;

/**
 * The default JobFactory used by Quartz - simply calls
 * <code>newInstance()</code> on the job class.
 *
 * @see JobFactory
 * @see PropertySettingJobFactory
 * @author jhouse
 */
public class SimpleJobFactory implements JobFactory
{

  private final Logger log = LoggerFactory.getLogger (getClass ());

  protected Logger getLog ()
  {
    return log;
  }

  public Job newJob (final TriggerFiredBundle bundle, final Scheduler Scheduler) throws SchedulerException
  {

    final JobDetail jobDetail = bundle.getJobDetail ();
    final Class <? extends Job> jobClass = jobDetail.getJobClass ();
    try
    {
      if (log.isDebugEnabled ())
      {
        log.debug ("Producing instance of Job '" + jobDetail.getKey () + "', class=" + jobClass.getName ());
      }

      return jobClass.newInstance ();
    }
    catch (final Exception e)
    {
      final SchedulerException se = new SchedulerException ("Problem instantiating class '" +
                                                            jobDetail.getJobClass ().getName () +
                                                            "'",
                                                            e);
      throw se;
    }
  }

}
