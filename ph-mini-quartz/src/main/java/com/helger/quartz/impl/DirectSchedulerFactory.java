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

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsCollection;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.core.IJobRunShellFactory;
import com.helger.quartz.core.QuartzScheduler;
import com.helger.quartz.core.QuartzSchedulerResources;
import com.helger.quartz.simpl.CascadingClassLoadHelper;
import com.helger.quartz.simpl.RAMJobStore;
import com.helger.quartz.simpl.SimpleThreadPool;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.IJobStore;
import com.helger.quartz.spi.ISchedulerPlugin;
import com.helger.quartz.spi.IThreadExecutor;
import com.helger.quartz.spi.IThreadPool;

/**
 * <p>
 * A singleton implementation of
 * <code>{@link com.helger.quartz.ISchedulerFactory}</code>.
 * </p>
 * <p>
 * Here are some examples of using this class:
 * </p>
 * <p>
 * To create a scheduler that does not write anything to the database (is not
 * persistent), you can call <code>createVolatileScheduler</code>:
 *
 * <pre>
 * DirectSchedulerFactory.getInstance ().createVolatileScheduler (10); // 10
 *                                                                     // threads
 *                                                                     // * //
 *                                                                     // don't
 *                                                                     // forget
 *                                                                     // to
 *                                                                     // start
 *                                                                     // the
 *                                                                     // scheduler:
 *                                                                     // DirectSchedulerFactory.getInstance().getScheduler().start();
 * </pre>
 * <p>
 * Several create methods are provided for convenience. All create methods
 * eventually end up calling the create method with all the parameters:
 * </p>
 *
 * <pre>
 *  public void createScheduler(String schedulerName, String schedulerInstanceId, ThreadPool threadPool, JobStore jobStore, String rmiRegistryHost, int rmiRegistryPort)
 * </pre>
 * <p>
 * Here is an example of using this method:
 * </p>
 * * *
 *
 * <pre>
 * // create the thread pool SimpleThreadPool threadPool = new SimpleThreadPool(maxThreads, Thread.NORM_PRIORITY); threadPool.initialize(); * // create the job store JobStore jobStore = new RAMJobStore();
 *
 *  DirectSchedulerFactory.getInstance().createScheduler("My Quartz Scheduler", "My Instance", threadPool, jobStore, "localhost", 1099); * // don't forget to start the scheduler: DirectSchedulerFactory.getInstance().getScheduler("My Quartz Scheduler", "My Instance").start();
 * </pre>
 * <p>
 * You can also use a JDBCJobStore instead of the RAMJobStore:
 * </p>
 *
 * <pre>
 * DBConnectionManager.getInstance ().addConnectionProvider ("someDatasource",
 *                                                           new JNDIConnectionProvider ("someDatasourceJNDIName"));
 *
 * JobStoreTX jdbcJobStore = new JobStoreTX ();
 * jdbcJobStore.setDataSource ("someDatasource");
 * jdbcJobStore.setPostgresStyleBlobs (true);
 * jdbcJobStore.setTablePrefix ("QRTZ_");
 * jdbcJobStore.setInstanceId ("My Instance");
 * </pre>
 *
 * @author Mohammad Rezaei
 * @author James House
 * @see IJobStore
 * @see IThreadPool
 */
public class DirectSchedulerFactory implements ISchedulerFactory
{
  public static final String DEFAULT_INSTANCE_ID = "SIMPLE_NON_CLUSTERED";
  public static final String DEFAULT_SCHEDULER_NAME = "SimpleQuartzScheduler";
  private static final DefaultThreadExecutor DEFAULT_THREAD_EXECUTOR = new DefaultThreadExecutor ();
  private static final int DEFAULT_BATCH_MAX_SIZE = 1;
  private static final long DEFAULT_BATCH_TIME_WINDOW = 0L;

  private boolean m_bInitialized = false;
  private static DirectSchedulerFactory s_aInstance = new DirectSchedulerFactory ();
  private static final Logger LOGGER = LoggerFactory.getLogger (DirectSchedulerFactory.class);

  protected Logger getLog ()
  {
    return LOGGER;
  }

  /**
   * Constructor
   */
  protected DirectSchedulerFactory ()
  {}

  @Nonnull
  public static DirectSchedulerFactory getInstance ()
  {
    return s_aInstance;
  }

  /**
   * Creates an in memory job store (<code>{@link RAMJobStore}</code>) The
   * thread priority is set to Thread.NORM_PRIORITY
   *
   * @param nMaxThreads
   *        The number of threads in the thread pool
   * @throws SchedulerException
   *         if initialization failed.
   */
  public void createVolatileScheduler (final int nMaxThreads) throws SchedulerException
  {
    final SimpleThreadPool aThreadPool = new SimpleThreadPool (nMaxThreads, Thread.NORM_PRIORITY);
    aThreadPool.initialize ();
    final IJobStore jobStore = new RAMJobStore ();
    createScheduler (aThreadPool, jobStore);
  }

