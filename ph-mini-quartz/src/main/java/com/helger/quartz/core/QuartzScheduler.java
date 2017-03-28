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
package com.helger.quartz.core;

import static com.helger.quartz.TriggerBuilder.newTrigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.lang.PropertiesHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.string.StringHelper;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IInterruptableJob;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.IListenerManager;
import com.helger.quartz.IMatcher;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.ITrigger.ETriggerState;
import com.helger.quartz.ITriggerListener;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.JobKey;
import com.helger.quartz.ObjectAlreadyExistsException;
import com.helger.quartz.SchedulerContext;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SchedulerMetaData;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.UnableToInterruptJobException;
import com.helger.quartz.impl.SchedulerRepository;
import com.helger.quartz.impl.matchers.GroupMatcher;
import com.helger.quartz.listeners.AbstractSchedulerListenerSupport;
import com.helger.quartz.simpl.PropertySettingJobFactory;
import com.helger.quartz.spi.IJobFactory;
import com.helger.quartz.spi.IOperableTrigger;
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
  private static String VERSION_MAJOR = "UNKNOWN";
  private static String VERSION_MINOR = "UNKNOWN";
  private static String VERSION_ITERATION = "UNKNOWN";

  static
  {
    final ICommonsMap <String, String> p = PropertiesHelper.loadProperties (new ClassPathResource ("quartz/quartz-build.properties"));
    if (p != null)
    {
      final String version = p.get ("version");
      if (version != null)
      {
        final String [] versionComponents = StringHelper.getExplodedArray ('.', version);
        VERSION_MAJOR = versionComponents[0];
        if (versionComponents.length > 1)
          VERSION_MINOR = versionComponents[1];
        else
          VERSION_MINOR = "0";
        if (versionComponents.length > 2)
          VERSION_ITERATION = versionComponents[2];
        else
          VERSION_ITERATION = "0";
      }
      else
      {
        LoggerFactory.getLogger (QuartzScheduler.class)
                     .error ("Can't parse Quartz version from quartz-build.properties");
      }
    }
  }

  private final QuartzSchedulerResources resources;
  private final QuartzSchedulerThread schedThread;
  private ThreadGroup threadGroup;
  private final SchedulerContext context = new SchedulerContext ();
  private final IListenerManager listenerManager = new ListenerManager ();
  private final Map <String, IJobListener> internalJobListeners = new HashMap <> (10);
  private final Map <String, ITriggerListener> internalTriggerListeners = new HashMap <> (10);
  private final List <ISchedulerListener> internalSchedulerListeners = new ArrayList <> (10);
  private IJobFactory jobFactory = new PropertySettingJobFactory ();
  ExecutingJobsManager jobMgr = null;
  ErrorLogger errLogger = null;
  private final ISchedulerSignaler signaler;
  private final Random random = RandomHelper.getRandom ();
  private final List <Object> holdToPreventGC = new ArrayList <> (5);
  private boolean signalOnSchedulingChange = true;
  private volatile boolean closed = false;
  private volatile boolean shuttingDown = false;
  private Date initialStart = null;

  private final Logger log = LoggerFactory.getLogger (getClass ());

  /**
   * <p>
   * Create a <code>QuartzScheduler</code> with the given configuration
   * properties.
   * </p>
   *
   * @see QuartzSchedulerResources
   * @throws SchedulerException
   */
  public QuartzScheduler (final QuartzSchedulerResources resources, final long idleWaitTime) throws SchedulerException
  {
    this.resources = resources;
    if (resources.getJobStore () instanceof IJobListener)
    {
      addInternalJobListener ((IJobListener) resources.getJobStore ());
    }

    this.schedThread = new QuartzSchedulerThread (this, resources);
    final IThreadExecutor schedThreadExecutor = resources.getThreadExecutor ();
    schedThreadExecutor.execute (this.schedThread);
    if (idleWaitTime > 0)
    {
      this.schedThread.setIdleWaitTime (idleWaitTime);
    }

    jobMgr = new ExecutingJobsManager ();
    addInternalJobListener (jobMgr);
    errLogger = new ErrorLogger ();
    addInternalSchedulerListener (errLogger);

    signaler = new SchedulerSignaler (this, this.schedThread);

    getLog ().info ("Mini Quartz Scheduler v." + getVersion () + " created.");
  }

  /**
   * @throws SchedulerException
   */
  public void initialize () throws SchedulerException
  {
    getLog ().info ("Scheduler meta-data: " + (new SchedulerMetaData (getSchedulerName (),
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
    return signaler;
  }

  public Logger getLog ()
  {
    return log;
  }

  /**
   * <p>
   * Returns the name of the <code>QuartzScheduler</code>.
   * </p>
   */
  public String getSchedulerName ()
  {
    return resources.getName ();
  }

  /**
   * <p>
   * Returns the instance Id of the <code>QuartzScheduler</code>.
   * </p>
   */
  public String getSchedulerInstanceId ()
  {
    return resources.getInstanceId ();
  }

  /**
   * <p>
   * Returns the name of the thread group for Quartz's main threads.
   * </p>
   */
  public ThreadGroup getSchedulerThreadGroup ()
  {
    if (threadGroup == null)
    {
      threadGroup = new ThreadGroup ("MiniQuartzScheduler:" + getSchedulerName ());
      if (resources.getMakeSchedulerThreadDaemon ())
      {
        threadGroup.setDaemon (true);
      }
    }

    return threadGroup;
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
    return context;
  }

  public boolean isSignalOnSchedulingChange ()
  {
    return signalOnSchedulingChange;
  }

  public void setSignalOnSchedulingChange (final boolean signalOnSchedulingChange)
  {
    this.signalOnSchedulingChange = signalOnSchedulingChange;
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

    if (shuttingDown || closed)
    {
      throw new SchedulerException ("The Scheduler cannot be restarted after shutdown() has been called.");
    }

    // QTZ-212 : calling new schedulerStarting() method on the listeners
    // right after entering start()
    notifySchedulerListenersStarting ();

    if (initialStart == null)
    {
      initialStart = new Date ();
      this.resources.getJobStore ().schedulerStarted ();
      _startPlugins ();
    }
    else
    {
      resources.getJobStore ().schedulerResumed ();
    }

    schedThread.togglePause (false);

    getLog ().info ("Scheduler " + resources.getUniqueIdentifier () + " started.");

    notifySchedulerListenersStarted ();
  }

  public void startDelayed (final int seconds) throws SchedulerException
  {
    if (shuttingDown || closed)
    {
      throw new SchedulerException ("The Scheduler cannot be restarted after shutdown() has been called.");
    }

    final Thread t = new Thread ( () -> {
      try
      {
        Thread.sleep (seconds * 1000L);
      }
      catch (final InterruptedException ignore)
      {}
      try
      {
        start ();
      }
      catch (final SchedulerException se)
      {
        getLog ().error ("Unable to start secheduler after startup delay.", se);
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
    resources.getJobStore ().schedulerPaused ();
    schedThread.togglePause (true);
    getLog ().info ("Scheduler " + resources.getUniqueIdentifier () + " paused.");
    notifySchedulerListenersInStandbyMode ();
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> is paused.
   * </p>
   */
  public boolean isInStandbyMode ()
  {
    return schedThread.isPaused ();
  }

  public Date runningSince ()
  {
    if (initialStart == null)
      return null;
    return new Date (initialStart.getTime ());
  }

  public int numJobsExecuted ()
  {
    return jobMgr.getNumJobsFired ();
  }

  public Class <?> getJobStoreClass ()
  {
    return resources.getJobStore ().getClass ();
  }

  public boolean supportsPersistence ()
  {
    return resources.getJobStore ().supportsPersistence ();
  }

  public boolean isClustered ()
  {
    return resources.getJobStore ().isClustered ();
  }

  public Class <?> getThreadPoolClass ()
  {
    return resources.getThreadPool ().getClass ();
  }

  public int getThreadPoolSize ()
  {
    return resources.getThreadPool ().getPoolSize ();
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

    if (shuttingDown || closed)
    {
      return;
    }

    shuttingDown = true;

    getLog ().info ("Scheduler " + resources.getUniqueIdentifier () + " shutting down.");

    standby ();

    schedThread.halt (waitForJobsToComplete);

    notifySchedulerListenersShuttingdown ();

    if ((resources.isInterruptJobsOnShutdown () && !waitForJobsToComplete) ||
        (resources.isInterruptJobsOnShutdownWithWait () && waitForJobsToComplete))
    {
      final List <IJobExecutionContext> jobs = getCurrentlyExecutingJobs ();
      for (final IJobExecutionContext job : jobs)
      {
        if (job.getJobInstance () instanceof IInterruptableJob)
          try
          {
            ((IInterruptableJob) job.getJobInstance ()).interrupt ();
          }
          catch (final Throwable e)
          {
            // do nothing, this was just a courtesy effort
            getLog ().warn ("Encountered error when interrupting job {} during shutdown: {}",
                            job.getJobDetail ().getKey (),
                            e);
          }
      }
    }

    resources.getThreadPool ().shutdown (waitForJobsToComplete);

    closed = true;

    _shutdownPlugins ();

    resources.getJobStore ().shutdown ();

    notifySchedulerListenersShutdown ();

    SchedulerRepository.getInstance ().remove (resources.getName ());

    holdToPreventGC.clear ();

    getLog ().info ("Scheduler " + resources.getUniqueIdentifier () + " shutdown complete.");
  }

  /**
   * <p>
   * Reports whether the <code>Scheduler</code> has been shutdown.
   * </p>
   */
  public boolean isShutdown ()
  {
    return closed;
  }

  public boolean isShuttingDown ()
  {
    return shuttingDown;
  }

  public boolean isStarted ()
  {
    return !shuttingDown && !closed && !isInStandbyMode () && initialStart != null;
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
  public List <IJobExecutionContext> getCurrentlyExecutingJobs ()
  {
    return jobMgr.getExecutingJobs ();
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
    if (log.isDebugEnabled ())
      log.debug ("scheduleJob (" + jobDetail + ", " + trigger + ")");

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
      cal = resources.getJobStore ().retrieveCalendar (trigger.getCalendarName ());
    }
    final Date ft = trig.computeFirstFireTime (cal);

    if (ft == null)
    {
      throw new SchedulerException ("Based on configured schedule, the given trigger '" +
                                    trigger.getKey () +
                                    "' will never fire.");
    }

    resources.getJobStore ().storeJobAndTrigger (jobDetail, trig);
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
      cal = resources.getJobStore ().retrieveCalendar (trigger.getCalendarName ());
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

    resources.getJobStore ().storeTrigger (trig, false);
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

    resources.getJobStore ().storeJob (jobDetail, replace);
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

    final List <? extends ITrigger> triggers = getTriggersOfJob (jobKey);
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

    result = resources.getJobStore ().removeJob (jobKey) || result;
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

    result = resources.getJobStore ().removeJobs (jobKeys);
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
    for (final Entry <IJobDetail, Set <? extends ITrigger>> e : triggersAndJobs.entrySet ())
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
          cal = resources.getJobStore ().retrieveCalendar (trigger.getCalendarName ());
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

    resources.getJobStore ().storeJobsAndTriggers (triggersAndJobs, replace);
    notifySchedulerThread (0L);
    for (final IJobDetail job : triggersAndJobs.keySet ())
      notifySchedulerListenersJobAdded (job);
  }

  public void scheduleJob (final IJobDetail jobDetail,
                           final Set <? extends ITrigger> triggersForJob,
                           final boolean replace) throws SchedulerException
  {
    final Map <IJobDetail, Set <? extends ITrigger>> triggersAndJobs = new HashMap <> ();
    triggersAndJobs.put (jobDetail, triggersForJob);
    scheduleJobs (triggersAndJobs, replace);
  }

  public boolean unscheduleJobs (final List <TriggerKey> triggerKeys) throws SchedulerException
  {
    validateState ();

    boolean result = false;

    result = resources.getJobStore ().removeTriggers (triggerKeys);
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

    if (resources.getJobStore ().removeTrigger (triggerKey))
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
      cal = resources.getJobStore ().retrieveCalendar (newTrigger.getCalendarName ());
    }
    final Date ft = trig.computeFirstFireTime (cal);

    if (ft == null)
    {
      throw new SchedulerException ("Based on configured schedule, the given trigger will never fire.");
    }

    if (resources.getJobStore ().replaceTrigger (triggerKey, trig))
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
    long r = random.nextLong ();
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
        resources.getJobStore ().storeTrigger (trig, false);
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
        resources.getJobStore ().storeTrigger (trig, false);
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

    resources.getJobStore ().pauseTrigger (triggerKey);
    notifySchedulerThread (0L);
    notifySchedulerListenersPausedTrigger (triggerKey);
  }

  private <T extends Key <T>> GroupMatcher <T> _getOrDefault (final GroupMatcher <T> matcher)
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

    final Collection <String> pausedGroups = resources.getJobStore ().pauseTriggers (_getOrDefault (matcher));
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

    resources.getJobStore ().pauseJob (jobKey);
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

    final Collection <String> pausedGroups = resources.getJobStore ().pauseJobs (_getOrDefault (groupMatcher));
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

    resources.getJobStore ().resumeTrigger (triggerKey);
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

    final Collection <String> pausedGroups = resources.getJobStore ().resumeTriggers (_getOrDefault (matcher));
    notifySchedulerThread (0L);
    for (final String pausedGroup : pausedGroups)
    {
      notifySchedulerListenersResumedTriggers (pausedGroup);
    }
  }

  public Set <String> getPausedTriggerGroups () throws SchedulerException
  {
    return resources.getJobStore ().getPausedTriggerGroups ();
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

    resources.getJobStore ().resumeJob (jobKey);
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

    final Collection <String> resumedGroups = resources.getJobStore ().resumeJobs (_getOrDefault (matcher));
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

    resources.getJobStore ().pauseAll ();
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

    resources.getJobStore ().resumeAll ();
    notifySchedulerThread (0L);
    notifySchedulerListenersResumedTrigger (null);
  }

  /**
   * <p>
   * Get the names of all known <code>{@link com.helger.quartz.IJob}</code>
   * groups.
   * </p>
   */
  public List <String> getJobGroupNames () throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().getJobGroupNames ();
  }

  /**
   * <p>
   * Get the names of all the <code>{@link com.helger.quartz.IJob}s</code> in
   * the matching groups.
   * </p>
   */
  public Set <JobKey> getJobKeys (final GroupMatcher <JobKey> matcher) throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().getJobKeys (_getOrDefault (matcher));
  }

  /**
   * <p>
   * Get all <code>{@link ITrigger}</code> s that are associated with the
   * identified <code>{@link com.helger.quartz.IJobDetail}</code>.
   * </p>
   */
  public List <? extends ITrigger> getTriggersOfJob (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().getTriggersForJob (jobKey);
  }

  /**
   * <p>
   * Get the names of all known <code>{@link com.helger.quartz.ITrigger}</code>
   * groups.
   * </p>
   */
  public List <String> getTriggerGroupNames () throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().getTriggerGroupNames ();
  }

  /**
   * <p>
   * Get the names of all the <code>{@link com.helger.quartz.ITrigger}s</code>
   * in the matching groups.
   * </p>
   */
  public Set <TriggerKey> getTriggerKeys (final GroupMatcher <TriggerKey> matcher) throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().getTriggerKeys (_getOrDefault (matcher));
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

    return resources.getJobStore ().retrieveJob (jobKey);
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

    return resources.getJobStore ().retrieveTrigger (triggerKey);
  }

  /**
   * Determine whether a {@link IJob} with the given identifier already exists
   * within the scheduler.
   *
   * @param jobKey
   *        the identifier to check for
   * @return true if a Job exists with the given identifier
   * @throws SchedulerException
   */
  public boolean checkExists (final JobKey jobKey) throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().checkExists (jobKey);

  }

  /**
   * Determine whether a {@link ITrigger} with the given identifier already
   * exists within the scheduler.
   *
   * @param triggerKey
   *        the identifier to check for
   * @return true if a Trigger exists with the given identifier
   * @throws SchedulerException
   */
  public boolean checkExists (final TriggerKey triggerKey) throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().checkExists (triggerKey);

  }

  /**
   * Clears (deletes!) all scheduling data - all {@link IJob}s,
   * {@link ITrigger}s {@link ICalendar}s.
   *
   * @throws SchedulerException
   */
  public void clear () throws SchedulerException
  {
    validateState ();

    resources.getJobStore ().clearAllSchedulingData ();
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

    return resources.getJobStore ().getTriggerState (triggerKey);
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

    resources.getJobStore ().storeCalendar (calName, calendar, replace, updateTriggers);
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

    return resources.getJobStore ().removeCalendar (calName);
  }

  /**
   * <p>
   * Get the <code>{@link ICalendar}</code> instance with the given name.
   * </p>
   */
  public ICalendar getCalendar (final String calName) throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().retrieveCalendar (calName);
  }

  /**
   * <p>
   * Get the names of all registered <code>{@link ICalendar}s</code>.
   * </p>
   */
  public List <String> getCalendarNames () throws SchedulerException
  {
    validateState ();

    return resources.getJobStore ().getCalendarNames ();
  }

  public IListenerManager getListenerManager ()
  {
    return listenerManager;
  }

  /**
   * <p>
   * Add the given <code>{@link com.helger.quartz.IJobListener}</code> to the
   * <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public void addInternalJobListener (final IJobListener jobListener)
  {
    if (jobListener.getName () == null || jobListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("JobListener name cannot be empty.");
    }

    synchronized (internalJobListeners)
    {
      internalJobListeners.put (jobListener.getName (), jobListener);
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
    synchronized (internalJobListeners)
    {
      return (internalJobListeners.remove (name) != null);
    }
  }

  /**
   * <p>
   * Get a List containing all of the
   * <code>{@link com.helger.quartz.IJobListener}</code>s in the
   * <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public List <IJobListener> getInternalJobListeners ()
  {
    synchronized (internalJobListeners)
    {
      return java.util.Collections.unmodifiableList (new LinkedList <> (internalJobListeners.values ()));
    }
  }

  /**
   * <p>
   * Get the <i>internal</i> <code>{@link com.helger.quartz.IJobListener}</code>
   * that has the given name.
   * </p>
   */
  public IJobListener getInternalJobListener (final String name)
  {
    synchronized (internalJobListeners)
    {
      return internalJobListeners.get (name);
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
    if (triggerListener.getName () == null || triggerListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("TriggerListener name cannot be empty.");
    }

    synchronized (internalTriggerListeners)
    {
      internalTriggerListeners.put (triggerListener.getName (), triggerListener);
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
    synchronized (internalTriggerListeners)
    {
      return (internalTriggerListeners.remove (name) != null);
    }
  }

  /**
   * <p>
   * Get a list containing all of the
   * <code>{@link com.helger.quartz.ITriggerListener}</code>s in the
   * <code>Scheduler</code>'s <i>internal</i> list.
   * </p>
   */
  public List <ITriggerListener> getInternalTriggerListeners ()
  {
    synchronized (internalTriggerListeners)
    {
      return java.util.Collections.unmodifiableList (new LinkedList <> (internalTriggerListeners.values ()));
    }
  }

  /**
   * <p>
   * Get the <i>internal</i> <code>{@link ITriggerListener}</code> that has the
   * given name.
   * </p>
   */
  public ITriggerListener getInternalTriggerListener (final String name)
  {
    synchronized (internalTriggerListeners)
    {
      return internalTriggerListeners.get (name);
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
    synchronized (internalSchedulerListeners)
    {
      internalSchedulerListeners.add (schedulerListener);
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
    synchronized (internalSchedulerListeners)
    {
      return internalSchedulerListeners.remove (schedulerListener);
    }
  }

  /**
   * <p>
   * Get a List containing all of the <i>internal</i>
   * <code>{@link ISchedulerListener}</code>s registered with the
   * <code>Scheduler</code>.
   * </p>
   */
  public List <ISchedulerListener> getInternalSchedulerListeners ()
  {
    synchronized (internalSchedulerListeners)
    {
      return java.util.Collections.unmodifiableList (new ArrayList <> (internalSchedulerListeners));
    }
  }

  protected void notifyJobStoreJobComplete (final IOperableTrigger trigger,
                                            final IJobDetail detail,
                                            final ECompletedExecutionInstruction instCode)
  {
    resources.getJobStore ().triggeredJobComplete (trigger, detail, instCode);
  }

  protected void notifyJobStoreJobVetoed (final IOperableTrigger trigger,
                                          final IJobDetail detail,
                                          final ECompletedExecutionInstruction instCode)
  {
    resources.getJobStore ().triggeredJobComplete (trigger, detail, instCode);
  }

  protected void notifySchedulerThread (final long candidateNewNextFireTime)
  {
    if (isSignalOnSchedulingChange ())
    {
      signaler.signalSchedulingChange (candidateNewNextFireTime);
    }
  }

  private List <ITriggerListener> _buildTriggerListenerList ()
  {
    final List <ITriggerListener> allListeners = new LinkedList <> ();
    allListeners.addAll (getListenerManager ().getTriggerListeners ());
    allListeners.addAll (getInternalTriggerListeners ());

    return allListeners;
  }

  private List <IJobListener> _buildJobListenerList ()
  {
    final List <IJobListener> allListeners = new LinkedList <> ();
    allListeners.addAll (getListenerManager ().getJobListeners ());
    allListeners.addAll (getInternalJobListeners ());

    return allListeners;
  }

  private List <ISchedulerListener> _buildSchedulerListenerList ()
  {
    final List <ISchedulerListener> allListeners = new ArrayList <> ();
    allListeners.addAll (getListenerManager ().getSchedulerListeners ());
    allListeners.addAll (getInternalSchedulerListeners ());

    return allListeners;
  }

  private boolean _matchJobListener (final IJobListener listener, final JobKey key)
  {
    final List <IMatcher <JobKey>> matchers = getListenerManager ().getJobListenerMatchers (listener.getName ());
    if (matchers == null)
      return true;
    for (final IMatcher <JobKey> matcher : matchers)
    {
      if (matcher.isMatch (key))
        return true;
    }
    return false;
  }

  private boolean _matchTriggerListener (final ITriggerListener listener, final TriggerKey key)
  {
    final List <IMatcher <TriggerKey>> matchers = getListenerManager ().getTriggerListenerMatchers (listener.getName ());
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
    final List <ITriggerListener> triggerListeners = _buildTriggerListenerList ();

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
        final SchedulerException se = new SchedulerException ("TriggerListener '" +
                                                              tl.getName () +
                                                              "' threw exception: " +
                                                              e.getMessage (),
                                                              e);
        throw se;
      }
    }

    return vetoedExecution;
  }

  public void notifyTriggerListenersMisfired (final ITrigger trigger) throws SchedulerException
  {
    // build a list of all trigger listeners that are to be notified...
    final List <ITriggerListener> triggerListeners = _buildTriggerListenerList ();

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
        final SchedulerException se = new SchedulerException ("TriggerListener '" +
                                                              tl.getName () +
                                                              "' threw exception: " +
                                                              e.getMessage (),
                                                              e);
        throw se;
      }
    }
  }

  public void notifyTriggerListenersComplete (final IJobExecutionContext jec,
                                              final ECompletedExecutionInstruction instCode) throws SchedulerException
  {
    // build a list of all trigger listeners that are to be notified...
    final List <ITriggerListener> triggerListeners = _buildTriggerListenerList ();

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
        final SchedulerException se = new SchedulerException ("TriggerListener '" +
                                                              tl.getName () +
                                                              "' threw exception: " +
                                                              e.getMessage (),
                                                              e);
        throw se;
      }
    }
  }

  public void notifyJobListenersToBeExecuted (final IJobExecutionContext jec) throws SchedulerException
  {
    // build a list of all job listeners that are to be notified...
    final List <IJobListener> jobListeners = _buildJobListenerList ();

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
        final SchedulerException se = new SchedulerException ("JobListener '" +
                                                              jl.getName () +
                                                              "' threw exception: " +
                                                              e.getMessage (),
                                                              e);
        throw se;
      }
    }
  }

  public void notifyJobListenersWasVetoed (final IJobExecutionContext jec) throws SchedulerException
  {
    // build a list of all job listeners that are to be notified...
    final List <IJobListener> jobListeners = _buildJobListenerList ();

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
        final SchedulerException se = new SchedulerException ("JobListener '" +
                                                              jl.getName () +
                                                              "' threw exception: " +
                                                              e.getMessage (),
                                                              e);
        throw se;
      }
    }
  }

  public void notifyJobListenersWasExecuted (final IJobExecutionContext jec,
                                             final JobExecutionException je) throws SchedulerException
  {
    // build a list of all job listeners that are to be notified...
    final List <IJobListener> jobListeners = _buildJobListenerList ();

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
        final SchedulerException se = new SchedulerException ("JobListener '" +
                                                              jl.getName () +
                                                              "' threw exception: " +
                                                              e.getMessage (),
                                                              e);
        throw se;
      }
    }
  }

  public void notifySchedulerListenersError (final String msg, final SchedulerException se)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerError (msg, se);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of error: ", e);
        getLog ().error ("  Original error (for notification) was: " + msg, se);
      }
    }
  }

  public void notifySchedulerListenersSchduled (final ITrigger trigger)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobScheduled (trigger);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of scheduled job." +
                         "  Triger=" +
                         trigger.getKey (),
                         e);
      }
    }
  }

  public void notifySchedulerListenersUnscheduled (final TriggerKey triggerKey)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

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
        getLog ().error ("Error while notifying SchedulerListener of unscheduled job." +
                         "  Triger=" +
                         (triggerKey == null ? "ALL DATA" : triggerKey),
                         e);
      }
    }
  }

  public void notifySchedulerListenersFinalized (final ITrigger trigger)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggerFinalized (trigger);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of finalized trigger." +
                         "  Triger=" +
                         trigger.getKey (),
                         e);
      }
    }
  }

  public void notifySchedulerListenersPausedTrigger (final TriggerKey triggerKey)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggerPaused (triggerKey);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of paused trigger: " + triggerKey, e);
      }
    }
  }

  public void notifySchedulerListenersPausedTriggers (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggersPaused (group);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of paused trigger group." + group, e);
      }
    }
  }

  public void notifySchedulerListenersResumedTrigger (final TriggerKey key)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggerResumed (key);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of resumed trigger: " + key, e);
      }
    }
  }

  public void notifySchedulerListenersResumedTriggers (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.triggersResumed (group);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of resumed group: " + group, e);
      }
    }
  }

  public void notifySchedulerListenersPausedJob (final JobKey key)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobPaused (key);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of paused job: " + key, e);
      }
    }
  }

  public void notifySchedulerListenersPausedJobs (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobsPaused (group);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of paused job group: " + group, e);
      }
    }
  }

  public void notifySchedulerListenersResumedJob (final JobKey key)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobResumed (key);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of resumed job: " + key, e);
      }
    }
  }

  public void notifySchedulerListenersResumedJobs (final String group)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobsResumed (group);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of resumed job group: " + group, e);
      }
    }
  }

  public void notifySchedulerListenersInStandbyMode ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerInStandbyMode ();
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of inStandByMode.", e);
      }
    }
  }

  public void notifySchedulerListenersStarted ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerStarted ();
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of startup.", e);
      }
    }
  }

  public void notifySchedulerListenersStarting ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerStarting ();
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of startup.", e);
      }
    }
  }

  public void notifySchedulerListenersShutdown ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerShutdown ();
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of shutdown.", e);
      }
    }
  }

  public void notifySchedulerListenersShuttingdown ()
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.schedulerShuttingdown ();
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of shutdown.", e);
      }
    }
  }

  public void notifySchedulerListenersJobAdded (final IJobDetail jobDetail)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobAdded (jobDetail);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of JobAdded.", e);
      }
    }
  }

  public void notifySchedulerListenersJobDeleted (final JobKey jobKey)
  {
    // build a list of all scheduler listeners that are to be notified...
    final List <ISchedulerListener> schedListeners = _buildSchedulerListenerList ();

    // notify all scheduler listeners
    for (final ISchedulerListener sl : schedListeners)
    {
      try
      {
        sl.jobDeleted (jobKey);
      }
      catch (final Exception e)
      {
        getLog ().error ("Error while notifying SchedulerListener of JobAdded.", e);
      }
    }
  }

  /**
   * @param factory
   * @throws SchedulerException
   */
  public void setJobFactory (final IJobFactory factory) throws SchedulerException
  {

    if (factory == null)
    {
      throw new IllegalArgumentException ("JobFactory cannot be set to null!");
    }

    getLog ().info ("JobFactory set to: " + factory);

    this.jobFactory = factory;
  }

  public IJobFactory getJobFactory ()
  {
    return jobFactory;
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

    final List <IJobExecutionContext> jobs = getCurrentlyExecutingJobs ();

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
    final List <IJobExecutionContext> jobs = getCurrentlyExecutingJobs ();

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
    resources.getSchedulerPlugins ().forEach (x -> x.shutdown ());
  }

  private void _startPlugins ()
  {
    resources.getSchedulerPlugins ().forEach (x -> x.start ());
  }
}

