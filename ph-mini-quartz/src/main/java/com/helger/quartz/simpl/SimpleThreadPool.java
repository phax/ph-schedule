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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.quartz.SchedulerConfigException;
import com.helger.quartz.spi.IThreadPool;

import jakarta.annotation.Nullable;

/**
 * <p>
 * This is class is a simple implementation of a thread pool, based on the
 * <code>{@link com.helger.quartz.spi.IThreadPool}</code> interface.
 * </p>
 * <p>
 * <CODE>Runnable</CODE> objects are sent to the pool with the
 * <code>{@link #runInThread(Runnable)}</code> method, which blocks until a <code>Thread</code>
 * becomes available.
 * </p>
 * <p>
 * The pool has a fixed number of <code>Thread</code>s, and does not grow or shrink based on demand.
 * </p>
 *
 * @author James House
 * @author Juergen Donnerstag
 */
public class SimpleThreadPool implements IThreadPool
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SimpleThreadPool.class);

  private int m_nCount = -1;

  private int m_nPrio = Thread.NORM_PRIORITY;

  private boolean m_bIsShutdown = false;
  private boolean m_bHandoffPending = false;

  private boolean m_bInheritLoader = false;

  private boolean m_bInheritGroup = true;

  private boolean m_bMakeThreadsDaemons = false;

  private ThreadGroup m_aThreadGroup;

  private final Object m_aNextRunnableLock = new Object ();

  private ICommonsList <WorkerThread> m_aWorkers;
  private final ICommonsList <WorkerThread> m_aAvailWorkers = new CommonsArrayList <> ();
  private final ICommonsList <WorkerThread> m_aBusyWorkers = new CommonsArrayList <> ();

  private String m_sThreadNamePrefix;

  private String m_sSchedulerInstanceName;

  /**
   * Create a new (unconfigured) <code>SimpleThreadPool</code>.
   *
   * @see #setThreadCount(int)
   * @see #setThreadPriority(int)
   */
  public SimpleThreadPool ()
  {}

  /**
   * <p>
   * Create a new <code>SimpleThreadPool</code> with the specified number of <code>Thread</code> s
   * that have the given priority.
   * </p>
   *
   * @param threadCount
   *        the number of worker <code>Threads</code> in the pool, must be &gt; 0.
   * @param threadPriority
   *        the thread priority for the worker threads.
   * @see java.lang.Thread
   */
  public SimpleThreadPool (final int threadCount, final int threadPriority)
  {
    setThreadCount (threadCount);
    setThreadPriority (threadPriority);
  }

  public int getPoolSize ()
  {
    return getThreadCount ();
  }

  /**
   * <p>
   * Set the number of worker threads in the pool - has no effect after <code>initialize()</code>
   * has been called.
   * </p>
   */
  public final void setThreadCount (final int nCount)
  {
    m_nCount = nCount;
  }

  /**
   * <p>
   * Get the number of worker threads in the pool.
   * </p>
   */
  public final int getThreadCount ()
  {
    return m_nCount;
  }

  /**
   * <p>
   * Set the thread priority of worker threads in the pool - has no effect after
   * <code>initialize()</code> has been called.
   * </p>
   */
  public final void setThreadPriority (final int prio)
  {
    m_nPrio = prio;
  }

  /**
   * <p>
   * Get the thread priority of worker threads in the pool.
   * </p>
   */
  public final int getThreadPriority ()
  {
    return m_nPrio;
  }

  public final void setThreadNamePrefix (@Nullable final String prfx)
  {
    m_sThreadNamePrefix = prfx;
  }

  @Nullable
  public final String getThreadNamePrefix ()
  {
    return m_sThreadNamePrefix;
  }

  /**
   * @return Returns the threadsInheritContextClassLoaderOfInitializingThread.
   */
  public final boolean isThreadsInheritContextClassLoaderOfInitializingThread ()
  {
    return m_bInheritLoader;
  }

  /**
   * @param inheritLoader
   *        The threadsInheritContextClassLoaderOfInitializingThread to set.
   */
  public final void setThreadsInheritContextClassLoaderOfInitializingThread (final boolean inheritLoader)
  {
    m_bInheritLoader = inheritLoader;
  }

  public final boolean isThreadsInheritGroupOfInitializingThread ()
  {
    return m_bInheritGroup;
  }

  public final void setThreadsInheritGroupOfInitializingThread (final boolean inheritGroup)
  {
    m_bInheritGroup = inheritGroup;
  }

  /**
   * @return Returns the value of makeThreadsDaemons.
   */
  public final boolean isMakeThreadsDaemons ()
  {
    return m_bMakeThreadsDaemons;
  }

  /**
   * @param makeThreadsDaemons
   *        The value of makeThreadsDaemons to set.
   */
  public final void setMakeThreadsDaemons (final boolean makeThreadsDaemons)
  {
    m_bMakeThreadsDaemons = makeThreadsDaemons;
  }

  public void setInstanceId (final String schedInstId)
  {}

  public void setInstanceName (final String schedName)
  {
    m_sSchedulerInstanceName = schedName;
  }

  public void initialize () throws SchedulerConfigException
  {
    // already initialized...
    if (m_aWorkers != null && m_aWorkers.isNotEmpty ())
      return;

    if (m_nCount <= 0)
      throw new SchedulerConfigException ("Thread count must be > 0 but is " + m_nCount);

    if (m_nPrio < Thread.MIN_PRIORITY || m_nPrio > Thread.MAX_PRIORITY)
      throw new SchedulerConfigException ("Thread priority must be <= " +
                                          Thread.MIN_PRIORITY +
                                          " and <= " +
                                          Thread.MAX_PRIORITY);

    if (isThreadsInheritGroupOfInitializingThread ())
    {
      m_aThreadGroup = Thread.currentThread ().getThreadGroup ();
    }
    else
    {
      // follow the threadGroup tree to the root thread group.
      m_aThreadGroup = Thread.currentThread ().getThreadGroup ();
      ThreadGroup aParent = m_aThreadGroup;
      while (!aParent.getName ().equals ("main"))
      {
        m_aThreadGroup = aParent;
        aParent = m_aThreadGroup.getParent ();
        if (aParent == null)
          throw new IllegalStateException ("Failed to resolve thread group with name 'main'");
      }
      m_aThreadGroup = new ThreadGroup (aParent, m_sSchedulerInstanceName + "-SimpleThreadPool");
      if (isMakeThreadsDaemons ())
      {
        // Deprecated since Java 16
        m_aThreadGroup.setDaemon (true);
      }
    }

    if (isThreadsInheritContextClassLoaderOfInitializingThread ())
    {
      LOGGER.info ("Job execution threads will use class loader of thread: " + Thread.currentThread ().getName ());
    }

    for (final WorkerThread aWT : createWorkerThreads (m_nCount))
    {
      aWT.start ();
      m_aAvailWorkers.add (aWT);
    }

    LOGGER.info ("Initialized " + m_nCount + " worker threads");
  }

  protected List <WorkerThread> createWorkerThreads (final int nCreateCount)
  {
    m_aWorkers = new CommonsArrayList <> ();
    for (int i = 1; i <= nCreateCount; ++i)
    {
      String sThreadPrefix = getThreadNamePrefix ();
      if (sThreadPrefix == null)
      {
        sThreadPrefix = m_sSchedulerInstanceName + "_Worker";
      }
      final WorkerThread wt = new WorkerThread (this,
                                                m_aThreadGroup,
                                                sThreadPrefix + "-" + i,
                                                getThreadPriority (),
                                                isMakeThreadsDaemons ());
      if (isThreadsInheritContextClassLoaderOfInitializingThread ())
      {
        wt.setContextClassLoader (Thread.currentThread ().getContextClassLoader ());
      }
      m_aWorkers.add (wt);
    }

    return m_aWorkers;
  }

  /**
   * <p>
   * Terminate any worker threads in this thread group.
   * </p>
   * <p>
   * Jobs currently in progress will complete.
   * </p>
   */
  public void shutdown ()
  {
    shutdown (true);
  }

  /**
   * <p>
   * Terminate any worker threads in this thread group.
   * </p>
   * <p>
   * Jobs currently in progress will complete.
   * </p>
   */
  public void shutdown (final boolean waitForJobsToComplete)
  {
    synchronized (m_aNextRunnableLock)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Shutting down threadpool...");

      m_bIsShutdown = true;

      if (m_aWorkers == null) // case where the pool wasn't even initialize()ed
        return;

      // signal each worker thread to shut down
      Iterator <WorkerThread> workerThreads = m_aWorkers.iterator ();
      while (workerThreads.hasNext ())
      {
        final WorkerThread wt = workerThreads.next ();
        wt.shutdown ();
        m_aAvailWorkers.remove (wt);
      }

      // Give waiting (wait(1000)) worker threads a chance to shut down.
      // Active worker threads will shut down after finishing their
      // current job.
      m_aNextRunnableLock.notifyAll ();

      if (waitForJobsToComplete)
      {

        boolean interrupted = false;
        try
        {
          // wait for hand-off in runInThread to complete...
          while (m_bHandoffPending)
          {
            try
            {
              m_aNextRunnableLock.wait (100);
            }
            catch (final InterruptedException ex)
            {
              interrupted = true;
            }
          }

          // Wait until all worker threads are shut down
          while (m_aBusyWorkers.isNotEmpty ())
          {
            final WorkerThread wt = m_aBusyWorkers.get (0);
            try
            {
              if (LOGGER.isDebugEnabled ())
                LOGGER.debug ("Waiting for thread " + wt.getName () + " to shut down");

              // note: with waiting infinite time the
              // application may appear to 'hang'.
              m_aNextRunnableLock.wait (2000);
            }
            catch (final InterruptedException ex)
            {
              interrupted = true;
            }
          }

          workerThreads = m_aWorkers.iterator ();
          while (workerThreads.hasNext ())
          {
            final WorkerThread wt = workerThreads.next ();
            try
            {
              wt.join ();
              workerThreads.remove ();
            }
            catch (final InterruptedException ex)
            {
              interrupted = true;
            }
          }
        }
        finally
        {
          if (interrupted)
          {
            Thread.currentThread ().interrupt ();
          }
        }

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("No executing jobs remaining, all threads stopped.");
      }
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Shutdown of threadpool complete.");
    }
  }

  /**
   * <p>
   * Run the given <code>Runnable</code> object in the next available <code>Thread</code>. If while
   * waiting the thread pool is asked to shut down, the Runnable is executed immediately within a
   * new additional thread.
   * </p>
   *
   * @param aRunnable
   *        the <code>Runnable</code> to be added.
   */
  public boolean runInThread (final Runnable aRunnable)
  {
    if (aRunnable == null)
      return false;

    synchronized (m_aNextRunnableLock)
    {
      m_bHandoffPending = true;

      // Wait until a worker thread is available
      while (m_aAvailWorkers.isEmpty () && !m_bIsShutdown)
      {
        try
        {
          m_aNextRunnableLock.wait (500);
        }
        catch (final InterruptedException ignore)
        {
          Thread.currentThread ().interrupt ();
        }
      }

      if (!m_bIsShutdown)
      {
        final WorkerThread wt = m_aAvailWorkers.remove (0);
        m_aBusyWorkers.add (wt);
        wt.run (aRunnable);
      }
      else
      {
        // If the thread pool is going down, execute the Runnable
        // within a new additional worker thread (no thread from the pool).
        final WorkerThread wt = new WorkerThread (this,
                                                  m_aThreadGroup,
                                                  "WorkerThread-LastJob",
                                                  m_nPrio,
                                                  isMakeThreadsDaemons (),
                                                  aRunnable);
        m_aBusyWorkers.add (wt);
        m_aWorkers.add (wt);
        wt.start ();
      }
      m_aNextRunnableLock.notifyAll ();
      m_bHandoffPending = false;
    }

    return true;
  }

  public int blockForAvailableThreads ()
  {
    synchronized (m_aNextRunnableLock)
    {

      while ((m_aAvailWorkers.isEmpty () || m_bHandoffPending) && !m_bIsShutdown)
      {
        try
        {
          m_aNextRunnableLock.wait (500);
        }
        catch (final InterruptedException ignore)
        {
          Thread.currentThread ().interrupt ();
        }
      }

      return m_aAvailWorkers.size ();
    }
  }

  protected void makeAvailable (final WorkerThread wt)
  {
    synchronized (m_aNextRunnableLock)
    {
      if (!m_bIsShutdown)
      {
        m_aAvailWorkers.add (wt);
      }
      m_aBusyWorkers.remove (wt);
      m_aNextRunnableLock.notifyAll ();
    }
  }

  protected void clearFromBusyWorkersList (final WorkerThread wt)
  {
    synchronized (m_aNextRunnableLock)
    {
      m_aBusyWorkers.remove (wt);
      m_aNextRunnableLock.notifyAll ();
    }
  }

  /**
   * A Worker loops, waiting to execute tasks.
   */
  final class WorkerThread extends Thread
  {
    private final Object m_aLock = new Object ();
    // A flag that signals the WorkerThread to terminate.
    private final AtomicBoolean m_aCanRun = new AtomicBoolean (true);
    private final SimpleThreadPool m_aSTP;
    private Runnable m_aRunnable;
    private boolean m_bRunOnce = false;

    /*
     * Create a worker thread and start it. Waiting for the next Runnable, executing it, and waiting
     * for the next Runnable, until the shutdown flag is set.
     */
    WorkerThread (final SimpleThreadPool aSTP,
                  final ThreadGroup aThreadGroup,
                  final String sName,
                  final int nPrio,
                  final boolean bIsDaemon)
    {
      this (aSTP, aThreadGroup, sName, nPrio, bIsDaemon, null);
    }

    /*
     * Create a worker thread, start it, execute the runnable and terminate the thread (one time
     * execution).
     */
    WorkerThread (final SimpleThreadPool aSTP,
                  final ThreadGroup aThreadGroup,
                  final String sName,
                  final int nPrio,
                  final boolean bIsDaemon,
                  final Runnable aRunnable)
    {
      super (aThreadGroup, sName);
      m_aSTP = aSTP;
      m_aRunnable = aRunnable;
      if (aRunnable != null)
        m_bRunOnce = true;
      setPriority (nPrio);
      setDaemon (bIsDaemon);
    }

    /**
     * <p>
     * Signal the thread that it should terminate.
     * </p>
     */
    void shutdown ()
    {
      m_aCanRun.set (false);
    }

    public void run (final Runnable aNewRunnable)
    {
      synchronized (m_aLock)
      {
        if (m_aRunnable != null)
          throw new IllegalStateException ("Already running a Runnable!");

        m_aRunnable = aNewRunnable;
        m_aLock.notifyAll ();
      }
    }

    /**
     * <p>
     * Loop, executing targets as they are received.
     * </p>
     */
    @Override
    public void run ()
    {
      boolean bRan = false;

      while (m_aCanRun.get ())
      {
        try
        {
          synchronized (m_aLock)
          {
            while (m_aRunnable == null && m_aCanRun.get ())
            {
              m_aLock.wait (500);
            }

            if (m_aRunnable != null)
            {
              bRan = true;
              m_aRunnable.run ();
            }
          }
        }
        catch (final InterruptedException unblock)
        {
          // do nothing (loop will terminate if shutdown() was called
          try
          {
            LOGGER.error ("Worker thread was interrupt'ed.", unblock);
          }
          catch (final Exception e)
          {
            // ignore to help with a tomcat glitch
          }
          Thread.currentThread ().interrupt ();
        }
        catch (final Exception exceptionInRunnable)
        {
          try
          {
            LOGGER.error ("Error while executing the Runnable: ", exceptionInRunnable);
          }
          catch (final Exception e)
          {
            // ignore to help with a tomcat glitch
          }
        }
        finally
        {
          synchronized (m_aLock)
          {
            m_aRunnable = null;
          }
          // repair the thread in case the runnable mucked it up...
          if (getPriority () != m_aSTP.getThreadPriority ())
          {
            setPriority (m_aSTP.getThreadPriority ());
          }

          if (m_bRunOnce)
          {
            m_aCanRun.set (false);
            clearFromBusyWorkersList (this);
          }
          else
            if (bRan)
            {
              bRan = false;
              makeAvailable (this);
            }
        }
      }

      try
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("WorkerThread is shut down.");
      }
      catch (final Exception e)
      {
        // ignore to help with a tomcat glitch
      }
    }
  }
}