  /**
   * Creates a scheduler using the specified thread pool and job store. This
   * scheduler can be retrieved via
   * {@link DirectSchedulerFactory#getScheduler()}
   *
   * @param threadPool
   *        The thread pool for executing jobs
   * @param jobStore
   *        The type of job store
   * @throws SchedulerException
   *         if initialization failed
   */
  public void createScheduler (final IThreadPool threadPool, final IJobStore jobStore) throws SchedulerException
  {
    createScheduler (DEFAULT_SCHEDULER_NAME, DEFAULT_INSTANCE_ID, threadPool, jobStore);
  }

  /**
   * Same as
   * {@link DirectSchedulerFactory#createScheduler(IThreadPool threadPool, IJobStore jobStore)},
   * with the addition of specifying the scheduler name and instance ID. This
   * scheduler can only be retrieved via
   * {@link DirectSchedulerFactory#getScheduler(String)}
   *
   * @param schedulerName
   *        The name for the scheduler.
   * @param schedulerInstanceId
   *        The instance ID for the scheduler.
   * @param threadPool
   *        The thread pool for executing jobs
   * @param jobStore
   *        The type of job store
   * @throws SchedulerException
   *         if initialization failed
   */
  public void createScheduler (final String schedulerName,
                               final String schedulerInstanceId,
                               final IThreadPool threadPool,
                               final IJobStore jobStore) throws SchedulerException
  {
    createScheduler (schedulerName, schedulerInstanceId, threadPool, jobStore, -1);
  }

  /**
   * Creates a scheduler using the specified thread pool and job store and binds
   * it to RMI.
   *
   * @param schedulerName
   *        The name for the scheduler.
   * @param schedulerInstanceId
   *        The instance ID for the scheduler.
   * @param threadPool
   *        The thread pool for executing jobs
   * @param jobStore
   *        The type of job store
   * @param idleWaitTime
   *        The idle wait time in milliseconds. You can specify "-1" for the
   *        default value, which is currently 30000 ms.
   * @throws SchedulerException
   *         if initialization failed
   */
  public void createScheduler (final String schedulerName,
                               final String schedulerInstanceId,
                               final IThreadPool threadPool,
                               final IJobStore jobStore,
                               final long idleWaitTime) throws SchedulerException
  {
    createScheduler (schedulerName,
                     schedulerInstanceId,
                     threadPool,
                     jobStore,
                     null, // plugins
                     idleWaitTime);
  }

  /**
   * Creates a scheduler using the specified thread pool, job store, and
   * plugins, and binds it to RMI.
   *
   * @param schedulerName
   *        The name for the scheduler.
   * @param schedulerInstanceId
   *        The instance ID for the scheduler.
   * @param threadPool
   *        The thread pool for executing jobs
   * @param jobStore
   *        The type of job store
   * @param schedulerPluginMap
   *        Map from a <code>String</code> plugin names to
   *        <code>{@link com.helger.quartz.spi.ISchedulerPlugin}</code>s. Can
   *        use "null" if no plugins are required.
   * @param idleWaitTime
   *        The idle wait time in milliseconds. You can specify "-1" for the
   *        default value, which is currently 30000 ms.
   * @throws SchedulerException
   *         if initialization failed
   */
  public void createScheduler (final String schedulerName,
                               final String schedulerInstanceId,
                               final IThreadPool threadPool,
                               final IJobStore jobStore,
                               final Map <String, ISchedulerPlugin> schedulerPluginMap,
                               final long idleWaitTime) throws SchedulerException
  {
    createScheduler (schedulerName,
                     schedulerInstanceId,
                     threadPool,
                     DEFAULT_THREAD_EXECUTOR,
                     jobStore,
                     schedulerPluginMap,
                     idleWaitTime);
  }

  /**
   * Creates a scheduler using the specified thread pool, job store, and
   * plugins, and binds it to RMI.
   *
   * @param schedulerName
   *        The name for the scheduler.
   * @param schedulerInstanceId
   *        The instance ID for the scheduler.
   * @param threadPool
   *        The thread pool for executing jobs
   * @param threadExecutor
   *        The thread executor for executing jobs
   * @param jobStore
   *        The type of job store
   * @param schedulerPluginMap
   *        Map from a <code>String</code> plugin names to
   *        <code>{@link com.helger.quartz.spi.ISchedulerPlugin}</code>s. Can
   *        use "null" if no plugins are required.
   * @param idleWaitTime
   *        The idle wait time in milliseconds. You can specify "-1" for the
   *        default value, which is currently 30000 ms.
   * @throws SchedulerException
   *         if initialization failed
   */
  public void createScheduler (final String schedulerName,
                               final String schedulerInstanceId,
                               final IThreadPool threadPool,
                               final IThreadExecutor threadExecutor,
                               final IJobStore jobStore,
                               final Map <String, ISchedulerPlugin> schedulerPluginMap,
                               final long idleWaitTime) throws SchedulerException
  {
    createScheduler (schedulerName,
                     schedulerInstanceId,
                     threadPool,
                     DEFAULT_THREAD_EXECUTOR,
                     jobStore,
                     schedulerPluginMap,
                     idleWaitTime,
                     DEFAULT_BATCH_MAX_SIZE,
                     DEFAULT_BATCH_TIME_WINDOW);
  }

