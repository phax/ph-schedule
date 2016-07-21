package com.helger.quartz.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.quartz.JobKey;
import com.helger.quartz.IJobListener;
import com.helger.quartz.IListenerManager;
import com.helger.quartz.IMatcher;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.ITriggerListener;
import com.helger.quartz.impl.matchers.EverythingMatcher;

public class ListenerManager implements IListenerManager
{
  private final Map <String, IJobListener> globalJobListeners = new LinkedHashMap<> (10);
  private final Map <String, ITriggerListener> globalTriggerListeners = new LinkedHashMap<> (10);
  private final Map <String, List <IMatcher <JobKey>>> globalJobListenersMatchers = new LinkedHashMap<> (10);
  private final Map <String, List <IMatcher <TriggerKey>>> globalTriggerListenersMatchers = new LinkedHashMap<> (10);
  private final List <ISchedulerListener> schedulerListeners = new ArrayList<> (10);

  @SafeVarargs
  public final void addJobListener (final IJobListener jobListener, final IMatcher <JobKey>... matchers)
  {
    addJobListener (jobListener, new CommonsArrayList<> (matchers));
  }

  public void addJobListener (final IJobListener jobListener, final List <IMatcher <JobKey>> matchers)
  {
    if (jobListener.getName () == null || jobListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("JobListener name cannot be empty.");
    }

    synchronized (globalJobListeners)
    {
      globalJobListeners.put (jobListener.getName (), jobListener);
      final List <IMatcher <JobKey>> matchersL = new LinkedList<> ();
      if (matchers != null && matchers.size () > 0)
        matchersL.addAll (matchers);
      else
        matchersL.add (EverythingMatcher.allJobs ());

      globalJobListenersMatchers.put (jobListener.getName (), matchersL);
    }
  }

  public void addJobListener (final IJobListener jobListener)
  {
    addJobListener (jobListener, EverythingMatcher.allJobs ());
  }

  public void addJobListener (final IJobListener jobListener, final IMatcher <JobKey> matcher)
  {
    if (jobListener.getName () == null || jobListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("JobListener name cannot be empty.");
    }

    synchronized (globalJobListeners)
    {
      globalJobListeners.put (jobListener.getName (), jobListener);
      final LinkedList <IMatcher <JobKey>> matchersL = new LinkedList<> ();
      if (matcher != null)
        matchersL.add (matcher);
      else
        matchersL.add (EverythingMatcher.allJobs ());

      globalJobListenersMatchers.put (jobListener.getName (), matchersL);
    }
  }

  public boolean addJobListenerMatcher (final String listenerName, final IMatcher <JobKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Null value not acceptable.");

    synchronized (globalJobListeners)
    {
      final List <IMatcher <JobKey>> matchers = globalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      matchers.add (matcher);
      return true;
    }
  }

