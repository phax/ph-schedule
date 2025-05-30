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
package com.helger.quartz.simpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.SchedulerConfigException;
import com.helger.quartz.spi.IThreadPool;

/**
 * <p>
 * This is class is a simple implementation of a zero size thread pool, based on
 * the <code>{@link com.helger.quartz.spi.IThreadPool}</code> interface.
 * </p>
 * <p>
 * The pool has zero <code>Thread</code>s and does not grow or shrink based on
 * demand. Which means it is obviously not useful for most scenarios. When it
 * may be useful is to prevent creating any worker threads at all - which may be
 * desirable for the sole purpose of preserving system resources in the case
 * where the scheduler instance only exists in order to schedule jobs, but which
 * will never execute jobs (e.g. will never have start() called on it).
 * </p>
 *
 * @author Wayne Fay
 */
public class ZeroSizeThreadPool implements IThreadPool
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ZeroSizeThreadPool.class);

  public ZeroSizeThreadPool ()
  {}

  public int getPoolSize ()
  {
    return 0;
  }

  public void initialize () throws SchedulerConfigException
  {}

  public void shutdown ()
  {
    shutdown (true);
  }

  public void shutdown (final boolean waitForJobsToComplete)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("shutdown complete");
  }

  public boolean runInThread (final Runnable runnable)
  {
    throw new UnsupportedOperationException ("This ThreadPool should not be used on Scheduler instances that are start()ed.");
  }

  public int blockForAvailableThreads ()
  {
    throw new UnsupportedOperationException ("This ThreadPool should not be used on Scheduler instances that are start()ed.");
  }

  public void setInstanceId (final String schedInstId)
  {}

  public void setInstanceName (final String schedName)
  {}

}
