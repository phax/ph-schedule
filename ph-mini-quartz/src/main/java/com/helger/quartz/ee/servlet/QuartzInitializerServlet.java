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
package com.helger.quartz.ee.servlet;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * <p>
 * A Servlet that can be used to initialize Quartz, if configured as a
 * load-on-startup servlet in a web application.
 * </p>
 * <p>
 * Using this start-up servlet may be preferred to using the
 * {@link QuartzInitializerListener} in some situations - namely when you want
 * to initialize more than one scheduler in the same application.
 * </p>
 * <p>
 * You'll want to add something like this to your WEB-INF/web.xml file:
 * </p>
 *
 * <pre>
 *     &lt;servlet&gt;
 *         &lt;servlet-name&gt;
 *             QuartzInitializer
 *         &lt;/servlet-name&gt;
 *         &lt;display-name&gt;
 *             Quartz Initializer Servlet
 *         &lt;/display-name&gt;
 *         &lt;servlet-class&gt;
 *             com.helger.quartz.ee.servlet.QuartzInitializerServlet
 *         &lt;/servlet-class&gt;
 *         &lt;load-on-startup&gt;
 *             1
 *         &lt;/load-on-startup&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;config-file&lt;/param-name&gt;
 *             &lt;param-value&gt;/some/path/my_quartz.properties&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;shutdown-on-unload&lt;/param-name&gt;
 *             &lt;param-value&gt;true&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;wait-on-shutdown&lt;/param-name&gt;
 *             &lt;param-value&gt;true&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *         &lt;init-param&gt;
 *             &lt;param-name&gt;start-scheduler-on-load&lt;/param-name&gt;
 *             &lt;param-value&gt;true&lt;/param-value&gt;
 *         &lt;/init-param&gt;
 *     &lt;/servlet&gt;
 * </pre>
 * <p>
 * The init parameter 'config-file' can be used to specify the path (and
 * filename) of your Quartz properties file. If you leave out this parameter,
 * the default ("quartz.properties") will be used.
 * </p>
 * <p>
 * The init parameter 'shutdown-on-unload' can be used to specify whether you
 * want scheduler.shutdown() called when the servlet is unloaded (usually when
 * the application server is being shutdown). Possible values are "true" or
 * "false". The default is "true".
 * </p>
 * <p>
 * The init parameter 'wait-on-shutdown' has effect when 'shutdown-on-unload' is
 * specified "true", and indicates whether you want scheduler.shutdown(true)
 * called when the listener is unloaded (usually when the application server is
 * being shutdown). Passing "true" to the shutdown() call causes the scheduler
 * to wait for existing jobs to complete. Possible values are "true" or "false".
 * The default is "false".
 * </p>
 * <p>
 * The init parameter 'start-scheduler-on-load' can be used to specify whether
 * you want the scheduler.start() method called when the servlet is first
 * loaded. If set to false, your application will need to call the start()
 * method before the scheduler begins to run and process jobs. Possible values
 * are "true" or "false". The default is "true", which means the scheduler is
 * started.
 * </p>
 * A StdSchedulerFactory instance is stored into the ServletContext. You can
 * gain access to the factory from a ServletContext instance like this: <br>
 *
 * <pre>
 * StdSchedulerFactory factory = (StdSchedulerFactory) ctx.getAttribute (QuartzFactoryServlet.QUARTZ_FACTORY_KEY);
 * </pre>
 * <p>
 * The init parameter 'servlet-context-factory-key' can be used to override the
 * name under which the StdSchedulerFactory is stored into the ServletContext,
 * in which case you will want to use this name rather than
 * <code>QuartzFactoryServlet.QUARTZ_FACTORY_KEY</code> in the above example.
 * </p>
 * <p>
 * The init parameter 'scheduler-context-servlet-context-key' if set, the
 * ServletContext will be stored in the SchedulerContext under the given key
 * name (and will therefore be available to jobs during execution).
 * </p>
 * <p>
 * The init parameter 'start-delay-seconds' can be used to specify the amount of
 * time to wait after initializing the scheduler before scheduler.start() is
 * called.
 * </p>
 * Once you have the factory instance, you can retrieve the Scheduler instance
 * by calling <code>getScheduler()</code> on the factory.
 *
 * @author James House
 * @author Chuck Cavaness
 */
