/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;

/**
 * <p>
 * Holds references to Scheduler instances - ensuring uniqueness, and preventing
 * garbage collection, and allowing 'global' lookups - all within a ClassLoader
 * space.
 * </p>
 *
 * @author James House
 */
public class SchedulerRepository
{
  private static final class SingletonHolder
  {
    static final SchedulerRepository INSTANCE = new SchedulerRepository ();
  }

  private final Map <String, IScheduler> schedulers;

  private SchedulerRepository ()
  {
    schedulers = new HashMap<> ();
  }

  public static SchedulerRepository getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }

  public synchronized void bind (final IScheduler sched) throws SchedulerException
  {
    final String sKey = sched.getSchedulerName ();
    if (schedulers.containsKey (sKey))
      throw new SchedulerException ("Scheduler with name '" + sKey + "' already exists.");
    schedulers.put (sKey, sched);
  }

  public synchronized boolean remove (final String schedName)
  {
    return schedulers.remove (schedName) != null;
  }

  public synchronized IScheduler lookup (final String schedName)
  {
    return schedulers.get (schedName);
  }

  public synchronized Collection <IScheduler> lookupAll ()
  {
    return Collections.unmodifiableCollection (schedulers.values ());
  }
}
