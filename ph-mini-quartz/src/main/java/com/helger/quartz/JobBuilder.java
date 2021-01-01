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
package com.helger.quartz;

import com.helger.quartz.impl.JobDetail;
import com.helger.quartz.utils.Key;

/**
 * <code>JobBuilder</code> is used to instantiate {@link IJobDetail}s.
 * <p>
 * The builder will always try to keep itself in a valid state, with reasonable
 * defaults set for calling build() at any point. For instance if you do not
 * invoke <i>withIdentity(..)</i> a job name will be generated for you.
 * </p>
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related
 * entities via a Domain-Specific Language (DSL). The DSL can best be utilized
 * through the usage of static imports of the methods on the classes
 * <code>TriggerBuilder</code>, <code>JobBuilder</code>,
 * <code>DateBuilder</code>, <code>JobKey</code>, <code>TriggerKey</code> and
 * the various <code>ScheduleBuilder</code> implementations.
 * </p>
 * <p>
 * Client code can then use the DSL to write code such as this:
 * </p>
 *
 * <pre>
 * JobDetail job = newJob (MyJob.class).withIdentity ("myJob").build ();
 * Trigger trigger = newTrigger ().withIdentity (triggerKey ("myTrigger", "myTriggerGroup"))
 *                                .withSchedule (simpleSchedule ().withIntervalInHours (1).repeatForever ())
 *                                .startAt (futureDate (10, MINUTES))
 *                                .build ();
 * scheduler.scheduleJob (job, trigger);
 * </pre>
 *
 * @see TriggerBuilder
 * @see DateBuilder
 * @see IJobDetail
 */
public class JobBuilder
{
  private JobKey m_aKey;
  private String m_sDescription;
  private Class <? extends IJob> m_aJobClass;
  private boolean m_bDurability;
  private boolean m_bShouldRecover;

  private JobDataMap m_aJobDataMap = new JobDataMap ();

  protected JobBuilder ()
  {}

  /**
   * Create a JobBuilder with which to define a <code>JobDetail</code>.
   *
   * @return a new JobBuilder
   */
  public static JobBuilder newJob ()
  {
    return new JobBuilder ();
  }

  /**
   * Create a JobBuilder with which to define a <code>JobDetail</code>, and set
   * the class name of the <code>Job</code> to be executed.
   *
   * @return a new JobBuilder
   */
  public static JobBuilder newJob (final Class <? extends IJob> jobClass)
  {
    final JobBuilder b = new JobBuilder ();
    b.ofType (jobClass);
    return b;
  }

  /**
   * Produce the <code>JobDetail</code> instance defined by this
   * <code>JobBuilder</code>.
   *
   * @return the defined JobDetail.
   */
  public IJobDetail build ()
  {

    final JobDetail job = new JobDetail ();

    job.setJobClass (m_aJobClass);
    job.setDescription (m_sDescription);
    if (m_aKey == null)
      m_aKey = new JobKey (Key.createUniqueName (null), null);
    job.setKey (m_aKey);
    job.setDurability (m_bDurability);
    job.setRequestsRecovery (m_bShouldRecover);

    if (!m_aJobDataMap.isEmpty ())
      job.setJobDataMap (m_aJobDataMap);

    return job;
  }

  /**
   * Use a <code>JobKey</code> with the given name and default group to identify
   * the JobDetail.
   * <p>
   * If none of the 'withIdentity' methods are set on the JobBuilder, then a
   * random, unique JobKey will be generated.
   * </p>
   *
   * @param name
   *        the name element for the Job's JobKey
   * @return the updated JobBuilder
   * @see JobKey
   * @see IJobDetail#getKey()
   */
  public JobBuilder withIdentity (final String name)
  {
    m_aKey = new JobKey (name, null);
    return this;
  }

  /**
   * Use a <code>JobKey</code> with the given name and group to identify the
   * JobDetail.
   * <p>
   * If none of the 'withIdentity' methods are set on the JobBuilder, then a
   * random, unique JobKey will be generated.
   * </p>
   *
   * @param name
   *        the name element for the Job's JobKey
   * @param group
   *        the group element for the Job's JobKey
   * @return the updated JobBuilder
   * @see JobKey
   * @see IJobDetail#getKey()
   */
  public JobBuilder withIdentity (final String name, final String group)
  {
    m_aKey = new JobKey (name, group);
    return this;
  }

  /**
   * Use a <code>JobKey</code> to identify the JobDetail.
   * <p>
   * If none of the 'withIdentity' methods are set on the JobBuilder, then a
   * random, unique JobKey will be generated.
   * </p>
   *
   * @param jobKey
   *        the Job's JobKey
   * @return the updated JobBuilder
   * @see JobKey
   * @see IJobDetail#getKey()
   */
  public JobBuilder withIdentity (final JobKey jobKey)
  {
    m_aKey = jobKey;
    return this;
  }

