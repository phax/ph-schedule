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
package org.quartz.listeners;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helpful abstract base class for implementors of
 * <code>{@link org.quartz.SchedulerListener}</code>.
 * <p>
 * The methods in this class are empty so you only need to override the subset
 * for the <code>{@link org.quartz.SchedulerListener}</code> events you care
 * about.
 * </p>
 *
 * @see org.quartz.SchedulerListener
 */
public abstract class SchedulerListenerSupport implements SchedulerListener
{
  private final Logger log = LoggerFactory.getLogger (getClass ());

  /**
   * Get the <code>{@link org.slf4j.Logger}</code> for this class's category.
   * This should be used by subclasses for logging.
   */
  protected Logger getLog ()
  {
    return log;
  }

  public void jobAdded (final JobDetail jobDetail)
  {}

  public void jobDeleted (final JobKey jobKey)
  {}

  public void jobPaused (final JobKey jobKey)
  {}

  public void jobResumed (final JobKey jobKey)
  {}

  public void jobScheduled (final Trigger trigger)
  {}

  public void jobsPaused (final String jobGroup)
  {}

  public void jobsResumed (final String jobGroup)
  {}

  public void jobUnscheduled (final TriggerKey triggerKey)
  {}

  public void schedulerError (final String msg, final SchedulerException cause)
  {}

  public void schedulerInStandbyMode ()
  {}

  public void schedulerShutdown ()
  {}

  public void schedulerShuttingdown ()
  {}

  public void schedulerStarted ()
  {}

  public void schedulerStarting ()
  {}

  public void triggerFinalized (final Trigger trigger)
  {}

  public void triggerPaused (final TriggerKey triggerKey)
  {}

  public void triggerResumed (final TriggerKey triggerKey)
  {}

  public void triggersPaused (final String triggerGroup)
  {}

  public void triggersResumed (final String triggerGroup)
  {}

  public void schedulingDataCleared ()
  {}

}
