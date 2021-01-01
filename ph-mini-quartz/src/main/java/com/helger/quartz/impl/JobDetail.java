/**
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
package com.helger.quartz.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IScheduler;
import com.helger.quartz.JobBuilder;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobKey;
import com.helger.quartz.PersistJobDataAfterExecution;
import com.helger.quartz.QCloneUtils;
import com.helger.quartz.utils.ClassUtils;

/**
 * <p>
 * Conveys the detail properties of a given <code>Job</code> instance.
 * </p>
 * <p>
 * Quartz does not store an actual instance of a <code>Job</code> class, but
 * instead allows you to define an instance of one, through the use of a
 * <code>JobDetail</code>.
 * </p>
 * <p>
 * <code>Job</code>s have a name and group associated with them, which should
 * uniquely identify them within a single <code>{@link IScheduler}</code>.
 * </p>
 * <p>
 * <code>Trigger</code>s are the 'mechanism' by which <code>Job</code>s are
 * scheduled. Many <code>Trigger</code>s can point to the same <code>Job</code>,
 * but a single <code>Trigger</code> can only point to one <code>Job</code>.
 * </p>
 *
 * @see IJob
 * @see JobDataMap
 * @see com.helger.quartz.ITrigger
 * @author James House
 * @author Sharada Jambula
 */
public class JobDetail implements IJobDetail
{
  private String m_sName;
  private String m_sGroup = IScheduler.DEFAULT_GROUP;
  private String m_sDescription;
  private Class <? extends IJob> m_aJobClass;
  private JobDataMap m_aJobDataMap;
  private boolean m_bDurability = false;
  private boolean m_bShouldRecover = false;
  private transient JobKey m_aKey;

  public JobDetail (@Nonnull final JobDetail aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");
    m_sName = aOther.m_sName;
    m_sGroup = aOther.m_sGroup;
    m_sDescription = aOther.m_sDescription;
    m_aJobClass = aOther.m_aJobClass;
    m_aJobDataMap = QCloneUtils.getClone (aOther.m_aJobDataMap);
    m_bDurability = aOther.m_bDurability;
    m_bShouldRecover = aOther.m_bShouldRecover;
    m_aKey = aOther.m_aKey;
  }

  /**
   * Create a <code>JobDetail</code> with no specified name or group, and the
   * default settings of all the other properties.<br>
   * Note that the {@link #setName(String)},{@link #setGroup(String)}and
   * {@link #setJobClass(Class)}methods must be called before the job can be
   * placed into a {@link IScheduler}
   */
  public JobDetail ()
  {}

  /**
   * Get the name of this <code>Job</code>.
   */
  public final String getName ()
  {
    return m_sName;
  }

  /**
   * <p>
   * Set the name of this <code>Job</code>.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if name is null or empty.
   */
  public final void setName (final String name)
  {
    ValueEnforcer.notNull (name, "Name");
    ValueEnforcer.isFalse (name.trim ().isEmpty (), "Job name cannot be empty.");

    m_sName = name;
    m_aKey = null;
  }

  /**
   * Get the group of this <code>Job</code>.
   */
  public final String getGroup ()
  {
    return m_sGroup;
  }

  /**
   * <p>
   * Set the group of this <code>Job</code>.
   * </p>
   *
   * @param group
   *        if <code>null</code>, Scheduler.DEFAULT_GROUP will be used.
   * @exception IllegalArgumentException
   *            if the group is an empty string.
   */
  public final void setGroup (final String group)
  {
    if (group != null)
      ValueEnforcer.isFalse (group.trim ().isEmpty (), "Group name cannot be empty.");

    if (group == null)
      m_sGroup = IScheduler.DEFAULT_GROUP;
    else
      m_sGroup = group;
    m_aKey = null;
  }

  /**
   * Returns the 'full name' of the <code>JobDetail</code> in the format
   * "group.name".
   */
  @Nonnull
  public final String getFullName ()
  {
    return m_sGroup + "." + m_sName;
  }

  @Nullable
  public final JobKey getKey ()
  {
    if (m_aKey == null)
    {
      if (m_sName == null)
        return null;
      m_aKey = new JobKey (m_sName, getGroup ());
    }

    return m_aKey;
  }

  public final void setKey (@Nonnull final JobKey key)
  {
    ValueEnforcer.notNull (key, "Key");

    setName (key.getName ());
    setGroup (key.getGroup ());
    m_aKey = key;
  }

