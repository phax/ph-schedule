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
package com.helger.quartz.plugins.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.ISchedulerPlugin;

/**
 * This plugin catches the event of the JVM terminating (such as upon a CRTL-C)
 * and tells the scheuler to shutdown.
 *
 * @see com.helger.quartz.IScheduler#shutdown(boolean)
 * @author James House
 */
public class ShutdownHookPlugin implements ISchedulerPlugin
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ShutdownHookPlugin.class);

  private boolean m_bCleanShutdown = true;

  public ShutdownHookPlugin ()
  {}

  /**
   * Determine whether or not the plug-in is configured to cause a clean
   * shutdown of the scheduler.
   * <p>
   * The default value is <code>true</code>.
   * </p>
   *
   * @see com.helger.quartz.IScheduler#shutdown(boolean)
   */
  public boolean isCleanShutdown ()
  {
    return m_bCleanShutdown;
  }

  /**
   * Set whether or not the plug-in is configured to cause a clean shutdown of
   * the scheduler.
   * <p>
   * The default value is <code>true</code>.
   * </p>
   *
   * @see com.helger.quartz.IScheduler#shutdown(boolean)
   */
  public void setCleanShutdown (final boolean b)
  {
    m_bCleanShutdown = b;
  }

  /**
   * <p>
   * Called during creation of the <code>Scheduler</code> in order to give the
   * <code>SchedulerPlugin</code> a chance to initialize.
   * </p>
   *
   * @throws SchedulerException
   *         if there is an error initializing.
   */
  public void initialize (final String name,
                          final IScheduler scheduler,
                          final IClassLoadHelper classLoadHelper) throws SchedulerException
  {

    LOGGER.info ("Registering Quartz shutdown hook.");

    final Thread t = new Thread ("Quartz Shutdown-Hook " + scheduler.getSchedulerName ())
    {
      @Override
      public void run ()
      {
        LOGGER.info ("Shutting down Quartz...");
        try
        {
          scheduler.shutdown (isCleanShutdown ());
        }
        catch (final SchedulerException e)
        {
          LOGGER.info ("Error shutting down Quartz: " + e.getMessage (), e);
        }
      }
    };

    Runtime.getRuntime ().addShutdownHook (t);
  }

  public void start ()
  {
    // do nothing.
  }

  /**
   * <p>
   * Called in order to inform the <code>SchedulerPlugin</code> that it should
   * free up all of it's resources because the scheduler is shutting down.
   * </p>
   */
  public void shutdown ()
  {
    // nothing to do in this case (since the scheduler is already shutting
    // down)
  }

}

// EOF
