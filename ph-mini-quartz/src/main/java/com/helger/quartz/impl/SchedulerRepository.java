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
package com.helger.quartz.impl;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
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

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final ICommonsMap <String, IScheduler> m_aSchedulers = new CommonsHashMap <> ();

  private SchedulerRepository ()
  {}

  @Nonnull
  public static SchedulerRepository getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }

  public void bind (final IScheduler sched) throws SchedulerException
  {
    final String sKey = sched.getSchedulerName ();
    m_aRWLock.writeLockedThrowing ( () -> {
      if (m_aSchedulers.containsKey (sKey))
        throw new SchedulerException ("Scheduler with name '" + sKey + "' already exists.");
      m_aSchedulers.put (sKey, sched);
    });
  }

  public boolean remove (final String schedName)
  {
    return m_aRWLock.writeLockedBoolean ( () -> m_aSchedulers.remove (schedName) != null);
  }

  public IScheduler lookup (final String schedName)
  {
    return m_aRWLock.readLockedGet ( () -> m_aSchedulers.get (schedName));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IScheduler> lookupAll ()
  {
    return m_aRWLock.readLockedGet (m_aSchedulers::copyOfValues);
  }
}