class ErrorLogger extends AbstractSchedulerListenerSupport
{
  ErrorLogger ()
  {}

  @Override
  public void schedulerError (final String msg, final SchedulerException cause)
  {
    getLog ().error (msg, cause);
  }
}

class ExecutingJobsManager implements IJobListener
{
  private final HashMap <String, IJobExecutionContext> executingJobs = new HashMap <> ();
  private final AtomicInteger numJobsFired = new AtomicInteger (0);

  ExecutingJobsManager ()
  {}

  public String getName ()
  {
    return getClass ().getName ();
  }

  public int getNumJobsCurrentlyExecuting ()
  {
    synchronized (executingJobs)
    {
      return executingJobs.size ();
    }
  }

  public void jobToBeExecuted (final IJobExecutionContext context)
  {
    numJobsFired.incrementAndGet ();

    synchronized (executingJobs)
    {
      executingJobs.put (((IOperableTrigger) context.getTrigger ()).getFireInstanceId (), context);
    }
  }

  public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {
    synchronized (executingJobs)
    {
      executingJobs.remove (((IOperableTrigger) context.getTrigger ()).getFireInstanceId ());
    }
  }

  public int getNumJobsFired ()
  {
    return numJobsFired.get ();
  }

  public List <IJobExecutionContext> getExecutingJobs ()
  {
    synchronized (executingJobs)
    {
      return new CommonsArrayList <> (executingJobs.values ()).getAsUnmodifiable ();
    }
  }

  public void jobExecutionVetoed (final IJobExecutionContext context)
  {}
}
