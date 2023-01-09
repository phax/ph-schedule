/*
 * Copyright (C) 2014-2023 Philip Helger (www.helger.com)
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
package com.helger.schedule.quartz.trigger;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.lang.GenericReflection;
import com.helger.quartz.DateBuilder;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IScheduleBuilder;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobBuilder;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobKey;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.spi.IMutableTrigger;
import com.helger.quartz.utils.Key;

/**
 * <code>JDK8TriggerBuilder</code> is used to instantiate {@link ITrigger}s.
 * <p>
 * The builder will always try to keep itself in a valid state, with reasonable
 * defaults set for calling build() at any point. For instance if you do not
 * invoke <i>withSchedule(..)</i> method, a default schedule of firing once
 * immediately will be used. As another example, if you do not invoked
 * <i>withIdentity(..)</i> a trigger name will be generated for you.
 * </p>
 * <p>
 * Quartz provides a builder-style API for constructing scheduling-related
 * entities via a Domain-Specific Language (DSL). The DSL can best be utilized
 * through the usage of static imports of the methods on the classes
 * <code>JDK8TriggerBuilder</code>, <code>JobBuilder</code>,
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
 * @see JobBuilder
 * @see IScheduleBuilder
 * @see DateBuilder
 * @see ITrigger
 * @param <T>
 *        Trigger type to create
 */
public class JDK8TriggerBuilder <T extends ITrigger>
{
  private TriggerKey m_aTriggerKey;
  private String m_sDescription;
  private LocalDateTime m_aStartTime = PDTFactory.getCurrentLocalDateTime ();
  private LocalDateTime m_aEndTime;
  private int m_nPriority = ITrigger.DEFAULT_PRIORITY;
  private String m_sCalendarName;
  private JobKey m_aJobKey;
  private JobDataMap m_aJobDataMap = new JobDataMap ();
  private IScheduleBuilder <? extends ITrigger> m_aScheduleBuilder;

  protected JDK8TriggerBuilder ()
  {}

  /**
   * Create a new JDK8TriggerBuilder with which to define a specification for a
   * Trigger.
   *
   * @return the new JDK8TriggerBuilder
   */
  @Nonnull
  public static JDK8TriggerBuilder <ITrigger> newTrigger ()
  {
    return new JDK8TriggerBuilder <> ();
  }

  /**
   * Produce the <code>Trigger</code>.
   *
   * @return a Trigger that meets the specifications of the builder.
   */
  @Nonnull
  public T build ()
  {
    if (m_aScheduleBuilder == null)
      m_aScheduleBuilder = SimpleScheduleBuilder.simpleSchedule ();
    final IMutableTrigger trig = m_aScheduleBuilder.build ();
    trig.setCalendarName (m_sCalendarName);
    trig.setDescription (m_sDescription);
    trig.setStartTime (PDTFactory.createDate (m_aStartTime));
    trig.setEndTime (PDTFactory.createDate (m_aEndTime));
    if (m_aTriggerKey == null)
      m_aTriggerKey = new TriggerKey (Key.createUniqueName (null), null);
    trig.setKey (m_aTriggerKey);
    if (m_aJobKey != null)
      trig.setJobKey (m_aJobKey);
    trig.setPriority (m_nPriority);

    if (!m_aJobDataMap.isEmpty ())
      trig.setJobDataMap (m_aJobDataMap);
    return GenericReflection.uncheckedCast (trig);
  }

