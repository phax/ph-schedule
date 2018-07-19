/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2018 Philip Helger (www.helger.com)
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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.WillNotClose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsCollection;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingBufferedInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.lang.NonBlockingProperties;
import com.helger.commons.system.SystemProperties;
import com.helger.quartz.IJobListener;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.ITriggerListener;
import com.helger.quartz.SchedulerConfigException;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.core.IJobRunShellFactory;
import com.helger.quartz.core.QuartzScheduler;
import com.helger.quartz.core.QuartzSchedulerResources;
import com.helger.quartz.impl.matchers.EverythingMatcher;
import com.helger.quartz.simpl.CascadingClassLoadHelper;
import com.helger.quartz.simpl.RAMJobStore;
import com.helger.quartz.simpl.SimpleInstanceIdGenerator;
import com.helger.quartz.simpl.SimpleThreadPool;
import com.helger.quartz.simpl.SystemPropertyInstanceIdGenerator;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.IInstanceIdGenerator;
import com.helger.quartz.spi.IJobFactory;
import com.helger.quartz.spi.IJobStore;
import com.helger.quartz.spi.ISchedulerPlugin;
import com.helger.quartz.spi.IThreadExecutor;
import com.helger.quartz.spi.IThreadPool;
import com.helger.quartz.utils.PropertiesParser;

/**
 * <p>
 * An implementation of <code>{@link com.helger.quartz.ISchedulerFactory}</code>
 * that does all of its work of creating a <code>QuartzScheduler</code> instance
 * based on the contents of a <code>Properties</code> file.
 * </p>
 * <p>
 * By default a properties file named "quartz.properties" is loaded from the
 * 'current working directory'. If that fails, then the "quartz.properties" file
 * located (as a resource) in the "quartz" package is loaded. If you wish to use
 * a file other than these defaults, you must define the system property
 * 'org.quartz.properties' to point to the file you want.
 * </p>
 * <p>
 * Alternatively, you can explicitly initialize the factory by calling one of
 * the <code>initialize(xx)</code> methods before calling
 * <code>getScheduler()</code>.
 * </p>
 * <p>
 * See the sample properties files that are distributed with Quartz for
 * information about the various settings available within the file. Full
 * configuration documentation can be found at
 * http://www.quartz-scheduler.org/docs/index.html
 * </p>
 * <p>
 * Instances of the specified
 * <code>{@link com.helger.quartz.spi.IJobStore}</code>,
 * <code>{@link com.helger.quartz.spi.IThreadPool}</code>, and other SPI classes
 * will be created by name, and then any additional properties specified for
 * them in the config file will be set on the instance by calling an equivalent
 * 'set' method. For example if the properties file contains the property
 * 'org.quartz.jobStore.myProp = 10' then after the JobStore class has been
 * instantiated, the method 'setMyProp()' will be called on it. Type conversion
 * to primitive Java types (int, long, float, double, boolean, and String) are
 * performed before calling the property's setter method.
 * </p>
 * <p>
 * One property can reference another property's value by specifying a value
 * following the convention of "$@other.property.name", for example, to
 * reference the scheduler's instance name as the value for some other property,
 * you would use "$@org.quartz.scheduler.instanceName".
 * </p>
 *
 * @author James House
 * @author Anthony Eden
 * @author Mohammad Rezaei
 */
