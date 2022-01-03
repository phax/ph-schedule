/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2022 Philip Helger (www.helger.com)
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

import static com.helger.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsLinkedList;
import com.helger.commons.collection.impl.ICommonsCollection;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.lang.PropertiesHelper;
import com.helger.commons.string.StringHelper;
import com.helger.quartz.*;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.ITrigger.ETriggerState;
import com.helger.quartz.impl.SchedulerRepository;
import com.helger.quartz.impl.matchers.GroupMatcher;
import com.helger.quartz.simpl.PropertySettingJobFactory;
import com.helger.quartz.spi.IJobFactory;
import com.helger.quartz.spi.IOperableTrigger;
import com.helger.quartz.spi.ISchedulerPlugin;
import com.helger.quartz.spi.ISchedulerSignaler;
import com.helger.quartz.spi.IThreadExecutor;
import com.helger.quartz.utils.Key;

/**
 * <p>
 * This is the heart of Quartz, an indirect implementation of the
 * <code>{@link com.helger.quartz.IScheduler}</code> interface, containing
 * methods to schedule <code>{@link com.helger.quartz.IJob}</code>s, register
 * <code>{@link com.helger.quartz.IJobListener}</code> instances, etc.
 * </p>
 *
 * @see com.helger.quartz.IScheduler
 * @see com.helger.quartz.core.QuartzSchedulerThread
 * @see com.helger.quartz.spi.IJobStore
 * @see com.helger.quartz.spi.IThreadPool
 * @author James House
 */
