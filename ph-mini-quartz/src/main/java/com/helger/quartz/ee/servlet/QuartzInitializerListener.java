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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * <p>
 * A ServletContextListner that can be used to initialize Quartz.
 * </p>
 * <p>
 * You'll want to add something like this to your <code>WEB-INF/web.xml</code>
 * file:
 * </p>
 *
 * <pre>
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;quartz:config-file&lt;/param-name&gt;
 *         &lt;param-value&gt;/some/path/my_quartz.properties&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;quartz:shutdown-on-unload&lt;/param-name&gt;
 *         &lt;param-value&gt;true&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;quartz:wait-on-shutdown&lt;/param-name&gt;
 *         &lt;param-value&gt;true&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 *     &lt;context-param&gt;
 *         &lt;param-name&gt;quartz:start-on-load&lt;/param-name&gt;
 *         &lt;param-value&gt;true&lt;/param-value&gt;
 *     &lt;/context-param&gt;
 *
 *     &lt;listener&gt;
 *         &lt;listener-class&gt;
 *             com.helger.quartz.ee.servlet.QuartzInitializerListener
 *         &lt;/listener-class&gt;
 *     &lt;/listener&gt;
 * </pre>
 * <p>
 * The init parameter 'quartz:config-file' can be used to specify the path (and
 * filename) of your Quartz properties file. If you leave out this parameter,
 * the default ("quartz.properties") will be used.
 * </p>
 * <p>
 * The init parameter 'quartz:shutdown-on-unload' can be used to specify whether
 * you want scheduler.shutdown() called when the listener is unloaded (usually
 * when the application server is being shutdown). Possible values are "true" or
 * "false". The default is "true".
 * </p>
 * <p>
 * The init parameter 'quartz:wait-on-shutdown' has effect when
 * 'quartz:shutdown-on-unload' is specified "true", and indicates whether you
 * want scheduler.shutdown(true) called when the listener is unloaded (usually
 * when the application server is being shutdown). Passing "true" to the
 * shutdown() call causes the scheduler to wait for existing jobs to complete.
 * Possible values are "true" or "false". The default is "false".
 * </p>
 * <p>
 * The init parameter 'quartz:start-on-load' can be used to specify whether you
 * want the scheduler.start() method called when the listener is first loaded.
 * If set to false, your application will need to call the start() method before
 * the scheduler begins to run and process jobs. Possible values are "true" or
 * "false". The default is "true", which means the scheduler is started.
 * </p>
 * A StdSchedulerFactory instance is stored into the ServletContext. You can
 * gain access to the factory from a ServletContext instance like this: <br>
 *
 * <pre>
 * StdSchedulerFactory factory = (StdSchedulerFactory) ctx.getAttribute (QuartzInitializerListener.QUARTZ_FACTORY_KEY);
 * </pre>
 * <p>
 * The init parameter 'quartz:servlet-context-factory-key' can be used to
 * override the name under which the StdSchedulerFactory is stored into the
 * ServletContext, in which case you will want to use this name rather than
 * <code>QuartzInitializerListener.QUARTZ_FACTORY_KEY</code> in the above
 * example.
 * </p>
 * <p>
 * The init parameter 'quartz:scheduler-context-servlet-context-key' if set, the
 * ServletContext will be stored in the SchedulerContext under the given key
 * name (and will therefore be available to jobs during execution).
 * </p>
 * <p>
 * The init parameter 'quartz:start-delay-seconds' can be used to specify the
 * amount of time to wait after initializing the scheduler before
 * scheduler.start() is called.
 * </p>
 * Once you have the factory instance, you can retrieve the Scheduler instance
 * by calling <code>getScheduler()</code> on the factory.
 *
 * @author James House
 * @author Chuck Cavaness
 * @author John Petrocik
 */
public class QuartzInitializerListener implements ServletContextListener
{
  public static final String QUARTZ_FACTORY_KEY = "com.helger.quartz.impl.StdSchedulerFactory.KEY";

