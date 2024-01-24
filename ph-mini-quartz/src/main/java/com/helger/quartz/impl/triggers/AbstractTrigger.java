/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2024 Philip Helger (www.helger.com)
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
package com.helger.quartz.impl.triggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.compare.CompareHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.lang.ICloneable;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduleBuilder;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.JobKey;
import com.helger.quartz.QCloneUtils;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.TriggerBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.spi.IOperableTrigger;

/**
 * <p>
 * The base abstract class to be extended by all <code>Trigger</code>s.
 * </p>
 * <p>
 * <code>Triggers</code> s have a name and group associated with them, which
 * should uniquely identify them within a single
 * <code>{@link IScheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code> s are
 * scheduled. Many <code>Trigger</code> s can point to the same
 * <code>Job</code>, but a single <code>Trigger</code> can only point to one
 * <code>Job</code>.
 * </p>
 * <p>
 * Triggers can 'send' parameters/data to <code>Job</code>s by placing contents
 * into the <code>JobDataMap</code> on the <code>Trigger</code>.
 * </p>
 *
 * @author James House
 * @author Sharada Jambula
 * @param <IMPLTYPE>
 *        Implementation type
 */
public abstract class AbstractTrigger <IMPLTYPE extends AbstractTrigger <IMPLTYPE>> implements
                                      IOperableTrigger,
                                      ICloneable <IMPLTYPE>
{
  private String m_sName;
  private String m_sGroup = IScheduler.DEFAULT_GROUP;
  private String m_sJobName;
  private String m_sJobGroup = IScheduler.DEFAULT_GROUP;
  private String m_sDescription;
  private JobDataMap m_aJobDataMap;
  private String m_sCalendarName;
  private String m_sFireInstanceId;
  private EMisfireInstruction m_eMisfireInstruction = EMisfireInstruction.MISFIRE_INSTRUCTION_SMART_POLICY;
  private int m_nPriority = DEFAULT_PRIORITY;
  private transient TriggerKey m_aKey;

  /**
   * Copy constructor
   *
   * @param aOther
   *        Calendar to copy from. May not be <code>null</code>.
   */
  protected AbstractTrigger (@Nonnull final AbstractTrigger <IMPLTYPE> aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    m_sName = aOther.m_sName;
    m_sGroup = aOther.m_sGroup;
    m_sJobName = aOther.m_sJobName;
    m_sJobGroup = aOther.m_sJobGroup;
    m_sDescription = aOther.m_sDescription;
    // Shallow copy the jobDataMap. Note that this means that if a user
    // modifies a value object in this map from the cloned Trigger
    // they will also be modifying this Trigger.
    m_aJobDataMap = QCloneUtils.getClone (aOther.m_aJobDataMap);
    m_sCalendarName = aOther.m_sCalendarName;
    m_sFireInstanceId = aOther.m_sFireInstanceId;
    m_eMisfireInstruction = aOther.m_eMisfireInstruction;
    m_nPriority = aOther.m_nPriority;
    m_aKey = aOther.m_aKey;
  }

  /**
   * <p>
   * Create a <code>Trigger</code> with no specified name, group, or
   * <code>{@link com.helger.quartz.IJobDetail}</code>.
   * </p>
   * <p>
   * Note that the {@link #setName(String)},{@link #setGroup(String)}and the
   * {@link #setJobName(String)}and {@link #setJobGroup(String)}methods must be
   * called before the <code>Trigger</code> can be placed into a
   * {@link IScheduler}.
   * </p>
   */
  public AbstractTrigger ()
  {}

  /**
   * <p>
   * Create a <code>Trigger</code> with the given name, and default group.
   * </p>
   * <p>
   * Note that the {@link #setJobName(String)}and
   * {@link #setJobGroup(String)}methods must be called before the
   * <code>Trigger</code> can be placed into a {@link IScheduler}.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if name is null or empty, or the group is an empty string.
   */
  public AbstractTrigger (final String name)
  {
    setName (name);
    setGroup (null);
  }

  /**
   * <p>
   * Create a <code>Trigger</code> with the given name, and group.
   * </p>
   * <p>
   * Note that the {@link #setJobName(String)}and
   * {@link #setJobGroup(String)}methods must be called before the
   * <code>Trigger</code> can be placed into a {@link IScheduler}.
   * </p>
   *
   * @param group
   *        if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
   * @exception IllegalArgumentException
   *            if name is null or empty, or the group is an empty string.
   */
  public AbstractTrigger (final String name, final String group)
  {
    setName (name);
    setGroup (group);
  }

  /**
   * <p>
   * Create a <code>Trigger</code> with the given name, and group.
   * </p>
   *
   * @param group
   *        if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
   * @exception IllegalArgumentException
   *            if name is null or empty, or the group is an empty string.
   */
  public AbstractTrigger (final String name, final String group, final String jobName, final String jobGroup)
  {
    setName (name);
    setGroup (group);
    setJobName (jobName);
    setJobGroup (jobGroup);
  }

  /**
   * Get the name of this <code>Trigger</code>.
   */
  public final String getName ()
  {
    return m_sName;
  }

  /**
   * <p>
   * Set the name of this <code>Trigger</code>.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if name is null or empty.
   */
  public final void setName (@Nonnull final String name)
  {
    ValueEnforcer.notNull (name, "Name");
    ValueEnforcer.isFalse (name.trim ().isEmpty (), "Trigger name cannot be null or empty.");

    m_sName = name;
    m_aKey = null;
  }

  /**
   * Get the group of this <code>Trigger</code>.
   */
  public final String getGroup ()
  {
    return m_sGroup;
  }

  /**
   * Set the name of this <code>Trigger</code>.
   *
   * @param group
   *        if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
   * @exception IllegalArgumentException
   *            if group is an empty string.
   */
  public final void setGroup (@Nullable final String group)
  {
    if (group != null)
      ValueEnforcer.isFalse (group.trim ().isEmpty (), "Group name cannot be an empty string.");

    if (group == null)
      m_sGroup = IScheduler.DEFAULT_GROUP;
    else
      m_sGroup = group;
    m_aKey = null;
  }

  public final void setKey (@Nonnull final TriggerKey key)
  {
    setName (key.getName ());
    setGroup (key.getGroup ());
    m_aKey = key;
  }

  /**
   * <p>
   * Get the name of the associated
   * <code>{@link com.helger.quartz.IJobDetail}</code>.
   * </p>
   */
  public final String getJobName ()
  {
    return m_sJobName;
  }

  /**
   * <p>
   * Set the name of the associated
   * <code>{@link com.helger.quartz.IJobDetail}</code>.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if jobName is null or empty.
   */
  public final void setJobName (@Nonnull final String jobName)
  {
    ValueEnforcer.notNull (jobName, "JobName");
    ValueEnforcer.isFalse (jobName.trim ().isEmpty (), "Job name cannot be null or empty.");

    m_sJobName = jobName;
  }

  /**
   * <p>
   * Get the name of the associated
   * <code>{@link com.helger.quartz.IJobDetail}</code>'s group.
   * </p>
   */
  public final String getJobGroup ()
  {
    return m_sJobGroup;
  }

  /**
   * <p>
   * Set the name of the associated
   * <code>{@link com.helger.quartz.IJobDetail}</code>'s group.
   * </p>
   *
   * @param jobGroup
   *        if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
   * @exception IllegalArgumentException
   *            if group is an empty string.
   */
  public final void setJobGroup (@Nullable final String jobGroup)
  {
    if (jobGroup != null)
      ValueEnforcer.isFalse (jobGroup.trim ().isEmpty (), "Group name cannot be null or empty.");

    if (jobGroup == null)
      m_sJobGroup = IScheduler.DEFAULT_GROUP;
    else
      m_sJobGroup = jobGroup;
  }

  public final void setJobKey (@Nonnull final JobKey key)
  {
    setJobName (key.getName ());
    setJobGroup (key.getGroup ());
  }

  /**
   * Returns the 'full name' of the <code>Trigger</code> in the format
   * "group.name".
   */
  @Nonnull
  public final String getFullName ()
  {
    return m_sGroup + "." + m_sName;
  }

  @Nullable
  public final TriggerKey getKey ()
  {
    if (m_aKey == null)
    {
      if (m_sName == null)
        return null;
      m_aKey = new TriggerKey (m_sName, m_sGroup);
    }

    return m_aKey;
  }

  @Nullable
  public final JobKey getJobKey ()
  {
    if (getJobName () == null)
      return null;

    return new JobKey (getJobName (), getJobGroup ());
  }

  /**
   * Returns the 'full name' of the <code>Job</code> that the
   * <code>Trigger</code> points to, in the format "group.name".
   */
  @Nonnull
  public final String getFullJobName ()
  {
    return m_sJobGroup + "." + m_sJobName;
  }

  @Nullable
  public final String getDescription ()
  {
    return m_sDescription;
  }

  public final void setDescription (@Nullable final String description)
  {
    m_sDescription = description;
  }

  @Nullable
  public final String getCalendarName ()
  {
    return m_sCalendarName;
  }

  public final void setCalendarName (@Nullable final String sCalendarName)
  {
    m_sCalendarName = sCalendarName;
  }

  @Nonnull
  public final JobDataMap getJobDataMap ()
  {
    if (m_aJobDataMap == null)
      m_aJobDataMap = new JobDataMap ();
    return m_aJobDataMap;
  }

  public final void setJobDataMap (@Nullable final JobDataMap jobDataMap)
  {
    m_aJobDataMap = jobDataMap;
  }

  public final int getPriority ()
  {
    return m_nPriority;
  }

  public final void setPriority (final int priority)
  {
    m_nPriority = priority;
  }

  /**
   * This method should not be used by the Quartz client.<br>
   * Called after the <code>{@link IScheduler}</code> has executed the
   * <code>{@link com.helger.quartz.IJobDetail}</code> associated with the
   * <code>Trigger</code> in order to get the final instruction code from the
   * trigger.
   *
   * @param context
   *        is the <code>JobExecutionContext</code> that was used by the
   *        <code>Job</code>'s<code>execute(xx)</code> method.
   * @param result
   *        is the <code>JobExecutionException</code> thrown by the
   *        <code>Job</code>, if any (may be null).
   * @return one of the CompletedExecutionInstruction constants.
   * @see com.helger.quartz.ITrigger.ECompletedExecutionInstruction
   * @see #triggered(ICalendar)
   */
  @Nonnull
  public ITrigger.ECompletedExecutionInstruction executionComplete (final IJobExecutionContext context,
                                                                    @Nullable final JobExecutionException result)
  {
    if (result != null)
    {
      if (result.refireImmediately ())
        return ECompletedExecutionInstruction.RE_EXECUTE_JOB;
      if (result.unscheduleFiringTrigger ())
        return ECompletedExecutionInstruction.SET_TRIGGER_COMPLETE;
      if (result.unscheduleAllTriggers ())
        return ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE;
    }

    if (!mayFireAgain ())
      return ECompletedExecutionInstruction.DELETE_TRIGGER;

    return ECompletedExecutionInstruction.NOOP;
  }

  public final EMisfireInstruction getMisfireInstruction ()
  {
    return m_eMisfireInstruction;
  }

  public final void setMisfireInstruction (final EMisfireInstruction misfireInstruction)
  {
    if (!validateMisfireInstruction (misfireInstruction))
      throw new IllegalArgumentException ("The misfire instruction code is invalid for this type of trigger.");
    m_eMisfireInstruction = misfireInstruction;
  }

  protected abstract boolean validateMisfireInstruction (EMisfireInstruction candidateMisfireInstruction);

  /**
   * <p>
   * Validates whether the properties of the <code>JobDetail</code> are valid
   * for submission into a <code>Scheduler</code>.
   *
   * @throws IllegalStateException
   *         if a required property (such as Name, Group, Class) is not set.
   */
  public void validate () throws SchedulerException
  {
    if (m_sName == null)
      throw new SchedulerException ("Trigger's name cannot be null");

    if (m_sGroup == null)
      throw new SchedulerException ("Trigger's group cannot be null");

    if (m_sJobName == null)
      throw new SchedulerException ("Trigger's related Job's name cannot be null");

    if (m_sJobGroup == null)
      throw new SchedulerException ("Trigger's related Job's group cannot be null");
  }

  @Nullable
  public final String getFireInstanceId ()
  {
    return m_sFireInstanceId;
  }

  public final void setFireInstanceId (@Nullable final String id)
  {
    m_sFireInstanceId = id;
  }

  /**
   * <p>
   * Return a simple string representation of this object.
   * </p>
   */
  @Override
  public String toString ()
  {
    return "Trigger '" +
           getFullName () +
           "':  triggerClass: '" +
           getClass ().getName () +
           " calendar: '" +
           getCalendarName () +
           "' misfireInstruction: " +
           getMisfireInstruction () +
           " nextFireTime: " +
           getNextFireTime ();
  }

  /**
   * <p>
   * Compare the next fire time of this <code>Trigger</code> to that of another
   * by comparing their keys, or in other words, sorts them according to the
   * natural (i.e. alphabetical) order of their keys.
   * </p>
   */
  public int compareTo (@Nonnull final ITrigger aOther)
  {
    return CompareHelper.compare (getKey (), aOther.getKey (), false);
  }

  /**
   * Trigger equality is based upon the equality of the TriggerKey.
   *
   * @return true if the key of this Trigger equals that of the given Trigger.
   */
  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final AbstractTrigger <?> rhs = (AbstractTrigger <?>) o;
    return EqualsHelper.equals (getKey (), rhs.getKey ());

  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (getKey ()).getHashCode ();
  }

  @Nonnull
  public TriggerBuilder <IMPLTYPE> getTriggerBuilder ()
  {
    return TriggerBuilder.newTrigger ()
                         .forJob (getJobKey ())
                         .modifiedByCalendar (getCalendarName ())
                         .usingJobData (getJobDataMap ())
                         .withDescription (getDescription ())
                         .endAt (getEndTime ())
                         .withIdentity (getKey ())
                         .withPriority (getPriority ())
                         .startAt (getStartTime ())
                         .withSchedule (getScheduleBuilder ());
  }

  @Nonnull
  public abstract IScheduleBuilder <IMPLTYPE> getScheduleBuilder ();
}