  /**
   * Set the given (human-meaningful) description of the Job.
   *
   * @param jobDescription
   *        the description for the Job
   * @return the updated JobBuilder
   * @see IJobDetail#getDescription()
   */
  public JobBuilder withDescription (final String jobDescription)
  {
    m_sDescription = jobDescription;
    return this;
  }

  /**
   * Set the class which will be instantiated and executed when a Trigger fires
   * that is associated with this JobDetail.
   *
   * @param jobClazz
   *        a class implementing the Job interface.
   * @return the updated JobBuilder
   * @see IJobDetail#getJobClass()
   */
  public JobBuilder ofType (final Class <? extends IJob> jobClazz)
  {
    m_aJobClass = jobClazz;
    return this;
  }

  /**
   * Instructs the <code>Scheduler</code> whether or not the <code>Job</code>
   * should be re-executed if a 'recovery' or 'fail-over' situation is
   * encountered.
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   *
   * @return the updated JobBuilder
   * @see IJobDetail#requestsRecovery()
   */
  public JobBuilder requestRecovery ()
  {
    m_bShouldRecover = true;
    return this;
  }

  /**
   * Instructs the <code>Scheduler</code> whether or not the <code>Job</code>
   * should be re-executed if a 'recovery' or 'fail-over' situation is
   * encountered.
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   *
   * @param jobShouldRecover
   *        the desired setting
   * @return the updated JobBuilder
   */
  public JobBuilder requestRecovery (final boolean jobShouldRecover)
  {
    m_bShouldRecover = jobShouldRecover;
    return this;
  }

  /**
   * Whether or not the <code>Job</code> should remain stored after it is
   * orphaned (no <code>{@link ITrigger}s</code> point to it).
   * <p>
   * If not explicitly set, the default value is <code>false</code> - this
   * method sets the value to <code>true</code>.
   * </p>
   *
   * @return the updated JobBuilder
   * @see IJobDetail#isDurable()
   */
  public JobBuilder storeDurably ()
  {
    m_bDurability = true;
    return this;
  }

  /**
   * Whether or not the <code>Job</code> should remain stored after it is
   * orphaned (no <code>{@link ITrigger}s</code> point to it).
   * <p>
   * If not explicitly set, the default value is <code>false</code>.
   * </p>
   *
   * @param jobDurability
   *        the value to set for the durability property.
   * @return the updated JobBuilder
   * @see IJobDetail#isDurable()
   */
  public JobBuilder storeDurably (final boolean jobDurability)
  {
    m_bDurability = jobDurability;
    return this;
  }

  /**
   * Add the given key-value pair to the JobDetail's {@link JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder usingJobData (final String dataKey, final String value)
  {
    m_aJobDataMap.put (dataKey, value);
    return this;
  }

  /**
   * Add the given key-value pair to the JobDetail's {@link JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder usingJobData (final String dataKey, final Integer value)
  {
    m_aJobDataMap.put (dataKey, value);
    return this;
  }

  /**
   * Add the given key-value pair to the JobDetail's {@link JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder usingJobData (final String dataKey, final Long value)
  {
    m_aJobDataMap.put (dataKey, value);
    return this;
  }

  /**
   * Add the given key-value pair to the JobDetail's {@link JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder usingJobData (final String dataKey, final Float value)
  {
    m_aJobDataMap.put (dataKey, value);
    return this;
  }

  /**
   * Add the given key-value pair to the JobDetail's {@link JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder usingJobData (final String dataKey, final Double value)
  {
    m_aJobDataMap.put (dataKey, value);
    return this;
  }

  /**
   * Add the given key-value pair to the JobDetail's {@link JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder usingJobData (final String dataKey, final Boolean value)
  {
    m_aJobDataMap.put (dataKey, value);
    return this;
  }

  /**
   * Add all the data from the given {@link JobDataMap} to the
   * {@code JobDetail}'s {@code JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder usingJobData (final JobDataMap newJobDataMap)
  {
    m_aJobDataMap.putAll (newJobDataMap);
    return this;
  }

  /**
   * Replace the {@code JobDetail}'s {@link JobDataMap} with the given
   * {@code JobDataMap}.
   *
   * @return the updated JobBuilder
   * @see IJobDetail#getJobDataMap()
   */
  public JobBuilder setJobData (final JobDataMap newJobDataMap)
  {
    m_aJobDataMap = newJobDataMap;
    return this;
  }
}