  public boolean removeJobListenerMatcher (final String listenerName, final IMatcher <JobKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalJobListeners)
    {
      final List <IMatcher <JobKey>> matchers = globalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      return matchers.remove (matcher);
    }
  }

  public List <IMatcher <JobKey>> getJobListenerMatchers (final String listenerName)
  {
    synchronized (globalJobListeners)
    {
      final List <IMatcher <JobKey>> matchers = globalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return null;
      return Collections.unmodifiableList (matchers);
    }
  }

  public boolean setJobListenerMatchers (final String listenerName, final List <IMatcher <JobKey>> matchers)
  {
    if (matchers == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalJobListeners)
    {
      final List <IMatcher <JobKey>> oldMatchers = globalJobListenersMatchers.get (listenerName);
      if (oldMatchers == null)
        return false;
      globalJobListenersMatchers.put (listenerName, matchers);
      return true;
    }
  }

  public boolean removeJobListener (final String name)
  {
    synchronized (globalJobListeners)
    {
      return (globalJobListeners.remove (name) != null);
    }
  }

  public List <IJobListener> getJobListeners ()
  {
    synchronized (globalJobListeners)
    {
      return java.util.Collections.unmodifiableList (new LinkedList<> (globalJobListeners.values ()));
    }
  }

  public IJobListener getJobListener (final String name)
  {
    synchronized (globalJobListeners)
    {
      return globalJobListeners.get (name);
    }
  }

  @SafeVarargs
  public final void addTriggerListener (final ITriggerListener triggerListener, final IMatcher <TriggerKey>... matchers)
  {
    addTriggerListener (triggerListener, Arrays.asList (matchers));
  }

  public void addTriggerListener (final ITriggerListener triggerListener, final List <IMatcher <TriggerKey>> matchers)
  {
    if (triggerListener.getName () == null || triggerListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("TriggerListener name cannot be empty.");
    }

    synchronized (globalTriggerListeners)
    {
      globalTriggerListeners.put (triggerListener.getName (), triggerListener);

      final LinkedList <IMatcher <TriggerKey>> matchersL = new LinkedList<> ();
      if (matchers != null && matchers.size () > 0)
        matchersL.addAll (matchers);
      else
        matchersL.add (EverythingMatcher.allTriggers ());

      globalTriggerListenersMatchers.put (triggerListener.getName (), matchersL);
    }
  }

  public void addTriggerListener (final ITriggerListener triggerListener)
  {
    addTriggerListener (triggerListener, EverythingMatcher.allTriggers ());
  }

  public void addTriggerListener (final ITriggerListener triggerListener, final IMatcher <TriggerKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Null value not acceptable for matcher.");

    if (triggerListener.getName () == null || triggerListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("TriggerListener name cannot be empty.");
    }

    synchronized (globalTriggerListeners)
    {
      globalTriggerListeners.put (triggerListener.getName (), triggerListener);
      final List <IMatcher <TriggerKey>> matchers = new LinkedList<> ();
      matchers.add (matcher);
      globalTriggerListenersMatchers.put (triggerListener.getName (), matchers);
    }
  }

  public boolean addTriggerListenerMatcher (final String listenerName, final IMatcher <TriggerKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> matchers = globalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      matchers.add (matcher);
      return true;
    }
  }

  public boolean removeTriggerListenerMatcher (final String listenerName, final IMatcher <TriggerKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> matchers = globalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      return matchers.remove (matcher);
    }
  }

  public List <IMatcher <TriggerKey>> getTriggerListenerMatchers (final String listenerName)
  {
    synchronized (globalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> matchers = globalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return null;
      return Collections.unmodifiableList (matchers);
    }
  }

  public boolean setTriggerListenerMatchers (final String listenerName, final List <IMatcher <TriggerKey>> matchers)
  {
    if (matchers == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalTriggerListeners)
    {
      final List <IMatcher <TriggerKey>> oldMatchers = globalTriggerListenersMatchers.get (listenerName);
      if (oldMatchers == null)
        return false;
      globalTriggerListenersMatchers.put (listenerName, matchers);
      return true;
    }
  }

  public boolean removeTriggerListener (final String name)
  {
    synchronized (globalTriggerListeners)
    {
      return (globalTriggerListeners.remove (name) != null);
    }
  }

  public List <ITriggerListener> getTriggerListeners ()
  {
    synchronized (globalTriggerListeners)
    {
      return java.util.Collections.unmodifiableList (new LinkedList<> (globalTriggerListeners.values ()));
    }
  }

  public ITriggerListener getTriggerListener (final String name)
  {
    synchronized (globalTriggerListeners)
    {
      return globalTriggerListeners.get (name);
    }
  }

  public void addSchedulerListener (final ISchedulerListener schedulerListener)
  {
    synchronized (schedulerListeners)
    {
      schedulerListeners.add (schedulerListener);
    }
  }

  public boolean removeSchedulerListener (final ISchedulerListener schedulerListener)
  {
    synchronized (schedulerListeners)
    {
      return schedulerListeners.remove (schedulerListener);
    }
  }

  public List <ISchedulerListener> getSchedulerListeners ()
  {
    synchronized (schedulerListeners)
    {
      return java.util.Collections.unmodifiableList (new ArrayList<> (schedulerListeners));
    }
  }
}