public class QuartzScheduler implements IQuartzScheduler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (QuartzScheduler.class);

  private static final String VERSION_MAJOR;
  private static final String VERSION_MINOR;
  private static final String VERSION_ITERATION;

  static
  {
    String sMajor = "UNKNOWN";
    String sMinor = "UNKNOWN";
    String sIter = "UNKNOWN";

    final ICommonsMap <String, String> p = PropertiesHelper.loadProperties (new ClassPathResource ("quartz/quartz-build.properties"));
    if (p != null)
    {
      final String version = p.get ("version");
      if (version != null)
      {
        final String [] aVersionComponents = StringHelper.getExplodedArray ('.', version);
        sMajor = aVersionComponents[0];
        sMinor = aVersionComponents.length > 1 ? aVersionComponents[1] : "0";
        sIter = aVersionComponents.length > 2 ? aVersionComponents[2] : "0";
      }
      else
      {
        LoggerFactory.getLogger (QuartzScheduler.class)
                     .error ("Can't parse Quartz version from quartz-build.properties");
      }
    }

    VERSION_MAJOR = sMajor;
    VERSION_MINOR = sMinor;
    VERSION_ITERATION = sIter;
  }

  private final QuartzSchedulerResources m_aResources;
  private final QuartzSchedulerThread m_aSchedThread;
  private ThreadGroup m_aThreadGroup;
  private final SchedulerContext m_aContext = new SchedulerContext ();
  private final IListenerManager m_aListenerManager = new ListenerManager ();
  private final ICommonsMap <String, IJobListener> m_aInternalJobListeners = new CommonsHashMap <> (10);
  private final ICommonsMap <String, ITriggerListener> m_aInternalTriggerListeners = new CommonsHashMap <> (10);
  private final ICommonsList <ISchedulerListener> m_aInternalSchedulerListeners = new CommonsArrayList <> (10);
  private IJobFactory m_aJobFactory = new PropertySettingJobFactory ();
  private final ExecutingJobsManager m_aJobMgr;
  private final ErrorLogger m_aErrLogger;
  private final ISchedulerSignaler m_aSignaler;
  private final Random m_aRandom = new Random ();
  private final ICommonsList <Object> holdToPreventGC = new CommonsArrayList <> (5);
  private boolean m_bSignalOnSchedulingChange = true;
  private volatile boolean m_bClosed = false;
  private volatile boolean m_bShuttingDown = false;
  private Date m_aInitialStart;

  /**
   * Create a <code>QuartzScheduler</code> with the given configuration
   * properties.
   *
   * @see QuartzSchedulerResources
   * @throws SchedulerException
   *         On error
   */
  public QuartzScheduler (final QuartzSchedulerResources resources, final long idleWaitTime) throws SchedulerException
  {
    m_aResources = resources;
    if (resources.getJobStore () instanceof IJobListener)
    {
      addInternalJobListener ((IJobListener) resources.getJobStore ());
    }

    m_aSchedThread = new QuartzSchedulerThread (this, resources);
    final IThreadExecutor schedThreadExecutor = resources.getThreadExecutor ();
    schedThreadExecutor.execute (m_aSchedThread);
    if (idleWaitTime > 0)
    {
      m_aSchedThread.setIdleWaitTime (idleWaitTime);
    }

    m_aJobMgr = new ExecutingJobsManager ();
    addInternalJobListener (m_aJobMgr);
    m_aErrLogger = new ErrorLogger ();
    addInternalSchedulerListener (m_aErrLogger);

    m_aSignaler = new SchedulerSignaler (this, m_aSchedThread);

    LOGGER.info ("Mini Quartz Scheduler v." + getVersion () + " created.");
  }

  /**
   * @throws SchedulerException
   *         on error
   */
  public void initialize () throws SchedulerException
  {
    LOGGER.info ("Scheduler meta-data: " +
                 (new SchedulerMetaData (getSchedulerName (),
                                         getSchedulerInstanceId (),
                                         getClass (),
                                         runningSince () != null,
                                         isInStandbyMode (),
                                         isShutdown (),
                                         runningSince (),
                                         numJobsExecuted (),
                                         getJobStoreClass (),
                                         supportsPersistence (),
                                         isClustered (),
                                         getThreadPoolClass (),
                                         getThreadPoolSize (),
                                         getVersion ())).toString ());
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public String getVersion ()
  {
    return getVersionMajor () + "." + getVersionMinor () + "." + getVersionIteration ();
  }

  public static String getVersionMajor ()
  {
    return VERSION_MAJOR;
  }

  public static String getVersionMinor ()
  {
    return VERSION_MINOR;
  }

  public static String getVersionIteration ()
  {
    return VERSION_ITERATION;
  }

  public ISchedulerSignaler getSchedulerSignaler ()
  {
    return m_aSignaler;
  }

  /**
   * <p>
   * Returns the name of the <code>QuartzScheduler</code>.
   * </p>
   */
  public String getSchedulerName ()
  {
    return m_aResources.getName ();
  }

  /**
   * <p>
   * Returns the instance Id of the <code>QuartzScheduler</code>.
   * </p>
   */
  public String getSchedulerInstanceId ()
  {
    return m_aResources.getInstanceId ();
  }

  /**
   * <p>
   * Returns the name of the thread group for Quartz's main threads.
   * </p>
   */
  public ThreadGroup getSchedulerThreadGroup ()
  {
    if (m_aThreadGroup == null)
    {
      m_aThreadGroup = new ThreadGroup ("MiniQuartzScheduler:" + getSchedulerName ());
      if (m_aResources.getMakeSchedulerThreadDaemon ())
      {
        m_aThreadGroup.setDaemon (true);
      }
    }

    return m_aThreadGroup;
  }

  public void addNoGCObject (final Object obj)
  {
    holdToPreventGC.add (obj);
  }

  public boolean removeNoGCObject (final Object obj)
  {
    return holdToPreventGC.remove (obj);
  }

  /**
   * <p>
   * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
   * </p>
   */
  public SchedulerContext getSchedulerContext () throws SchedulerException
  {
    return m_aContext;
  }

  public boolean isSignalOnSchedulingChange ()
  {
    return m_bSignalOnSchedulingChange;
  }

  public void setSignalOnSchedulingChange (final boolean signalOnSchedulingChange)
  {
    m_bSignalOnSchedulingChange = signalOnSchedulingChange;
  }

  ///////////////////////////////////////////////////////////////////////////
  ///
  /// Scheduler State Management Methods
  ///
  ///////////////////////////////////////////////////////////////////////////

  /**
   * <p>
   * Starts the <code>QuartzScheduler</code>'s threads that fire
   * <code>{@link com.helger.quartz.ITrigger}s</code>.
   * </p>
   * <p>
   * All <code>{@link com.helger.quartz.ITrigger}s</code> that have misfired
   * will be passed to the appropriate TriggerListener(s).
   * </p>
   */
  public void start () throws SchedulerException
  {

    if (m_bShuttingDown || m_bClosed)
    {
      throw new SchedulerException ("The Scheduler cannot be restarted after shutdown() has been called.");
    }

    // QTZ-212 : calling new schedulerStarting() method on the listeners
    // right after entering start()
    notifySchedulerListenersStarting ();

    if (m_aInitialStart == null)
    {
      m_aInitialStart = new Date ();
      m_aResources.getJobStore ().schedulerStarted ();
      _startPlugins ();
    }
    else
    {
      m_aResources.getJobStore ().schedulerResumed ();
    }

    m_aSchedThread.togglePause (false);

    LOGGER.info ("Scheduler " + m_aResources.getUniqueIdentifier () + " started.");

    notifySchedulerListenersStarted ();
  }

  public void startDelayed (final int seconds) throws SchedulerException
  {
    if (m_bShuttingDown || m_bClosed)
    {
      throw new SchedulerException ("The Scheduler cannot be restarted after shutdown() has been called.");
    }

    final Thread t = new Thread ( () -> {
      try
      {
        Thread.sleep (seconds * 1000L);
      }
      catch (final InterruptedException ignore)
      {
        Thread.currentThread ().interrupt ();
      }
      try
      {
        start ();
      }
      catch (final SchedulerException se)
      {
        LOGGER.error ("Unable to start secheduler after startup delay.", se);
      }
    });
    t.start ();
  }

  /**
   * <p>
   * Temporarily halts the <code>QuartzScheduler</code>'s firing of
   * <code>{@link com.helger.quartz.ITrigger}s</code>.
   * </p>
   * <p>
   * The scheduler is not destroyed, and can be re-started at any time.
   * </p>
   */
  public void standby ()
  {
    m_aResources.getJobStore ().schedulerPaused ();
    m_aSchedThread.togglePause (true);
    LOGGER.info ("Scheduler " + m_aResources.getUniqueIdentifier () + " paused.");
    notifySchedulerListenersInStandbyMode ();
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> is paused.
   * </p>
   */
  public boolean isInStandbyMode ()
  {
    return m_aSchedThread.isPaused ();
  }

  public Date runningSince ()
  {
    if (m_aInitialStart == null)
      return null;
    return new Date (m_aInitialStart.getTime ());
  }

  public int numJobsExecuted ()
  {
    return m_aJobMgr.getNumJobsFired ();
  }

  public Class <?> getJobStoreClass ()
  {
    return m_aResources.getJobStore ().getClass ();
  }

  public boolean supportsPersistence ()
  {
    return m_aResources.getJobStore ().supportsPersistence ();
  }

  public boolean isClustered ()
  {
    return m_aResources.getJobStore ().isClustered ();
  }

  public Class <?> getThreadPoolClass ()
  {
    return m_aResources.getThreadPool ().getClass ();
  }

  public int getThreadPoolSize ()
  {
    return m_aResources.getThreadPool ().getPoolSize ();
  }

  /**
   * <p>
   * Halts the <code>QuartzScheduler</code>'s firing of
   * <code>{@link com.helger.quartz.ITrigger}s</code>, and cleans up all
   * resources associated with the QuartzScheduler. Equivalent to
   * <code>shutdown(false)</code>.
   * </p>
   * <p>
   * The scheduler cannot be re-started.
   * </p>
   */
  public void shutdown ()
  {
    shutdown (false);
  }

  /**
   * <p>
   * Halts the <code>QuartzScheduler</code>'s firing of
   * <code>{@link com.helger.quartz.ITrigger}s</code>, and cleans up all
   * resources associated with the QuartzScheduler.
   * </p>
   * <p>
   * The scheduler cannot be re-started.
   * </p>
   *
   * @param waitForJobsToComplete
   *        if <code>true</code> the scheduler will not allow this method to
   *        return until all currently executing jobs have completed.
   */
  public void shutdown (final boolean waitForJobsToComplete)
  {
    if (m_bShuttingDown || m_bClosed)
      return;

    m_bShuttingDown = true;

    LOGGER.info ("Scheduler " + m_aResources.getUniqueIdentifier () + " shutting down.");

    standby ();

    m_aSchedThread.halt (waitForJobsToComplete);

    notifySchedulerListenersShuttingdown ();

    if ((m_aResources.isInterruptJobsOnShutdown () && !waitForJobsToComplete) ||
        (m_aResources.isInterruptJobsOnShutdownWithWait () && waitForJobsToComplete))
    {
      final ICommonsList <IJobExecutionContext> jobs = getCurrentlyExecutingJobs ();
      for (final IJobExecutionContext job : jobs)
      {
        if (job.getJobInstance () instanceof IInterruptableJob)
          try
          {
            ((IInterruptableJob) job.getJobInstance ()).interrupt ();
          }
          catch (final Exception e)
          {
            // do nothing, this was just a courtesy effort
            LOGGER.warn ("Encountered error when interrupting job " +
                         job.getJobDetail ().getKey () +
                         " during shutdown",
                         e);
          }
      }
    }

    m_aResources.getThreadPool ().shutdown (waitForJobsToComplete);

    m_bClosed = true;

    _shutdownPlugins ();

    m_aResources.getJobStore ().shutdown ();

    notifySchedulerListenersShutdown ();

    SchedulerRepository.getInstance ().remove (m_aResources.getName ());

    holdToPreventGC.clear ();

    LOGGER.info ("Scheduler " + m_aResources.getUniqueIdentifier () + " shutdown complete.");
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> has been shutdown.
   * </p>
   */
  public boolean isShutdown ()
  {
    return m_bClosed;
  }

  public boolean isShuttingDown ()
  {
    return m_bShuttingDown;
  }

  public boolean isStarted ()
  {
    return !m_bShuttingDown && !m_bClosed && !isInStandbyMode () && m_aInitialStart != null;
  }

  public void validateState () throws SchedulerException
  {
    if (isShutdown ())
    {
      throw new SchedulerException ("The Scheduler has been shutdown.");
    }

    // other conditions to check (?)
  }

  /**
   * <p>
   * Return a list of <code>JobExecutionContext</code> objects that represent
   * all currently executing Jobs in this Scheduler instance.
   * </p>
   * <p>
   * This method is not cluster aware. That is, it will only return Jobs
   * currently executing in this Scheduler instance, not across the entire
   * cluster.
   * </p>
   * <p>
   * Note that the list returned is an 'instantaneous' snap-shot, and that as
   * soon as it's returned, the true list of executing jobs may be different.
   * </p>
   */
  public ICommonsList <IJobExecutionContext> getCurrentlyExecutingJobs ()
  {
    return m_aJobMgr.getExecutingJobs ();
  }

  ///////////////////////////////////////////////////////////////////////////
  ///
  /// Scheduling-related Methods
  ///
  ///////////////////////////////////////////////////////////////////////////

  /**
   * <p>
   * Add the <code>{@link com.helger.quartz.IJob}</code> identified by the given
   * <code>{@link com.helger.quartz.IJobDetail}</code> to the Scheduler, and
   * associate the given <code>{@link com.helger.quartz.ITrigger}</code> with
   * it.
   * </p>
   * <p>
   * If the given Trigger does not reference any <code>Job</code>, then it will
   * be set to reference the Job passed with it into this method.
   * </p>
   *
   * @throws SchedulerException
   *         if the Job or Trigger cannot be added to the Scheduler, or there is
   *         an internal Scheduler error.
   */
  public Date scheduleJob (final IJobDetail jobDetail, final ITrigger trigger) throws SchedulerException
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("scheduleJob (" + jobDetail + ", " + trigger + ")");

    validateState ();

    if (jobDetail == null)
      throw new SchedulerException ("JobDetail cannot be null");

    if (trigger == null)
      throw new SchedulerException ("Trigger cannot be null");

    if (jobDetail.getKey () == null)
      throw new SchedulerException ("Job's key cannot be null");

    if (jobDetail.getJobClass () == null)
      throw new SchedulerException ("Job's class cannot be null");

    final IOperableTrigger trig = (IOperableTrigger) trigger;

    if (trigger.getJobKey () == null)
    {
      trig.setJobKey (jobDetail.getKey ());
    }
    else
      if (!trigger.getJobKey ().equals (jobDetail.getKey ()))
      {
        throw new SchedulerException ("Trigger does not reference given job!");
      }

    trig.validate ();

    ICalendar cal = null;
    if (trigger.getCalendarName () != null)
    {
      cal = m_aResources.getJobStore ().retrieveCalendar (trigger.getCalendarName ());
    }
    final Date ft = trig.computeFirstFireTime (cal);

    if (ft == null)
    {
      throw new SchedulerException ("Based on configured schedule, the given trigger '" +
                                    trigger.getKey () +
                                    "' will never fire.");
    }

    m_aResources.getJobStore ().storeJobAndTrigger (jobDetail, trig);
    notifySchedulerListenersJobAdded (jobDetail);
    notifySchedulerThread (trigger.getNextFireTime ().getTime ());
    notifySchedulerListenersSchduled (trigger);

    return ft;
  }

  /**
   * <p>
   * Schedule the given <code>{@link com.helger.quartz.ITrigger}</code> with the
   * <code>Job</code> identified by the <code>Trigger</code>'s settings.
   * </p>
   *
   * @throws SchedulerException
   *         if the indicated Job does not exist, or the Trigger cannot be added
   *         to the Scheduler, or there is an internal Scheduler error.
   */
  public Date scheduleJob (final ITrigger trigger) throws SchedulerException
  {
    validateState ();

    if (trigger == null)
    {
      throw new SchedulerException ("Trigger cannot be null");
    }

    final IOperableTrigger trig = (IOperableTrigger) trigger;

    trig.validate ();

    ICalendar cal = null;
    if (trigger.getCalendarName () != null)
    {
      cal = m_aResources.getJobStore ().retrieveCalendar (trigger.getCalendarName ());
      if (cal == null)
      {
        throw new SchedulerException ("Calendar not found: " + trigger.getCalendarName ());
      }
    }
    final Date ft = trig.computeFirstFireTime (cal);

    if (ft == null)
    {
      throw new SchedulerException ("Based on configured schedule, the given trigger '" +
                                    trigger.getKey () +
                                    "' will never fire.");
    }

    m_aResources.getJobStore ().storeTrigger (trig, false);
    notifySchedulerThread (trigger.getNextFireTime ().getTime ());
    notifySchedulerListenersSchduled (trigger);

    return ft;
  }

  /**
   * <p>
   * Add the given <code>Job</code> to the Scheduler - with no associated
   * <code>Trigger</code>. The <code>Job</code> will be 'dormant' until it is
   * scheduled with a <code>Trigger</code>, or
   * <code>Scheduler.triggerJob()</code> is called for it.
   * </p>
   * <p>
   * The <code>Job</code> must by definition be 'durable', if it is not,
   * SchedulerException will be thrown.
   * </p>
   *
   * @throws SchedulerException
   *         if there is an internal Scheduler error, or if the Job is not
   *         durable, or a Job with the same name already exists, and
   *         <code>replace</code> is <code>false</code>.
   */
  public void addJob (final IJobDetail jobDetail, final boolean replace) throws SchedulerException
  {
    addJob (jobDetail, replace, false);
  }

  public void addJob (final IJobDetail jobDetail,
                      final boolean replace,
                      final boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException
  {
    validateState ();

    if (!storeNonDurableWhileAwaitingScheduling && !jobDetail.isDurable ())
    {
      throw new SchedulerException ("Jobs added with no trigger must be durable.");
    }

    m_aResources.getJobStore ().storeJob (jobDetail, replace);
    notifySchedulerThread (0L);
    notifySchedulerListenersJobAdded (jobDetail);
  }

  /**
   * <p>
   * Delete the identified <code>Job</code> from the Scheduler - and any
   * associated <code>Trigger</code>s.
   * </p>
   *
   * @return true if the Job was found and deleted.
   * @throws SchedulerException
   *         if there is an internal Scheduler error.
   */
  public boolean deleteJob (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    boolean result = false;

    final ICommonsList <? extends ITrigger> triggers = getTriggersOfJob (jobKey);
    for (final ITrigger trigger : triggers)
    {
      if (!unscheduleJob (trigger.getKey ()))
      {
        final StringBuilder sb = new StringBuilder ().append ("Unable to unschedule trigger [")
                                                     .append (trigger.getKey ())
                                                     .append ("] while deleting job [")
                                                     .append (jobKey)
                                                     .append ("]");
        throw new SchedulerException (sb.toString ());
      }
      result = true;
    }

    result = m_aResources.getJobStore ().removeJob (jobKey) || result;
    if (result)
    {
      notifySchedulerThread (0L);
      notifySchedulerListenersJobDeleted (jobKey);
    }
    return result;
  }

  public boolean deleteJobs (final List <JobKey> jobKeys) throws SchedulerException
  {
    validateState ();

    boolean result = false;

    result = m_aResources.getJobStore ().removeJobs (jobKeys);
    notifySchedulerThread (0L);
    for (final JobKey key : jobKeys)
      notifySchedulerListenersJobDeleted (key);
    return result;
  }

  public void scheduleJobs (final Map <IJobDetail, Set <? extends ITrigger>> triggersAndJobs,
                            final boolean replace) throws SchedulerException
  {
    validateState ();

    // make sure all triggers refer to their associated job
    for (final Map.Entry <IJobDetail, Set <? extends ITrigger>> e : triggersAndJobs.entrySet ())
    {
      final IJobDetail job = e.getKey ();
      if (job == null)
      {
        // there can be one of these (for adding a bulk set of
        // triggers for pre-existing jobs)
        continue;
      }
      final Set <? extends ITrigger> triggers = e.getValue ();
      if (triggers == null)
      {
        // this is possible because the job may be durable,
        // and not yet be having triggers
        continue;
      }
      for (final ITrigger trigger : triggers)
      {
        final IOperableTrigger opt = (IOperableTrigger) trigger;
        opt.setJobKey (job.getKey ());

        opt.validate ();

        ICalendar cal = null;
        if (trigger.getCalendarName () != null)
        {
          cal = m_aResources.getJobStore ().retrieveCalendar (trigger.getCalendarName ());
          if (cal == null)
          {
            throw new SchedulerException ("Calendar '" +
                                          trigger.getCalendarName () +
                                          "' not found for trigger: " +
                                          trigger.getKey ());
          }
        }
        final Date ft = opt.computeFirstFireTime (cal);

        if (ft == null)
        {
          throw new SchedulerException ("Based on configured schedule, the given trigger will never fire.");
        }
      }
    }

    m_aResources.getJobStore ().storeJobsAndTriggers (triggersAndJobs, replace);
    notifySchedulerThread (0L);
    for (final IJobDetail job : triggersAndJobs.keySet ())
      notifySchedulerListenersJobAdded (job);
  }

  public void scheduleJob (final IJobDetail jobDetail,
                           final Set <? extends ITrigger> triggersForJob,
                           final boolean replace) throws SchedulerException
  {
    final ICommonsMap <IJobDetail, Set <? extends ITrigger>> triggersAndJobs = new CommonsHashMap <> ();
    triggersAndJobs.put (jobDetail, triggersForJob);
    scheduleJobs (triggersAndJobs, replace);
  }

  public boolean unscheduleJobs (final List <TriggerKey> triggerKeys) throws SchedulerException
  {
    validateState ();

    boolean result = false;

    result = m_aResources.getJobStore ().removeTriggers (triggerKeys);
    notifySchedulerThread (0L);
    for (final TriggerKey key : triggerKeys)
      notifySchedulerListenersUnscheduled (key);
    return result;
  }

  /**
   * <p>
   * Remove the indicated <code>{@link com.helger.quartz.ITrigger}</code> from
   * the scheduler.
   * </p>
   */
  public boolean unscheduleJob (final TriggerKey triggerKey) throws SchedulerException
  {
    validateState ();

    if (m_aResources.getJobStore ().removeTrigger (triggerKey))
    {
      notifySchedulerThread (0L);
      notifySchedulerListenersUnscheduled (triggerKey);
    }
    else
    {
      return false;
    }

    return true;
  }

  /**
   * <p>
   * Remove (delete) the <code>{@link com.helger.quartz.ITrigger}</code> with
   * the given name, and store the new given one - which must be associated with
   * the same job.
   * </p>
   *
   * @param newTrigger
   *        The new <code>Trigger</code> to be stored.
   * @return <code>null</code> if a <code>Trigger</code> with the given name
   *         &amp; group was not found and removed from the store, otherwise the
   *         first fire time of the newly scheduled trigger.
   */
  public Date rescheduleJob (final TriggerKey triggerKey, final ITrigger newTrigger) throws SchedulerException
  {
    validateState ();

    if (triggerKey == null)
      throw new IllegalArgumentException ("triggerKey cannot be null");
    if (newTrigger == null)
      throw new IllegalArgumentException ("newTrigger cannot be null");

    final IOperableTrigger trig = (IOperableTrigger) newTrigger;
    final ITrigger oldTrigger = getTrigger (triggerKey);
    if (oldTrigger == null)
    {
      return null;
    }
    trig.setJobKey (oldTrigger.getJobKey ());
    trig.validate ();

    ICalendar cal = null;
    if (newTrigger.getCalendarName () != null)
    {
      cal = m_aResources.getJobStore ().retrieveCalendar (newTrigger.getCalendarName ());
    }
    final Date ft = trig.computeFirstFireTime (cal);

    if (ft == null)
    {
      throw new SchedulerException ("Based on configured schedule, the given trigger will never fire.");
    }

    if (m_aResources.getJobStore ().replaceTrigger (triggerKey, trig))
    {
      notifySchedulerThread (newTrigger.getNextFireTime ().getTime ());
      notifySchedulerListenersUnscheduled (triggerKey);
      notifySchedulerListenersSchduled (newTrigger);
    }
    else
    {
      return null;
    }

    return ft;
  }

  private String _newTriggerId ()
  {
    long r = m_aRandom.nextLong ();
    if (r < 0)
      r = -r;
    return "MT_" + Long.toString (r, 30 + (int) (System.currentTimeMillis () % 7));
  }

  /**
   * <p>
   * Trigger the identified <code>{@link com.helger.quartz.IJob}</code> (execute
   * it now) - with a non-volatile trigger.
   * </p>
   */
  public void triggerJob (final JobKey jobKey, final JobDataMap data) throws SchedulerException
  {
    validateState ();

    final IOperableTrigger trig = (IOperableTrigger) newTrigger ().withIdentity (_newTriggerId (),
                                                                                 IScheduler.DEFAULT_GROUP)
                                                                  .forJob (jobKey)
                                                                  .build ();
    trig.computeFirstFireTime (null);
    if (data != null)
    {
      trig.setJobDataMap (data);
    }

    boolean collision = true;
    while (collision)
    {
      try
      {
        m_aResources.getJobStore ().storeTrigger (trig, false);
        collision = false;
      }
      catch (final ObjectAlreadyExistsException oaee)
      {
        trig.setKey (new TriggerKey (_newTriggerId (), IScheduler.DEFAULT_GROUP));
      }
    }

    notifySchedulerThread (trig.getNextFireTime ().getTime ());
    notifySchedulerListenersSchduled (trig);
  }

  /**
   * <p>
   * Store and schedule the identified
   * <code>{@link com.helger.quartz.spi.IOperableTrigger}</code>
   * </p>
   */
  public void triggerJob (final IOperableTrigger trig) throws SchedulerException
  {
    validateState ();

    trig.computeFirstFireTime (null);

    boolean collision = true;
    while (collision)
    {
      try
      {
        m_aResources.getJobStore ().storeTrigger (trig, false);
        collision = false;
      }
      catch (final ObjectAlreadyExistsException oaee)
      {
        trig.setKey (new TriggerKey (_newTriggerId (), IScheduler.DEFAULT_GROUP));
      }
    }

    notifySchedulerThread (trig.getNextFireTime ().getTime ());
    notifySchedulerListenersSchduled (trig);
  }

  /**
   * <p>
   * Pause the <code>{@link ITrigger}</code> with the given name.
   * </p>
   */
  public void pauseTrigger (final TriggerKey triggerKey) throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().pauseTrigger (triggerKey);
    notifySchedulerThread (0L);
    notifySchedulerListenersPausedTrigger (triggerKey);
  }

  private static <T extends Key <T>> GroupMatcher <T> _getOrDefault (final GroupMatcher <T> matcher)
  {
    return matcher != null ? matcher : GroupMatcher.groupEquals (IScheduler.DEFAULT_GROUP);
  }

  /**
   * <p>
   * Pause all of the <code>{@link ITrigger}s</code> in the matching groups.
   * </p>
   */
  public void pauseTriggers (final GroupMatcher <TriggerKey> matcher) throws SchedulerException
  {
    validateState ();

    final ICommonsCollection <String> pausedGroups = m_aResources.getJobStore ()
                                                                 .pauseTriggers (_getOrDefault (matcher));
    notifySchedulerThread (0L);
    for (final String pausedGroup : pausedGroups)
    {
      notifySchedulerListenersPausedTriggers (pausedGroup);
    }
  }

  /**
   * <p>
   * Pause the <code>{@link com.helger.quartz.IJobDetail}</code> with the given
   * name - by pausing all of its current <code>Trigger</code>s.
   * </p>
   */
  public void pauseJob (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().pauseJob (jobKey);
    notifySchedulerThread (0L);
    notifySchedulerListenersPausedJob (jobKey);
  }

  /**
   * <p>
   * Pause all of the <code>{@link com.helger.quartz.IJobDetail}s</code> in the
   * matching groups - by pausing all of their <code>Trigger</code>s.
   * </p>
   */
  public void pauseJobs (final GroupMatcher <JobKey> groupMatcher) throws SchedulerException
  {
    validateState ();

    final ICommonsCollection <String> pausedGroups = m_aResources.getJobStore ()
                                                                 .pauseJobs (_getOrDefault (groupMatcher));
    notifySchedulerThread (0L);
    for (final String pausedGroup : pausedGroups)
    {
      notifySchedulerListenersPausedJobs (pausedGroup);
    }
  }

  /**
   * <p>
   * Resume (un-pause) the <code>{@link ITrigger}</code> with the given name.
   * </p>
   * <p>
   * If the <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   */
  public void resumeTrigger (final TriggerKey triggerKey) throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().resumeTrigger (triggerKey);
    notifySchedulerThread (0L);
    notifySchedulerListenersResumedTrigger (triggerKey);
  }

  /**
   * <p>
   * Resume (un-pause) all of the <code>{@link ITrigger}s</code> in the matching
   * groups.
   * </p>
   * <p>
   * If any <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   */
  public void resumeTriggers (final GroupMatcher <TriggerKey> matcher) throws SchedulerException
  {
    validateState ();

    final ICommonsCollection <String> pausedGroups = m_aResources.getJobStore ()
                                                                 .resumeTriggers (_getOrDefault (matcher));
    notifySchedulerThread (0L);
    for (final String pausedGroup : pausedGroups)
    {
      notifySchedulerListenersResumedTriggers (pausedGroup);
    }
  }

  public ICommonsSet <String> getPausedTriggerGroups () throws SchedulerException
  {
    return m_aResources.getJobStore ().getPausedTriggerGroups ();
  }

  /**
   * <p>
   * Resume (un-pause) the <code>{@link com.helger.quartz.IJobDetail}</code>
   * with the given name.
   * </p>
   * <p>
   * If any of the <code>Job</code>'s<code>Trigger</code> s missed one or more
   * fire-times, then the <code>Trigger</code>'s misfire instruction will be
   * applied.
   * </p>
   */
  public void resumeJob (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().resumeJob (jobKey);
    notifySchedulerThread (0L);
    notifySchedulerListenersResumedJob (jobKey);
  }

  /**
   * <p>
   * Resume (un-pause) all of the
   * <code>{@link com.helger.quartz.IJobDetail}s</code> in the matching groups.
   * </p>
   * <p>
   * If any of the <code>Job</code> s had <code>Trigger</code> s that missed one
   * or more fire-times, then the <code>Trigger</code>'s misfire instruction
   * will be applied.
   * </p>
   */
  public void resumeJobs (final GroupMatcher <JobKey> matcher) throws SchedulerException
  {
    validateState ();

    final ICommonsCollection <String> resumedGroups = m_aResources.getJobStore ().resumeJobs (_getOrDefault (matcher));
    notifySchedulerThread (0L);
    for (final String pausedGroup : resumedGroups)
    {
      notifySchedulerListenersResumedJobs (pausedGroup);
    }
  }

  /**
   * <p>
   * Pause all triggers - equivalent of calling
   * <code>pauseTriggers(GroupMatcher&lt;TriggerKey&gt;)</code> with a matcher
   * matching all known groups.
   * </p>
   * <p>
   * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
   * instructions WILL be applied.
   * </p>
   *
   * @see #resumeAll()
   * @see #pauseTriggers(GroupMatcher)
   * @see #standby()
   */
  public void pauseAll () throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().pauseAll ();
    notifySchedulerThread (0L);
    notifySchedulerListenersPausedTriggers (null);
  }

  /**
   * <p>
   * Resume (un-pause) all triggers - equivalent of calling
   * <code>resumeTriggerGroup(group)</code> on every group.
   * </p>
   * <p>
   * If any <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   *
   * @see #pauseAll()
   */
  public void resumeAll () throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().resumeAll ();
    notifySchedulerThread (0L);
    notifySchedulerListenersResumedTrigger (null);
  }

  /**
   * <p>
   * Get the names of all known <code>{@link com.helger.quartz.IJob}</code>
   * groups.
   * </p>
   */
  public ICommonsList <String> getJobGroupNames () throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().getJobGroupNames ();
  }

  /**
   * <p>
   * Get the names of all the <code>{@link com.helger.quartz.IJob}s</code> in
   * the matching groups.
   * </p>
   */
  public ICommonsSet <JobKey> getJobKeys (final GroupMatcher <JobKey> matcher) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().getJobKeys (_getOrDefault (matcher));
  }

  /**
   * <p>
   * Get all <code>{@link ITrigger}</code> s that are associated with the
   * identified <code>{@link com.helger.quartz.IJobDetail}</code>.
   * </p>
   */
  public ICommonsList <? extends ITrigger> getTriggersOfJob (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().getTriggersForJob (jobKey);
  }

  /**
   * <p>
   * Get the names of all known <code>{@link com.helger.quartz.ITrigger}</code>
   * groups.
   * </p>
   */
  public ICommonsList <String> getTriggerGroupNames () throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().getTriggerGroupNames ();
  }

  /**
   * <p>
   * Get the names of all the <code>{@link com.helger.quartz.ITrigger}s</code>
   * in the matching groups.
   * </p>
   */
  public ICommonsSet <TriggerKey> getTriggerKeys (final GroupMatcher <TriggerKey> matcher) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().getTriggerKeys (_getOrDefault (matcher));
  }

  /**
   * <p>
   * Get the <code>{@link IJobDetail}</code> for the <code>Job</code> instance
   * with the given name and group.
   * </p>
   */
  public IJobDetail getJobDetail (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().retrieveJob (jobKey);
  }

  /**
   * <p>
   * Get the <code>{@link ITrigger}</code> instance with the given name and
   * group.
   * </p>
   */
  public ITrigger getTrigger (final TriggerKey triggerKey) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().retrieveTrigger (triggerKey);
  }

  /**
   * Determine whether a {@link IJob} with the given identifier already exists
   * within the scheduler.
   *
   * @param jobKey
   *        the identifier to check for
   * @return true if a Job exists with the given identifier
   * @throws SchedulerException
   *         on error
   */
  public boolean checkExists (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().checkExists (jobKey);
  }

  /**
   * Determine whether a {@link ITrigger} with the given identifier already
   * exists within the scheduler.
   *
   * @param triggerKey
   *        the identifier to check for
   * @return true if a Trigger exists with the given identifier
   * @throws SchedulerException
   *         on error
   */
  public boolean checkExists (final TriggerKey triggerKey) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().checkExists (triggerKey);

  }

  /**
   * Clears (deletes!) all scheduling data - all {@link IJob}s,
   * {@link ITrigger}s {@link ICalendar}s.
   *
   * @throws SchedulerException
   *         on error
   */
  public void clear () throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().clearAllSchedulingData ();
    notifySchedulerListenersUnscheduled (null);
  }

  /**
   * <p>
   * Get the current state of the identified <code>{@link ITrigger}</code>.
   * </p>
   * J *
   *
   * @see ETriggerState
   */
  public ETriggerState getTriggerState (final TriggerKey triggerKey) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().getTriggerState (triggerKey);
  }

  /**
   * <p>
   * Add (register) the given <code>Calendar</code> to the Scheduler.
   * </p>
   *
   * @throws SchedulerException
   *         if there is an internal Scheduler error, or a Calendar with the
   *         same name already exists, and <code>replace</code> is
   *         <code>false</code>.
   */
  public void addCalendar (final String calName,
                           final ICalendar calendar,
                           final boolean replace,
                           final boolean updateTriggers) throws SchedulerException
  {
    validateState ();

    m_aResources.getJobStore ().storeCalendar (calName, calendar, replace, updateTriggers);
  }

  /**
   * <p>
   * Delete the identified <code>Calendar</code> from the Scheduler.
   * </p>
   *
   * @return true if the Calendar was found and deleted.
   * @throws SchedulerException
   *         if there is an internal Scheduler error.
   */
  public boolean deleteCalendar (final String calName) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().removeCalendar (calName);
  }

  /**
   * <p>
   * Get the <code>{@link ICalendar}</code> instance with the given name.
   * </p>
   */
  public ICalendar getCalendar (final String calName) throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().retrieveCalendar (calName);
  }

  /**
   * <p>
   * Get the names of all registered <code>{@link ICalendar}s</code>.
   * </p>
   */
  public ICommonsList <String> getCalendarNames () throws SchedulerException
  {
    validateState ();

    return m_aResources.getJobStore ().getCalendarNames ();
  }

  @Nonnull
  public IListenerManager getListenerManager ()
  {
    return m_aListenerManager;
  }

  /**
   * <p>
   * Add the given <code>{@link com.helger.quartz.IJobListener}</code> to the
   * <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public void addInternalJobListener (final IJobListener jobListener)
  {
    ValueEnforcer.notNull (jobListener, "JobListener");
    ValueEnforcer.notEmpty (jobListener.getName (), "JobListener.getName()");

    synchronized (m_aInternalJobListeners)
    {
      m_aInternalJobListeners.put (jobListener.getName (), jobListener);
    }
  }

  /**
   * <p>
   * Remove the identified <code>{@link IJobListener}</code> from the
   * <code>Scheduler</code>'s list of <i>internal</i> listeners.
   * </p>
   *
   * @return true if the identified listener was found in the list, and removed.
   */
  public boolean removeInternalJobListener (final String name)
  {
    synchronized (m_aInternalJobListeners)
    {
      return (m_aInternalJobListeners.remove (name) != null);
    }
  }

  /**
   * <p>
   * Get a List containing all of the
   * <code>{@link com.helger.quartz.IJobListener}</code>s in the
   * <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IJobListener> getInternalJobListeners ()
  {
    synchronized (m_aInternalJobListeners)
    {
      return new CommonsLinkedList <> (m_aInternalJobListeners.values ());
    }
  }

  /**
   * <p>
   * Get the <i>internal</i> <code>{@link com.helger.quartz.IJobListener}</code>
   * that has the given name.
   * </p>
   */
  @Nullable
  public IJobListener getInternalJobListener (final String name)
  {
    synchronized (m_aInternalJobListeners)
    {
      return m_aInternalJobListeners.get (name);
    }
  }

  /**
   * <p>
   * Add the given <code>{@link com.helger.quartz.ITriggerListener}</code> to
   * the <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public void addInternalTriggerListener (final ITriggerListener triggerListener)
  {
    ValueEnforcer.notNull (triggerListener, "TriggerListener");
    ValueEnforcer.notEmpty (triggerListener.getName (), "TriggerListener.getName()");

    synchronized (m_aInternalTriggerListeners)
    {
      m_aInternalTriggerListeners.put (triggerListener.getName (), triggerListener);
    }
  }

  /**
   * <p>
   * Remove the identified <code>{@link ITriggerListener}</code> from the
   * <code>Scheduler</code>'s list of <i>internal</i> listeners.
   * </p>
   *
   * @return true if the identified listener was found in the list, and removed.
   */
  public boolean removeinternalTriggerListener (final String name)
  {
    synchronized (m_aInternalTriggerListeners)
    {
      return m_aInternalTriggerListeners.remove (name) != null;
    }
  }

  /**
   * <p>
   * Get a list containing all of the
   * <code>{@link com.helger.quartz.ITriggerListener}</code>s in the
   * <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ITriggerListener> getInternalTriggerListeners ()
  {
    synchronized (m_aInternalTriggerListeners)
    {
      return new CommonsLinkedList <> (m_aInternalTriggerListeners.values ());
    }
  }

  /**
   * <p>
   * Get the <i>internal</i> <code>{@link ITriggerListener}</code> that has the
   * given name.
   * </p>
   */
  @Nullable
  public ITriggerListener getInternalTriggerListener (final String name)
  {
    synchronized (m_aInternalTriggerListeners)
    {
      return m_aInternalTriggerListeners.get (name);
    }
  }

  /**
   * <p>
   * Register the given <code>{@link ISchedulerListener}</code> with the
   * <code>Scheduler</code>'s list of internal listeners.
   * </p>
   */
  public void addInternalSchedulerListener (final ISchedulerListener schedulerListener)
  {
    synchronized (m_aInternalSchedulerListeners)
    {
      m_aInternalSchedulerListeners.add (schedulerListener);
    }
  }

  /**
   * <p>
   * Remove the given <code>{@link ISchedulerListener}</code> from the
   * <code>Scheduler</code>'s list of internal listeners.
   * </p>
   *
   * @return true if the identified listener was found in the list, and removed.
   */
  public boolean removeInternalSchedulerListener (final ISchedulerListener schedulerListener)
  {
    synchronized (m_aInternalSchedulerListeners)
    {
      return m_aInternalSchedulerListeners.remove (schedulerListener);
    }
  }

  /**
   * <p>
   * Get a List containing all of the <i>internal</i>
   * <code>{@link ISchedulerListener}</code>s registered with the
   * <code>Scheduler</code>.
   * </p>
   */
  public ICommonsList <ISchedulerListener> getInternalSchedulerListeners ()
  {
    synchronized (m_aInternalSchedulerListeners)
    {
      return new CommonsArrayList <> (m_aInternalSchedulerListeners);
    }
  }

  protected void notifyJobStoreJobComplete (final IOperableTrigger trigger,
                                            final IJobDetail detail,
                                            final ECompletedExecutionInstruction instCode)
  {
    m_aResources.getJobStore ().triggeredJobComplete (trigger, detail, instCode);
  }

  protected void notifyJobStoreJobVetoed (final IOperableTrigger trigger,
                                          final IJobDetail detail,
                                          final ECompletedExecutionInstruction instCode)
  {
    m_aResources.getJobStore ().triggeredJobComplete (trigger, detail, instCode);
  }

  protected void notifySchedulerThread (final long candidateNewNextFireTime)
  {
    if (isSignalOnSchedulingChange ())
    {
      m_aSignaler.signalSchedulingChange (candidateNewNextFireTime);
    }
  }

  private ICommonsList <ITriggerListener> _buildTriggerListenerList ()
  {
    final ICommonsList <ITriggerListener> allListeners = new CommonsLinkedList <> ();
    allListeners.addAll (getListenerManager ().getTriggerListeners ());
    allListeners.addAll (getInternalTriggerListeners ());

    return allListeners;
  }

  private ICommonsList <IJobListener> _buildJobListenerList ()
  {
    final ICommonsList <IJobListener> allListeners = new CommonsLinkedList <> ();
    allListeners.addAll (getListenerManager ().getJobListeners ());
    allListeners.addAll (getInternalJobListeners ());

    return allListeners;
  }

  private ICommonsList <ISchedulerListener> _buildSchedulerListenerList ()
  {
    final ICommonsList <ISchedulerListener> allListeners = new CommonsArrayList <> ();
    allListeners.addAll (getListenerManager ().getSchedulerListeners ());
    allListeners.addAll (getInternalSchedulerListeners ());

    return allListeners;
  }

  private boolean _matchJobListener (final IJobListener listener, final JobKey key)
  {
    final ICommonsList <IMatcher <JobKey>> matchers = getListenerManager ().getJobListenerMatchers (listener.getName ());
    if (matchers == null)
      return true;

    for (final IMatcher <JobKey> matcher : matchers)
      if (matcher.isMatch (key))
        return true;
    return false;
  }

  private boolean _matchTriggerListener (final ITriggerListener listener, final TriggerKey key)
  {
    final ICommonsList <IMatcher <TriggerKey>> matchers = getListenerManager ().getTriggerListenerMatchers (listener.getName ());
    if (matchers == null)
      return true;
    for (final IMatcher <TriggerKey> matcher : matchers)
    {
      if (matcher.isMatch (key))
        return true;
    }
    return false;
  }

  public boolean notifyTriggerListenersFired (final IJobExecutionContext jec) throws SchedulerException
  {
    boolean vetoedExecution = false;

    // build a list of all trigger listeners that are to be notified...
    final ICommonsList <ITriggerListener> triggerListeners = _buildTriggerListenerList ();

    // notify all trigger listeners in the list
    for (final ITriggerListener tl : triggerListeners)
    {
      try
      {
        if (!_matchTriggerListener (tl, jec.getTrigger ().getKey ()))
          continue;
        tl.triggerFired (jec.getTrigger (), jec);

        if (tl.vetoJobExecution (jec.getTrigger (), jec))
        {
          vetoedExecution = true;
        }
      }
      catch (final Exception e)
      {
        throw new SchedulerException ("TriggerListener '" + tl.getName () + "' threw exception: " + e.getMessage (), e);
      }
    }

    return vetoedExecution;
  }

  public void notifyTriggerListenersMisfired (final ITrigger trigger) throws SchedulerException
  {
    // build a list of all trigger listeners that are to be notified...
    final ICommonsList <ITriggerListener> triggerListeners = _buildTriggerListenerList ();

    // notify all trigger listeners in the list
    for (final ITriggerListener tl : triggerListeners)
    {
      try
      {
        if (!_matchTriggerListener (tl, trigger.getKey ()))
          continue;
        tl.triggerMisfired (trigger);
      }
      catch (final Exception e)
      {
        throw new SchedulerException ("TriggerListener '" + tl.getName () + "' threw exception: " + e.getMessage (), e);
      }
    }
  }

  public void notifyTriggerListenersComplete (final IJobExecutionContext jec,
                                              final ECompletedExecutionInstruction instCode) throws SchedulerException
  {
    // build a list of all trigger listeners that are to be notified...
    final ICommonsList <ITriggerListener> triggerListeners = _buildTriggerListenerList ();

    // notify all trigger listeners in the list
    for (final ITriggerListener tl : triggerListeners)
    {
      try
      {
        if (!_matchTriggerListener (tl, jec.getTrigger ().getKey ()))
          continue;
        tl.triggerComplete (jec.getTrigger (), jec, instCode);
      }
      catch (final Exception e)
      {
        throw new SchedulerException ("TriggerListener '" + tl.getName () + "' threw exception: " + e.getMessage (), e);
      }
    }
  }

  public void notifyJobListenersToBeExecuted (final IJobExecutionContext jec) throws SchedulerException
  {
    // build a list of all job listeners that are to be notified...
    final ICommonsList <IJobListener> jobListeners = _buildJobListenerList ();

    // notify all job listeners
    for (final IJobListener jl : jobListeners)
    {
      try
      {
        if (!_matchJobListener (jl, jec.getJobDetail ().getKey ()))
          continue;
        jl.jobToBeExecuted (jec);
      }
      catch (final Exception e)
      {
        throw new SchedulerException ("JobListener '" + jl.getName () + "' threw exception: " + e.getMessage (), e);
      }
    }
  }

  public void notifyJobListenersWasVetoed (final IJobExecutionContext jec) throws SchedulerException
  {
    // build a list of all job listeners that are to be notified...
    final ICommonsList <IJobListener> jobListeners = _buildJobListenerList ();

    // notify all job listeners
    for (final IJobListener jl : jobListeners)
    {
      try
      {
        if (!_matchJobListener (jl, jec.getJobDetail ().getKey ()))
          continue;
        jl.jobExecutionVetoed (jec);
      }
      catch (final Exception e)
      {
        throw new SchedulerException ("JobListener '" + jl.getName () + "' threw exception: " + e.getMessage (), e);
      }
    }
  }

  public void notifyJobListenersWasExecuted (final IJobExecutionContext jec,
                                             final JobExecutionException je) throws SchedulerException
  {
    // build a list of all job listeners that are to be notified...
    final ICommonsList <IJobListener> jobListeners = _buildJobListenerList ();

    // notify all job listeners
    for (final IJobListener jl : jobListeners)
    {
      try
      {
        if (!_matchJobListener (jl, jec.getJobDetail ().getKey ()))
          continue;
        jl.jobWasExecuted (jec, je);
      }
      catch (final Exception e)
      {
        throw new SchedulerException ("JobListener '" + jl.getName () + "' threw exception: " + e.getMessage (), e);
      }
    }
  }

  public void notifySchedulerListenersError (final String msg, final SchedulerException se)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerError (msg, se);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of error: ", e);
        LOGGER.error ("  Original error (for notification) was: " + msg, se);
      }
    }
  }

  public void notifySchedulerListenersSchduled (final ITrigger trigger)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobScheduled (trigger);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of scheduled job." + "  Triger=" + trigger.getKey (), e);
      }
    }
  }

  public void notifySchedulerListenersUnscheduled (final TriggerKey triggerKey)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        if (triggerKey == null)
          sl.schedulingDataCleared ();
        else
          sl.jobUnscheduled (triggerKey);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of unscheduled job." +
                      "  Triger=" +
                      (triggerKey == null ? "ALL DATA" : triggerKey),
                      e);
      }
    }
  }

  public void notifySchedulerListenersFinalized (final ITrigger trigger)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggerFinalized (trigger);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of finalized trigger." + "  Triger=" + trigger.getKey (),
                      e);
      }
    }
  }

  public void notifySchedulerListenersPausedTrigger (final TriggerKey triggerKey)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggerPaused (triggerKey);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of paused trigger: " + triggerKey, e);
      }
    }
  }

  public void notifySchedulerListenersPausedTriggers (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggersPaused (group);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of paused trigger group." + group, e);
      }
    }
  }

  public void notifySchedulerListenersResumedTrigger (final TriggerKey key)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggerResumed (key);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of resumed trigger: " + key, e);
      }
    }
  }

  public void notifySchedulerListenersResumedTriggers (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggersResumed (group);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of resumed group: " + group, e);
      }
    }
  }

  public void notifySchedulerListenersPausedJob (final JobKey key)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobPaused (key);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of paused job: " + key, e);
      }
    }
  }

  public void notifySchedulerListenersPausedJobs (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobsPaused (group);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of paused job group: " + group, e);
      }
    }
  }

  public void notifySchedulerListenersResumedJob (final JobKey key)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobResumed (key);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of resumed job: " + key, e);
      }
    }
  }

  public void notifySchedulerListenersResumedJobs (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobsResumed (group);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of resumed job group: " + group, e);
      }
    }
  }

  public void notifySchedulerListenersInStandbyMode ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerInStandbyMode ();
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of inStandByMode.", e);
      }
    }
  }

  public void notifySchedulerListenersStarted ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerStarted ();
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of startup.", e);
      }
    }
  }

  public void notifySchedulerListenersStarting ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerStarting ();
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of startup.", e);
      }
    }
  }

  public void notifySchedulerListenersShutdown ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerShutdown ();
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of shutdown.", e);
      }
    }
  }

  public void notifySchedulerListenersShuttingdown ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerShuttingdown ();
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of shutdown.", e);
      }
    }
  }

  public void notifySchedulerListenersJobAdded (final IJobDetail jobDetail)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobAdded (jobDetail);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of JobAdded.", e);
      }
    }
  }

  public void notifySchedulerListenersJobDeleted (final JobKey jobKey)
  {
    // build a list of all scheduler listeners that are to be notified...
    final ICommonsList <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobDeleted (jobKey);
      }
      catch (final Exception e)
      {
        LOGGER.error ("Error while notifying SchedulerListener of JobAdded.", e);
      }
    }
  }

  /**
   * @param aFactory
   *        Factory. May not be <code>null</code>
   * @throws SchedulerException
   *         on error
   */
  public void setJobFactory (final IJobFactory aFactory) throws SchedulerException
  {
    ValueEnforcer.notNull (aFactory, "JobFactory");
    LOGGER.info ("JobFactory set to: " + aFactory.toString ());
    m_aJobFactory = aFactory;
  }

  public IJobFactory getJobFactory ()
  {
    return m_aJobFactory;
  }

  /**
   * Interrupt all instances of the identified InterruptableJob executing in
   * this Scheduler instance.
   * <p>
   * This method is not cluster aware. That is, it will only interrupt instances
   * of the identified InterruptableJob currently executing in this Scheduler
   * instance, not across the entire cluster.
   * </p>
   *
   * @see com.helger.quartz.core.IQuartzScheduler#interrupt(JobKey)
   */
  public boolean interrupt (final JobKey jobKey) throws UnableToInterruptJobException
  {

    final ICommonsList <IJobExecutionContext> jobs = getCurrentlyExecutingJobs ();

    IJobDetail jobDetail = null;
    IJob job = null;

    boolean interrupted = false;

    for (final IJobExecutionContext jec : jobs)
    {
      jobDetail = jec.getJobDetail ();
      if (jobKey.equals (jobDetail.getKey ()))
      {
        job = jec.getJobInstance ();
        if (job instanceof IInterruptableJob)
        {
          ((IInterruptableJob) job).interrupt ();
          interrupted = true;
        }
        else
        {
          throw new UnableToInterruptJobException ("Job " +
                                                   jobDetail.getKey () +
                                                   " can not be interrupted, since it does not implement " +
                                                   IInterruptableJob.class.getName ());
        }
      }
    }

    return interrupted;
  }

  /**
   * Interrupt the identified InterruptableJob executing in this Scheduler
   * instance.
   * <p>
   * This method is not cluster aware. That is, it will only interrupt instances
   * of the identified InterruptableJob currently executing in this Scheduler
   * instance, not across the entire cluster.
   * </p>
   *
   * @see com.helger.quartz.core.IQuartzScheduler#interrupt(JobKey)
   */
  public boolean interrupt (final String fireInstanceId) throws UnableToInterruptJobException
  {
    final ICommonsList <IJobExecutionContext> jobs = getCurrentlyExecutingJobs ();

    IJob job = null;

    for (final IJobExecutionContext jec : jobs)
    {
      if (jec.getFireInstanceId ().equals (fireInstanceId))
      {
        job = jec.getJobInstance ();
        if (job instanceof IInterruptableJob)
        {
          ((IInterruptableJob) job).interrupt ();
          return true;
        }
        throw new UnableToInterruptJobException ("Job " +
                                                 jec.getJobDetail ().getKey () +
                                                 " can not be interrupted, since it does not implement " +
                                                 IInterruptableJob.class.getName ());
      }
    }

    return false;
  }

  private void _shutdownPlugins ()
  {
    m_aResources.getSchedulerPlugins ().forEach (ISchedulerPlugin::shutdown);
  }

  private void _startPlugins ()
  {
    m_aResources.getSchedulerPlugins ().forEach (ISchedulerPlugin::start);
  }
}