public class StdSchedulerFactory implements ISchedulerFactory
{
  public static final String PROPERTIES_FILE = "org.quartz.properties";
  public static final String PROP_SCHED_INSTANCE_NAME = "org.quartz.scheduler.instanceName";
  public static final String PROP_SCHED_INSTANCE_ID = "org.quartz.scheduler.instanceId";
  public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX = "org.quartz.scheduler.instanceIdGenerator";
  public static final String PROP_SCHED_INSTANCE_ID_GENERATOR_CLASS = PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX +
                                                                      ".class";
  public static final String PROP_SCHED_THREAD_NAME = "org.quartz.scheduler.threadName";
  public static final String PROP_SCHED_BATCH_TIME_WINDOW = "org.quartz.scheduler.batchTriggerAcquisitionFireAheadTimeWindow";
  public static final String PROP_SCHED_MAX_BATCH_SIZE = "org.quartz.scheduler.batchTriggerAcquisitionMaxCount";
  public static final String PROP_SCHED_IDLE_WAIT_TIME = "org.quartz.scheduler.idleWaitTime";
  public static final String PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON = "org.quartz.scheduler.makeSchedulerThreadDaemon";
  public static final String PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD = "org.quartz.scheduler.threadsInheritContextClassLoaderOfInitializer";
  public static final String PROP_SCHED_CLASS_LOAD_HELPER_CLASS = "org.quartz.scheduler.classLoadHelper.class";
  public static final String PROP_SCHED_JOB_FACTORY_CLASS = "org.quartz.scheduler.jobFactory.class";
  public static final String PROP_SCHED_JOB_FACTORY_PREFIX = "org.quartz.scheduler.jobFactory";
  public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN = "org.quartz.scheduler.interruptJobsOnShutdown";
  public static final String PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT = "org.quartz.scheduler.interruptJobsOnShutdownWithWait";
  public static final String PROP_SCHED_CONTEXT_PREFIX = "org.quartz.context.key";
  public static final String PROP_THREAD_POOL_PREFIX = "org.quartz.threadPool";
  public static final String PROP_THREAD_POOL_CLASS = "org.quartz.threadPool.class";
  public static final String PROP_JOB_STORE_PREFIX = "org.quartz.jobStore";
  public static final String PROP_JOB_STORE_LOCK_HANDLER_PREFIX = PROP_JOB_STORE_PREFIX + ".lockHandler";
  public static final String PROP_JOB_STORE_LOCK_HANDLER_CLASS = PROP_JOB_STORE_LOCK_HANDLER_PREFIX + ".class";
  public static final String PROP_JOB_STORE_CLASS = "org.quartz.jobStore.class";
  public static final String PROP_JOB_STORE_USE_PROP = "org.quartz.jobStore.useProperties";
  public static final String PROP_PLUGIN_PREFIX = "org.quartz.plugin";
  public static final String PROP_PLUGIN_CLASS = "class";
  public static final String PROP_JOB_LISTENER_PREFIX = "org.quartz.jobListener";
  public static final String PROP_TRIGGER_LISTENER_PREFIX = "org.quartz.triggerListener";
  public static final String PROP_LISTENER_CLASS = "class";
  public static final String DEFAULT_INSTANCE_ID = "NON_CLUSTERED";
  public static final String AUTO_GENERATE_INSTANCE_ID = "AUTO";
  public static final String PROP_THREAD_EXECUTOR = "org.quartz.threadExecutor";
  public static final String PROP_THREAD_EXECUTOR_CLASS = "org.quartz.threadExecutor.class";
  public static final String SYSTEM_PROPERTY_AS_INSTANCE_ID = "SYS_PROP";

  private SchedulerException m_aInitException;
  private String m_sPropSrc;
  private PropertiesParser m_aCfg;
  private static final Logger LOGGER = LoggerFactory.getLogger (StdSchedulerFactory.class);

  /**
   * Create an uninitialized StdSchedulerFactory.
   */
  public StdSchedulerFactory ()
  {}

  protected Logger getLog ()
  {
    return LOGGER;
  }

  /**
   * <p>
   * Initialize the <code>{@link com.helger.quartz.ISchedulerFactory}</code>
   * with the contents of a <code>NonBlockingProperties</code> file and
   * overriding System properties.
   * </p>
   * <p>
   * By default a properties file named "quartz.properties" is loaded from the
   * 'current working directory'. If that fails, then the "quartz.properties"
   * file located (as a resource) in the "quartz" package is loaded. If you wish
   * to use a file other than these defaults, you must define the system
   * property 'org.quartz.properties' to point to the file you want.
   * </p>
   * <p>
   * System properties (environment variables, and -D definitions on the
   * command-line when running the JVM) override any properties in the loaded
   * file. For this reason, you may want to use a different initialize() method
   * if your application security policy prohibits access to
   * <code>{@link java.lang.System#getProperties()}</code>.
   * </p>
   *
   * @return this
   */
  @Nonnull
  public StdSchedulerFactory initialize () throws SchedulerException
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("StdSchedulerFactory.initalize");

    // short-circuit if already initialized
    if (m_aCfg != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("StdSchedulerFactory already initalized");
      return this;
    }
    if (m_aInitException != null)
    {
      throw m_aInitException;
    }

    final String requestedFile = SystemProperties.getPropertyValueOrNull (PROPERTIES_FILE);
    final String propFileName = requestedFile != null ? requestedFile : "quartz.properties";
    final File propFile = new File (propFileName);

    final NonBlockingProperties props = new NonBlockingProperties ();

    InputStream in = null;
    try
    {
      if (propFile.exists ())
      {
        try
        {
          if (requestedFile != null)
            m_sPropSrc = "specified file: '" + requestedFile + "'";
          else
            m_sPropSrc = "default file in current working dir: 'quartz.properties'";

          in = new NonBlockingBufferedInputStream (new FileInputStream (propFileName));
          props.load (in);
        }
        catch (final IOException ioe)
        {
          m_aInitException = new SchedulerException ("Properties file: '" + propFileName + "' could not be read.", ioe);
          throw m_aInitException;
        }
      }
      else
        if (requestedFile != null)
        {
          in = ClassPathResource.getInputStream (requestedFile);
          if (in == null)
          {
            m_aInitException = new SchedulerException ("Properties file: '" + requestedFile + "' could not be found.");
            throw m_aInitException;
          }

          m_sPropSrc = "specified file: '" + requestedFile + "' in the class resource path.";

          in = new NonBlockingBufferedInputStream (in);
          try
          {
            props.load (in);
          }
          catch (final IOException ioe)
          {
            m_aInitException = new SchedulerException ("Properties file: '" + requestedFile + "' could not be read.",
                                                       ioe);
            throw m_aInitException;
          }
        }
        else
        {
          m_sPropSrc = "default resource file in Quartz package: 'quartz.properties'";

          in = ClassPathResource.getInputStream ("quartz.properties");
          if (in == null)
            in = ClassPathResource.getInputStream ("quartz/quartz.properties");

          if (in == null)
          {
            m_aInitException = new SchedulerException ("Default quartz.properties not found in class path");
            throw m_aInitException;
          }
          try
          {
            props.load (in);
          }
          catch (final IOException ioe)
          {
            m_aInitException = new SchedulerException ("Resource properties file: 'quartz/quartz.properties' " +
                                                       "could not be read from the classpath.",
                                                       ioe);
            throw m_aInitException;
          }
        }
    }
    finally
    {
      StreamHelper.close (in);
    }

