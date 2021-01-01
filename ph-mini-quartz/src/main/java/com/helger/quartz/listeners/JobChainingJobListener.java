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
package com.helger.quartz.listeners;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;

/**
 * Keeps a collection of mappings of which Job to trigger after the completion
 * of a given job. If this listener is notified of a job completing that has a
 * mapping, then it will then attempt to trigger the follow-up job. This
 * achieves "job chaining", or a "poor man's workflow".
 * <p>
 * Generally an instance of this listener would be registered as a global job
 * listener, rather than being registered directly to a given job.
 * </p>
 * <p>
 * If for some reason there is a failure creating the trigger for the follow-up
 * job (which would generally only be caused by a rare serious failure in the
 * system, or the non-existence of the follow-up job), an error messsage is
 * logged, but no other action is taken. If you need more rigorous handling of
 * the error, consider scheduling the triggering of the flow-up job within your
 * job itself.
 * </p>
 *
 * @author James House (jhouse AT revolition DOT net)
 */
public class JobChainingJobListener implements IJobListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger (JobChainingJobListener.class);

  private final String m_sName;
  private final ICommonsMap <JobKey, JobKey> m_aChainLinks = new CommonsHashMap <> ();

  /**
   * Construct an instance with the given name.
   *
   * @param name
   *        the name of this instance
   */
  public JobChainingJobListener (@Nonnull final String name)
  {
    ValueEnforcer.notNull (name, "Name");
    m_sName = name;
  }

  @Nonnull
  public String getName ()
  {
    return m_sName;
  }

  /**
   * Add a chain mapping - when the Job identified by the first key completes
   * the job identified by the second key will be triggered.
   *
   * @param firstJob
   *        a JobKey with the name and group of the first job
   * @param secondJob
   *        a JobKey with the name and group of the follow-up job
   */
  public void addJobChainLink (final JobKey firstJob, final JobKey secondJob)
  {
    ValueEnforcer.notNull (firstJob, "FirstJob");
    ValueEnforcer.notNull (firstJob.getName (), "FirstJob.Name");
    ValueEnforcer.notNull (secondJob, "SecondJob");
    ValueEnforcer.notNull (secondJob.getName (), "SecondJob.Name");

    m_aChainLinks.put (firstJob, secondJob);
  }

  @Override
  public void jobWasExecuted (@Nonnull final IJobExecutionContext context, final JobExecutionException jobException)
  {
    final JobKey sj = m_aChainLinks.get (context.getJobDetail ().getKey ());
    if (sj == null)
      return;

    LOGGER.info ("Job '" + context.getJobDetail ().getKey () + "' will now chain to Job '" + sj + "'");
    try
    {
      context.getScheduler ().triggerJob (sj);
    }
    catch (final SchedulerException se)
    {
      LOGGER.error ("Error encountered during chaining to Job '" + sj + "'", se);
    }
  }
}
