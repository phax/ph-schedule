/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
import com.helger.quartz.Calendar;
import com.helger.quartz.DateBuilder;
import com.helger.quartz.JobBuilder;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobDetail;
import com.helger.quartz.JobKey;
import com.helger.quartz.ScheduleBuilder;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.Trigger;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.spi.MutableTrigger;
import com.helger.quartz.utils.Key;

/**
 * <code>JDK8TriggerBuilder</code> is used to instantiate {@link Trigger}s.
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
 * @see ScheduleBuilder
 * @see DateBuilder
 * @see Trigger
 * @param <T>
 *        Trigger type to create
 */
public class JDK8TriggerBuilder <T extends Trigger>
{
  private TriggerKey key;
  private String description;
  private LocalDateTime startTime = PDTFactory.getCurrentLocalDateTime ();
  private LocalDateTime endTime;
  private int priority = Trigger.DEFAULT_PRIORITY;
  private String calendarName;
  private JobKey jobKey;
  private JobDataMap jobDataMap = new JobDataMap ();
  private SimpleScheduleBuilder scheduleBuilder = null;

  protected JDK8TriggerBuilder ()
  {}

  /**
   * Create a new JDK8TriggerBuilder with which to define a specification for a
   * Trigger.
   *
   * @return the new JDK8TriggerBuilder
   */
  public static JDK8TriggerBuilder <Trigger> newTrigger ()
  {
    return new JDK8TriggerBuilder<> ();
  }

  /**
   * Produce the <code>Trigger</code>.
   *
   * @return a Trigger that meets the specifications of the builder.
   */
  @SuppressWarnings ("unchecked")
  public T build ()
  {
    if (scheduleBuilder == null)
      scheduleBuilder = SimpleScheduleBuilder.simpleSchedule ();
    final MutableTrigger trig = scheduleBuilder.build ();
    trig.setCalendarName (calendarName);
    trig.setDescription (description);
    trig.setStartTime (PDTFactory.createDate (startTime));
    trig.setEndTime (PDTFactory.createDate (endTime));
    if (key == null)
      key = new TriggerKey (Key.createUniqueName (null), null);
    trig.setKey (key);
    if (jobKey != null)
      trig.setJobKey (jobKey);
    trig.setPriority (priority);

    if (!jobDataMap.isEmpty ())
      trig.setJobDataMap (jobDataMap);

    return (T) trig;
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
   * @see Trigger#getKey()
   */
  public JDK8TriggerBuilder <T> withIdentity (final String name)
  {
    key = new TriggerKey (name, null);
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
   * @see Trigger#getKey()
   */
  public JDK8TriggerBuilder <T> withIdentity (final String name, final String group)
  {
    key = new TriggerKey (name, group);
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
   * @see Trigger#getKey()
   */
  public JDK8TriggerBuilder <T> withIdentity (final TriggerKey triggerKey)
  {
    this.key = triggerKey;
    return this;
  }

  /**
   * Set the given (human-meaningful) description of the Trigger.
   *
   * @param triggerDescription
   *        the description for the Trigger
   * @return the updated JDK8TriggerBuilder
   * @see Trigger#getDescription()
   */
  public JDK8TriggerBuilder <T> withDescription (final String triggerDescription)
  {
    this.description = triggerDescription;
    return this;
  }

  /**
   * Set the Trigger's priority. When more than one Trigger have the same fire
   * time, the scheduler will fire the one with the highest priority first.
   *
   * @param triggerPriority
   *        the priority for the Trigger
   * @return the updated JDK8TriggerBuilder
   * @see Trigger#DEFAULT_PRIORITY
   * @see Trigger#getPriority()
   */
  public JDK8TriggerBuilder <T> withPriority (final int triggerPriority)
  {
    this.priority = triggerPriority;
    return this;
  }

  /**
   * Set the name of the {@link Calendar} that should be applied to this
   * Trigger's schedule.
   *
   * @param calName
   *        the name of the Calendar to reference.
   * @return the updated JDK8TriggerBuilder
   * @see Calendar
   * @see Trigger#getCalendarName()
   */
  public JDK8TriggerBuilder <T> modifiedByCalendar (final String calName)
  {
    this.calendarName = calName;
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
   * @see Trigger#getStartTime()
   * @see DateBuilder
   */
  public JDK8TriggerBuilder <T> startAt (final LocalDateTime triggerStartTime)
  {
    this.startTime = triggerStartTime;
    return this;
  }

  /**
   * Set the time the Trigger should start at to the current moment - the
   * trigger may or may not fire at this time - depending upon the schedule
   * configured for the Trigger.
   *
   * @return the updated JDK8TriggerBuilder
   * @see Trigger#getStartTime()
   */
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
   * @see Trigger#getEndTime()
   * @see DateBuilder
   */
  public JDK8TriggerBuilder <T> endAt (final LocalDateTime triggerEndTime)
  {
    this.endTime = triggerEndTime;
    return this;
  }

  /**
   * Set the {@link ScheduleBuilder} that will be used to define the Trigger's
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
  public JDK8TriggerBuilder <T> withSchedule (final SimpleScheduleBuilder schedBuilder)
  {
    this.scheduleBuilder = schedBuilder;
    return this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger.
   *
   * @param keyOfJobToFire
   *        the identity of the Job to fire.
   * @return the updated JDK8TriggerBuilder
   * @see Trigger#getJobKey()
   */
  public JDK8TriggerBuilder <T> forJob (final JobKey keyOfJobToFire)
  {
    this.jobKey = keyOfJobToFire;
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
   * @see Trigger#getJobKey()
   */
  public JDK8TriggerBuilder <T> forJob (final String jobName)
  {
    this.jobKey = new JobKey (jobName, null);
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
   * @see Trigger#getJobKey()
   */
  public JDK8TriggerBuilder <T> forJob (final String jobName, final String jobGroup)
  {
    this.jobKey = new JobKey (jobName, jobGroup);
    return this;
  }

  /**
   * Set the identity of the Job which should be fired by the produced Trigger,
   * by extracting the JobKey from the given job.
   *
   * @param jobDetail
   *        the Job to fire.
   * @return the updated JDK8TriggerBuilder
   * @see Trigger#getJobKey()
   */
  public JDK8TriggerBuilder <T> forJob (final JobDetail jobDetail)
  {
    final JobKey k = jobDetail.getKey ();
    if (k.getName () == null)
      throw new IllegalArgumentException ("The given job has not yet had a name assigned to it.");
    this.jobKey = k;
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
   * @see Trigger#getJobDataMap()
   */
  public <V> JDK8TriggerBuilder <T> usingJobData (final String dataKey, final V value)
  {
    jobDataMap.put (dataKey, value);
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
   * @see Trigger#getJobDataMap()
   */
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
   * @see Trigger#getJobDataMap()
   */
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
   * @see Trigger#getJobDataMap()
   */
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
   * @see Trigger#getJobDataMap()
   */
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
   * @see Trigger#getJobDataMap()
   */
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
   * @see Trigger#getJobDataMap()
   */
  public JDK8TriggerBuilder <T> usingJobData (@Nonnull final JobDataMap newJobDataMap)
  {
    // add any existing data to this new map
    newJobDataMap.putAll (jobDataMap);
    jobDataMap = newJobDataMap; // set new map as the map to use
    return this;
  }
}
