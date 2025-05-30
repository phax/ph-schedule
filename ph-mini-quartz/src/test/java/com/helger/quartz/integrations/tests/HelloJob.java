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

/*
 * Copyright 2005 - 2009 Terracotta, Inc.
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

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobExecutionException;

/**
 * <p>
 * This is just a simple job that says "Hello" to the world.
 * </p>
 *
 * @author Bill Kratzer
 */
public class HelloJob implements IJob
{

  private static Logger _log = LoggerFactory.getLogger (HelloJob.class);

  /**
   * <p>
   * Empty constructor for job initilization
   * </p>
   * <p>
   * Quartz requires a public empty constructor so that the scheduler can
   * instantiate the class whenever it needs.
   * </p>
   */
  public HelloJob ()
  {}

  /**
   * <p>
   * Called by the <code>{@link com.helger.quartz.IScheduler}</code> when a
   * <code>{@link com.helger.quartz.ITrigger}</code> fires that is associated
   * with the <code>Job</code>.
   * </p>
   * 
   * @throws JobExecutionException
   *         if there is an exception while executing the job.
   */
  public void execute (final IJobExecutionContext context) throws JobExecutionException
  {

    // Say Hello to the World and display the date/time
    _log.info ("Hello World! - " + new Date ());
  }

}
