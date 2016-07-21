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
package com.helger.quartz.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.JobExecutionContext;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.JobListener;

/**
 * A helpful abstract base class for implementors of
 * <code>{@link com.helger.quartz.JobListener}</code>.
 * <p>
 * The methods in this class are empty so you only need to override the subset
 * for the <code>{@link com.helger.quartz.JobListener}</code> events you care about.
 * </p>
 * <p>
 * You are required to implement
 * <code>{@link com.helger.quartz.JobListener#getName()}</code> to return the unique
 * name of your <code>JobListener</code>.
 * </p>
 *
 * @see com.helger.quartz.JobListener
 */
public abstract class JobListenerSupport implements JobListener
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

  public void jobToBeExecuted (final JobExecutionContext context)
  {}

  public void jobExecutionVetoed (final JobExecutionContext context)
  {}

  public void jobWasExecuted (final JobExecutionContext context, final JobExecutionException jobException)
  {}
}