  public final String getDescription ()
  {
    return m_sDescription;
  }

  /**
   * Set a description for the <code>Job</code> instance - may be useful for
   * remembering/displaying the purpose of the job, though the description has
   * no meaning to Quartz.
   */
  public final void setDescription (final String description)
  {
    m_sDescription = description;
  }

  public final Class <? extends IJob> getJobClass ()
  {
    return m_aJobClass;
  }

  /**
   * <p>
   * Set the instance of <code>Job</code> that will be executed.
   * </p>
   *
   * @exception IllegalArgumentException
   *            if jobClass is null or the class is not a <code>Job</code>.
   */
  public final void setJobClass (final Class <? extends IJob> jobClass)
  {
    ValueEnforcer.notNull (jobClass, "JobClass");
    m_aJobClass = jobClass;
  }

  @Nonnull
  public JobDataMap getJobDataMap ()
  {
    if (m_aJobDataMap == null)
      m_aJobDataMap = new JobDataMap ();
    return m_aJobDataMap;
  }

  /**
   * Set the <code>JobDataMap</code> to be associated with the <code>Job</code>.
   *
   * @param jobDataMap
   *        May be <code>null</code>.
   */
  public void setJobDataMap (@Nullable final JobDataMap jobDataMap)
  {
    m_aJobDataMap = jobDataMap;
  }

  /**
   * Set whether or not the <code>Job</code> should remain stored after it is
   * orphaned (no <code>Triggers</code> point to it).<br>
   * If not explicitly set, the default value is <code>false</code>.
   */
  public void setDurability (final boolean durability)
  {
    m_bDurability = durability;
  }

  /**
   * Set whether or not the the <code>Scheduler</code> should re-execute the
   * <code>Job</code> if a 'recovery' or 'fail-over' situation is
   * encountered.<br>
   * If not explicitly set, the default value is <code>false</code>.
   *
   * @see com.helger.quartz.IJobExecutionContext#isRecovering()
   */
  public void setRequestsRecovery (final boolean shouldRecover)
  {
    m_bShouldRecover = shouldRecover;
  }

  public boolean isDurable ()
  {
    return m_bDurability;
  }

  /**
   * @return whether the associated Job class carries the
   *         {@link PersistJobDataAfterExecution} annotation.
   */
  public boolean isPersistJobDataAfterExecution ()
  {
    return ClassUtils.isAnnotationPresent (m_aJobClass, PersistJobDataAfterExecution.class);
  }

  /**
   * @return whether the associated Job class carries the
   *         {@link DisallowConcurrentExecution} annotation.
   */
  public boolean isConcurrentExectionDisallowed ()
  {
    return ClassUtils.isAnnotationPresent (m_aJobClass, DisallowConcurrentExecution.class);
  }

  public boolean requestsRecovery ()
  {
    return m_bShouldRecover;
  }

  /**
   * <p>
   * Return a simple string representation of this object.
   * </p>
   */
  @Override
  public String toString ()
  {
    return "JobDetail '" +
           getFullName () +
           "':  jobClass: '" +
           (getJobClass () == null ? null : getJobClass ().getName ()) +
           " concurrentExectionDisallowed: " +
           isConcurrentExectionDisallowed () +
           " persistJobDataAfterExecution: " +
           isPersistJobDataAfterExecution () +
           " isDurable: " +
           isDurable () +
           " requestsRecovers: " +
           requestsRecovery ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final JobDetail rhs = (JobDetail) o;
    return EqualsHelper.equals (getKey (), rhs.getKey ());
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (getKey ()).getHashCode ();
  }

  @Nonnull
  public JobBuilder getJobBuilder ()
  {
    return JobBuilder.newJob ()
                     .ofType (getJobClass ())
                     .requestRecovery (requestsRecovery ())
                     .storeDurably (isDurable ())
                     .usingJobData (getJobDataMap ())
                     .withDescription (getDescription ())
                     .withIdentity (getKey ());
  }

  @Nonnull
  public JobDetail getClone ()
  {
    return new JobDetail (this);
  }

  @Nonnull
  public static JobDetail create (final String name, final String group, final Class <? extends IJob> jobClass)
  {
    final JobDetail ret = new JobDetail ();
    ret.setName (name);
    ret.setGroup (group);
    ret.setJobClass (jobClass);
    return ret;
  }
}