  /**
   * Use a <code>TriggerKey</code> with the given name and default group to
   * identify the Trigger.
   * <p>
   * If none of the 'withIdentity' methods are set on the JDK8TriggerBuilder,
   * then a random, unique TriggerKey will be generated.
   * </p>
   *
   * @param name
   *        the name element for the Trigger's TriggerKey
   * @return the updated JDK8TriggerBuilder
   * @see TriggerKey
   * @see ITrigger#getKey()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> withIdentity (final String name)
  {
    m_aTriggerKey = new TriggerKey (name, null);
    return this;
  }

  /**
   * Use a TriggerKey with the given name and group to identify the Trigger.
   * <p>
   * If none of the 'withIdentity' methods are set on the JDK8TriggerBuilder,
   * then a random, unique TriggerKey will be generated.
   * </p>
   *
   * @param name
   *        the name element for the Trigger's TriggerKey
   * @param group
   *        the group element for the Trigger's TriggerKey
   * @return the updated JDK8TriggerBuilder
   * @see TriggerKey
   * @see ITrigger#getKey()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> withIdentity (final String name, final String group)
  {
    m_aTriggerKey = new TriggerKey (name, group);
    return this;
  }

  /**
   * Use the given TriggerKey to identify the Trigger.
   * <p>
   * If none of the 'withIdentity' methods are set on the JDK8TriggerBuilder,
   * then a random, unique TriggerKey will be generated.
   * </p>
   *
   * @param triggerKey
   *        the TriggerKey for the Trigger to be built
   * @return the updated JDK8TriggerBuilder
   * @see TriggerKey
   * @see ITrigger#getKey()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> withIdentity (final TriggerKey triggerKey)
  {
    m_aTriggerKey = triggerKey;
    return this;
  }

  /**
   * Set the given (human-meaningful) description of the Trigger.
   *
   * @param triggerDescription
   *        the description for the Trigger
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getDescription()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> withDescription (final String triggerDescription)
  {
    m_sDescription = triggerDescription;
    return this;
  }

  /**
   * Set the Trigger's priority. When more than one Trigger have the same fire
   * time, the scheduler will fire the one with the highest priority first.
   *
   * @param triggerPriority
   *        the priority for the Trigger
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#DEFAULT_PRIORITY
   * @see ITrigger#getPriority()
   */
  public JDK8TriggerBuilder <T> withPriority (final int triggerPriority)
  {
    m_nPriority = triggerPriority;
    return this;
  }

  /**
   * Set the name of the {@link ICalendar} that should be applied to this
   * Trigger's schedule.
   *
   * @param calName
   *        the name of the Calendar to reference.
   * @return the updated JDK8TriggerBuilder
   * @see ICalendar
   * @see ITrigger#getCalendarName()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> modifiedByCalendar (final String calName)
  {
    m_sCalendarName = calName;
    return this;
  }

  /**
   * Set the time the Trigger should start at - the trigger may or may not fire
   * at this time - depending upon the schedule configured for the Trigger.
   * However the Trigger will NOT fire before this time, regardless of the
   * Trigger's schedule.
   *
   * @param triggerStartTime
   *        the start time for the Trigger.
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getStartTime()
   * @see DateBuilder
   */
  @Nonnull
  public JDK8TriggerBuilder <T> startAt (final LocalDateTime triggerStartTime)
  {
    m_aStartTime = triggerStartTime;
    return this;
  }

  /**
   * Set the time the Trigger should start at to the current moment - the
   * trigger may or may not fire at this time - depending upon the schedule
   * configured for the Trigger.
   *
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getStartTime()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> startNow ()
  {
    return startAt (PDTFactory.getCurrentLocalDateTime ());
  }

  /**
   * Set the time at which the Trigger will no longer fire - even if it's
   * schedule has remaining repeats.
   *
   * @param triggerEndTime
   *        the end time for the Trigger. If null, the end time is indefinite.
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getEndTime()
   * @see DateBuilder
   */
  @Nonnull
  public JDK8TriggerBuilder <T> endAt (final LocalDateTime triggerEndTime)
  {
    m_aEndTime = triggerEndTime;
    return this;
  }

