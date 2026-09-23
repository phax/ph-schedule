/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2026 Philip Helger (www.helger.com)
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
package com.helger.quartz.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.quartz.IJobListener;
import com.helger.quartz.IListenerManager;
import com.helger.quartz.IMatcher;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.ITriggerListener;
import com.helger.quartz.JobKey;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.EverythingMatcher;

public class ListenerManager implements IListenerManager
{
  private final ICommonsMap <String, IJobListener> m_aGlobalJobListeners = new CommonsLinkedHashMap <> (10);
  private final ICommonsMap <String, ITriggerListener> m_aGlobalTriggerListeners = new CommonsLinkedHashMap <> (10);
  private final ICommonsMap <String, List <IMatcher <JobKey>>> m_aGlobalJobListenersMatchers = new CommonsLinkedHashMap <> (10);
  private final ICommonsMap <String, List <IMatcher <TriggerKey>>> m_aGlobalTriggerListenersMatchers = new CommonsLinkedHashMap <> (10);
  private final ICommonsList <ISchedulerListener> m_aSchedulerListeners = new CommonsArrayList <> (10);

  @SafeVarargs
  public final void addJobListener (@NonNull final IJobListener aJobListener,
                                    @Nullable final IMatcher <JobKey>... matchers)
  {
    addJobListener (aJobListener, new CommonsArrayList <> (matchers));
  }

  public void addJobListener (@NonNull final IJobListener jobListener,
                              @Nullable final List <IMatcher <JobKey>> matchers)
  {
    ValueEnforcer.notNull (jobListener, "JobListener");
    ValueEnforcer.notEmpty (jobListener.getName (), "JobListener.Name");

    synchronized (m_aGlobalJobListeners)
    {
      m_aGlobalJobListeners.put (jobListener.getName (), jobListener);
      final ICommonsList <IMatcher <JobKey>> matchersL = new CommonsArrayList <> ();
      if (matchers != null && !matchers.isEmpty ())
        matchersL.addAll (matchers);
      else
        matchersL.add (EverythingMatcher.allJobs ());

      m_aGlobalJobListenersMatchers.put (jobListener.getName (), matchersL);
    }
  }

  public void addJobListener (@NonNull final IJobListener jobListener)
  {
    addJobListener (jobListener, EverythingMatcher.allJobs ());
  }

  public void addJobListener (@NonNull final IJobListener jobListener, @Nullable final IMatcher <JobKey> matcher)
  {
    ValueEnforcer.notNull (jobListener, "JobListener");
    ValueEnforcer.notEmpty (jobListener.getName (), "JobListener.Name");

    synchronized (m_aGlobalJobListeners)
    {
      m_aGlobalJobListeners.put (jobListener.getName (), jobListener);
      final ICommonsList <IMatcher <JobKey>> matchersL = new CommonsArrayList <> ();
      if (matcher != null)
        matchersL.add (matcher);
      else
        matchersL.add (EverythingMatcher.allJobs ());

      m_aGlobalJobListenersMatchers.put (jobListener.getName (), matchersL);
    }
  }

  public boolean addJobListenerMatcher (@NonNull final String listenerName, @NonNull final IMatcher <JobKey> matcher)
  {
    ValueEnforcer.notNull (matcher, "Matcher");

    synchronized (m_aGlobalJobListeners)
    {
      final List <IMatcher <JobKey>> matchers = m_aGlobalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      matchers.add (matcher);
      return true;
    }
  }

