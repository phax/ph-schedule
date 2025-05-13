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
package com.helger.quartz.impl;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.io.stream.NonBlockingBufferedReader;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.SchedulerException;

/**
 * <p>
 * Instantiates an instance of Quartz Scheduler as a stand-alone program, if the scheduler is
 * configured for RMI it will be made available.
 * </p>
 * <p>
 * The main() method of this class currently accepts 0 or 1 arguemtns, if there is an argument, and
 * its value is <code>"console"</code>, then the program will print a short message on the console
 * (std-out) and wait for the user to type "exit" - at which time the scheduler will be shutdown.
 * </p>
 * <p>
 * Future versions of this server should allow additional configuration for responding to scheduler
 * events by allowing the user to specify <code>{@link com.helger.quartz.IJobListener}</code>,
 * <code>{@link com.helger.quartz.ITriggerListener}</code> and
 * <code>{@link com.helger.quartz.ISchedulerListener}</code> classes.
 * </p>
 * <p>
 * Please read the Quartz FAQ entries about RMI before asking questions in the forums or mail-lists.
 * </p>
 *
 * @author James House
 */
public class QuartzServer implements ISchedulerListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger (QuartzServer.class);

  private IScheduler sched;

  QuartzServer ()
  {}

  public void serve (final ISchedulerFactory schedFact, final boolean bConsole) throws Exception
  {
    sched = schedFact.getScheduler ();

    sched.start ();

    ThreadHelper.sleep (3000L);

    LOGGER.info ("\n*** The scheduler successfully started.");

    if (bConsole)
    {
      LOGGER.info ("\n");
      LOGGER.info ("The scheduler will now run until you type \"exit\"");
      LOGGER.info ("   If it was configured to export itself via RMI,");
      LOGGER.info ("   then other process may now use it.");

      // Don't close System.in
      @SuppressWarnings ("resource")
      final NonBlockingBufferedReader rdr = new NonBlockingBufferedReader (new InputStreamReader (System.in,
                                                                                                  StandardCharsets.ISO_8859_1));
      while (true)
      {
        LOGGER.info ("Type 'exit' to shutdown the server: ");
        if ("exit".equals (rdr.readLine ()))
        {
          break;
        }
      }

      LOGGER.info ("\n...Shutting down server...");

      sched.shutdown (true);
    }
  }

  /**
   * <p>
   * Called by the <code>{@link IScheduler}</code> when a serious error has occured within the
   * scheduler - such as repeated failures in the <code>JobStore</code>, or the inability to
   * instantiate a <code>Job</code> instance when its <code>Trigger</code> has fired.
   * </p>
   * <p>
   * The <code>getErrorCode()</code> method of the given SchedulerException can be used to determine
   * more specific information about the type of error that was encountered.
   * </p>
   */
  @Override
  public void schedulerError (final String msg, final SchedulerException cause)
  {
    LOGGER.error ("*** " + msg, cause);
  }

  /**
   * <p>
   * Called by the <code>{@link IScheduler}</code> to inform the listener that it has shutdown.
   * </p>
   */
  @Override
  public void schedulerShutdown ()
  {
    LOGGER.info ("\n*** The scheduler is now shutdown.");
    sched = null;
  }

  public static void main (final String [] args) throws Exception
  {
    // //Configure Log4J
    // org.apache.log4j.PropertyConfigurator.configure(
    // System.getProperty("log4jConfigFile", "log4j.properties"));

    try
    {
      final QuartzServer server = new QuartzServer ();
      if (args.length == 0)
      {
        server.serve (new StdSchedulerFactory (), false);
      }
      else
        if (args.length == 1 && args[0].equalsIgnoreCase ("console"))
        {
          server.serve (new StdSchedulerFactory (), true);
        }
        else
        {
          LOGGER.info ("\nUsage: QuartzServer [console]");
        }
    }
    catch (final Exception e)
    {
      LOGGER.error ("Internal error", e);
    }
  }

}