public class QuartzInitializerServlet extends HttpServlet
{
  public static final String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";

  private boolean m_bPerformShutdown = true;
  private boolean m_bWaitOnShutdown = false;

  private transient IScheduler m_aScheduler;

  @Override
  public void init (final ServletConfig cfg) throws javax.servlet.ServletException
  {
    super.init (cfg);

    log ("Quartz Initializer Servlet loaded, initializing Scheduler...");

    StdSchedulerFactory factory;
    try
    {

      final String configFile = cfg.getInitParameter ("config-file");
      final String shutdownPref = cfg.getInitParameter ("shutdown-on-unload");

      if (shutdownPref != null)
      {
        m_bPerformShutdown = Boolean.valueOf (shutdownPref).booleanValue ();
      }
      final String shutdownWaitPref = cfg.getInitParameter ("wait-on-shutdown");
      if (shutdownPref != null)
      {
        m_bWaitOnShutdown = Boolean.valueOf (shutdownWaitPref).booleanValue ();
      }

      factory = getSchedulerFactory (configFile);

      // Always want to get the scheduler, even if it isn't starting,
      // to make sure it is both initialized and registered.
      m_aScheduler = factory.getScheduler ();

      // Should the Scheduler being started now or later
      final String startOnLoad = cfg.getInitParameter ("start-scheduler-on-load");

      int startDelay = 0;
      final String startDelayS = cfg.getInitParameter ("start-delay-seconds");
      try
      {
        if (startDelayS != null && startDelayS.trim ().length () > 0)
          startDelay = Integer.parseInt (startDelayS);
      }
      catch (final Exception e)
      {
        log ("Cannot parse value of 'start-delay-seconds' to an integer: " +
             startDelayS +
             ", defaulting to 5 seconds.",
             e);
        startDelay = 5;
      }

      /*
       * If the "start-scheduler-on-load" init-parameter is not specified, the
       * scheduler will be started. This is to maintain backwards compatability.
       */
      if (startOnLoad == null || (Boolean.valueOf (startOnLoad).booleanValue ()))
      {
        if (startDelay <= 0)
        {
          // Start now
          m_aScheduler.start ();
          log ("Scheduler has been started...");
        }
        else
        {
          // Start delayed
          m_aScheduler.startDelayed (startDelay);
          log ("Scheduler will start in " + startDelay + " seconds.");
        }
      }
      else
      {
        log ("Scheduler has not been started. Use scheduler.start()");
      }

      String factoryKey = cfg.getInitParameter ("servlet-context-factory-key");
      if (factoryKey == null)
      {
        factoryKey = QUARTZ_FACTORY_KEY;
      }

      log ("Storing the Quartz Scheduler Factory in the servlet context at key: " + factoryKey);
      cfg.getServletContext ().setAttribute (factoryKey, factory);

      final String servletCtxtKey = cfg.getInitParameter ("scheduler-context-servlet-context-key");
      if (servletCtxtKey != null)
      {
        log ("Storing the ServletContext in the scheduler context at key: " + servletCtxtKey);
        m_aScheduler.getContext ().put (servletCtxtKey, cfg.getServletContext ());
      }

    }
    catch (final Exception e)
    {
      log ("Mini Quartz Scheduler failed to initialize: " + e.toString ());
      throw new ServletException (e);
    }
  }

  @Nonnull
  protected StdSchedulerFactory getSchedulerFactory (final String configFile) throws SchedulerException
  {
    final StdSchedulerFactory factory = new StdSchedulerFactory ();
    if (configFile != null)
      factory.initialize (configFile);
    return factory;
  }

  @Override
  public void destroy ()
  {
    if (!m_bPerformShutdown)
      return;

    try
    {
      if (m_aScheduler != null)
        m_aScheduler.shutdown (m_bWaitOnShutdown);
    }
    catch (final Exception e)
    {
      log ("Mini Quartz Scheduler failed to shutdown cleanly", e);
    }

    log ("Mini Quartz Scheduler successful shutdown.");
  }

  @Override
  public void doPost (final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
                                                                                            IOException
  {
    response.sendError (HttpServletResponse.SC_FORBIDDEN);
  }

  @Override
  public void doGet (final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
                                                                                           IOException
  {
    response.sendError (HttpServletResponse.SC_FORBIDDEN);
  }

}