    return initialize (_overrideWithSysProps (props));
  }

  /**
   * Add all System properties to the given <code>props</code>. Will override
   * any properties that already exist in the given <code>props</code>.
   */
  @Nonnull
  private NonBlockingProperties _overrideWithSysProps (@Nonnull final NonBlockingProperties props)
  {
    Properties sysProps = null;
    try
    {
      sysProps = System.getProperties ();
    }
    catch (final AccessControlException e)
    {
      getLog ().warn ("Skipping overriding MiniQuartz properties with System properties " +
                      "during initialization because of an AccessControlException.  " +
                      "This is likely due to not having read/write access for " +
                      "java.util.PropertyPermission as required by java.lang.System.getProperties().  " +
                      "To resolve this warning, either add this permission to your policy file or " +
                      "use a non-default version of initialize().",
                      e);
    }

    if (sysProps != null)
    {
      for (final Map.Entry <?, ?> aEntry : sysProps.entrySet ())
        props.put ((String) aEntry.getKey (), (String) aEntry.getValue ());
    }

    return props;
  }

  /**
   * <p>
   * Initialize the <code>{@link com.helger.quartz.ISchedulerFactory}</code>
   * with the contents of the <code>Properties</code> file with the given name.
   * </p>
   *
   * @return this
   */
  @Nonnull
  public StdSchedulerFactory initialize (final String filename) throws SchedulerException
  {
    // short-circuit if already initialized
    if (m_aCfg != null)
      return this;

    if (m_aInitException != null)
      throw m_aInitException;

    final NonBlockingProperties props = new NonBlockingProperties ();

    InputStream is = ClassPathResource.getInputStream (filename);
    try
    {
      if (is != null)
      {
        is = new NonBlockingBufferedInputStream (is);
        m_sPropSrc = "the specified file : '" + filename + "' from the class resource path.";
      }
      else
      {
        is = new NonBlockingBufferedInputStream (new FileInputStream (filename));
        m_sPropSrc = "the specified file : '" + filename + "'";
      }
      props.load (is);
    }
    catch (final IOException ioe)
    {
      m_aInitException = new SchedulerException ("Properties file: '" + filename + "' could not be read.", ioe);
      throw m_aInitException;
    }
    finally
    {
      StreamHelper.close (is);
    }

    return initialize (props);
  }

  /**
   * <p>
   * Initialize the <code>{@link com.helger.quartz.ISchedulerFactory}</code>
   * with the contents of the <code>Properties</code> file opened with the given
   * <code>InputStream</code>.
   * </p>
   *
   * @return this
   */
  @Nonnull
  public StdSchedulerFactory initialize (@Nonnull @WillNotClose final InputStream propertiesStream) throws SchedulerException
  {
    // short-circuit if already initialized
    if (m_aCfg != null)
      return this;

    if (m_aInitException != null)
      throw m_aInitException;

    final NonBlockingProperties props = new NonBlockingProperties ();
    if (propertiesStream != null)
    {
      try
      {
        props.load (propertiesStream);
        m_sPropSrc = "an externally opened InputStream.";
      }
      catch (final IOException e)
      {
        m_aInitException = new SchedulerException ("Error loading property data from InputStream", e);
        throw m_aInitException;
      }
    }
    else
    {
      m_aInitException = new SchedulerException ("Error loading property data from InputStream - InputStream is null.");
      throw m_aInitException;
    }

    return initialize (props);
  }

  /**
   * Initialize the <code>{@link com.helger.quartz.ISchedulerFactory}</code>
   * with the contents of the given <code>Properties</code> object.
   *
   * @throws SchedulerException
   * @return this
   */
  @Nonnull
  public StdSchedulerFactory initialize (final NonBlockingProperties props) throws SchedulerException
  {
    if (m_sPropSrc == null)
      m_sPropSrc = "an externally provided properties instance.";

    m_aCfg = new PropertiesParser (props);
    return this;
  }

  private IScheduler _instantiate () throws SchedulerException
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("StdSchedulerFactory._instantiate");

    if (m_aCfg == null)
    {
      initialize ();
    }

    if (m_aInitException != null)
    {
      throw m_aInitException;
    }

    IJobStore js = null;
    IThreadPool tp = null;
    QuartzScheduler qs = null;
    String instanceIdGeneratorClass = null;
    NonBlockingProperties tProps;
    boolean autoId = false;
    long idleWaitTime = -1;
    String classLoadHelperClass;
    String jobFactoryClass;
    IThreadExecutor threadExecutor;

    final SchedulerRepository schedRep = SchedulerRepository.getInstance ();

    // Get Scheduler Properties
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final String schedName = m_aCfg.getStringProperty (PROP_SCHED_INSTANCE_NAME, "MiniQuartzScheduler");

    final String threadName = m_aCfg.getStringProperty (PROP_SCHED_THREAD_NAME,
                                                        schedName + "_MiniQuartzSchedulerThread");

    String schedInstId = m_aCfg.getStringProperty (PROP_SCHED_INSTANCE_ID, DEFAULT_INSTANCE_ID);

    if (schedInstId.equals (AUTO_GENERATE_INSTANCE_ID))
    {
      autoId = true;
      instanceIdGeneratorClass = m_aCfg.getStringProperty (PROP_SCHED_INSTANCE_ID_GENERATOR_CLASS,
                                                           SimpleInstanceIdGenerator.class.getName ());
    }
    else
      if (schedInstId.equals (SYSTEM_PROPERTY_AS_INSTANCE_ID))
      {
        autoId = true;
        instanceIdGeneratorClass = SystemPropertyInstanceIdGenerator.class.getName ();
      }

    classLoadHelperClass = m_aCfg.getStringProperty (PROP_SCHED_CLASS_LOAD_HELPER_CLASS,
                                                     CascadingClassLoadHelper.class.getName ());

    jobFactoryClass = m_aCfg.getStringProperty (PROP_SCHED_JOB_FACTORY_CLASS, null);

    idleWaitTime = m_aCfg.getLongProperty (PROP_SCHED_IDLE_WAIT_TIME, idleWaitTime);
    if (idleWaitTime > -1 && idleWaitTime < 1000)
    {
      throw new SchedulerException (PROP_SCHED_IDLE_WAIT_TIME + " of less than 1000ms is not legal.");
    }

    final boolean makeSchedulerThreadDaemon = m_aCfg.getBooleanProperty (PROP_SCHED_MAKE_SCHEDULER_THREAD_DAEMON);

    final boolean threadsInheritInitalizersClassLoader = m_aCfg.getBooleanProperty (PROP_SCHED_SCHEDULER_THREADS_INHERIT_CONTEXT_CLASS_LOADER_OF_INITIALIZING_THREAD);

    final long batchTimeWindow = m_aCfg.getLongProperty (PROP_SCHED_BATCH_TIME_WINDOW, 0L);
    final int maxBatchSize = m_aCfg.getIntProperty (PROP_SCHED_MAX_BATCH_SIZE, 1);

    final boolean interruptJobsOnShutdown = m_aCfg.getBooleanProperty (PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN, false);
    final boolean interruptJobsOnShutdownWithWait = m_aCfg.getBooleanProperty (PROP_SCHED_INTERRUPT_JOBS_ON_SHUTDOWN_WITH_WAIT,
                                                                               false);

    final NonBlockingProperties schedCtxtProps = m_aCfg.getPropertyGroup (PROP_SCHED_CONTEXT_PREFIX, true);

    // Create class load helper
    IClassLoadHelper loadHelper = null;
    try
    {
      loadHelper = (IClassLoadHelper) _loadClass (classLoadHelperClass).getDeclaredConstructor ().newInstance ();
    }
    catch (final Exception e)
    {
      throw new SchedulerConfigException ("Unable to instantiate class load helper class: " + e.getMessage (), e);
    }
    loadHelper.initialize ();

    IJobFactory jobFactory = null;
    if (jobFactoryClass != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Creating jobFactoryClass " + jobFactoryClass);

      try
      {
        jobFactory = (IJobFactory) loadHelper.loadClass (jobFactoryClass).getDeclaredConstructor ().newInstance ();
      }
      catch (final Exception e)
      {
        throw new SchedulerConfigException ("Unable to instantiate JobFactory class: " + e.getMessage (), e);
      }

      tProps = m_aCfg.getPropertyGroup (PROP_SCHED_JOB_FACTORY_PREFIX, true);
      try
      {
        _setBeanProps (jobFactory, tProps);
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("JobFactory class '" +
                                                   jobFactoryClass +
                                                   "' props could not be configured.",
                                                   e);
        throw m_aInitException;
      }
    }

    IInstanceIdGenerator instanceIdGenerator = null;
    if (instanceIdGeneratorClass != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Creating instanceIdGeneratorClass " + instanceIdGeneratorClass);

      try
      {
        instanceIdGenerator = (IInstanceIdGenerator) loadHelper.loadClass (instanceIdGeneratorClass)
                                                               .getDeclaredConstructor ()
                                                               .newInstance ();
      }
      catch (final Exception e)
      {
        throw new SchedulerConfigException ("Unable to instantiate InstanceIdGenerator class: " + e.getMessage (), e);
      }

      tProps = m_aCfg.getPropertyGroup (PROP_SCHED_INSTANCE_ID_GENERATOR_PREFIX, true);
      try
      {
        _setBeanProps (instanceIdGenerator, tProps);
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("InstanceIdGenerator class '" +
                                                   instanceIdGeneratorClass +
                                                   "' props could not be configured.",
                                                   e);
        throw m_aInitException;
      }
    }

    // Get ThreadPool Properties
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final String tpClass = m_aCfg.getStringProperty (PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName ());
    if (tpClass == null)
    {
      m_aInitException = new SchedulerException ("ThreadPool class not specified. ");
      throw m_aInitException;
    }

    try
    {
      tp = (IThreadPool) loadHelper.loadClass (tpClass).getDeclaredConstructor ().newInstance ();
    }
    catch (final Exception e)
    {
      m_aInitException = new SchedulerException ("ThreadPool class '" + tpClass + "' could not be instantiated.", e);
      throw m_aInitException;
    }
    tProps = m_aCfg.getPropertyGroup (PROP_THREAD_POOL_PREFIX, true);
    try
    {
      _setBeanProps (tp, tProps);
    }
    catch (final Exception e)
    {
      m_aInitException = new SchedulerException ("ThreadPool class '" + tpClass + "' props could not be configured.",
                                                 e);
      throw m_aInitException;
    }

    // Get JobStore Properties
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final String jsClass = m_aCfg.getStringProperty (PROP_JOB_STORE_CLASS, RAMJobStore.class.getName ());

    if (jsClass == null)
    {
      m_aInitException = new SchedulerException ("JobStore class not specified. ");
      throw m_aInitException;
    }

    try
    {
      js = (IJobStore) loadHelper.loadClass (jsClass).getDeclaredConstructor ().newInstance ();
    }
    catch (final Exception e)
    {
      m_aInitException = new SchedulerException ("JobStore class '" + jsClass + "' could not be instantiated.", e);
      throw m_aInitException;
    }

    SchedulerDetailsSetter.setDetails (js, schedName, schedInstId);

    tProps = m_aCfg.getPropertyGroup (PROP_JOB_STORE_PREFIX,
                                      true,
                                      new String [] { PROP_JOB_STORE_LOCK_HANDLER_PREFIX });
    try
    {
      _setBeanProps (js, tProps);
    }
    catch (final Exception e)
    {
      m_aInitException = new SchedulerException ("JobStore class '" + jsClass + "' props could not be configured.", e);
      throw m_aInitException;
    }

    // Set up any SchedulerPlugins
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final ICommonsList <String> pluginNames = m_aCfg.getPropertyGroups (PROP_PLUGIN_PREFIX);
    final ICommonsList <ISchedulerPlugin> plugins = new CommonsArrayList <> ();
    for (final String pluginName : pluginNames)
    {
      final NonBlockingProperties pp = m_aCfg.getPropertyGroup (PROP_PLUGIN_PREFIX + "." + pluginName, true);

      final String plugInClass = pp.getProperty (PROP_PLUGIN_CLASS, null);
      if (plugInClass == null)
      {
        m_aInitException = new SchedulerException ("SchedulerPlugin class not specified for plugin '" +
                                                   pluginName +
                                                   "'");
        throw m_aInitException;
      }

      ISchedulerPlugin aPlugin = null;
      try
      {
        aPlugin = (ISchedulerPlugin) loadHelper.loadClass (plugInClass).getDeclaredConstructor ().newInstance ();
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("SchedulerPlugin class '" +
                                                   plugInClass +
                                                   "' could not be instantiated.",
                                                   e);
        throw m_aInitException;
      }
      try
      {
        _setBeanProps (aPlugin, pp);
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("JobStore SchedulerPlugin '" +
                                                   plugInClass +
                                                   "' props could not be configured.",
                                                   e);
        throw m_aInitException;
      }

      plugins.add (aPlugin);
    }

    // Set up any JobListeners
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final Class <?> [] strArg = new Class [] { String.class };
    final ICommonsList <String> jobListenerNames = m_aCfg.getPropertyGroups (PROP_JOB_LISTENER_PREFIX);
    final ICommonsList <IJobListener> jobListeners = new CommonsArrayList <> ();
    for (final String jobListenerName : jobListenerNames)
    {
      final NonBlockingProperties lp = m_aCfg.getPropertyGroup (PROP_JOB_LISTENER_PREFIX + "." + jobListenerName, true);

      final String listenerClass = lp.getProperty (PROP_LISTENER_CLASS, null);

      if (listenerClass == null)
      {
        m_aInitException = new SchedulerException ("JobListener class not specified for listener '" +
                                                   jobListenerName +
                                                   "'");
        throw m_aInitException;
      }
      IJobListener listener = null;
      try
      {
        listener = (IJobListener) loadHelper.loadClass (listenerClass).getDeclaredConstructor ().newInstance ();
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("JobListener class '" +
                                                   listenerClass +
                                                   "' could not be instantiated.",
                                                   e);
        throw m_aInitException;
      }
      try
      {
        Method nameSetter = null;
        try
        {
          nameSetter = listener.getClass ().getMethod ("setName", strArg);
        }
        catch (final NoSuchMethodException ignore)
        {
          /* do nothing */
        }
        if (nameSetter != null)
        {
          nameSetter.invoke (listener, new Object [] { jobListenerName });
        }
        _setBeanProps (listener, lp);
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("JobListener '" + listenerClass + "' props could not be configured.",
                                                   e);
        throw m_aInitException;
      }
      jobListeners.add (listener);
    }

    // Set up any TriggerListeners
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final ICommonsList <String> triggerListenerNames = m_aCfg.getPropertyGroups (PROP_TRIGGER_LISTENER_PREFIX);
    final ICommonsList <ITriggerListener> triggerListeners = new CommonsArrayList <> ();
    for (final String triggerListenerName : triggerListenerNames)
    {
      final NonBlockingProperties lp = m_aCfg.getPropertyGroup (PROP_TRIGGER_LISTENER_PREFIX +
                                                                "." +
                                                                triggerListenerName,
                                                                true);

      final String listenerClass = lp.getProperty (PROP_LISTENER_CLASS, null);

      if (listenerClass == null)
      {
        m_aInitException = new SchedulerException ("TriggerListener class not specified for listener '" +
                                                   triggerListenerName +
                                                   "'");
        throw m_aInitException;
      }
      ITriggerListener listener = null;
      try
      {
        listener = (ITriggerListener) loadHelper.loadClass (listenerClass).getDeclaredConstructor ().newInstance ();
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("TriggerListener class '" +
                                                   listenerClass +
                                                   "' could not be instantiated.",
                                                   e);
        throw m_aInitException;
      }
      try
      {
        Method nameSetter = null;
        try
        {
          nameSetter = listener.getClass ().getMethod ("setName", strArg);
        }
        catch (final NoSuchMethodException ignore)
        { /* do nothing */ }
        if (nameSetter != null)
        {
          nameSetter.invoke (listener, new Object [] { triggerListenerName });
        }
        _setBeanProps (listener, lp);
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("TriggerListener '" +
                                                   listenerClass +
                                                   "' props could not be configured.",
                                                   e);
        throw m_aInitException;
      }
      triggerListeners.add (listener);
    }

    boolean tpInited = false;
    boolean qsInited = false;

    // Get ThreadExecutor Properties
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    final String threadExecutorClass = m_aCfg.getStringProperty (PROP_THREAD_EXECUTOR_CLASS);
    if (threadExecutorClass != null)
    {
      tProps = m_aCfg.getPropertyGroup (PROP_THREAD_EXECUTOR, true);
      try
      {
        threadExecutor = (IThreadExecutor) loadHelper.loadClass (threadExecutorClass)
                                                     .getDeclaredConstructor ()
                                                     .newInstance ();
        LOGGER.info ("Using custom implementation for ThreadExecutor: " + threadExecutorClass);

        _setBeanProps (threadExecutor, tProps);
      }
      catch (final Exception e)
      {
        m_aInitException = new SchedulerException ("ThreadExecutor class '" +
                                                   threadExecutorClass +
                                                   "' could not be instantiated.",
                                                   e);
        throw m_aInitException;
      }
    }
    else
    {
      LOGGER.info ("Using default implementation for ThreadExecutor");
      threadExecutor = new DefaultThreadExecutor ();
    }

    // Fire everything up
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    try
    {

      // Create correct run-shell
      // factory...
      final IJobRunShellFactory jrsf = new StdJobRunShellFactory ();

      if (autoId)
      {
        try
        {
          schedInstId = DEFAULT_INSTANCE_ID;
          if (js.isClustered ())
          {
            schedInstId = instanceIdGenerator.generateInstanceId ();
          }
        }
        catch (final Exception e)
        {
          getLog ().error ("Couldn't generate instance Id!", e);
          throw new IllegalStateException ("Cannot run without an instance id.");
        }
      }

      if (js.getClass ().getName ().startsWith ("org.terracotta.quartz"))
      {
        try
        {
          final String uuid = (String) js.getClass ().getMethod ("getUUID").invoke (js);
          if (schedInstId.equals (DEFAULT_INSTANCE_ID))
          {
            schedInstId = "TERRACOTTA_CLUSTERED,node=" + uuid;
          }
        }
        catch (final Exception e)
        {
          throw new RuntimeException ("Problem obtaining node id from TerracottaJobStore.", e);
        }
      }

      final QuartzSchedulerResources rsrcs = new QuartzSchedulerResources ();
      rsrcs.setName (schedName);
      rsrcs.setThreadName (threadName);
      rsrcs.setInstanceId (schedInstId);
      rsrcs.setJobRunShellFactory (jrsf);
      rsrcs.setMakeSchedulerThreadDaemon (makeSchedulerThreadDaemon);
      rsrcs.setThreadsInheritInitializersClassLoadContext (threadsInheritInitalizersClassLoader);
      rsrcs.setBatchTimeWindow (batchTimeWindow);
      rsrcs.setMaxBatchSize (maxBatchSize);
      rsrcs.setInterruptJobsOnShutdown (interruptJobsOnShutdown);
      rsrcs.setInterruptJobsOnShutdownWithWait (interruptJobsOnShutdownWithWait);

      SchedulerDetailsSetter.setDetails (tp, schedName, schedInstId);

      rsrcs.setThreadExecutor (threadExecutor);
      threadExecutor.initialize ();

      rsrcs.setThreadPool (tp);
      if (tp instanceof SimpleThreadPool)
      {
        if (threadsInheritInitalizersClassLoader)
          ((SimpleThreadPool) tp).setThreadsInheritContextClassLoaderOfInitializingThread (threadsInheritInitalizersClassLoader);
      }
      tp.initialize ();
      tpInited = true;

      rsrcs.setJobStore (js);

      // add plugins
      for (final ISchedulerPlugin plugin : plugins)
      {
        rsrcs.addSchedulerPlugin (plugin);
      }

      qs = new QuartzScheduler (rsrcs, idleWaitTime);
      qsInited = true;

      // Create Scheduler ref...
      final IScheduler scheduler = instantiate (rsrcs, qs);

      // set job factory if specified
      if (jobFactory != null)
      {
        qs.setJobFactory (jobFactory);
      }

      // Initialize plugins now that we have a Scheduler instance.
      for (int i = 0; i < plugins.size (); i++)
      {
        plugins.getAtIndex (i).initialize (pluginNames.getAtIndex (i), scheduler, loadHelper);
      }

      // add listeners
      for (final IJobListener jobListener : jobListeners)
      {
        qs.getListenerManager ().addJobListener (jobListener, EverythingMatcher.allJobs ());
      }
      for (final ITriggerListener triggerListener : triggerListeners)
      {
        qs.getListenerManager ().addTriggerListener (triggerListener, EverythingMatcher.allTriggers ());
      }

      // set scheduler context data...
      for (final Object key : schedCtxtProps.keySet ())
      {
        final String val = schedCtxtProps.getProperty ((String) key);
        scheduler.getContext ().put ((String) key, val);
      }

      // fire up job store, and runshell factory

      js.setInstanceId (schedInstId);
      js.setInstanceName (schedName);
      js.setThreadPoolSize (tp.getPoolSize ());
      js.initialize (loadHelper, qs.getSchedulerSignaler ());

      jrsf.initialize (scheduler);

      qs.initialize ();

      getLog ().info ("Quartz scheduler '" + scheduler.getSchedulerName () + "' initialized from " + m_sPropSrc);

      getLog ().info ("Quartz scheduler version: " + qs.getVersion ());

      // prevents the repository from being garbage collected
      qs.addNoGCObject (schedRep);

      schedRep.bind (scheduler);
      return scheduler;
    }
    catch (final SchedulerException e)
    {
      _shutdownFromInstantiateException (tp, qs, tpInited, qsInited);
      throw e;
    }
    catch (final RuntimeException re)
    {
      _shutdownFromInstantiateException (tp, qs, tpInited, qsInited);
      throw re;
    }
    catch (final Error re)
    {
      _shutdownFromInstantiateException (tp, qs, tpInited, qsInited);
      throw re;
    }
  }

  private void _shutdownFromInstantiateException (final IThreadPool tp,
                                                  final QuartzScheduler qs,
                                                  final boolean tpInited,
                                                  final boolean qsInited)
  {
    try
    {
      if (qsInited)
        qs.shutdown (false);
      else
        if (tpInited)
          tp.shutdown (false);
    }
    catch (final Exception e)
    {
      getLog ().error ("Got another exception while shutting down after instantiation exception", e);
    }
  }

  /**
   * @param rsrcs
   * @param qs
   * @return Never null
   */
  @Nonnull
  protected IScheduler instantiate (final QuartzSchedulerResources rsrcs, final QuartzScheduler qs)
  {
    final IScheduler scheduler = new StdScheduler (qs);
    return scheduler;
  }

  private void _setBeanProps (final Object obj, final NonBlockingProperties props) throws NoSuchMethodException,
                                                                                   IllegalAccessException,
                                                                                   InvocationTargetException,
                                                                                   IntrospectionException,
                                                                                   SchedulerConfigException
  {
    props.remove ("class");

    final BeanInfo bi = Introspector.getBeanInfo (obj.getClass ());
    final PropertyDescriptor [] propDescs = bi.getPropertyDescriptors ();
    final PropertiesParser pp = new PropertiesParser (props);

    for (final String name : props.keySet ())
    {
      final String c = name.substring (0, 1).toUpperCase (Locale.US);
      final String methName = "set" + c + name.substring (1);

      final java.lang.reflect.Method setMeth = _getSetMethod (methName, propDescs);

      try
      {
        if (setMeth == null)
        {
          throw new NoSuchMethodException ("No setter for property '" + name + "'");
        }

        final Class <?> [] params = setMeth.getParameterTypes ();
        if (params.length != 1)
        {
          throw new NoSuchMethodException ("No 1-argument setter for property '" + name + "'");
        }

        // does the property value reference another property's value? If so,
        // swap to look at its value
        PropertiesParser refProps = pp;
        String refName = pp.getStringProperty (name);
        if (refName != null && refName.startsWith ("$@"))
        {
          refName = refName.substring (2);
          refProps = m_aCfg;
        }
        else
          refName = name;

        if (params[0].equals (int.class))
        {
          setMeth.invoke (obj, new Object [] { Integer.valueOf (refProps.getIntProperty (refName)) });
        }
        else
          if (params[0].equals (long.class))
          {
            setMeth.invoke (obj, new Object [] { Long.valueOf (refProps.getLongProperty (refName)) });
          }
          else
            if (params[0].equals (float.class))
            {
              setMeth.invoke (obj, new Object [] { Float.valueOf (refProps.getFloatProperty (refName)) });
            }
            else
              if (params[0].equals (double.class))
              {
                setMeth.invoke (obj, new Object [] { Double.valueOf (refProps.getDoubleProperty (refName)) });
              }
              else
                if (params[0].equals (boolean.class))
                {
                  setMeth.invoke (obj, new Object [] { Boolean.valueOf (refProps.getBooleanProperty (refName)) });
                }
                else
                  if (params[0].equals (String.class))
                  {
                    setMeth.invoke (obj, new Object [] { refProps.getStringProperty (refName) });
                  }
                  else
                  {
                    throw new NoSuchMethodException ("No primitive-type setter for property '" + name + "'");
                  }
      }
      catch (final NumberFormatException nfe)
      {
        throw new SchedulerConfigException ("Could not parse property '" +
                                            name +
                                            "' into correct data type: " +
                                            nfe.toString ());
      }
    }
  }

  private java.lang.reflect.Method _getSetMethod (final String name, final PropertyDescriptor [] props)
  {
    for (final PropertyDescriptor prop : props)
    {
      final java.lang.reflect.Method wMeth = prop.getWriteMethod ();

      if (wMeth != null && wMeth.getName ().equals (name))
      {
        return wMeth;
      }
    }

    return null;
  }

  private Class <?> _loadClass (final String className) throws ClassNotFoundException, SchedulerConfigException
  {

    try
    {
      final ClassLoader cl = _findClassloader ();
      if (cl != null)
        return cl.loadClass (className);
      throw new SchedulerConfigException ("Unable to find a class loader on the current thread or class.");
    }
    catch (final ClassNotFoundException e)
    {
      if (getClass ().getClassLoader () != null)
        return getClass ().getClassLoader ().loadClass (className);
      throw e;
    }
  }

  private ClassLoader _findClassloader ()
  {
    // work-around set context loader for windows-service started jvms
    // (QUARTZ-748)
    final Thread t = Thread.currentThread ();
    ClassLoader ret = t.getContextClassLoader ();
    if (ret == null)
    {
      ret = getClass ().getClassLoader ();
      t.setContextClassLoader (ret);
    }
    return ret;
  }

  private String _getSchedulerName ()
  {
    return m_aCfg.getStringProperty (PROP_SCHED_INSTANCE_NAME, "MiniQuartzScheduler");
  }

  /**
   * <p>
   * Returns a handle to the Scheduler produced by this factory.
   * </p>
   * <p>
   * If one of the <code>initialize</code> methods has not be previously called,
   * then the default (no-arg) <code>initialize()</code> method will be called
   * by this method.
   * </p>
   */
  public IScheduler getScheduler () throws SchedulerException
  {
    if (m_aCfg == null)
      initialize ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Looking up scheduler with name '" + _getSchedulerName () + "'");

    final SchedulerRepository schedRep = SchedulerRepository.getInstance ();

    IScheduler sched = schedRep.lookup (_getSchedulerName ());
    if (sched != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Reusing existing scheduler with name '" + _getSchedulerName () + "'");

      if (sched.isShutdown ())
        schedRep.remove (_getSchedulerName ());
      else
        return sched;
    }

    sched = _instantiate ();
    return sched;
  }

  /**
   * <p>
   * Returns a handle to the default Scheduler, creating it if it does not yet
   * exist.
   * </p>
   *
   * @see #initialize()
   */
  public static IScheduler getDefaultScheduler () throws SchedulerException
  {
    final StdSchedulerFactory fact = new StdSchedulerFactory ();

    return fact.getScheduler ();
  }

  /**
   * <p>
   * Returns a handle to the Scheduler with the given name, if it exists (if it
   * has already been instantiated).
   * </p>
   */
  public IScheduler getScheduler (final String schedName) throws SchedulerException
  {
    return SchedulerRepository.getInstance ().lookup (schedName);
  }

  /**
   * <p>
   * Returns a handle to all known Schedulers (made by any StdSchedulerFactory
   * instance.).
   * </p>
   */
  public ICommonsCollection <IScheduler> getAllSchedulers () throws SchedulerException
  {
    return SchedulerRepository.getInstance ().lookupAll ();
  }
}