  /**
   * Set the {@link IScheduleBuilder} that will be used to define the Trigger's
   * schedule.
   * <p>
   * The particular <code>SchedulerBuilder</code> used will dictate the concrete
   * type of Trigger that is produced by the JDK8TriggerBuilder.
   * </p>
   *
   * @param schedBuilder
   *        the SchedulerBuilder to use.
   * @return the updated JDK8TriggerBuilder
   * @see SimpleScheduleBuilder
   */
  @Nonnull
  public JDK8TriggerBuilder <T> withSchedule (final IScheduleBuilder <? extends T> schedBuilder)
  {
    m_aScheduleBuilder = schedBuilder;
    return this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger.
   *
   * @param keyOfJobToFire
   *        the identity of the Job to fire.
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobKey()
   */
  public JDK8TriggerBuilder <T> forJob (final JobKey keyOfJobToFire)
  {
    m_aJobKey = keyOfJobToFire;
    return this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger -
   * a <code>JobKey</code> will be produced with the given name and default
   * group.
   *
   * @param jobName
   *        the name of the job (in default group) to fire.
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobKey()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> forJob (final String jobName)
  {
    m_aJobKey = new JobKey (jobName, null);
    return this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger -
   * a <code>JobKey</code> will be produced with the given name and group.
   *
   * @param jobName
   *        the name of the job to fire.
   * @param jobGroup
   *        the group of the job to fire.
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobKey()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> forJob (final String jobName, final String jobGroup)
  {
    m_aJobKey = new JobKey (jobName, jobGroup);
    return this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger,
   * by extracting the JobKey from the given job.
   *
   * @param jobDetail
   *        the Job to fire.
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobKey()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> forJob (final IJobDetail jobDetail)
  {
    final JobKey k = jobDetail.getKey ();
    if (k.getName () == null)
      throw new IllegalArgumentException ("The given job has not yet had a name assigned to it.");
    m_aJobKey = k;
    return this;
  }

  /**
   * Add the given key-value pair to the Trigger's {@link JobDataMap}.
   *
   * @param dataKey
   *        Job data key.
   * @param value
   *        Job data value
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobDataMap()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> usingJobData (final String dataKey, final Object value)
  {
    m_aJobDataMap.put (dataKey, value);
    return this;
  }

  /**
   * Add the given key-value pair to the Trigger's {@link JobDataMap}.
   *
   * @param dataKey
   *        Job data key.
   * @param value
   *        Job data value
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobDataMap()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> usingJobData (final String dataKey, final int value)
  {
    return usingJobData (dataKey, Integer.valueOf (value));
  }

  /**
   * Add the given key-value pair to the Trigger's {@link JobDataMap}.
   *
   * @param dataKey
   *        Job data key.
   * @param value
   *        Job data value
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobDataMap()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> usingJobData (final String dataKey, final long value)
  {
    return usingJobData (dataKey, Long.valueOf (value));
  }

  /**
   * Add the given key-value pair to the Trigger's {@link JobDataMap}.
   *
   * @param dataKey
   *        Job data key.
   * @param value
   *        Job data value
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobDataMap()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> usingJobData (final String dataKey, final float value)
  {
    return usingJobData (dataKey, Float.valueOf (value));
  }

  /**
   * Add the given key-value pair to the Trigger's {@link JobDataMap}.
   *
   * @param dataKey
   *        Job data key.
   * @param value
   *        Job data value
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobDataMap()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> usingJobData (final String dataKey, final double value)
  {
    return usingJobData (dataKey, Double.valueOf (value));
  }

  /**
   * Add the given key-value pair to the Trigger's {@link JobDataMap}.
   *
   * @param dataKey
   *        Job data key.
   * @param value
   *        Job data value
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobDataMap()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> usingJobData (final String dataKey, final boolean value)
  {
    return usingJobData (dataKey, Boolean.valueOf (value));
  }

  /**
   * Set the Trigger's {@link JobDataMap}, adding any values to it that were
   * already set on this JDK8TriggerBuilder using any of the other
   * 'usingJobData' methods.
   *
   * @param newJobDataMap
   *        New map to use. May not be <code>null</code>.
   * @return the updated JDK8TriggerBuilder
   * @see ITrigger#getJobDataMap()
   */
  @Nonnull
  public JDK8TriggerBuilder <T> usingJobData (@Nonnull final JobDataMap newJobDataMap)
  {
    // add any existing data to this new map
    newJobDataMap.putAll (m_aJobDataMap);
    m_aJobDataMap = newJobDataMap; // set new map as the map to use
    return this;
  }
}
