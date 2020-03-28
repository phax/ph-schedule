/**
 * Copyright (C) 2014-2020 Philip Helger (www.helger.com)
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
package com.helger.schedule.jobstore;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.MustBeLocked;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsCollection;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsNavigableSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
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
 * {@link IJobStore} implementation based on
 * {@link com.helger.quartz.simpl.RAMJobStore}
 *
 * @author Philip Helger
 */
public class BaseJobStore implements IJobStore
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BaseJobStore.class);
  private static final AtomicLong s_aFiredTriggerRecordID = new AtomicLong (System.currentTimeMillis ());

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private ISchedulerSignaler m_aSignaler;
  private final ICommonsMap <JobKey, JobWrapper> m_aJobsByKey = new CommonsHashMap <> (1000);
  private final ICommonsMap <TriggerKey, TriggerWrapper> m_aTriggersByKey = new CommonsHashMap <> (1000);
  private final ICommonsMap <String, ICommonsMap <JobKey, JobWrapper>> m_aJobsByGroup = new CommonsHashMap <> (25);
  private final ICommonsMap <String, ICommonsMap <TriggerKey, TriggerWrapper>> m_aTriggersByGroup = new CommonsHashMap <> (25);
  private final ICommonsNavigableSet <TriggerWrapper> m_aTimeTriggers = new CommonsTreeSet <> (new TriggerWrapperComparator ());
  private final ICommonsMap <String, ICalendar> m_aCalendarsByName = new CommonsHashMap <> (25);
  private final ICommonsList <TriggerWrapper> m_aTriggers = new CommonsArrayList <> (1000);
  private final ICommonsSet <String> m_aPausedTriggerGroups = new CommonsHashSet <> ();
  private final ICommonsSet <String> m_aPausedJobGroups = new CommonsHashSet <> ();
  private final ICommonsSet <JobKey> m_aBlockedJobs = new CommonsHashSet <> ();
  private long m_nMisfireThreshold = 5000L;

  public BaseJobStore ()
  {}

  public void initialize (final IClassLoadHelper loadHelper, final ISchedulerSignaler aSignaler)
  {
    m_aRWLock.writeLockedGet ( () -> m_aSignaler = aSignaler);
    LOGGER.info ("ph-schedule JobStore initialized.");
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

  @Nonnegative
  public long getMisfireThreshold ()
  {
    return m_aRWLock.readLockedLong ( () -> m_nMisfireThreshold);
  }

  /**
   * The number of milliseconds by which a trigger must have missed its
   * next-fire-time, in order for it to be considered "misfired" and thus have
   * its misfire instruction applied.
   *
   * @param nMisfireThreshold
   *        the new misfire threshold
   */
  public void setMisfireThreshold (@Nonnegative final long nMisfireThreshold)
  {
    ValueEnforcer.isGT0 (nMisfireThreshold, "MisfireThreshold");
    m_aRWLock.writeLocked ( () -> {
      m_nMisfireThreshold = nMisfireThreshold;
    });
  }

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
   *         in case a nested call throws this
   */
  public void clearAllSchedulingData () throws JobPersistenceException
  {
    // unschedule jobs (delete triggers)
    for (final String group : getTriggerGroupNames ())
    {
      final ICommonsSet <TriggerKey> keys = getTriggerKeys (GroupMatcher.triggerGroupEquals (group));
      for (final TriggerKey key : keys)
        removeTrigger (key);
    }

    // delete jobs
    for (final String group : getJobGroupNames ())
    {
      final ICommonsSet <JobKey> keys = getJobKeys (GroupMatcher.jobGroupEquals (group));
      for (final JobKey key : keys)
        removeJob (key);
    }

    // delete calendars
    for (final String name : getCalendarNames ())
      removeCalendar (name);
  }

  public void storeJobAndTrigger (final IJobDetail aNewJob,
                                  final IOperableTrigger aNewTrigger) throws JobPersistenceException
  {
    storeJob (aNewJob, false);
    storeTrigger (aNewTrigger, false);
  }

  public void storeJob (final IJobDetail aNewJob, final boolean bReplaceExisting) throws ObjectAlreadyExistsException
  {
    final JobKey aKey = aNewJob.getKey ();

    boolean bReplace = false;

    final JobWrapper aOld = m_aRWLock.readLockedGet ( () -> m_aJobsByKey.get (aKey));
    if (aOld != null)
    {
      if (!bReplaceExisting)
        throw new ObjectAlreadyExistsException (aNewJob);
      bReplace = true;
    }

    if (!bReplace)
    {
      m_aRWLock.writeLocked ( () -> {
        // get job group
        final String sGroupName = aKey.getGroup ();
        final ICommonsMap <JobKey, JobWrapper> aMap = m_aJobsByGroup.computeIfAbsent (sGroupName,
                                                                                      k -> new CommonsHashMap <> (100));

        final JobWrapper jw = new JobWrapper (aNewJob.clone ());
        // add to jobs by group
        aMap.put (aKey, jw);
        // add to jobs by FQN map
        m_aJobsByKey.put (aKey, jw);
      });
    }
    else
    {
      m_aRWLock.writeLocked ( () ->
      // update job detail
      aOld.setJobDetail (aNewJob.clone ()));
    }
  }

  public boolean removeJob (final JobKey jobKey)
  {
    boolean bFoundTrigger = false;

    final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
    for (final IOperableTrigger trig : triggersOfJob)
    {
      removeTrigger (trig.getKey ());
      bFoundTrigger = true;
    }
    final boolean bFinalFoundTrigger = bFoundTrigger;

    return m_aRWLock.writeLockedBoolean ( () -> {
      final boolean bRemovedJob = m_aJobsByKey.remove (jobKey) != null;
      if (!bFinalFoundTrigger && !bRemovedJob)
        return false;

      final ICommonsMap <JobKey, JobWrapper> aGrpMap = m_aJobsByGroup.get (jobKey.getGroup ());
      if (aGrpMap != null)
      {
        aGrpMap.remove (jobKey);
        if (aGrpMap.isEmpty ())
          m_aJobsByGroup.remove (jobKey.getGroup ());
      }
      return true;
    });
  }

  public boolean removeJobs (@Nonnull final List <JobKey> jobKeys) throws JobPersistenceException
  {
    boolean bAllFound = true;
    for (final JobKey key : jobKeys)
      if (!removeJob (key))
        bAllFound = false;
    return bAllFound;
  }

  public boolean removeTriggers (@Nonnull final List <TriggerKey> triggerKeys) throws JobPersistenceException
  {
    boolean bAllFound = true;
    for (final TriggerKey key : triggerKeys)
      if (!removeTrigger (key))
        bAllFound = false;
    return bAllFound;
  }

  public void storeJobsAndTriggers (final Map <IJobDetail, Set <? extends ITrigger>> aTriggersAndJobs,
                                    final boolean bReplace) throws JobPersistenceException
  {
    // make sure there are no collisions...
    if (!bReplace)
    {
      for (final Map.Entry <IJobDetail, Set <? extends ITrigger>> aEntry : aTriggersAndJobs.entrySet ())
      {
        final IJobDetail aJobDetail = aEntry.getKey ();
        if (checkExists (aJobDetail.getKey ()))
          throw new ObjectAlreadyExistsException (aJobDetail);
        for (final ITrigger trigger : aEntry.getValue ())
          if (checkExists (trigger.getKey ()))
            throw new ObjectAlreadyExistsException (trigger);
      }
    }
    // do bulk add...
    for (final Map.Entry <IJobDetail, Set <? extends ITrigger>> aEntry : aTriggersAndJobs.entrySet ())
    {
      storeJob (aEntry.getKey (), true);
      for (final ITrigger aTrigger : aEntry.getValue ())
        storeTrigger ((IOperableTrigger) aTrigger, true);
    }
  }

  public void storeTrigger (@Nonnull final IOperableTrigger aNewTrigger,
                            final boolean bReplaceExisting) throws JobPersistenceException
  {
    final TriggerKey aTriggerKey = aNewTrigger.getKey ();

    if (checkExists (aTriggerKey))
    {
      if (!bReplaceExisting)
        throw new ObjectAlreadyExistsException (aNewTrigger);
      _removeTrigger (aTriggerKey, false);
    }

    if (retrieveJob (aNewTrigger.getJobKey ()) == null)
      throw new JobPersistenceException ("The job (" +
                                         aNewTrigger.getJobKey () +
                                         ") referenced by the trigger does not exist.");

    final TriggerWrapper tw = new TriggerWrapper (aNewTrigger.clone ());

    m_aRWLock.writeLocked ( () -> {
      // add to triggers array
      m_aTriggers.add (tw);
      // add to triggers by group
      final String sTriggerGroupName = aTriggerKey.getGroup ();
      final ICommonsMap <TriggerKey, TriggerWrapper> aGrpMap = m_aTriggersByGroup.computeIfAbsent (sTriggerGroupName,
                                                                                                   k -> new CommonsHashMap <> (100));
      aGrpMap.put (aTriggerKey, tw);
      // add to triggers by FQN map
      m_aTriggersByKey.put (aTriggerKey, tw);

      if (m_aPausedTriggerGroups.contains (sTriggerGroupName) ||
          m_aPausedJobGroups.contains (aNewTrigger.getJobKey ().getGroup ()))
      {
        tw.setState (TriggerWrapper.STATE_PAUSED);
        if (m_aBlockedJobs.contains (tw.getJobKey ()))
          tw.setState (TriggerWrapper.STATE_PAUSED_BLOCKED);
      }
      else
        if (m_aBlockedJobs.contains (tw.getJobKey ()))
          tw.setState (TriggerWrapper.STATE_BLOCKED);
        else
        {
          m_aTimeTriggers.add (tw);
        }
    });
  }

  /**
   * <p>
   * Remove (delete) the <code>{@link com.helger.quartz.ITrigger}</code> with
   * the given name.
   * </p>
   *
   * @return <code>true</code> if a <code>Trigger</code> with the given name and
   *         group was found and removed from the store.
   */
  public boolean removeTrigger (@Nonnull final TriggerKey triggerKey)
  {
    return _removeTrigger (triggerKey, true);
  }

  @IsLocked (ELockType.WRITE)
  private boolean _removeTrigger (@Nonnull final TriggerKey key, final boolean bRemoveOrphanedJob)
  {
    return m_aRWLock.writeLockedBoolean ( () -> {
      // remove from triggers by FQN map
      if (m_aTriggersByKey.remove (key) == null)
        return false;

      // remove from triggers by group
      final ICommonsMap <TriggerKey, TriggerWrapper> aGrpMap = m_aTriggersByGroup.get (key.getGroup ());
      if (aGrpMap != null)
      {
        aGrpMap.remove (key);
        if (aGrpMap.isEmpty ())
          m_aTriggersByGroup.remove (key.getGroup ());
      }
      // remove from triggers list
      TriggerWrapper tw = null;
      final Iterator <TriggerWrapper> tgs = m_aTriggers.iterator ();
      while (tgs.hasNext ())
      {
        tw = tgs.next ();
        if (key.equals (tw.getTriggerKey ()))
        {
          tgs.remove ();
          break;
        }
      }
      m_aTimeTriggers.remove (tw);

      if (bRemoveOrphanedJob)
      {
        final JobWrapper jw = m_aJobsByKey.get (tw.getJobKey ());
        final ICommonsList <IOperableTrigger> trigs = getTriggersForJob (tw.getJobKey ());
        if ((trigs == null || trigs.size () == 0) && !jw.getJobDetail ().isDurable ())
        {
          if (removeJob (jw.getJobKey ()))
            m_aSignaler.notifySchedulerListenersJobDeleted (jw.getJobKey ());
        }
      }

      return true;
    });
  }

  /**
   * @see com.helger.quartz.spi.IJobStore#replaceTrigger(TriggerKey triggerKey,
   *      IOperableTrigger newTrigger)
   */
  public boolean replaceTrigger (final TriggerKey aTriggerKey,
                                 final IOperableTrigger aNewTrigger) throws JobPersistenceException
  {
    TriggerWrapper tw;
    m_aRWLock.writeLock ().lock ();
    try
    {
      // remove from triggers by FQN map
      tw = m_aTriggersByKey.remove (aTriggerKey);
      if (tw == null)
        return false;

      if (!tw.getTrigger ().getJobKey ().equals (aNewTrigger.getJobKey ()))
        throw new JobPersistenceException ("New trigger is not related to the same job as the old trigger.");

      // remove from triggers by group
      final ICommonsMap <TriggerKey, TriggerWrapper> aGrpMap = m_aTriggersByGroup.get (aTriggerKey.getGroup ());
      if (aGrpMap != null)
      {
        aGrpMap.remove (aTriggerKey);
        if (aGrpMap.isEmpty ())
          m_aTriggersByGroup.remove (aTriggerKey.getGroup ());
      }

      // remove from triggers array
      tw = null;
      final Iterator <TriggerWrapper> tgs = m_aTriggers.iterator ();
      while (tgs.hasNext ())
      {
        tw = tgs.next ();
        if (aTriggerKey.equals (tw.getTriggerKey ()))
        {
          tgs.remove ();
          break;
        }
      }
      m_aTimeTriggers.remove (tw);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    try
    {
      storeTrigger (aNewTrigger, false);
    }
    catch (final JobPersistenceException jpe)
    {
      // put previous trigger back...
      storeTrigger (tw.getTrigger (), false);
      throw jpe;
    }
    return true;
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
    return m_aRWLock.readLockedGet ( () -> {
      final JobWrapper jw = m_aJobsByKey.get (jobKey);
      return jw != null ? jw.getJobDetail ().clone () : null;
    });
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
    return m_aRWLock.readLockedGet ( () -> {
      final TriggerWrapper tw = m_aTriggersByKey.get (triggerKey);
      return tw != null ? tw.getTrigger ().clone () : null;
    });
  }

  public boolean checkExists (final JobKey jobKey) throws JobPersistenceException
  {
    return m_aRWLock.readLockedBoolean ( () -> m_aJobsByKey.containsKey (jobKey));
  }

  public boolean checkExists (final TriggerKey aTriggerKey) throws JobPersistenceException
  {
    return m_aRWLock.readLockedBoolean ( () -> m_aTriggersByKey.containsKey (aTriggerKey));
  }

  @Nonnull
  public ETriggerState getTriggerState (final TriggerKey triggerKey) throws JobPersistenceException
  {
    return m_aRWLock.readLockedGet ( () -> {
      final TriggerWrapper tw = m_aTriggersByKey.get (triggerKey);

      if (tw == null)
        return ETriggerState.NONE;

      if (tw.getState () == TriggerWrapper.STATE_COMPLETE)
        return ETriggerState.COMPLETE;

      if (tw.getState () == TriggerWrapper.STATE_PAUSED)
        return ETriggerState.PAUSED;

      if (tw.getState () == TriggerWrapper.STATE_PAUSED_BLOCKED)
        return ETriggerState.PAUSED;

      if (tw.getState () == TriggerWrapper.STATE_BLOCKED)
        return ETriggerState.BLOCKED;

      if (tw.getState () == TriggerWrapper.STATE_ERROR)
        return ETriggerState.ERROR;

      return ETriggerState.NORMAL;
    });
  }

  public void storeCalendar (final String name,
                             final ICalendar aCalendar,
                             final boolean bReplaceExisting,
                             final boolean bUpdateTriggers) throws ObjectAlreadyExistsException
  {
    m_aRWLock.writeLockedThrowing ( () -> {
      final ICalendar aOld = m_aCalendarsByName.get (name);
      if (aOld != null)
      {
        if (!bReplaceExisting)
          throw new ObjectAlreadyExistsException ("Calendar with name '" + name + "' already exists.");
        m_aCalendarsByName.remove (name);
      }

      final ICalendar aCalendarClone = aCalendar.getClone ();
      m_aCalendarsByName.put (name, aCalendarClone);

      if (aOld != null && bUpdateTriggers)
      {
        for (final TriggerWrapper tw : getTriggerWrappersForCalendar (name))
        {
          final IOperableTrigger trig = tw.getTrigger ();
          final boolean bRemoved = m_aTimeTriggers.remove (tw);
          trig.updateWithNewCalendar (aCalendarClone, getMisfireThreshold ());

          if (bRemoved)
            m_aTimeTriggers.add (tw);
        }
      }
    });
  }

  public boolean removeCalendar (final String calName) throws JobPersistenceException
  {
    m_aRWLock.readLockedThrowing ( () -> {
      int numRefs = 0;
      for (final TriggerWrapper aTriggerWrapper : m_aTriggers)
      {
        final IOperableTrigger trigg = aTriggerWrapper.getTrigger ();
        if (trigg.getCalendarName () != null && trigg.getCalendarName ().equals (calName))
          numRefs++;
      }

      if (numRefs > 0)
        throw new JobPersistenceException ("Calender cannot be removed if it referenced by a Trigger!");
    });

    return m_aRWLock.writeLockedBoolean ( () -> m_aCalendarsByName.remove (calName) != null);
  }

  public ICalendar retrieveCalendar (final String calName)
  {
    return m_aRWLock.readLockedGet ( () -> {
      final ICalendar cal = m_aCalendarsByName.get (calName);
      return cal != null ? cal.getClone () : null;
    });
  }

  public int getNumberOfJobs ()
  {
    return m_aRWLock.readLockedInt (m_aJobsByKey::size);
  }

  public int getNumberOfTriggers ()
  {
    return m_aRWLock.readLockedInt (m_aTriggers::size);
  }

  public int getNumberOfCalendars ()
  {
    return m_aRWLock.readLockedInt (m_aCalendarsByName::size);
  }

  public ICommonsSet <JobKey> getJobKeys (final GroupMatcher <JobKey> matcher)
  {
    final ICommonsSet <JobKey> ret = new CommonsHashSet <> ();

    final StringMatcher.StringOperatorName eOperator = matcher.getCompareWithOperator ();
    final String compareToValue = matcher.getCompareToValue ();

    m_aRWLock.readLocked ( () -> {
      switch (eOperator)
      {
        case EQUALS:
          final ICommonsMap <JobKey, JobWrapper> aGrpMap = m_aJobsByGroup.get (compareToValue);
          if (aGrpMap != null)
            for (final JobWrapper jw : aGrpMap.values ())
              if (jw != null)
                ret.add (jw.getJobKey ());
          break;
        default:
          for (final Map.Entry <String, ICommonsMap <JobKey, JobWrapper>> entry : m_aJobsByGroup.entrySet ())
            if (eOperator.evaluate (entry.getKey (), compareToValue) && entry.getValue () != null)
              for (final JobWrapper jobWrapper : entry.getValue ().values ())
                if (jobWrapper != null)
                  ret.add (jobWrapper.getJobKey ());
          break;
      }
    });

    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getCalendarNames ()
  {
    return m_aRWLock.readLockedGet ( () -> new CommonsArrayList <> (m_aCalendarsByName.keySet ()));
  }

  public ICommonsSet <TriggerKey> getTriggerKeys (final GroupMatcher <TriggerKey> matcher)
  {
    final ICommonsSet <TriggerKey> ret = new CommonsHashSet <> ();
    final StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator ();
    final String compareToValue = matcher.getCompareToValue ();

    m_aRWLock.readLocked ( () -> {
      switch (operator)
      {
        case EQUALS:
          final ICommonsMap <TriggerKey, TriggerWrapper> grpMap = m_aTriggersByGroup.get (compareToValue);
          if (grpMap != null)
            for (final TriggerWrapper tw : grpMap.values ())
              if (tw != null)
                ret.add (tw.getTriggerKey ());
          break;
        default:
          for (final Map.Entry <String, ICommonsMap <TriggerKey, TriggerWrapper>> entry : m_aTriggersByGroup.entrySet ())
            if (operator.evaluate (entry.getKey (), compareToValue) && entry.getValue () != null)
              for (final TriggerWrapper triggerWrapper : entry.getValue ().values ())
                if (triggerWrapper != null)
                  ret.add (triggerWrapper.getTriggerKey ());
          break;
      }
    });

    return ret;
  }

  public ICommonsList <String> getJobGroupNames ()
  {
    return m_aRWLock.readLockedGet ( () -> new CommonsArrayList <> (m_aJobsByGroup.keySet ()));
  }

  public ICommonsList <String> getTriggerGroupNames ()
  {
    return m_aRWLock.readLockedGet ( () -> new CommonsArrayList <> (m_aTriggersByGroup.keySet ()));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IOperableTrigger> getTriggersForJob (final JobKey aJobKey)
  {
    return m_aRWLock.readLockedGet ( () -> {
      final ICommonsList <IOperableTrigger> ret = new CommonsArrayList <> ();
      for (final TriggerWrapper aTW : m_aTriggers)
        if (aTW.getJobKey ().equals (aJobKey))
          ret.add (aTW.getTrigger ().clone ());
      return ret;
    });
  }

  @Nonnull
  @ReturnsMutableCopy
  protected ICommonsList <TriggerWrapper> getTriggerWrappersForJob (final JobKey aJobKey)
  {
    return m_aRWLock.readLockedGet ( () -> {
      final ICommonsList <TriggerWrapper> ret = new CommonsArrayList <> ();
      for (final TriggerWrapper aTW : m_aTriggers)
        if (aTW.getJobKey ().equals (aJobKey))
          ret.add (aTW);
      return ret;
    });
  }

  @Nonnull
  @ReturnsMutableCopy
  protected ICommonsList <TriggerWrapper> getTriggerWrappersForCalendar (final String calName)
  {
    return m_aRWLock.readLockedGet ( () -> {
      final ICommonsList <TriggerWrapper> ret = new CommonsArrayList <> ();
      for (final TriggerWrapper tw : m_aTriggers)
      {
        final String tcalName = tw.getTrigger ().getCalendarName ();
        if (tcalName != null && tcalName.equals (calName))
          ret.add (tw);
      }
      return ret;
    });
  }

  /**
   * <p>
   * Pause the <code>{@link ITrigger}</code> with the given name.
   * </p>
   */
  public void pauseTrigger (final TriggerKey triggerKey)
  {
    final TriggerWrapper tw = m_aRWLock.readLockedGet ( () -> m_aTriggersByKey.get (triggerKey));

    // does the trigger exist?
    if (tw == null)
      return;

    // if the trigger is "complete" pausing it does not make sense...
    if (tw.getState () == TriggerWrapper.STATE_COMPLETE)
      return;

    m_aRWLock.writeLocked ( () -> {
      if (tw.getState () == TriggerWrapper.STATE_BLOCKED)
        tw.setState (TriggerWrapper.STATE_PAUSED_BLOCKED);
      else
        tw.setState (TriggerWrapper.STATE_PAUSED);

      m_aTimeTriggers.remove (tw);
    });
  }

  public ICommonsList <String> pauseTriggers (final GroupMatcher <TriggerKey> matcher)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();

    final StringMatcher.StringOperatorName eOperator = matcher.getCompareWithOperator ();

    m_aRWLock.writeLocked ( () -> {
      switch (eOperator)
      {
        case EQUALS:
          if (m_aPausedTriggerGroups.add (matcher.getCompareToValue ()))
            ret.add (matcher.getCompareToValue ());
          break;
        default:
          for (final String group : m_aTriggersByGroup.keySet ())
            if (eOperator.evaluate (group, matcher.getCompareToValue ()))
              if (m_aPausedTriggerGroups.add (matcher.getCompareToValue ()))
                ret.add (group);
      }
    });

    for (final String pausedGroup : ret)
    {
      final ICommonsSet <TriggerKey> keys = getTriggerKeys (GroupMatcher.triggerGroupEquals (pausedGroup));
      for (final TriggerKey key : keys)
        pauseTrigger (key);
    }

    return ret;
  }

  /**
   * <p>
   * Pause the <code>{@link com.helger.quartz.IJobDetail}</code> with the given
   * name - by pausing all of its current <code>Trigger</code>s.
   * </p>
   */
  public void pauseJob (final JobKey jobKey)
  {
    final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
    for (final IOperableTrigger trigger : triggersOfJob)
      pauseTrigger (trigger.getKey ());
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
    final StringMatcher.StringOperatorName eOperator = matcher.getCompareWithOperator ();

    m_aRWLock.writeLocked ( () -> {
      switch (eOperator)
      {
        case EQUALS:
          if (m_aPausedJobGroups.add (matcher.getCompareToValue ()))
            pausedGroups.add (matcher.getCompareToValue ());
          break;
        default:
          for (final String group : m_aJobsByGroup.keySet ())
            if (eOperator.evaluate (group, matcher.getCompareToValue ()))
              if (m_aPausedJobGroups.add (group))
                pausedGroups.add (group);
          break;
      }
    });

    for (final String groupName : pausedGroups)
      for (final JobKey jobKey : getJobKeys (GroupMatcher.jobGroupEquals (groupName)))
      {
        final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
        for (final IOperableTrigger trigger : triggersOfJob)
          pauseTrigger (trigger.getKey ());
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
    final TriggerWrapper tw = m_aRWLock.readLockedGet ( () -> m_aTriggersByKey.get (triggerKey));

    // does the trigger exist?
    if (tw == null)
      return;

    // if the trigger is not paused resuming it does not make sense...
    if (tw.getState () != TriggerWrapper.STATE_PAUSED && tw.getState () != TriggerWrapper.STATE_PAUSED_BLOCKED)
      return;

    m_aRWLock.writeLocked ( () -> {
      final IOperableTrigger trig = tw.getTrigger ();
      if (m_aBlockedJobs.contains (trig.getJobKey ()))
        tw.setState (TriggerWrapper.STATE_BLOCKED);
      else
        tw.setState (TriggerWrapper.STATE_WAITING);

      applyMisfire (tw);

      if (tw.getState () == TriggerWrapper.STATE_WAITING)
        m_aTimeTriggers.add (tw);
    });
  }

  public ICommonsList <String> resumeTriggers (final GroupMatcher <TriggerKey> matcher)
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    final ICommonsSet <TriggerKey> keys = getTriggerKeys (matcher);

    for (final TriggerKey triggerKey : keys)
    {
      ret.add (triggerKey.getGroup ());

      if (m_aRWLock.readLockedBoolean ( () -> {
        final TriggerWrapper aTW = m_aTriggersByKey.get (triggerKey);
        if (aTW != null)
        {
          final String sJobGroupName = aTW.getJobKey ().getGroup ();
          if (m_aPausedJobGroups.contains (sJobGroupName))
            return false;
        }
        return true;
      }))
      {
        resumeTrigger (triggerKey);
      }
    }

    m_aRWLock.writeLocked ( () -> {
      for (final String group : ret)
        m_aPausedTriggerGroups.remove (group);
    });

    // Convert to list
    return ret.getCopyAsList ();
  }

  public void resumeJob (final JobKey jobKey)
  {
    final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (jobKey);
    for (final IOperableTrigger trigger : triggersOfJob)
      resumeTrigger (trigger.getKey ());
  }

  public ICommonsCollection <String> resumeJobs (final GroupMatcher <JobKey> matcher)
  {
    final ICommonsSet <String> ret = new CommonsHashSet <> ();

    final ICommonsSet <JobKey> keys = getJobKeys (matcher);

    m_aRWLock.readLocked ( () -> {
      for (final String pausedJobGroup : m_aPausedJobGroups)
        if (matcher.getCompareWithOperator ().evaluate (pausedJobGroup, matcher.getCompareToValue ()))
          ret.add (pausedJobGroup);
    });

    m_aRWLock.writeLocked ( () -> {
      for (final String resumedGroup : ret)
        m_aPausedJobGroups.remove (resumedGroup);
    });

    for (final JobKey key : keys)
    {
      final ICommonsList <IOperableTrigger> triggersOfJob = getTriggersForJob (key);
      for (final IOperableTrigger trigger : triggersOfJob)
        resumeTrigger (trigger.getKey ());
    }
    return ret;
  }

  public void pauseAll ()
  {
    final ICommonsList <String> names = getTriggerGroupNames ();
    for (final String name : names)
      pauseTriggers (GroupMatcher.triggerGroupEquals (name));
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
    m_aRWLock.writeLocked ( () -> {
      m_aPausedJobGroups.clear ();
    });

    resumeTriggers (GroupMatcher.anyTriggerGroup ());
  }

  @MustBeLocked (ELockType.WRITE)
  protected boolean applyMisfire (final TriggerWrapper tw)
  {
    long misfireTime = System.currentTimeMillis ();
    if (getMisfireThreshold () > 0)
      misfireTime -= getMisfireThreshold ();

    final Date tnft = tw.getTrigger ().getNextFireTime ();
    if (tnft == null ||
        tnft.getTime () > misfireTime ||
        tw.getTrigger ().getMisfireInstruction () == ITrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY)
    {
      return false;
    }

    ICalendar cal = null;
    if (tw.getTrigger ().getCalendarName () != null)
    {
      cal = retrieveCalendar (tw.getTrigger ().getCalendarName ());
    }

    m_aSignaler.notifyTriggerListenersMisfired (tw.getTrigger ().clone ());

    tw.getTrigger ().updateAfterMisfire (cal);

    if (tw.getTrigger ().getNextFireTime () == null)
    {
      tw.setState (TriggerWrapper.STATE_COMPLETE);
      m_aSignaler.notifySchedulerListenersFinalized (tw.getTrigger ());
      m_aTimeTriggers.remove (tw);
    }
    else
      if (tnft.equals (tw.getTrigger ().getNextFireTime ()))
        return false;

    return true;
  }

  protected String getFiredTriggerRecordId ()
  {
    return Long.toString (s_aFiredTriggerRecordID.incrementAndGet ());
  }

  public ICommonsList <IOperableTrigger> acquireNextTriggers (final long noLaterThan,
                                                              final int maxCount,
                                                              final long timeWindow)
  {
    return m_aRWLock.writeLockedGet ( () -> {
      final ICommonsList <IOperableTrigger> ret = new CommonsArrayList <> ();
      final ICommonsSet <JobKey> acquiredJobKeysForNoConcurrentExec = new CommonsHashSet <> ();
      final ICommonsSet <TriggerWrapper> excludedTriggers = new CommonsHashSet <> ();
      long firstAcquiredTriggerFireTime = 0;

      // return empty list if store has no triggers.
      if (m_aTimeTriggers.isEmpty ())
        return ret;

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
        catch (final NoSuchElementException nsee)
        {
          break;
        }

        if (tw.getTrigger ().getNextFireTime () == null)
          continue;

        if (applyMisfire (tw))
        {
          if (tw.getTrigger ().getNextFireTime () != null)
            m_aTimeTriggers.add (tw);
          continue;
        }

        if (tw.getTrigger ().getNextFireTime ().getTime () > noLaterThan + timeWindow)
        {
          m_aTimeTriggers.add (tw);
          break;
        }

        // If trigger's job is set as @DisallowConcurrentExecution, and it has
        // already been added to result, then
        // put it back into the timeTriggers set and continue to search for next
        // trigger.
        final JobKey jobKey = tw.getJobKey ();
        final IJobDetail job = m_aJobsByKey.get (tw.getJobKey ()).getJobDetail ();
        if (job.isConcurrentExectionDisallowed ())
        {
          if (!acquiredJobKeysForNoConcurrentExec.add (jobKey))
          {
            excludedTriggers.add (tw);
            // go to next trigger in store.
            continue;
          }
        }

        tw.setState (TriggerWrapper.STATE_ACQUIRED);
        tw.getTrigger ().setFireInstanceId (getFiredTriggerRecordId ());
        final IOperableTrigger trig = tw.getTrigger ().clone ();
        ret.add (trig);
        if (firstAcquiredTriggerFireTime == 0)
          firstAcquiredTriggerFireTime = tw.getTrigger ().getNextFireTime ().getTime ();

        if (ret.size () == maxCount)
          break;
      }

      // If we did excluded triggers to prevent ACQUIRE state due to
      // DisallowConcurrentExecution, we need to add them back to store.
      if (!excludedTriggers.isEmpty ())
        m_aTimeTriggers.addAll (excludedTriggers);

      return ret;
    });

  }

  /**
   * <p>
   * Inform the <code>JobStore</code> that the scheduler no longer plans to fire
   * the given <code>Trigger</code>, that it had previously acquired (reserved).
   * </p>
   */
  public void releaseAcquiredTrigger (final IOperableTrigger trigger)
  {
    m_aRWLock.writeLocked ( () -> {
      final TriggerWrapper tw = m_aTriggersByKey.get (trigger.getKey ());
      if (tw != null && tw.getState () == TriggerWrapper.STATE_ACQUIRED)
      {
        tw.setState (TriggerWrapper.STATE_WAITING);
        m_aTimeTriggers.add (tw);
      }
    });
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
    return m_aRWLock.writeLockedGet ( () -> {
      final ICommonsList <TriggerFiredResult> ret = new CommonsArrayList <> ();

      for (final IOperableTrigger trigger : firedTriggers)
      {
        final TriggerWrapper tw = m_aTriggersByKey.get (trigger.getKey ());
        // was the trigger deleted since being acquired?
        if (tw == null)
          continue;

        // was the trigger completed, paused, blocked, etc. since being
        // acquired?
        if (tw.getState () != TriggerWrapper.STATE_ACQUIRED)
          continue;

        ICalendar cal = null;
        if (tw.getTrigger ().getCalendarName () != null)
        {
          cal = retrieveCalendar (tw.getTrigger ().getCalendarName ());
          if (cal == null)
            continue;
        }
        final Date prevFireTime = trigger.getPreviousFireTime ();
        // in case trigger was replaced between acquiring and firing
        m_aTimeTriggers.remove (tw);
        // call triggered on our copy, and the scheduler's copy
        tw.getTrigger ().triggered (cal);
        trigger.triggered (cal);
        // tw.state = TriggerWrapper.STATE_EXECUTING;
        tw.setState (TriggerWrapper.STATE_WAITING);

        final TriggerFiredBundle bndle = new TriggerFiredBundle (retrieveJob (tw.getJobKey ()),
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
            if (ttw.getState () == TriggerWrapper.STATE_WAITING)
              ttw.setState (TriggerWrapper.STATE_BLOCKED);
            if (ttw.getState () == TriggerWrapper.STATE_PAUSED)
              ttw.setState (TriggerWrapper.STATE_PAUSED_BLOCKED);
            m_aTimeTriggers.remove (ttw);
          }
          m_aBlockedJobs.add (job.getKey ());
        }
        else
          if (tw.getTrigger ().getNextFireTime () != null)
            m_aTimeTriggers.add (tw);

        ret.add (new TriggerFiredResult (bndle));
      }

      return ret;
    });
  }

  public void triggeredJobComplete (final IOperableTrigger trigger,
                                    final IJobDetail jobDetail,
                                    final ECompletedExecutionInstruction triggerInstCode)
  {
    m_aRWLock.writeLocked ( () -> {
      final JobWrapper jw = m_aJobsByKey.get (jobDetail.getKey ());
      final TriggerWrapper tw = m_aTriggersByKey.get (trigger.getKey ());

      // It's possible that the job is null if:
      // 1- it was deleted during execution
      // 2- RAMJobStore is being used only for volatile jobs / triggers
      // from the JDBC job store
      if (jw != null)
      {
        IJobDetail jd = jw.getJobDetail ();

        if (jd.isPersistJobDataAfterExecution ())
        {
          JobDataMap newData = jobDetail.getJobDataMap ();
          if (newData != null)
          {
            newData = newData.getClone ();
            // newData.clearDirtyFlag ();
          }
          jd = jd.getJobBuilder ().setJobData (newData).build ();
          jw.setJobDetail (jd);
        }
        if (jd.isConcurrentExectionDisallowed ())
        {
          m_aBlockedJobs.remove (jd.getKey ());
          final ICommonsList <TriggerWrapper> trigs = getTriggerWrappersForJob (jd.getKey ());
          for (final TriggerWrapper ttw : trigs)
          {
            if (ttw.getState () == TriggerWrapper.STATE_BLOCKED)
            {
              ttw.setState (TriggerWrapper.STATE_WAITING);
              m_aTimeTriggers.add (ttw);
            }
            if (ttw.getState () == TriggerWrapper.STATE_PAUSED_BLOCKED)
              ttw.setState (TriggerWrapper.STATE_PAUSED);
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
            tw.setState (TriggerWrapper.STATE_COMPLETE);
            m_aTimeTriggers.remove (tw);
            m_aSignaler.signalSchedulingChange (0L);
          }
          else
            if (triggerInstCode == ECompletedExecutionInstruction.SET_TRIGGER_ERROR)
            {
              LOGGER.info ("Trigger " + trigger.getKey () + " set to ERROR state.");
              tw.setState (TriggerWrapper.STATE_ERROR);
              m_aSignaler.signalSchedulingChange (0L);
            }
            else
              if (triggerInstCode == ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR)
              {
                LOGGER.info ("All triggers of Job " + trigger.getJobKey () + " set to ERROR state.");
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
    });
  }

  @MustBeLocked (ELockType.WRITE)
  protected void setAllTriggersOfJobToState (final JobKey jobKey, final int state)
  {
    final ICommonsList <TriggerWrapper> tws = getTriggerWrappersForJob (jobKey);
    for (final TriggerWrapper tw : tws)
    {
      tw.setState (state);
      if (state != TriggerWrapper.STATE_WAITING)
        m_aTimeTriggers.remove (tw);
    }
  }

  protected String peekTriggers ()
  {
    return m_aRWLock.readLockedGet ( () -> {
      final StringBuilder aSB = new StringBuilder ();
      for (final TriggerWrapper triggerWrapper : m_aTriggersByKey.values ())
      {
        aSB.append (triggerWrapper.getTriggerKey ().getName ());
        aSB.append ("/");
      }
      aSB.append (" | ");

      for (final TriggerWrapper timeTrigger : m_aTimeTriggers)
      {
        aSB.append (timeTrigger.getTriggerKey ().getName ());
        aSB.append ("->");
      }
      return aSB.toString ();
    });
  }

  /**
   * @see com.helger.quartz.spi.IJobStore#getPausedTriggerGroups()
   */
  public ICommonsSet <String> getPausedTriggerGroups () throws JobPersistenceException
  {
    return m_aRWLock.readLockedGet (m_aPausedTriggerGroups::getClone);
  }

  public void setInstanceId (final String schedInstId)
  {}

  public void setInstanceName (final String schedName)
  {}

  public void setThreadPoolSize (final int poolSize)
  {}

  public long getEstimatedTimeToReleaseAndAcquireTrigger ()
  {
    return 5;
  }

  public boolean isClustered ()
  {
    return false;
  }
}

final class TriggerWrapperComparator implements Comparator <TriggerWrapper>, Serializable
{
  private final TriggerTimeComparator m_aComp = new TriggerTimeComparator ();

  public int compare (final TriggerWrapper trig1, final TriggerWrapper trig2)
  {
    return m_aComp.compare (trig1.getTrigger (), trig2.getTrigger ());
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    return true;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).getHashCode ();
  }
}

@Immutable
final class JobWrapper
{
  private final JobKey m_aKey;
  private IJobDetail m_aJobDetail;

  JobWrapper (@Nonnull final IJobDetail aJobDetail)
  {
    m_aKey = aJobDetail.getKey ();
    setJobDetail (aJobDetail);
  }

  @Nonnull
  public JobKey getJobKey ()
  {
    return m_aKey;
  }

  @Nonnull
  public IJobDetail getJobDetail ()
  {
    return m_aJobDetail;
  }

  public void setJobDetail (@Nonnull final IJobDetail aJobDetail)
  {
    ValueEnforcer.notNull (aJobDetail, "JobDetail");
    ValueEnforcer.isTrue (aJobDetail.getKey ().equals (m_aKey), "Different JobKey!");
    m_aJobDetail = aJobDetail;
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
  public static final int STATE_WAITING = 0;
  public static final int STATE_ACQUIRED = 1;
  public static final int STATE_EXECUTING = 2;
  public static final int STATE_COMPLETE = 3;
  public static final int STATE_PAUSED = 4;
  public static final int STATE_BLOCKED = 5;
  public static final int STATE_PAUSED_BLOCKED = 6;
  public static final int STATE_ERROR = 7;

  private final IOperableTrigger m_aTrigger;
  private final TriggerKey m_aTriggerKey;
  private final JobKey m_aJobKey;
  private int m_nState = STATE_WAITING;

  TriggerWrapper (@Nonnull final IOperableTrigger aTrigger)
  {
    ValueEnforcer.notNull (aTrigger, "Trigger");
    m_aTrigger = aTrigger;
    m_aTriggerKey = aTrigger.getKey ();
    m_aJobKey = aTrigger.getJobKey ();
  }

  @Nonnull
  public IOperableTrigger getTrigger ()
  {
    return m_aTrigger;
  }

  @Nonnull
  public TriggerKey getTriggerKey ()
  {
    return m_aTriggerKey;
  }

  @Nonnull
  public JobKey getJobKey ()
  {
    return m_aJobKey;
  }

  public int getState ()
  {
    return m_nState;
  }

  public void setState (final int nState)
  {
    m_nState = nState;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final TriggerWrapper rhs = (TriggerWrapper) o;
    return m_aTriggerKey.equals (rhs.m_aTriggerKey);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aTriggerKey).getHashCode ();
  }
}
