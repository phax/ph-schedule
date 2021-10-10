/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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
package com.helger.quartz.core;

import java.util.List;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.spi.IJobStore;
import com.helger.quartz.spi.ISchedulerPlugin;
import com.helger.quartz.spi.IThreadExecutor;
import com.helger.quartz.spi.IThreadPool;

/**
 * <p>
 * Contains all of the resources (<code>JobStore</code>,<code>ThreadPool</code>,
 * etc.) necessary to create a <code>{@link QuartzScheduler}</code> instance.
 * </p>
 *
 * @see QuartzScheduler
 * @author James House
 */
public class QuartzSchedulerResources
{
  public static final String CREATE_REGISTRY_NEVER = "never";
  public static final String CREATE_REGISTRY_ALWAYS = "always";
  public static final String CREATE_REGISTRY_AS_NEEDED = "as_needed";

  private String m_sName;
  private String m_sInstanceId;
  private String m_sThreadName;
  private IThreadPool m_aThreadPool;
  private IJobStore m_aJobStore;
  private IJobRunShellFactory m_aJobRunShellFactory;
  private final ICommonsList <ISchedulerPlugin> m_aSchedulerPlugins = new CommonsArrayList <> (10);
  private boolean m_bMakeSchedulerThreadDaemon = false;
  private boolean m_bThreadsInheritInitializersClassLoadContext = false;
  private IThreadExecutor m_aThreadExecutor;
  private long m_nBatchTimeWindow = 0;
  private int m_nMaxBatchSize = 1;
  private boolean m_bInterruptJobsOnShutdown = false;
  private boolean m_bInterruptJobsOnShutdownWithWait = false;

  /**
   * <p>
   * Create an instance with no properties initialized.
   * </p>
   */
  public QuartzSchedulerResources ()
  {
    // do nothing...
  }

  /**
   * <p>
   * Get the name for the <code>{@link QuartzScheduler}</code>.
   * </p>
   */
  public String getName ()
  {
    return m_sName;
  }

  /**
   * <p>
   * Set the name for the <code>{@link QuartzScheduler}</code>.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if name is null or empty.
   */
  public void setName (final String name)
  {
    if (name == null || name.trim ().length () == 0)
    {
      throw new IllegalArgumentException ("Scheduler name cannot be empty.");
    }

    m_sName = name;

    if (m_sThreadName == null)
    {
      // thread name not already set, use default thread name
      setThreadName (name + "_QuartzSchedulerThread");
    }
  }

  /**
   * <p>
   * Get the instance Id for the <code>{@link QuartzScheduler}</code>.
   * </p>
   */
  public String getInstanceId ()
  {
    return m_sInstanceId;
  }

  /**
   * <p>
   * Set the name for the <code>{@link QuartzScheduler}</code>.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if name is null or empty.
   */
  public void setInstanceId (final String instanceId)
  {
    if (instanceId == null || instanceId.trim ().length () == 0)
    {
      throw new IllegalArgumentException ("Scheduler instanceId cannot be empty.");
    }

    m_sInstanceId = instanceId;
  }

  public static String getUniqueIdentifier (final String schedName, final String schedInstId)
  {
    return schedName + "_$_" + schedInstId;
  }

  public String getUniqueIdentifier ()
  {
    return getUniqueIdentifier (m_sName, m_sInstanceId);
  }

  /**
   * <p>
   * Get the name for the <code>{@link QuartzSchedulerThread}</code>.
   * </p>
   */
  public String getThreadName ()
  {
    return m_sThreadName;
  }

  /**
   * <p>
   * Set the name for the <code>{@link QuartzSchedulerThread}</code>.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if name is null or empty.
   */
  public void setThreadName (final String threadName)
  {
    if (threadName == null || threadName.trim ().length () == 0)
    {
      throw new IllegalArgumentException ("Scheduler thread name cannot be empty.");
    }

    m_sThreadName = threadName;
  }

  /**
   * <p>
   * Get the <code>{@link IThreadPool}</code> for the
   * <code>{@link QuartzScheduler}</code> to use.
   * </p>
   */
  public IThreadPool getThreadPool ()
  {
    return m_aThreadPool;
  }

  /**
   * <p>
   * Set the <code>{@link IThreadPool}</code> for the
   * <code>{@link QuartzScheduler}</code> to use.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if threadPool is null.
   */
  public void setThreadPool (final IThreadPool threadPool)
  {
    if (threadPool == null)
    {
      throw new IllegalArgumentException ("ThreadPool cannot be null.");
    }

    m_aThreadPool = threadPool;
  }

  /**
   * <p>
   * Get the <code>{@link IJobStore}</code> for the
   * <code>{@link QuartzScheduler}</code> to use.
   * </p>
   */
  public IJobStore getJobStore ()
  {
    return m_aJobStore;
  }