  /**
   * Creates a scheduler using the specified thread pool, job store, and
   * plugins, and binds it to RMI.
   *
   * @param schedulerName
   *        The name for the scheduler.
   * @param schedulerInstanceId
   *        The instance ID for the scheduler.
   * @param threadPool
   *        The thread pool for executing jobs
   * @param threadExecutor
   *        The thread executor for executing jobs
   * @param jobStore
   *        The type of job store
   * @param schedulerPluginMap
   *        Map from a <code>String</code> plugin names to
   *        <code>{@link com.helger.quartz.spi.ISchedulerPlugin}</code>s. Can
   *        use "null" if no plugins are required.
   * @param idleWaitTime
   *        The idle wait time in milliseconds. You can specify "-1" for the
   *        default value, which is currently 30000 ms.
   * @param maxBatchSize
   *        The maximum batch size of triggers, when acquiring them
   * @param batchTimeWindow
   *        The time window for which it is allowed to "pre-acquire" triggers to
   *        fire
   * @throws SchedulerException
   *         if initialization failed
   */
  public void createScheduler (final String schedulerName,
                               final String schedulerInstanceId,
                               final IThreadPool threadPool,
                               final IThreadExecutor threadExecutor,
                               final IJobStore jobStore,
                               final Map <String, ISchedulerPlugin> schedulerPluginMap,
                               final long idleWaitTime,
                               final int maxBatchSize,
                               final long batchTimeWindow) throws SchedulerException
  {
    // Currently only one run-shell factory is available...
    final IJobRunShellFactory jrsf = new StdJobRunShellFactory ();

    // Fire everything up
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    threadPool.initialize ();

    final QuartzSchedulerResources qrs = new QuartzSchedulerResources ();

    qrs.setName (schedulerName);
    qrs.setInstanceId (schedulerInstanceId);
    SchedulerDetailsSetter.setDetails (threadPool, schedulerName, schedulerInstanceId);
    qrs.setJobRunShellFactory (jrsf);
    qrs.setThreadPool (threadPool);
    qrs.setThreadExecutor (threadExecutor);
    qrs.setJobStore (jobStore);
    qrs.setMaxBatchSize (maxBatchSize);
    qrs.setBatchTimeWindow (batchTimeWindow);

    // add plugins
    if (schedulerPluginMap != null)
    {
      for (final ISchedulerPlugin schedulerPlugin : schedulerPluginMap.values ())
      {
        qrs.addSchedulerPlugin (schedulerPlugin);
      }
    }

    final QuartzScheduler qs = new QuartzScheduler (qrs, idleWaitTime);

    final IClassLoadHelper cch = new CascadingClassLoadHelper ();
    cch.initialize ();

    SchedulerDetailsSetter.setDetails (jobStore, schedulerName, schedulerInstanceId);

    jobStore.initialize (cch, qs.getSchedulerSignaler ());

    final IScheduler scheduler = new StdScheduler (qs);

    jrsf.initialize (scheduler);

    qs.initialize ();

    // Initialize plugins now that we have a Scheduler instance.
    if (schedulerPluginMap != null)
    {
      for (final Entry <String, ISchedulerPlugin> pluginEntry : schedulerPluginMap.entrySet ())
      {
        pluginEntry.getValue ().initialize (pluginEntry.getKey (), scheduler, cch);
      }
    }

    getLog ().info ("Quartz scheduler '" + scheduler.getSchedulerName ());

    getLog ().info ("Quartz scheduler version: " + qs.getVersion ());

    final SchedulerRepository schedRep = SchedulerRepository.getInstance ();

    qs.addNoGCObject (schedRep); // prevents the repository from being
    // garbage collected

    schedRep.bind (scheduler);

    m_bInitialized = true;
  }

  /*
   * public void registerSchedulerForRmi(String schedulerName, String
   * schedulerId, String registryHost, int registryPort) throws
   * SchedulerException, RemoteException { QuartzScheduler scheduler =
   * (QuartzScheduler) this.getScheduler(); scheduler.bind(registryHost,
   * registryPort); }
   */

  /**
   * <p>
   * Returns a handle to the Scheduler produced by this factory.
   * </p>
   * <p>
   * you must call createRemoteScheduler or createScheduler methods before
   * calling getScheduler()
   * </p>
   */
  public IScheduler getScheduler () throws SchedulerException
  {
    if (!m_bInitialized)
    {
      throw new SchedulerException ("you must call createRemoteScheduler or createScheduler methods before calling getScheduler()");
    }

    return getScheduler (DEFAULT_SCHEDULER_NAME);
  }

  /**
   * <p>
   * Returns a handle to the Scheduler with the given name, if it exists.
   * </p>
   */
  public IScheduler getScheduler (final String schedName) throws SchedulerException
  {
    final SchedulerRepository schedRep = SchedulerRepository.getInstance ();

    return schedRep.lookup (schedName);
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
