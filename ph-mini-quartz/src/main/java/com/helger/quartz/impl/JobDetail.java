/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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

import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobBuilder;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobKey;
import com.helger.quartz.PersistJobDataAfterExecution;
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
 * @see ITrigger
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

  /**
   * <p>
   * Create a <code>JobDetail</code> with no specified name or group, and the
   * default settings of all the other properties.
   * </p>
   * <p>
   * Note that the {@link #setName(String)},{@link #setGroup(String)}and
   * {@link #setJobClass(Class)}methods must be called before the job can be
   * placed into a {@link IScheduler}
   * </p>
   */
  public JobDetail ()
  {}

  /**
   * <p>
   * Get the name of this <code>Job</code>.
   * </p>
   */
  public String getName ()
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
  public void setName (final String name)
  {
    if (name == null || name.trim ().length () == 0)
    {
      throw new IllegalArgumentException ("Job name cannot be empty.");
    }

    m_sName = name;
    m_aKey = null;
  }

  /**
   * <p>
   * Get the group of this <code>Job</code>.
   * </p>
   */
  public String getGroup ()
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
  public void setGroup (final String group)
  {
    if (group != null && group.trim ().length () == 0)
    {
      throw new IllegalArgumentException ("Group name cannot be empty.");
    }

    if (group == null)
      m_sGroup = IScheduler.DEFAULT_GROUP;
    else
      m_sGroup = group;
    m_aKey = null;
  }

  /**
   * <p>
   * Returns the 'full name' of the <code>JobDetail</code> in the format
   * "group.name".
   * </p>
   */
  public String getFullName ()
  {
    return m_sGroup + "." + m_sName;
  }

  public JobKey getKey ()
  {
    if (m_aKey == null)
    {
      if (getName () == null)
        return null;
      m_aKey = new JobKey (getName (), getGroup ());
    }

    return m_aKey;
  }

  public void setKey (final JobKey key)
  {
    if (key == null)
      throw new IllegalArgumentException ("Key cannot be null!");

    setName (key.getName ());
    setGroup (key.getGroup ());
    m_aKey = key;
  }

  public String getDescription ()
  {
    return m_sDescription;
  }

  /**
   * <p>
   * Set a description for the <code>Job</code> instance - may be useful for
   * remembering/displaying the purpose of the job, though the description has
   * no meaning to Quartz.
   * </p>
   */
  public void setDescription (final String description)
  {
    m_sDescription = description;
  }

  public Class <? extends IJob> getJobClass ()
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
  public void setJobClass (final Class <? extends IJob> jobClass)
  {
    if (jobClass == null)
    {
      throw new IllegalArgumentException ("Job class cannot be null.");
    }

    if (!IJob.class.isAssignableFrom (jobClass))
    {
      throw new IllegalArgumentException ("Job class must implement the Job interface.");
    }

    m_aJobClass = jobClass;
  }

  public JobDataMap getJobDataMap ()
  {
    if (m_aJobDataMap == null)
    {
      m_aJobDataMap = new JobDataMap ();
    }
    return m_aJobDataMap;
  }

  /**
   * <p>
   * Set the <code>JobDataMap</code> to be associated with the <code>Job</code>.
   * </p>
   */
  public void setJobDataMap (final JobDataMap jobDataMap)
  {
    m_aJobDataMap = jobDataMap;
  }

  /**
   * <p>
   * Set whether or not the <code>Job</code> should remain stored after it is
   * orphaned (no <code>{@link ITrigger}s</code> point to it).
   * </p>
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   */
  public void setDurability (final boolean durability)
  {
    m_bDurability = durability;
  }

  /**
   * <p>
   * Set whether or not the the <code>Scheduler</code> should re-execute the
   * <code>Job</code> if a 'recovery' or 'fail-over' situation is encountered.
   * </p>
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   *
   * @see IJobExecutionContext#isRecovering()
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
           ((getJobClass () == null) ? null : getJobClass ().getName ()) +
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
  public boolean equals (final Object obj)
  {
    if (!(obj instanceof IJobDetail))
    {
      return false;
    }

    final IJobDetail other = (IJobDetail) obj;

    if (other.getKey () == null || getKey () == null)
      return false;

    if (!other.getKey ().equals (getKey ()))
    {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode ()
  {
    final JobKey key = getKey ();
    return key == null ? 0 : getKey ().hashCode ();
  }

  @Override
  public JobDetail clone ()
  {
    try
    {
      final JobDetail copy = (JobDetail) super.clone ();
      if (m_aJobDataMap != null)
        copy.m_aJobDataMap = (JobDataMap) m_aJobDataMap.clone ();
      return copy;
    }
    catch (final CloneNotSupportedException ex)
    {
      throw new IncompatibleClassChangeError ("Not Cloneable.");
    }
  }

  public JobBuilder getJobBuilder ()
  {
    final JobBuilder b = JobBuilder.newJob ()
                                   .ofType (getJobClass ())
                                   .requestRecovery (requestsRecovery ())
                                   .storeDurably (isDurable ())
                                   .usingJobData (getJobDataMap ())
                                   .withDescription (getDescription ())
                                   .withIdentity (getKey ());
    return b;
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
