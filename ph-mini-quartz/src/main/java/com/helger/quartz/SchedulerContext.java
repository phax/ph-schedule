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
package com.helger.quartz;

import java.util.Map;

import com.helger.quartz.utils.StringKeyDirtyFlagMap;

/**
 * Holds context/environment data that can be made available to Jobs as they are
 * executed. This feature is much like the ServletContext feature when working
 * with J2EE servlets.
 * <p>
 * Future versions of Quartz may make distinctions on how it propagates data in
 * <code>SchedulerContext</code> between instances of proxies to a single
 * scheduler instance - i.e. if Quartz is being used via RMI.
 * </p>
 *
 * @see IScheduler#getContext
 * @author James House
 */
public class SchedulerContext extends StringKeyDirtyFlagMap
{
  /**
   * Create an empty <code>SchedulerContext</code>.
   */
  public SchedulerContext ()
  {
    super (15);
  }

  /**
   * Create a <code>SchedulerContext</code> with the given data.
   */
  public SchedulerContext (final Map <?, ?> map)
  {
    this ();
    @SuppressWarnings ("unchecked") // param must be a String key map.
    final Map <String, ?> mapTyped = (Map <String, ?>) map;
    putAll (mapTyped);
  }
}