  /**
   * <p>
   * Set the <code>{@link IJobStore}</code> for the
   * <code>{@link QuartzScheduler}</code> to use.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if jobStore is null.
   */
  public void setJobStore (final IJobStore jobStore)
  {
    if (jobStore == null)
    {
      throw new IllegalArgumentException ("JobStore cannot be null.");
    }

    m_aJobStore = jobStore;
  }

  /**
   * <p>
   * Get the <code>{@link IJobRunShellFactory}</code> for the
   * <code>{@link QuartzScheduler}</code> to use.
   * </p>
   */
  public IJobRunShellFactory getJobRunShellFactory ()
  {
    return m_aJobRunShellFactory;
  }

  /**
   * <p>
   * Set the <code>{@link IJobRunShellFactory}</code> for the
   * <code>{@link QuartzScheduler}</code> to use.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if jobRunShellFactory is null.
   */
  public void setJobRunShellFactory (final IJobRunShellFactory jobRunShellFactory)
  {
    if (jobRunShellFactory == null)
    {
      throw new IllegalArgumentException ("JobRunShellFactory cannot be null.");
    }

    m_aJobRunShellFactory = jobRunShellFactory;
  }

  /**
   * <p>
   * Add the given <code>{@link com.helger.quartz.spi.ISchedulerPlugin}</code>
   * for the <code>{@link QuartzScheduler}</code> to use. This method expects
   * the plugin's "initialize" method to be invoked externally (either before or
   * after this method is called).
   * </p>
   */
  public void addSchedulerPlugin (final ISchedulerPlugin plugin)
  {
    m_aSchedulerPlugins.add (plugin);
  }

  /**
   * <p>
   * Get the <code>List</code> of all
   * <code>{@link com.helger.quartz.spi.ISchedulerPlugin}</code>s for the
   * <code>{@link QuartzScheduler}</code> to use.
   * </p>
   */
  public List <ISchedulerPlugin> getSchedulerPlugins ()
  {
    return m_aSchedulerPlugins;
  }

  /**
   * Get whether to mark the Quartz scheduling thread as daemon.
   *
   * @see Thread#setDaemon(boolean)
   */
  public boolean getMakeSchedulerThreadDaemon ()
  {
    return m_bMakeSchedulerThreadDaemon;
  }

  /**
   * Set whether to mark the Quartz scheduling thread as daemon.
   *
   * @see Thread#setDaemon(boolean)
   */
  public void setMakeSchedulerThreadDaemon (final boolean makeSchedulerThreadDaemon)
  {
    m_bMakeSchedulerThreadDaemon = makeSchedulerThreadDaemon;
  }

  /**
   * Get whether to set the class load context of spawned threads to that of the
   * initializing thread.
   */
  public boolean isThreadsInheritInitializersClassLoadContext ()
  {
    return m_bThreadsInheritInitializersClassLoadContext;
  }

  /**
   * Set whether to set the class load context of spawned threads to that of the
   * initializing thread.
   */
  public void setThreadsInheritInitializersClassLoadContext (final boolean threadsInheritInitializersClassLoadContext)
  {
    m_bThreadsInheritInitializersClassLoadContext = threadsInheritInitializersClassLoadContext;
  }

  /**
   * Get the ThreadExecutor which runs the QuartzSchedulerThread
   */
  public IThreadExecutor getThreadExecutor ()
  {
    return m_aThreadExecutor;
  }

  /**
   * Set the ThreadExecutor which runs the QuartzSchedulerThread
   */
  public void setThreadExecutor (final IThreadExecutor threadExecutor)
  {
    m_aThreadExecutor = threadExecutor;
  }

  public long getBatchTimeWindow ()
  {
    return m_nBatchTimeWindow;
  }

  public void setBatchTimeWindow (final long batchTimeWindow)
  {
    m_nBatchTimeWindow = batchTimeWindow;
  }

  public int getMaxBatchSize ()
  {
    return m_nMaxBatchSize;
  }

  public void setMaxBatchSize (final int maxBatchSize)
  {
    m_nMaxBatchSize = maxBatchSize;
  }

  public boolean isInterruptJobsOnShutdown ()
  {
    return m_bInterruptJobsOnShutdown;
  }

  public void setInterruptJobsOnShutdown (final boolean interruptJobsOnShutdown)
  {
    m_bInterruptJobsOnShutdown = interruptJobsOnShutdown;
  }

  public boolean isInterruptJobsOnShutdownWithWait ()
  {
    return m_bInterruptJobsOnShutdownWithWait;
  }

  public void setInterruptJobsOnShutdownWithWait (final boolean interruptJobsOnShutdownWithWait)
  {
    m_bInterruptJobsOnShutdownWithWait = interruptJobsOnShutdownWithWait;
  }
}