  private boolean m_bPerformShutdown = true;
  private boolean m_bWaitOnShutdown = false;
  private IScheduler m_aScheduler;

  private static final Logger s_aLogger = LoggerFactory.getLogger (QuartzInitializerListener.class);

  public void contextInitialized (final ServletContextEvent sce)
  {
    s_aLogger.info ("Quartz Initializer Servlet loaded, initializing Scheduler...");

    final ServletContext servletContext = sce.getServletContext ();
    StdSchedulerFactory factory;
    try
    {

      final String configFile = servletContext.getInitParameter ("quartz:config-file");
      final String shutdownPref = servletContext.getInitParameter ("quartz:shutdown-on-unload");
      if (shutdownPref != null)
      {
        m_bPerformShutdown = Boolean.valueOf (shutdownPref).booleanValue ();
      }
      final String shutdownWaitPref = servletContext.getInitParameter ("quartz:wait-on-shutdown");
      if (shutdownPref != null)
      {
        m_bWaitOnShutdown = Boolean.valueOf (shutdownWaitPref).booleanValue ();
      }

      factory = getSchedulerFactory (configFile);

      // Always want to get the scheduler, even if it isn't starting,
      // to make sure it is both initialized and registered.
      m_aScheduler = factory.getScheduler ();

      // Should the Scheduler being started now or later
      final String startOnLoad = servletContext.getInitParameter ("quartz:start-on-load");

      int startDelay = 0;
      final String startDelayS = servletContext.getInitParameter ("quartz:start-delay-seconds");
      try
      {
        if (startDelayS != null && startDelayS.trim ().length () > 0)
          startDelay = Integer.parseInt (startDelayS);
      }
      catch (final Exception e)
      {
        s_aLogger.error ("Cannot parse value of 'start-delay-seconds' to an integer: " +
                         startDelayS +
                         ", defaulting to 5 seconds.");
        startDelay = 5;
      }

      /*
       * If the "quartz:start-on-load" init-parameter is not specified, the
       * scheduler will be started. This is to maintain backwards compatability.
       */
      if (startOnLoad == null || (Boolean.valueOf (startOnLoad).booleanValue ()))
      {
        if (startDelay <= 0)
        {
          // Start now
          m_aScheduler.start ();
          s_aLogger.info ("Scheduler has been started...");
        }
        else
        {
          // Start delayed
          m_aScheduler.startDelayed (startDelay);
          s_aLogger.info ("Scheduler will start in " + startDelay + " seconds.");
        }
      }
      else
      {
        s_aLogger.info ("Scheduler has not been started. Use scheduler.start()");
      }

      String factoryKey = servletContext.getInitParameter ("quartz:servlet-context-factory-key");
      if (factoryKey == null)
      {
        factoryKey = QUARTZ_FACTORY_KEY;
      }

      s_aLogger.info ("Storing the Quartz Scheduler Factory in the servlet context at key: " + factoryKey);
      servletContext.setAttribute (factoryKey, factory);

      final String servletCtxtKey = servletContext.getInitParameter ("quartz:scheduler-context-servlet-context-key");
      if (servletCtxtKey != null)
      {
        s_aLogger.info ("Storing the ServletContext in the scheduler context at key: " + servletCtxtKey);
        m_aScheduler.getContext ().put (servletCtxtKey, servletContext);
      }

    }
    catch (final Exception e)
    {
      s_aLogger.error ("Mini Quartz Scheduler failed to initialize", e);
    }
  }

  protected StdSchedulerFactory getSchedulerFactory (final String configFile) throws SchedulerException
  {
    final StdSchedulerFactory factory = new StdSchedulerFactory ();
    // get Properties
    if (configFile != null)
      factory.initialize (configFile);
    return factory;
  }

  public void contextDestroyed (final ServletContextEvent sce)
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
      s_aLogger.error ("Mini Quartz Scheduler failed to shutdown cleanly", e);
    }

    s_aLogger.info ("Mini Quartz Scheduler successful shutdown.");
  }
}