  public boolean removeJobListenerMatcher (@NonNull final String listenerName, @NonNull final IMatcher <JobKey> matcher)
  {
    ValueEnforcer.notNull (matcher, "Matcher");

    synchronized (m_aGlobalJobListeners)
    {
      final List <IMatcher <JobKey>> matchers = m_aGlobalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      return matchers.remove (matcher);
    }
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <IMatcher <JobKey>> getJobListenerMatchers (@Nullable final String listenerName)
  {
    synchronized (m_aGlobalJobListeners)
    {
      final List <IMatcher <JobKey>> matchers = m_aGlobalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return null;
      return new CommonsArrayList <> (matchers);
    }
  }

  public boolean setJobListenerMatchers (@NonNull final String listenerName,
                                         @NonNull final List <IMatcher <JobKey>> matchers)
  {
    ValueEnforcer.notNull (matchers, "Matchers");

    synchronized (m_aGlobalJobListeners)
    {
      final List <IMatcher <JobKey>> oldMatchers = m_aGlobalJobListenersMatchers.get (listenerName);
      if (oldMatchers == null)
        return false;
      m_aGlobalJobListenersMatchers.put (listenerName, matchers);
      return true;
    }
  }

  public boolean removeJobListener (@Nullable final String name)
  {
    synchronized (m_aGlobalJobListeners)
    {
      return m_aGlobalJobListeners.remove (name) != null;
    }
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IJobListener> getJobListeners ()
  {
    synchronized (m_aGlobalJobListeners)
    {
      return m_aGlobalJobListeners.copyOfValues ();
    }
  }

  @Nullable
  public IJobListener getJobListener (@Nullable final String name)
  {
    synchronized (m_aGlobalJobListeners)
    {
      return m_aGlobalJobListeners.get (name);
    }
  }

  @SafeVarargs
  public final void addTriggerListener (@NonNull final ITriggerListener triggerListener,
                                        @Nullable final IMatcher <TriggerKey>... matchers)
  {
    addTriggerListener (triggerListener, Arrays.asList (matchers));
  }

  public void addTriggerListener (@NonNull final ITriggerListener triggerListener,
                                  @Nullable final List <IMatcher <TriggerKey>> matchers)
  {
    ValueEnforcer.notNull (triggerListener, "TriggerListener");
    ValueEnforcer.notEmpty (triggerListener.getName (), "TriggerListener.Name");

    synchronized (m_aGlobalTriggerListeners)
    {
      m_aGlobalTriggerListeners.put (triggerListener.getName (), triggerListener);

      final List <IMatcher <TriggerKey>> matchersL = new ArrayList <> ();
      if (matchers != null && !matchers.isEmpty ())
        matchersL.addAll (matchers);
      else
        matchersL.add (EverythingMatcher.allTriggers ());

      m_aGlobalTriggerListenersMatchers.put (triggerListener.getName (), matchersL);
    }
  }

  public void addTriggerListener (@NonNull final ITriggerListener triggerListener)
  {
    addTriggerListener (triggerListener, EverythingMatcher.allTriggers ());
  }

  public void addTriggerListener (@NonNull final ITriggerListener triggerListener,
                                  @NonNull final IMatcher <TriggerKey> matcher)
  {
    ValueEnforcer.notNull (triggerListener, "TriggerListener");
    ValueEnforcer.notNull (matcher, "Matcher");
    ValueEnforcer.notEmpty (triggerListener.getName (), "TriggerListener.Name");

    synchronized (m_aGlobalTriggerListeners)
    {
      m_aGlobalTriggerListeners.put (triggerListener.getName (), triggerListener);
      final List <IMatcher <TriggerKey>> matchers = new ArrayList <> ();
      matchers.add (matcher);
      m_aGlobalTriggerListenersMatchers.put (triggerListener.getName (), matchers);
    }
  }

  public boolean addTriggerListenerMatcher (@NonNull final String listenerName,
                                            @NonNull final IMatcher <TriggerKey> matcher)
  {
    ValueEnforcer.notNull (matcher, "Matcher");

    synchronized (m_aGlobalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> matchers = m_aGlobalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      matchers.add (matcher);
      return true;
    }
  }

  public boolean removeTriggerListenerMatcher (@NonNull final String listenerName,
                                               @NonNull final IMatcher <TriggerKey> matcher)
  {
    ValueEnforcer.notNull (matcher, "Matcher");

    synchronized (m_aGlobalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> matchers = m_aGlobalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      return matchers.remove (matcher);
    }
  }

  @Nullable
  @ReturnsMutableCopy
  public ICommonsList <IMatcher <TriggerKey>> getTriggerListenerMatchers (@Nullable final String listenerName)
  {
    synchronized (m_aGlobalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> matchers = m_aGlobalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return null;
      return new CommonsArrayList <> (matchers);
    }
  }

  public boolean setTriggerListenerMatchers (@NonNull final String listenerName,
                                             @NonNull final List <IMatcher <TriggerKey>> matchers)
  {
    ValueEnforcer.noNullValue (matchers, "Matchers");

    synchronized (m_aGlobalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> oldMatchers = m_aGlobalTriggerListenersMatchers.get (listenerName);
      if (oldMatchers == null)
        return false;
      m_aGlobalTriggerListenersMatchers.put (listenerName, matchers);
      return true;
    }
  }

  public boolean removeTriggerListener (@Nullable final String name)
  {
    synchronized (m_aGlobalTriggerListeners)
    {
      return m_aGlobalTriggerListeners.remove (name) != null;
    }
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ITriggerListener> getTriggerListeners ()
  {
    synchronized (m_aGlobalTriggerListeners)
    {
      return m_aGlobalTriggerListeners.copyOfValues ();
    }
  }

  @Nullable
  public ITriggerListener getTriggerListener (@Nullable final String name)
  {
    synchronized (m_aGlobalTriggerListeners)
    {
      return m_aGlobalTriggerListeners.get (name);
    }
  }

  public void addSchedulerListener (@NonNull final ISchedulerListener schedulerListener)
  {
    synchronized (m_aSchedulerListeners)
    {
      m_aSchedulerListeners.add (schedulerListener);
    }
  }

  public boolean removeSchedulerListener (@Nullable final ISchedulerListener schedulerListener)
  {
    synchronized (m_aSchedulerListeners)
    {
      return m_aSchedulerListeners.remove (schedulerListener);
    }
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISchedulerListener> getSchedulerListeners ()
  {
    synchronized (m_aSchedulerListeners)
    {
      return m_aSchedulerListeners.getClone ();
    }
  }
}
