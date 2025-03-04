/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2025 Philip Helger (www.helger.com)
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

import javax.annotation.concurrent.Immutable;

import com.helger.quartz.utils.Key;

/**
 * Uniquely identifies a {@link ITrigger}.
 * <p>
 * Keys are composed of both a name and group, and the name must be unique
 * within the group. If only a name is specified then the default group name
 * will be used.
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
 * @see ITrigger
 * @see Key#DEFAULT_GROUP
 */
@Immutable
public final class TriggerKey extends Key <TriggerKey>
{
  public TriggerKey (final String name)
  {
    super (name, null);
  }

  public TriggerKey (final String name, final String group)
  {
    super (name, group);
  }

  public static TriggerKey triggerKey (final String name)
  {
    return new TriggerKey (name, null);
  }

  public static TriggerKey triggerKey (final String name, final String group)
  {
    return new TriggerKey (name, group);
  }
}
