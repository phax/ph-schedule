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
package com.helger.quartz.simpl;

import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsCollection;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.ITrigger.ETriggerState;
import com.helger.quartz.ITrigger.TriggerTimeComparator;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobKey;
import com.helger.quartz.JobPersistenceException;
import com.helger.quartz.ObjectAlreadyExistsException;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.GroupMatcher;
import com.helger.quartz.impl.matchers.StringMatcher;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.IJobStore;
import com.helger.quartz.spi.IOperableTrigger;
import com.helger.quartz.spi.ISchedulerSignaler;
import com.helger.quartz.spi.TriggerFiredBundle;
import com.helger.quartz.spi.TriggerFiredResult;

/**
 * <p>
 * This class implements a <code>{@link com.helger.quartz.spi.IJobStore}</code>
 * that utilizes RAM as its storage device.
 * </p>
 * <p>
 * As you should know, the ramification of this is that access is extrememly
 * fast, but the data is completely volatile - therefore this
 * <code>JobStore</code> should not be used if true persistence between program
 * shutdowns is required.
 * </p>
 *
 * @author James House
 * @author Sharada Jambula
 * @author Eric Mueller
 */
public class RAMJobStore implements IJobStore
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RAMJobStore.class);

  protected final ICommonsMap <JobKey, JobWrapper> m_aJobsByKey = new CommonsHashMap <> (1000);
  protected final ICommonsMap <TriggerKey, TriggerWrapper> m_aTriggersByKey = new CommonsHashMap <> (1000);
  protected final ICommonsMap <String, ICommonsMap <JobKey, JobWrapper>> m_aJobsByGroup = new CommonsHashMap <> (25);
  protected final ICommonsMap <String, ICommonsMap <TriggerKey, TriggerWrapper>> m_aTriggersByGroup = new CommonsHashMap <> (25);
  protected final ICommonsSortedSet <TriggerWrapper> m_aTimeTriggers = new CommonsTreeSet <> (new TriggerWrapperComparator ());
  protected final ICommonsMap <String, ICalendar> m_aCalendarsByName = new CommonsHashMap <> (25);
  protected final ICommonsList <TriggerWrapper> m_aTriggers = new CommonsArrayList <> (1000);
  protected final Object m_aLock = new Object ();
  protected final ICommonsSet <String> m_aPausedTriggerGroups = new CommonsHashSet <> ();
  protected final ICommonsSet <String> m_aPausedJobGroups = new CommonsHashSet <> ();
  protected final ICommonsSet <JobKey> m_aBlockedJobs = new CommonsHashSet <> ();
  protected long m_nMisfireThreshold = 5000l;
  protected ISchedulerSignaler m_aSignaler;

  /**
   * Create a new <code>RAMJobStore</code>.
   */
  public RAMJobStore ()
  {}

  protected Logger getLog ()
  {
    return LOGGER;
  }

  /**
   * <p>
   * Called by the QuartzScheduler before the <code>JobStore</code> is used, in
   * order to give the it a chance to initialize.
   * </p>
   */
  public void initialize (final IClassLoadHelper loadHelper, final ISchedulerSignaler schedSignaler)
  {
    this.m_aSignaler = schedSignaler;
    getLog ().info ("RAMJobStore initialized.");
  }

  public void schedulerStarted ()
  {
    // nothing to do
  }

  public void schedulerPaused ()
  {
    // nothing to do
  }

  public void schedulerResumed ()
  {
    // nothing to do
  }

  public long getMisfireThreshold ()
  {
    return m_nMisfireThreshold;
  }

  /**
   * The number of milliseconds by which a trigger must have missed its
   * next-fire-time, in order for it to be considered "misfired" and thus have
   * its misfire instruction applied.
   *
   * @param misfireThreshold
   *        the new misfire threshold
   */
  public void setMisfireThreshold (final long misfireThreshold)
  {
    if (misfireThreshold < 1)
    {
      throw new IllegalArgumentException ("Misfire threshold must be larger than 0");
    }
    this.m_nMisfireThreshold = misfireThreshold;
  }

  /**
   * <p>
   * Called by the QuartzScheduler to inform the <code>JobStore</code> that it
   * should free up all of it's resources because the scheduler is shutting
   * down.
   * </p>
   */
  public void shutdown ()
  {}

  public boolean supportsPersistence ()
  {
    return false;
  }

  /**
   * Clear (delete!) all scheduling data - all {@link IJob}s, {@link ITrigger}s
   * {@link ICalendar}s.
   *
   * @throws JobPersistenceException
   *         on error
   */
  public void clearAllSchedulingData () throws JobPersistenceException
  {
    synchronized (m_aLock)
    {
      // unschedule jobs (delete triggers)
      ICommonsList <String> lst = getTriggerGroupNames ();
      for (final String group : lst)
      {
        final ICommonsSet <TriggerKey> keys = getTriggerKeys (GroupMatcher.triggerGroupEquals (group));
        for (final TriggerKey key : keys)
        {
          removeTrigger (key);
        }
      }
      // delete jobs
      lst = getJobGroupNames ();
      for (final String group : lst)
      {
        final ICommonsSet <JobKey> keys = getJobKeys (GroupMatcher.jobGroupEquals (group));
        for (final JobKey key : keys)
        {
          removeJob (key);
        }
      }
      // delete calendars
      lst = getCalendarNames ();
      for (final String name : lst)
      {
        removeCalendar (name);
      }
    }
  }

  /**
   * <p>
   * Store the given <code>{@link com.helger.quartz.IJobDetail}</code> and
   * <code>{@link com.helger.quartz.ITrigger}</code>.
   * </p>
   *
   * @param newJob
   *        The <code>JobDetail</code> to be stored.
   * @param newTrigger
   *        The <code>Trigger</code> to be stored.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Job</code> with the same name/group already exists.
   */
  public void storeJobAndTrigger (final IJobDetail newJob,
                                  final IOperableTrigger newTrigger) throws JobPersistenceException
  {
    storeJob (newJob, false);
    storeTrigger (newTrigger, false);
  }

  /**
   * <p>
   * Store the given <code>{@link com.helger.quartz.IJob}</code>.
   * </p>
   *
   * @param newJob
   *        The <code>Job</code> to be stored.
   * @param bReplaceExisting
   *        If <code>true</code>, any <code>Job</code> existing in the
   *        <code>JobStore</code> with the same name &amp; group should be
   *        over-written.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Job</code> with the same name/group already exists, and
   *         replaceExisting is set to false.
   */
  public void storeJob (final IJobDetail newJob, final boolean bReplaceExisting) throws ObjectAlreadyExistsException
  {
    final JobWrapper jw = new JobWrapper (newJob.clone ());
    boolean bReplace = false;

    synchronized (m_aLock)
    {
      if (m_aJobsByKey.get (jw.m_aKey) != null)
      {
        if (!bReplaceExisting)
          throw new ObjectAlreadyExistsException (newJob);
        bReplace = true;
      }

      if (!bReplace)
      {
        // get job group
        ICommonsMap <JobKey, JobWrapper> aGrpMap = m_aJobsByGroup.get (newJob.getKey ().getGroup ());
        if (aGrpMap == null)
        {
          aGrpMap = new CommonsHashMap <> (100);
          m_aJobsByGroup.put (newJob.getKey ().getGroup (), aGrpMap);
        }
        // add to jobs by group
        aGrpMap.put (newJob.getKey (), jw);
        // add to jobs by FQN map
        m_aJobsByKey.put (jw.m_aKey, jw);
      }
      else
      {
        // update job detail
        final JobWrapper orig = m_aJobsByKey.get (jw.m_aKey);
        orig.m_aJobDetail = jw.m_aJobDetail; // already cloned
      }
    }
  }

  /**
   * <p>
   * Remove (delete) the <code>{@link com.helger.quartz.IJob}</code> with the
   * given name, and any <code>{@link com.helger.quartz.ITrigger}</code> s that
   * reference it.
   * </p>
   *
   * @return <code>true</code> if a <code>Job</code> with the given name &amp;
   *         group was found and removed from the store.
   */
  public boolean removeJob (final JobKey jobKey)
  {

    boolean found = false;

    synchronized (m_aLock)
    {
      final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
      for (final IOperableTrigger trig : triggersOfJob)
      {
        this.removeTrigger (trig.getKey ());
        found = true;
      }

      found = (m_aJobsByKey.remove (jobKey) != null) | found;
      if (found)
      {

        final ICommonsMap <JobKey, JobWrapper> grpMap = m_aJobsByGroup.get (jobKey.getGroup ());
        if (grpMap != null)
        {
          grpMap.remove (jobKey);
          if (grpMap.isEmpty ())
          {
            m_aJobsByGroup.remove (jobKey.getGroup ());
          }
        }
      }
    }

    return found;
  }

  public boolean removeJobs (final List <JobKey> jobKeys) throws JobPersistenceException
  {
    boolean allFound = true;

    synchronized (m_aLock)
    {
      for (final JobKey key : jobKeys)
        allFound = removeJob (key) && allFound;
    }

    return allFound;
  }

  public boolean removeTriggers (final List <TriggerKey> triggerKeys) throws JobPersistenceException
  {
    boolean allFound = true;

    synchronized (m_aLock)
    {
      for (final TriggerKey key : triggerKeys)
        allFound = removeTrigger (key) && allFound;
    }

    return allFound;
  }

  public void storeJobsAndTriggers (final Map <IJobDetail, Set <? extends ITrigger>> triggersAndJobs,
                                    final boolean replace) throws JobPersistenceException
  {

    synchronized (m_aLock)
    {
      // make sure there are no collisions...
      if (!replace)
      {
        for (final Entry <IJobDetail, Set <? extends ITrigger>> e : triggersAndJobs.entrySet ())
        {
          if (checkExists (e.getKey ().getKey ()))
            throw new ObjectAlreadyExistsException (e.getKey ());
          for (final ITrigger trigger : e.getValue ())
          {
            if (checkExists (trigger.getKey ()))
              throw new ObjectAlreadyExistsException (trigger);
          }
        }
      }
      // do bulk add...
      for (final Entry <IJobDetail, Set <? extends ITrigger>> e : triggersAndJobs.entrySet ())
      {
        storeJob (e.getKey (), true);
        for (final ITrigger trigger : e.getValue ())
        {
          storeTrigger ((IOperableTrigger) trigger, true);
        }
      }
    }

  }

  /**
   * <p>
   * Store the given <code>{@link com.helger.quartz.ITrigger}</code>.
   * </p>
   *
   * @param newTrigger
   *        The <code>Trigger</code> to be stored.
   * @param bReplaceExisting
   *        If <code>true</code>, any <code>Trigger</code> existing in the
   *        <code>JobStore</code> with the same name &amp; group should be
   *        over-written.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Trigger</code> with the same name/group already exists,
   *         and replaceExisting is set to false.
   * @see #pauseTriggers(com.helger.quartz.impl.matchers.GroupMatcher)
   */
  public void storeTrigger (final IOperableTrigger newTrigger,
                            final boolean bReplaceExisting) throws JobPersistenceException
  {
    final TriggerWrapper tw = new TriggerWrapper (newTrigger.clone ());

    synchronized (m_aLock)
    {
      if (m_aTriggersByKey.get (tw.m_aKey) != null)
      {
        if (!bReplaceExisting)
        {
          throw new ObjectAlreadyExistsException (newTrigger);
        }

        removeTrigger (newTrigger.getKey (), false);
      }

      if (retrieveJob (newTrigger.getJobKey ()) == null)
      {
        throw new JobPersistenceException ("The job (" +
                                           newTrigger.getJobKey () +
                                           ") referenced by the trigger does not exist.");
      }

      // add to triggers array
      m_aTriggers.add (tw);
      // add to triggers by group
      ICommonsMap <TriggerKey, TriggerWrapper> grpMap = m_aTriggersByGroup.get (newTrigger.getKey ().getGroup ());
      if (grpMap == null)
      {
        grpMap = new CommonsHashMap <> (100);
        m_aTriggersByGroup.put (newTrigger.getKey ().getGroup (), grpMap);
      }
      grpMap.put (newTrigger.getKey (), tw);
      // add to triggers by FQN map
      m_aTriggersByKey.put (tw.m_aKey, tw);

      if (m_aPausedTriggerGroups.contains (newTrigger.getKey ().getGroup ()) ||
          m_aPausedJobGroups.contains (newTrigger.getJobKey ().getGroup ()))
      {
        tw.state = TriggerWrapper.STATE_PAUSED;
        if (m_aBlockedJobs.contains (tw.m_aJobKey))
        {
          tw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
        }
      }
      else
        if (m_aBlockedJobs.contains (tw.m_aJobKey))
        {
          tw.state = TriggerWrapper.STATE_BLOCKED;
        }
        else
        {
          m_aTimeTriggers.add (tw);
        }
    }
  }

  /**
   * <p>
   * Remove (delete) the <code>{@link com.helger.quartz.ITrigger}</code> with
   * the given name.
   * </p>
   *
   * @return <code>true</code> if a <code>Trigger</code> with the given name
   *         &amp; group was found and removed from the store.
   */
  public boolean removeTrigger (final TriggerKey triggerKey)
  {
    return removeTrigger (triggerKey, true);
  }

  private boolean removeTrigger (final TriggerKey key, final boolean removeOrphanedJob)
  {

    boolean found;

    synchronized (m_aLock)
    {
      // remove from triggers by FQN map
      found = (m_aTriggersByKey.remove (key) != null);
      if (found)
      {
        TriggerWrapper tw = null;
        // remove from triggers by group
        final ICommonsMap <TriggerKey, TriggerWrapper> grpMap = m_aTriggersByGroup.get (key.getGroup ());
        if (grpMap != null)
        {
          grpMap.remove (key);
          if (grpMap.isEmpty ())
          {
            m_aTriggersByGroup.remove (key.getGroup ());
          }
        }
        // remove from triggers array
        final Iterator <TriggerWrapper> tgs = m_aTriggers.iterator ();
        while (tgs.hasNext ())
        {
          tw = tgs.next ();
          if (key.equals (tw.m_aKey))
          {
            tgs.remove ();
            break;
          }
        }
        m_aTimeTriggers.remove (tw);

        if (removeOrphanedJob)
        {
          final JobWrapper jw = m_aJobsByKey.get (tw.m_aJobKey);
          final ICommonsList <IOperableTrigger> trigs = getTriggersForJob (tw.m_aJobKey);
          if ((trigs == null || trigs.isEmpty ()) && !jw.m_aJobDetail.isDurable ())
          {
            if (removeJob (jw.m_aKey))
            {
              m_aSignaler.notifySchedulerListenersJobDeleted (jw.m_aKey);
            }
          }
        }
      }
    }

    return found;
  }

  /**
   * @see com.helger.quartz.spi.IJobStore#replaceTrigger(TriggerKey triggerKey,
   *      IOperableTrigger newTrigger)
   */
  public boolean replaceTrigger (final TriggerKey triggerKey,
                                 final IOperableTrigger newTrigger) throws JobPersistenceException
  {

    boolean found;

    synchronized (m_aLock)
    {
      // remove from triggers by FQN map
      TriggerWrapper tw = m_aTriggersByKey.remove (triggerKey);
      found = (tw != null);

      if (found)
      {

        if (!tw.getTrigger ().getJobKey ().equals (newTrigger.getJobKey ()))
        {
          throw new JobPersistenceException ("New trigger is not related to the same job as the old trigger.");
        }

        tw = null;
        // remove from triggers by group
        final ICommonsMap <TriggerKey, TriggerWrapper> grpMap = m_aTriggersByGroup.get (triggerKey.getGroup ());
        if (grpMap != null)
        {
          grpMap.remove (triggerKey);
          if (grpMap.isEmpty ())
          {
            m_aTriggersByGroup.remove (triggerKey.getGroup ());
          }
        }
        // remove from triggers array
        final Iterator <TriggerWrapper> tgs = m_aTriggers.iterator ();
        while (tgs.hasNext ())
        {
          tw = tgs.next ();
          if (triggerKey.equals (tw.m_aKey))
          {
            tgs.remove ();
            break;
          }
        }
        m_aTimeTriggers.remove (tw);

        try
        {
          storeTrigger (newTrigger, false);
        }
        catch (final JobPersistenceException jpe)
        {
          storeTrigger (tw.getTrigger (), false); // put previous trigger
                                                  // back...
          throw jpe;
        }
      }
    }

    return found;
  }

  /**
   * <p>
   * Retrieve the <code>{@link com.helger.quartz.IJobDetail}</code> for the
   * given <code>{@link com.helger.quartz.IJob}</code>.
   * </p>
   *
   * @return The desired <code>Job</code>, or null if there is no match.
   */
  public IJobDetail retrieveJob (final JobKey jobKey)
  {
    synchronized (m_aLock)
    {
      final JobWrapper jw = m_aJobsByKey.get (jobKey);
      return (jw != null) ? (IJobDetail) jw.m_aJobDetail.clone () : null;
    }
  }

  /**
   * <p>
   * Retrieve the given <code>{@link com.helger.quartz.ITrigger}</code>.
   * </p>
   *
   * @return The desired <code>Trigger</code>, or null if there is no match.
   */
  public IOperableTrigger retrieveTrigger (final TriggerKey triggerKey)
  {
    synchronized (m_aLock)
    {
      final TriggerWrapper tw = m_aTriggersByKey.get (triggerKey);

      return (tw != null) ? (IOperableTrigger) tw.getTrigger ().clone () : null;
    }
  }

  /**
   * Determine whether a {@link IJob} with the given identifier already exists
   * within the scheduler.
   *
   * @param jobKey
   *        the identifier to check for
   * @return true if a Job exists with the given identifier
   * @throws JobPersistenceException
   *         on error
   */
  public boolean checkExists (final JobKey jobKey) throws JobPersistenceException
  {
    synchronized (m_aLock)
    {
      final JobWrapper jw = m_aJobsByKey.get (jobKey);
      return jw != null;
    }
  }

  /**
   * Determine whether a {@link ITrigger} with the given identifier already
   * exists within the scheduler.
   *
   * @param triggerKey
   *        the identifier to check for
   * @return true if a Trigger exists with the given identifier
   * @throws JobPersistenceException
   *         on error
   */
  public boolean checkExists (final TriggerKey triggerKey) throws JobPersistenceException
  {
    synchronized (m_aLock)
    {
      final TriggerWrapper tw = m_aTriggersByKey.get (triggerKey);
      return tw != null;
    }
  }

  /**
   * <p>
   * Get the current state of the identified <code>{@link ITrigger}</code>.
   * </p>
   *
   * @see ETriggerState#NORMAL
   * @see ETriggerState#PAUSED
   * @see ETriggerState#COMPLETE
   * @see ETriggerState#ERROR
   * @see ETriggerState#BLOCKED
   * @see ETriggerState#NONE
   */
  public ETriggerState getTriggerState (final TriggerKey triggerKey) throws JobPersistenceException
  {
    synchronized (m_aLock)
    {
      final TriggerWrapper tw = m_aTriggersByKey.get (triggerKey);

      if (tw == null)
      {
        return ETriggerState.NONE;
      }

      if (tw.state == TriggerWrapper.STATE_COMPLETE)
      {
        return ETriggerState.COMPLETE;
      }

      if (tw.state == TriggerWrapper.STATE_PAUSED)
      {
        return ETriggerState.PAUSED;
      }

      if (tw.state == TriggerWrapper.STATE_PAUSED_BLOCKED)
      {
        return ETriggerState.PAUSED;
      }

      if (tw.state == TriggerWrapper.STATE_BLOCKED)
      {
        return ETriggerState.BLOCKED;
      }

      if (tw.state == TriggerWrapper.STATE_ERROR)
      {
        return ETriggerState.ERROR;
      }

      return ETriggerState.NORMAL;
    }
  }

  /**
   * <p>
   * Store the given <code>{@link com.helger.quartz.ICalendar}</code>.
   * </p>
   *
   * @param name
   *        Name
   * @param aCalendar
   *        The {@link ICalendar} to be stored.
   * @param replaceExisting
   *        If <code>true</code>, any <code>Calendar</code> existing in the
   *        <code>JobStore</code> with the same name &amp; group should be
   *        over-written.
   * @param updateTriggers
   *        If <code>true</code>, any <code>Trigger</code>s existing in the
   *        <code>JobStore</code> that reference an existing Calendar with the
   *        same name with have their next fire time re-computed with the new
   *        <code>Calendar</code>.
   * @throws ObjectAlreadyExistsException
   *         if a <code>Calendar</code> with the same name already exists, and
   *         replaceExisting is set to false.
   */
  public void storeCalendar (final String name,
                             final ICalendar aCalendar,
                             final boolean replaceExisting,
                             final boolean updateTriggers) throws ObjectAlreadyExistsException
  {
    final ICalendar calendar = aCalendar.clone ();

    synchronized (m_aLock)
    {
      final Object obj = m_aCalendarsByName.get (name);

      if (obj != null && !replaceExisting)
        throw new ObjectAlreadyExistsException ("Calendar with name '" + name + "' already exists.");

      if (obj != null)
        m_aCalendarsByName.remove (name);
      m_aCalendarsByName.put (name, calendar);

      if (obj != null && updateTriggers)
      {
        for (final TriggerWrapper tw : getTriggerWrappersForCalendar (name))
        {
          final IOperableTrigger trig = tw.getTrigger ();
          final boolean removed = m_aTimeTriggers.remove (tw);

          trig.updateWithNewCalendar (calendar, getMisfireThreshold ());

          if (removed)
          {
            m_aTimeTriggers.add (tw);
          }
        }
      }
    }
  }

  /**
   * <p>
   * Remove (delete) the <code>{@link com.helger.quartz.ICalendar}</code> with
   * the given name.
   * </p>
   * <p>
   * If removal of the <code>Calendar</code> would result in
   * <code>Trigger</code>s pointing to non-existent calendars, then a
   * <code>JobPersistenceException</code> will be thrown.
   * </p>
   * *
   *
   * @param calName
   *        The name of the <code>Calendar</code> to be removed.
   * @return <code>true</code> if a <code>Calendar</code> with the given name
   *         was found and removed from the store.
   */
  public boolean removeCalendar (final String calName) throws JobPersistenceException
  {
    int numRefs = 0;

    synchronized (m_aLock)
    {
      for (final TriggerWrapper trigger : m_aTriggers)
      {
        final IOperableTrigger trigg = trigger.m_aTrigger;
        if (trigg.getCalendarName () != null && trigg.getCalendarName ().equals (calName))
        {
          numRefs++;
        }
      }
    }

    if (numRefs > 0)
    {
      throw new JobPersistenceException ("Calender cannot be removed if it referenced by a Trigger!");
    }

    return (m_aCalendarsByName.remove (calName) != null);
  }

  /**
   * <p>
   * Retrieve the given <code>{@link com.helger.quartz.ITrigger}</code>.
   * </p>
   *
   * @param calName
   *        The name of the <code>Calendar</code> to be retrieved.
   * @return The desired <code>Calendar</code>, or null if there is no match.
   */
  public ICalendar retrieveCalendar (final String calName)
  {
    synchronized (m_aLock)
    {
      final ICalendar cal = m_aCalendarsByName.get (calName);
      if (cal != null)
        return cal.clone ();
      return null;
    }
  }

  /**
   * <p>
   * Get the number of <code>{@link com.helger.quartz.IJobDetail}</code> s that
   * are stored in the <code>JobsStore</code>.
   * </p>
   */
  public int getNumberOfJobs ()
  {
    synchronized (m_aLock)
    {
      return m_aJobsByKey.size ();
    }
  }

  /**
   * <p>
   * Get the number of <code>{@link com.helger.quartz.ITrigger}</code> s that
   * are stored in the <code>JobsStore</code>.
   * </p>
   */
  public int getNumberOfTriggers ()
  {
    synchronized (m_aLock)
    {
      return m_aTriggers.size ();
    }
  }

  /**
   * <p>
   * Get the number of <code>{@link com.helger.quartz.ICalendar}</code> s that
   * are stored in the <code>JobsStore</code>.
   * </p>
   */
  public int getNumberOfCalendars ()
  {
    synchronized (m_aLock)
    {
      return m_aCalendarsByName.size ();
    }
  }

  /**
   * <p>
   * Get the names of all of the <code>{@link com.helger.quartz.IJob}</code> s
   * that match the given groupMatcher.
   * </p>
   */
  public ICommonsSet <JobKey> getJobKeys (final GroupMatcher <JobKey> matcher)
  {
    ICommonsSet <JobKey> outList = null;
    synchronized (m_aLock)
    {

      final StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator ();
      final String compareToValue = matcher.getCompareToValue ();

      switch (operator)
      {
        case EQUALS:
          final ICommonsMap <JobKey, JobWrapper> grpMap = m_aJobsByGroup.get (compareToValue);
          if (grpMap != null)
          {
            outList = new CommonsHashSet <> ();
            for (final JobWrapper jw : grpMap.values ())
              if (jw != null)
                outList.add (jw.m_aJobDetail.getKey ());
          }
          break;

        default:
          for (final Map.Entry <String, ICommonsMap <JobKey, JobWrapper>> entry : m_aJobsByGroup.entrySet ())
          {
            if (operator.evaluate (entry.getKey (), compareToValue) && entry.getValue () != null)
            {
              if (outList == null)
                outList = new CommonsHashSet <> ();

              for (final JobWrapper jobWrapper : entry.getValue ().values ())
                if (jobWrapper != null)
                  outList.add (jobWrapper.m_aJobDetail.getKey ());
            }
          }
      }
    }

    return outList == null ? new CommonsHashSet <> () : outList;
  }

  /**
   * <p>
   * Get the names of all of the
   * <code>{@link com.helger.quartz.ICalendar}</code> s in the
   * <code>JobStore</code>.
   * </p>
   * <p>
   * If there are no Calendars in the given group name, the result should be a
   * zero-length array (not <code>null</code>).
   * </p>
   */
  public ICommonsList <String> getCalendarNames ()
  {
    synchronized (m_aLock)
    {
      return new CommonsArrayList <> (m_aCalendarsByName.keySet ());
    }
  }

  /**
   * <p>
   * Get the names of all of the <code>{@link com.helger.quartz.ITrigger}</code>
   * s that match the given groupMatcher.
   * </p>
   */
  public ICommonsSet <TriggerKey> getTriggerKeys (final GroupMatcher <TriggerKey> matcher)
  {
    ICommonsSet <TriggerKey> outList = null;
    synchronized (m_aLock)
    {

      final StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator ();
      final String compareToValue = matcher.getCompareToValue ();

      switch (operator)
      {
        case EQUALS:
          final ICommonsMap <TriggerKey, TriggerWrapper> grpMap = m_aTriggersByGroup.get (compareToValue);
          if (grpMap != null)
          {
            outList = new CommonsHashSet <> ();

            for (final TriggerWrapper tw : grpMap.values ())
              if (tw != null)
                outList.add (tw.m_aTrigger.getKey ());
          }
          break;

        default:
          for (final Map.Entry <String, ICommonsMap <TriggerKey, TriggerWrapper>> entry : m_aTriggersByGroup.entrySet ())
          {
            if (operator.evaluate (entry.getKey (), compareToValue) && entry.getValue () != null)
            {
              if (outList == null)
                outList = new CommonsHashSet <> ();

              for (final TriggerWrapper triggerWrapper : entry.getValue ().values ())
                if (triggerWrapper != null)
                  outList.add (triggerWrapper.m_aTrigger.getKey ());
            }
          }
      }
    }

    return outList == null ? new CommonsHashSet <> () : outList;
  }

  /**
   * <p>
   * Get the names of all of the <code>{@link com.helger.quartz.IJob}</code>
   * groups.
   * </p>
   */
  public ICommonsList <String> getJobGroupNames ()
  {
    synchronized (m_aLock)
    {
      return new CommonsArrayList <> (m_aJobsByGroup.keySet ());
    }
  }

  /**
   * <p>
   * Get the names of all of the <code>{@link com.helger.quartz.ITrigger}</code>
   * groups.
   * </p>
   */
  public ICommonsList <String> getTriggerGroupNames ()
  {
    synchronized (m_aLock)
    {
      return new CommonsArrayList <> (m_aTriggersByGroup.keySet ());
    }
  }

  /**
   * <p>
   * Get all of the Triggers that are associated to the given Job.
   * </p>
   * <p>
   * If there are no matches, a zero-length array should be returned.
   * </p>
   */
  public ICommonsList <IOperableTrigger> getTriggersForJob (final JobKey jobKey)
  {
    final ICommonsList <IOperableTrigger> trigList = new CommonsArrayList <> ();

    synchronized (m_aLock)
    {
      for (final TriggerWrapper tw : m_aTriggers)
        if (tw.m_aJobKey.equals (jobKey))
          trigList.add (tw.m_aTrigger.clone ());
    }

    return trigList;
  }

  protected ICommonsList <TriggerWrapper> getTriggerWrappersForJob (final JobKey jobKey)
  {
    final ICommonsList <TriggerWrapper> trigList = new CommonsArrayList <> ();

    synchronized (m_aLock)
    {
      for (final TriggerWrapper trigger : m_aTriggers)
        if (trigger.m_aJobKey.equals (jobKey))
          trigList.add (trigger);
    }

    return trigList;
  }

  protected ICommonsList <TriggerWrapper> getTriggerWrappersForCalendar (final String calName)
  {
    final ICommonsList <TriggerWrapper> trigList = new CommonsArrayList <> ();

    synchronized (m_aLock)
    {
      for (final TriggerWrapper tw : m_aTriggers)
      {
        final String tcalName = tw.getTrigger ().getCalendarName ();
        if (tcalName != null && tcalName.equals (calName))
          trigList.add (tw);
      }
    }

    return trigList;
  }

  /**
   * <p>
   * Pause the <code>{@link ITrigger}</code> with the given name.
   * </p>
   */
  public void pauseTrigger (final TriggerKey triggerKey)
  {
    synchronized (m_aLock)
    {
      final TriggerWrapper tw = m_aTriggersByKey.get (triggerKey);

      // does the trigger exist?
      if (tw == null || tw.m_aTrigger == null)
      {
        return;
      }

      // if the trigger is "complete" pausing it does not make sense...
      if (tw.state == TriggerWrapper.STATE_COMPLETE)
      {
        return;
      }

      if (tw.state == TriggerWrapper.STATE_BLOCKED)
      {
        tw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
      }
      else
      {
        tw.state = TriggerWrapper.STATE_PAUSED;
      }

      m_aTimeTriggers.remove (tw);
    }
  }

  /**
   * <p>
   * Pause all of the known <code>{@link ITrigger}s</code> matching.
   * </p>
   * <p>
   * The JobStore should "remember" the groups paused, and impose the pause on
   * any new triggers that are added to one of these groups while the group is
   * paused.
   * </p>
   */
  public ICommonsList <String> pauseTriggers (final GroupMatcher <TriggerKey> matcher)
  {

    ICommonsList <String> pausedGroups;
    synchronized (m_aLock)
    {
      pausedGroups = new CommonsArrayList <> ();

      final StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator ();
      switch (operator)
      {
        case EQUALS:
          if (m_aPausedTriggerGroups.add (matcher.getCompareToValue ()))
          {
            pausedGroups.add (matcher.getCompareToValue ());
          }
          break;
        default:
          for (final String group : m_aTriggersByGroup.keySet ())
          {
            if (operator.evaluate (group, matcher.getCompareToValue ()))
            {
              if (m_aPausedTriggerGroups.add (matcher.getCompareToValue ()))
              {
                pausedGroups.add (group);
              }
            }
          }
      }

      for (final String pausedGroup : pausedGroups)
      {
        final ICommonsSet <TriggerKey> keys = getTriggerKeys (GroupMatcher.triggerGroupEquals (pausedGroup));
        for (final TriggerKey key : keys)
        {
          pauseTrigger (key);
        }
      }
    }

    return pausedGroups;
  }

  /**
   * <p>
   * Pause the <code>{@link com.helger.quartz.IJobDetail}</code> with the given
   * name - by pausing all of its current <code>Trigger</code>s.
   * </p>
   */
  public void pauseJob (final JobKey jobKey)
  {
    synchronized (m_aLock)
    {
      final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
      for (final IOperableTrigger trigger : triggersOfJob)
      {
        pauseTrigger (trigger.getKey ());
      }
    }
  }

  /**
   * <p>
   * Pause all of the <code>{@link com.helger.quartz.IJobDetail}s</code> in the
   * given group - by pausing all of their <code>Trigger</code>s.
   * </p>
   * <p>
   * The JobStore should "remember" that the group is paused, and impose the
   * pause on any new jobs that are added to the group while the group is
   * paused.
   * </p>
   */
  public ICommonsList <String> pauseJobs (final GroupMatcher <JobKey> matcher)
  {
    final ICommonsList <String> pausedGroups = new CommonsArrayList <> ();
    synchronized (m_aLock)
    {
      final StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator ();
      switch (operator)
      {
        case EQUALS:
          if (m_aPausedJobGroups.add (matcher.getCompareToValue ()))
          {
            pausedGroups.add (matcher.getCompareToValue ());
          }
          break;
        default:
          for (final String group : m_aJobsByGroup.keySet ())
          {
            if (operator.evaluate (group, matcher.getCompareToValue ()))
            {
              if (m_aPausedJobGroups.add (group))
              {
                pausedGroups.add (group);
              }
            }
          }
      }

      for (final String groupName : pausedGroups)
      {
        for (final JobKey jobKey : getJobKeys (GroupMatcher.jobGroupEquals (groupName)))
        {
          final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
          for (final IOperableTrigger trigger : triggersOfJob)
          {
            pauseTrigger (trigger.getKey ());
          }
        }
      }
    }

    return pausedGroups;
  }

  /**
   * <p>
   * Resume (un-pause) the <code>{@link ITrigger}</code> with the given key.
   * </p>
   * <p>
   * If the <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   */
  public void resumeTrigger (final TriggerKey triggerKey)
  {
    synchronized (m_aLock)
    {
      final TriggerWrapper tw = m_aTriggersByKey.get (triggerKey);

      // does the trigger exist?
      if (tw == null || tw.m_aTrigger == null)
      {
        return;
      }

      final IOperableTrigger trig = tw.getTrigger ();

      // if the trigger is not paused resuming it does not make sense...
      if (tw.state != TriggerWrapper.STATE_PAUSED && tw.state != TriggerWrapper.STATE_PAUSED_BLOCKED)
      {
        return;
      }

      if (m_aBlockedJobs.contains (trig.getJobKey ()))
        tw.state = TriggerWrapper.STATE_BLOCKED;
      else
        tw.state = TriggerWrapper.STATE_WAITING;

      applyMisfire (tw);

      if (tw.state == TriggerWrapper.STATE_WAITING)
        m_aTimeTriggers.add (tw);
    }
  }

  /**
   * <p>
   * Resume (un-pause) all of the <code>{@link ITrigger}s</code> in the given
   * group.
   * </p>
   * <p>
   * If any <code>Trigger</code> missed one or more fire-times, then the
   * <code>Trigger</code>'s misfire instruction will be applied.
   * </p>
   */
  public ICommonsList <String> resumeTriggers (final GroupMatcher <TriggerKey> matcher)
  {
    final ICommonsSet <String> groups = new CommonsHashSet <> ();

    synchronized (m_aLock)
    {
      final ICommonsSet <TriggerKey> keys = getTriggerKeys (matcher);
      for (final TriggerKey triggerKey : keys)
      {
        groups.add (triggerKey.getGroup ());
        if (m_aTriggersByKey.get (triggerKey) != null)
        {
          final String jobGroup = m_aTriggersByKey.get (triggerKey).m_aJobKey.getGroup ();
          if (m_aPausedJobGroups.contains (jobGroup))
          {
            continue;
          }
        }
        resumeTrigger (triggerKey);
      }
      for (final String group : groups)
      {
        m_aPausedTriggerGroups.remove (group);
      }
    }

    return new CommonsArrayList <> (groups);
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
  public void resumeJob (final JobKey jobKey)
  {
    synchronized (m_aLock)
    {
      final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
      for (final IOperableTrigger trigger : triggersOfJob)
      {
        resumeTrigger (trigger.getKey ());
      }
    }
  }

  /**
   * <p>
   * Resume (un-pause) all of the
   * <code>{@link com.helger.quartz.IJobDetail}s</code> in the given group.
   * </p>
   * <p>
   * If any of the <code>Job</code> s had <code>Trigger</code> s that missed one
   * or more fire-times, then the <code>Trigger</code>'s misfire instruction
   * will be applied.
   * </p>
   */
  public ICommonsCollection <String> resumeJobs (final GroupMatcher <JobKey> matcher)
  {
    final ICommonsSet <String> resumedGroups = new CommonsHashSet <> ();
    synchronized (m_aLock)
    {
      final ICommonsSet <JobKey> keys = getJobKeys (matcher);

      for (final String pausedJobGroup : m_aPausedJobGroups)
        if (matcher.getCompareWithOperator ().evaluate (pausedJobGroup, matcher.getCompareToValue ()))
          resumedGroups.add (pausedJobGroup);

      for (final String resumedGroup : resumedGroups)
        m_aPausedJobGroups.remove (resumedGroup);

      for (final JobKey key : keys)
      {
        final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (key);
        for (final IOperableTrigger trigger : triggersOfJob)
          resumeTrigger (trigger.getKey ());
      }
    }
    return resumedGroups;
  }

  /**
   * <p>
   * Pause all triggers - equivalent of calling
   * <code>pauseTriggerGroup(group)</code> on every group.
   * </p>
   * <p>
   * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
   * instructions WILL be applied.
   * </p>
   *
   * @see #resumeAll()
   * @see #pauseTrigger(com.helger.quartz.TriggerKey)
   * @see #pauseTriggers(com.helger.quartz.impl.matchers.GroupMatcher)
   */
  public void pauseAll ()
  {
    synchronized (m_aLock)
    {
      final ICommonsList <String> names = getTriggerGroupNames ();
      for (final String name : names)
        pauseTriggers (GroupMatcher.triggerGroupEquals (name));
    }
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
  public void resumeAll ()
  {
    synchronized (m_aLock)
    {
      m_aPausedJobGroups.clear ();
      resumeTriggers (GroupMatcher.anyTriggerGroup ());
    }
  }

  protected boolean applyMisfire (final TriggerWrapper tw)
  {
    long misfireTime = System.currentTimeMillis ();
    if (getMisfireThreshold () > 0)
      misfireTime -= getMisfireThreshold ();

    final Date tnft = tw.m_aTrigger.getNextFireTime ();
    if (tnft == null ||
        tnft.getTime () > misfireTime ||
        tw.m_aTrigger.getMisfireInstruction () == ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY)
    {
      return false;
    }

    ICalendar cal = null;
    if (tw.m_aTrigger.getCalendarName () != null)
    {
      cal = retrieveCalendar (tw.m_aTrigger.getCalendarName ());
    }

    m_aSignaler.notifyTriggerListenersMisfired (tw.m_aTrigger.clone ());

    tw.m_aTrigger.updateAfterMisfire (cal);

    if (tw.m_aTrigger.getNextFireTime () == null)
    {
      tw.state = TriggerWrapper.STATE_COMPLETE;
      m_aSignaler.notifySchedulerListenersFinalized (tw.m_aTrigger);
      synchronized (m_aLock)
      {
        m_aTimeTriggers.remove (tw);
      }
    }
    else
      if (tnft.equals (tw.m_aTrigger.getNextFireTime ()))
      {
        return false;
      }

    return true;
  }

  private static final AtomicLong ftrCtr = new AtomicLong (System.currentTimeMillis ());

  protected String getFiredTriggerRecordId ()
  {
    return String.valueOf (ftrCtr.incrementAndGet ());
  }

  /**
   * <p>
   * Get a handle to the next trigger to be fired, and mark it as 'reserved' by
   * the calling scheduler.
   * </p>
   *
   * @see #releaseAcquiredTrigger(IOperableTrigger)
   */
  public ICommonsList <IOperableTrigger> acquireNextTriggers (final long noLaterThan,
                                                              final int maxCount,
                                                              final long timeWindow)
  {
    synchronized (m_aLock)
    {
      final ICommonsList <IOperableTrigger> result = new CommonsArrayList <> ();
      final ICommonsSet <JobKey> acquiredJobKeysForNoConcurrentExec = new CommonsHashSet <> ();
      final ICommonsSet <TriggerWrapper> excludedTriggers = new CommonsHashSet <> ();
      long batchEnd = noLaterThan;

      // return empty list if store has no triggers.
      if (m_aTimeTriggers.isEmpty ())
        return result;

      while (true)
      {
        TriggerWrapper tw;

        try
        {
          tw = m_aTimeTriggers.first ();
          if (tw == null)
            break;
          m_aTimeTriggers.remove (tw);
        }
        catch (final java.util.NoSuchElementException nsee)
        {
          break;
        }

        if (tw.m_aTrigger.getNextFireTime () == null)
        {
          continue;
        }

        if (applyMisfire (tw))
        {
          if (tw.m_aTrigger.getNextFireTime () != null)
          {
            m_aTimeTriggers.add (tw);
          }
          continue;
        }

        if (tw.getTrigger ().getNextFireTime ().getTime () > batchEnd)
        {
          m_aTimeTriggers.add (tw);
          break;
        }

        // If trigger's job is set as @DisallowConcurrentExecution, and it has
        // already been added to result, then
        // put it back into the timeTriggers set and continue to search for next
        // trigger.
        final JobKey jobKey = tw.m_aTrigger.getJobKey ();
        final IJobDetail job = m_aJobsByKey.get (tw.m_aTrigger.getJobKey ()).m_aJobDetail;
        if (job.isConcurrentExectionDisallowed ())
        {
          if (!acquiredJobKeysForNoConcurrentExec.add (jobKey))
          {
            excludedTriggers.add (tw);
            continue; // go to next trigger in store.
          }
        }

        tw.state = TriggerWrapper.STATE_ACQUIRED;
        tw.m_aTrigger.setFireInstanceId (getFiredTriggerRecordId ());
        final IOperableTrigger trig = tw.m_aTrigger.clone ();
        if (result.isEmpty ())
        {
          batchEnd = Math.max (tw.m_aTrigger.getNextFireTime ().getTime (), System.currentTimeMillis ()) + timeWindow;
        }
        result.add (trig);
        if (result.size () == maxCount)
          break;
      }

      // If we did excluded triggers to prevent ACQUIRE state due to
      // DisallowConcurrentExecution, we need to add them back to store.
      if (excludedTriggers.size () > 0)
        m_aTimeTriggers.addAll (excludedTriggers);
      return result;
    }
  }

  /**
   * <p>
   * Inform the <code>JobStore</code> that the scheduler no longer plans to fire
   * the given <code>Trigger</code>, that it had previously acquired (reserved).
   * </p>
   */
  public void releaseAcquiredTrigger (final IOperableTrigger trigger)
  {
    synchronized (m_aLock)
    {
      final TriggerWrapper tw = m_aTriggersByKey.get (trigger.getKey ());
      if (tw != null && tw.state == TriggerWrapper.STATE_ACQUIRED)
      {
        tw.state = TriggerWrapper.STATE_WAITING;
        m_aTimeTriggers.add (tw);
      }
    }
  }

  /**
   * <p>
   * Inform the <code>JobStore</code> that the scheduler is now firing the given
   * <code>Trigger</code> (executing its associated <code>Job</code>), that it
   * had previously acquired (reserved).
   * </p>
   */
  public ICommonsList <TriggerFiredResult> triggersFired (final List <IOperableTrigger> firedTriggers)
  {
    synchronized (m_aLock)
    {
      final ICommonsList <TriggerFiredResult> results = new CommonsArrayList <> ();

      for (final IOperableTrigger trigger : firedTriggers)
      {
        final TriggerWrapper tw = m_aTriggersByKey.get (trigger.getKey ());
        // was the trigger deleted since being acquired?
        if (tw == null || tw.m_aTrigger == null)
        {
          continue;
        }
        // was the trigger completed, paused, blocked, etc. since being
        // acquired?
        if (tw.state != TriggerWrapper.STATE_ACQUIRED)
        {
          continue;
        }

        ICalendar cal = null;
        if (tw.m_aTrigger.getCalendarName () != null)
        {
          cal = retrieveCalendar (tw.m_aTrigger.getCalendarName ());
          if (cal == null)
            continue;
        }
        final Date prevFireTime = trigger.getPreviousFireTime ();
        // in case trigger was replaced between acquiring and firing
        m_aTimeTriggers.remove (tw);
        // call triggered on our copy, and the scheduler's copy
        tw.m_aTrigger.triggered (cal);
        trigger.triggered (cal);
        // tw.state = TriggerWrapper.STATE_EXECUTING;
        tw.state = TriggerWrapper.STATE_WAITING;

        final TriggerFiredBundle bndle = new TriggerFiredBundle (retrieveJob (tw.m_aJobKey),
                                                                 trigger,
                                                                 cal,
                                                                 false,
                                                                 new Date (),
                                                                 trigger.getPreviousFireTime (),
                                                                 prevFireTime,
                                                                 trigger.getNextFireTime ());

        final IJobDetail job = bndle.getJobDetail ();

        if (job.isConcurrentExectionDisallowed ())
        {
          final ICommonsList <TriggerWrapper> trigs = getTriggerWrappersForJob (job.getKey ());
          for (final TriggerWrapper ttw : trigs)
          {
            if (ttw.state == TriggerWrapper.STATE_WAITING)
            {
              ttw.state = TriggerWrapper.STATE_BLOCKED;
            }
            if (ttw.state == TriggerWrapper.STATE_PAUSED)
            {
              ttw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
            }
            m_aTimeTriggers.remove (ttw);
          }
          m_aBlockedJobs.add (job.getKey ());
        }
        else
          if (tw.m_aTrigger.getNextFireTime () != null)
          {
            synchronized (m_aLock)
            {
              m_aTimeTriggers.add (tw);
            }
          }

        results.add (new TriggerFiredResult (bndle));
      }
      return results;
    }
  }

  /**
   * <p>
   * Inform the <code>JobStore</code> that the scheduler has completed the
   * firing of the given <code>Trigger</code> (and the execution its associated
   * <code>Job</code>), and that the
   * <code>{@link com.helger.quartz.JobDataMap}</code> in the given
   * <code>JobDetail</code> should be updated if the <code>Job</code> is
   * stateful.
   * </p>
   */
  public void triggeredJobComplete (final IOperableTrigger trigger,
                                    final IJobDetail jobDetail,
                                    final ECompletedExecutionInstruction triggerInstCode)
  {
    synchronized (m_aLock)
    {
      final JobWrapper jw = m_aJobsByKey.get (jobDetail.getKey ());
      final TriggerWrapper tw = m_aTriggersByKey.get (trigger.getKey ());

      // It's possible that the job is null if:
      // 1- it was deleted during execution
      // 2- RAMJobStore is being used only for volatile jobs / triggers
      // from the JDBC job store
      if (jw != null)
      {
        IJobDetail jd = jw.m_aJobDetail;

        if (jd.isPersistJobDataAfterExecution ())
        {
          JobDataMap newData = jobDetail.getJobDataMap ();
          if (newData != null)
          {
            newData = (JobDataMap) newData.clone ();
            // newData.clearDirtyFlag ();
          }
          jd = jd.getJobBuilder ().setJobData (newData).build ();
          jw.m_aJobDetail = jd;
        }
        if (jd.isConcurrentExectionDisallowed ())
        {
          m_aBlockedJobs.remove (jd.getKey ());
          final ICommonsList <TriggerWrapper> trigs = getTriggerWrappersForJob (jd.getKey ());
          for (final TriggerWrapper ttw : trigs)
          {
            if (ttw.state == TriggerWrapper.STATE_BLOCKED)
            {
              ttw.state = TriggerWrapper.STATE_WAITING;
              m_aTimeTriggers.add (ttw);
            }
            if (ttw.state == TriggerWrapper.STATE_PAUSED_BLOCKED)
            {
              ttw.state = TriggerWrapper.STATE_PAUSED;
            }
          }
          m_aSignaler.signalSchedulingChange (0L);
        }
      }
      else
      { // even if it was deleted, there may be cleanup to do
        m_aBlockedJobs.remove (jobDetail.getKey ());
      }

      // check for trigger deleted during execution...
      if (tw != null)
      {
        if (triggerInstCode == ECompletedExecutionInstruction.DELETE_TRIGGER)
        {

          if (trigger.getNextFireTime () == null)
          {
            // double check for possible reschedule within job
            // execution, which would cancel the need to delete...
            if (tw.getTrigger ().getNextFireTime () == null)
            {
              removeTrigger (trigger.getKey ());
            }
          }
          else
          {
            removeTrigger (trigger.getKey ());
            m_aSignaler.signalSchedulingChange (0L);
          }
        }
        else
          if (triggerInstCode == ECompletedExecutionInstruction.SET_TRIGGER_COMPLETE)
          {
            tw.state = TriggerWrapper.STATE_COMPLETE;
            m_aTimeTriggers.remove (tw);
            m_aSignaler.signalSchedulingChange (0L);
          }
          else
            if (triggerInstCode == ECompletedExecutionInstruction.SET_TRIGGER_ERROR)
            {
              getLog ().info ("Trigger " + trigger.getKey () + " set to ERROR state.");
              tw.state = TriggerWrapper.STATE_ERROR;
              m_aSignaler.signalSchedulingChange (0L);
            }
            else
              if (triggerInstCode == ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR)
              {
                getLog ().info ("All triggers of Job " + trigger.getJobKey () + " set to ERROR state.");
                setAllTriggersOfJobToState (trigger.getJobKey (), TriggerWrapper.STATE_ERROR);
                m_aSignaler.signalSchedulingChange (0L);
              }
              else
                if (triggerInstCode == ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE)
                {
                  setAllTriggersOfJobToState (trigger.getJobKey (), TriggerWrapper.STATE_COMPLETE);
                  m_aSignaler.signalSchedulingChange (0L);
                }
      }
    }
  }

  protected void setAllTriggersOfJobToState (final JobKey jobKey, final int state)
  {
    final ICommonsList <TriggerWrapper> tws = getTriggerWrappersForJob (jobKey);
    for (final TriggerWrapper tw : tws)
    {
      tw.state = state;
      if (state != TriggerWrapper.STATE_WAITING)
      {
        m_aTimeTriggers.remove (tw);
      }
    }
  }

  protected String peekTriggers ()
  {

    final StringBuilder str = new StringBuilder ();
    synchronized (m_aLock)
    {
      for (final TriggerWrapper triggerWrapper : m_aTriggersByKey.values ())
      {
        str.append (triggerWrapper.m_aTrigger.getKey ().getName ());
        str.append ("/");
      }
      str.append (" | ");
      for (final TriggerWrapper timeTrigger : m_aTimeTriggers)
      {
        str.append (timeTrigger.m_aTrigger.getKey ().getName ());
        str.append ("->");
      }
    }

    return str.toString ();
  }

  /**
   * @see com.helger.quartz.spi.IJobStore#getPausedTriggerGroups()
   */
  public ICommonsSet <String> getPausedTriggerGroups () throws JobPersistenceException
  {
    final ICommonsSet <String> set = new CommonsHashSet <> ();

    set.addAll (m_aPausedTriggerGroups);

    return set;
  }

  public void setInstanceId (final String schedInstId)
  {
    //
  }

  public void setInstanceName (final String schedName)
  {
    //
  }

  public void setThreadPoolSize (final int poolSize)
  {
    //
  }

  public long getEstimatedTimeToReleaseAndAcquireTrigger ()
  {
    return 5;
  }

  public boolean isClustered ()
  {
    return false;
  }

}

/*******************************************************************************
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * Helper
 * Classes. * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

class TriggerWrapperComparator implements Comparator <TriggerWrapper>, java.io.Serializable
{
  TriggerTimeComparator ttc = new TriggerTimeComparator ();

  public int compare (final TriggerWrapper trig1, final TriggerWrapper trig2)
  {
    return ttc.compare (trig1.m_aTrigger, trig2.m_aTrigger);
  }

  @Override
  public boolean equals (final Object obj)
  {
    return (obj instanceof TriggerWrapperComparator);
  }

  @Override
  public int hashCode ()
  {
    return super.hashCode ();
  }
}

final class JobWrapper
{
  public final JobKey m_aKey;
  public IJobDetail m_aJobDetail;

  JobWrapper (@Nonnull final IJobDetail jobDetail)
  {
    m_aKey = jobDetail.getKey ();
    m_aJobDetail = jobDetail;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final JobWrapper rhs = (JobWrapper) o;
    return m_aKey.equals (rhs.m_aKey);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aKey).getHashCode ();
  }
}

final class TriggerWrapper
{
  public final TriggerKey m_aKey;
  public final JobKey m_aJobKey;
  public final IOperableTrigger m_aTrigger;

  public int state = STATE_WAITING;
  public static final int STATE_WAITING = 0;
  public static final int STATE_ACQUIRED = 1;
  public static final int STATE_EXECUTING = 2;
  public static final int STATE_COMPLETE = 3;
  public static final int STATE_PAUSED = 4;
  public static final int STATE_BLOCKED = 5;
  public static final int STATE_PAUSED_BLOCKED = 6;
  public static final int STATE_ERROR = 7;

  TriggerWrapper (@Nonnull final IOperableTrigger trigger)
  {
    ValueEnforcer.notNull (trigger, "Trigger");
    m_aTrigger = trigger;
    m_aKey = trigger.getKey ();
    m_aJobKey = trigger.getJobKey ();
  }

  public IOperableTrigger getTrigger ()
  {
    return m_aTrigger;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final TriggerWrapper rhs = (TriggerWrapper) o;
    return m_aKey.equals (rhs.m_aKey);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aKey).getHashCode ();
  }
}
