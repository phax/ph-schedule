/*
 * Copyright (C) 2014-2025 Philip Helger (www.helger.com)
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
package com.helger.schedule.quartz;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SchedulerMetaData;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * Misc utility methods around Quartz schedulers
 *
 * @author Philip Helger
 */
@Immutable
public final class QuartzSchedulerHelper
{
  public static final boolean DEFAULT_START_AUTOMATICALLY = true;
  private static final ISchedulerFactory SCHEDULER_FACTORY = new StdSchedulerFactory ();

  private QuartzSchedulerHelper ()
  {}

  /**
   * @return The global scheduler factory that is used. Never <code>null</code>.
   * @since 1.8.2
   */
  @NonNull
  public static ISchedulerFactory getSchedulerFactory ()
  {
    return SCHEDULER_FACTORY;
  }

  /**
   * @return The single {@link IScheduler} instance that is ensured to be
   *         started. Never <code>null</code>.
   * @see #getScheduler(boolean)
   */
  @NonNull
  public static IScheduler getScheduler ()
  {
    return getScheduler (DEFAULT_START_AUTOMATICALLY);
  }

  /**
   * Get the underlying Quartz scheduler
   *
   * @param bStartAutomatically
   *        If <code>true</code> the returned scheduler is automatically
   *        started. If <code>false</code> the state is not changed.
   * @return The underlying Quartz scheduler. Never <code>null</code>.
   */
  @NonNull
  public static IScheduler getScheduler (final boolean bStartAutomatically)
  {
    try
    {
      // Don't try to use a name - results in NPE
      final IScheduler aScheduler = SCHEDULER_FACTORY.getScheduler ();
      if (bStartAutomatically && !aScheduler.isStarted ())
        aScheduler.start ();
      return aScheduler;
    }
    catch (final SchedulerException ex)
    {
      throw new IllegalStateException ("Failed to create" + (bStartAutomatically ? " and start" : "") + " scheduler!",
                                       ex);
    }
  }

  /**
   * Get the metadata of the scheduler. The state of the scheduler is not
   * changed within this method.
   *
   * @return The metadata of the underlying scheduler.
   */
  @NonNull
  public static SchedulerMetaData getSchedulerMetaData ()
  {
    try
    {
      // Get the scheduler without starting it
      return SCHEDULER_FACTORY.getScheduler ().getMetaData ();
    }
    catch (final SchedulerException ex)
    {
      throw new IllegalStateException ("Failed to get scheduler metadata", ex);
    }
  }

  /**
   * @return The state of the scheduler. Never <code>null</code>.
   */
  @NonNull
  public static ESchedulerState getSchedulerState ()
  {
    try
    {
      // Get the scheduler without starting it
      final IScheduler aScheduler = SCHEDULER_FACTORY.getScheduler ();
      if (aScheduler.isStarted ())
        return ESchedulerState.STARTED;
      if (aScheduler.isInStandbyMode ())
        return ESchedulerState.STANDBY;
      if (aScheduler.isShutdown ())
        return ESchedulerState.SHUTDOWN;
      throw new IllegalStateException ("Unknown scheduler state: " + aScheduler.toString ());
    }
    catch (final SchedulerException ex)
    {
      throw new IllegalStateException ("Error retrieving scheduler state!", ex);
    }
  }
}
