/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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

import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJob;
import com.helger.quartz.JobBuilder;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobKey;
import com.helger.quartz.PersistJobDataAfterExecution;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
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
  private String name;

  private String group = IScheduler.DEFAULT_GROUP;

  private String description;

  private Class <? extends IJob> jobClass;

  private JobDataMap jobDataMap;

  private boolean durability = false;

  private boolean shouldRecover = false;

  private transient JobKey key = null;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

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

  @Deprecated
  public JobDetail (final String name, final String group, final Class <? extends IJob> jobClass)
  {
    setName (name);
    setGroup (group);
    setJobClass (jobClass);
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Get the name of this <code>Job</code>.
   * </p>
   */
  public String getName ()
  {
    return name;
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

    this.name = name;
    this.key = null;
  }

  /**
   * <p>
   * Get the group of this <code>Job</code>.
   * </p>
   */
  public String getGroup ()
  {
    return group;
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
      this.group = IScheduler.DEFAULT_GROUP;
    else
      this.group = group;
    this.key = null;
  }

  /**
   * <p>
   * Returns the 'full name' of the <code>JobDetail</code> in the format
   * "group.name".
   * </p>
   */
  public String getFullName ()
  {
    return group + "." + name;
  }

  public JobKey getKey ()
  {
    if (key == null)
    {
      if (getName () == null)
        return null;
      key = new JobKey (getName (), getGroup ());
    }

    return key;
  }

  public void setKey (final JobKey key)
  {
    if (key == null)
      throw new IllegalArgumentException ("Key cannot be null!");

    setName (key.getName ());
    setGroup (key.getGroup ());
    this.key = key;
  }

  public String getDescription ()
  {
    return description;
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
    this.description = description;
  }

  public Class <? extends IJob> getJobClass ()
  {
    return jobClass;
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

    this.jobClass = jobClass;
  }

  public JobDataMap getJobDataMap ()
  {
    if (jobDataMap == null)
    {
      jobDataMap = new JobDataMap ();
    }
    return jobDataMap;
  }

  /**
   * <p>
   * Set the <code>JobDataMap</code> to be associated with the <code>Job</code>.
   * </p>
   */
  public void setJobDataMap (final JobDataMap jobDataMap)
  {
    this.jobDataMap = jobDataMap;
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
    this.durability = durability;
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
    this.shouldRecover = shouldRecover;
  }

  public boolean isDurable ()
  {
    return durability;
  }

  /**
   * @return whether the associated Job class carries the
   *         {@link PersistJobDataAfterExecution} annotation.
   */
  public boolean isPersistJobDataAfterExecution ()
  {

    return ClassUtils.isAnnotationPresent (jobClass, PersistJobDataAfterExecution.class);
  }

  /**
   * @return whether the associated Job class carries the
   *         {@link DisallowConcurrentExecution} annotation.
   */
  public boolean isConcurrentExectionDisallowed ()
  {

    return ClassUtils.isAnnotationPresent (jobClass, DisallowConcurrentExecution.class);
  }

  public boolean requestsRecovery ()
  {
    return shouldRecover;
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
  public Object clone ()
  {
    JobDetail copy;
    try
    {
      copy = (JobDetail) super.clone ();
      if (jobDataMap != null)
      {
        copy.jobDataMap = (JobDataMap) jobDataMap.clone ();
      }
    }
    catch (final CloneNotSupportedException ex)
    {
      throw new IncompatibleClassChangeError ("Not Cloneable.");
    }

    return copy;
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
}