class ErrorLogger implements ISchedulerListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ErrorLogger.class);

  ErrorLogger ()
  {}

  @Override
  public void schedulerError (final String msg, final SchedulerException cause)
  {
    LOGGER.error (msg, cause);
  }
}

class ExecutingJobsManager implements IJobListener
{
  private final ICommonsMap <String, IJobExecutionContext> m_aExecutingJobs = new CommonsHashMap <> ();
  private final AtomicInteger m_aNumJobsFired = new AtomicInteger (0);

  ExecutingJobsManager ()
  {}

  public String getName ()
  {
    return getClass ().getName ();
  }

  public int getNumJobsCurrentlyExecuting ()
  {
    synchronized (m_aExecutingJobs)
    {
      return m_aExecutingJobs.size ();
    }
  }

  @Override
  public void jobToBeExecuted (final IJobExecutionContext context)
  {
    m_aNumJobsFired.incrementAndGet ();

    synchronized (m_aExecutingJobs)
    {
      m_aExecutingJobs.put (((IOperableTrigger) context.getTrigger ()).getFireInstanceId (), context);
    }
  }

  @Override
  public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {
    synchronized (m_aExecutingJobs)
    {
      m_aExecutingJobs.remove (((IOperableTrigger) context.getTrigger ()).getFireInstanceId ());
    }
  }

  public int getNumJobsFired ()
  {
    return m_aNumJobsFired.get ();
  }

  public ICommonsList <IJobExecutionContext> getExecutingJobs ()
  {
    synchronized (m_aExecutingJobs)
    {
      return new CommonsArrayList <> (m_aExecutingJobs.values ());
    }
  }

  @Override
  public void jobExecutionVetoed (final IJobExecutionContext context)
  {}
}
