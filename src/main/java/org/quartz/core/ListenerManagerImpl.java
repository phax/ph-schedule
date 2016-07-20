package org.quartz.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Matcher;
import org.quartz.SchedulerListener;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.matchers.EverythingMatcher;

import com.helger.commons.collection.ext.CommonsArrayList;

public class ListenerManagerImpl implements ListenerManager
{
  private final Map <String, JobListener> globalJobListeners = new LinkedHashMap<> (10);
  private final Map <String, TriggerListener> globalTriggerListeners = new LinkedHashMap<> (10);
  private final Map <String, List <Matcher <JobKey>>> globalJobListenersMatchers = new LinkedHashMap<> (10);
  private final Map <String, List <Matcher <TriggerKey>>> globalTriggerListenersMatchers = new LinkedHashMap<> (10);
  private final List <SchedulerListener> schedulerListeners = new ArrayList<> (10);

  public void addJobListener (final JobListener jobListener, final Matcher <JobKey>... matchers)
  {
    addJobListener (jobListener, new CommonsArrayList<> (matchers));
  }

  public void addJobListener (final JobListener jobListener, final List <Matcher <JobKey>> matchers)
  {
    if (jobListener.getName () == null || jobListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("JobListener name cannot be empty.");
    }

    synchronized (globalJobListeners)
    {
      globalJobListeners.put (jobListener.getName (), jobListener);
      final LinkedList <Matcher <JobKey>> matchersL = new LinkedList<> ();
      if (matchers != null && matchers.size () > 0)
        matchersL.addAll (matchers);
      else
        matchersL.add (EverythingMatcher.allJobs ());

      globalJobListenersMatchers.put (jobListener.getName (), matchersL);
    }
  }

  public void addJobListener (final JobListener jobListener)
  {
    addJobListener (jobListener, EverythingMatcher.allJobs ());
  }

  public void addJobListener (final JobListener jobListener, final Matcher <JobKey> matcher)
  {
    if (jobListener.getName () == null || jobListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("JobListener name cannot be empty.");
    }

    synchronized (globalJobListeners)
    {
      globalJobListeners.put (jobListener.getName (), jobListener);
      final LinkedList <Matcher <JobKey>> matchersL = new LinkedList<> ();
      if (matcher != null)
        matchersL.add (matcher);
      else
        matchersL.add (EverythingMatcher.allJobs ());

      globalJobListenersMatchers.put (jobListener.getName (), matchersL);
    }
  }

  public boolean addJobListenerMatcher (final String listenerName, final Matcher <JobKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Null value not acceptable.");

    synchronized (globalJobListeners)
    {
      final List <Matcher <JobKey>> matchers = globalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      matchers.add (matcher);
      return true;
    }
  }

  public boolean removeJobListenerMatcher (final String listenerName, final Matcher <JobKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalJobListeners)
    {
      final List <Matcher <JobKey>> matchers = globalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      return matchers.remove (matcher);
    }
  }

  public List <Matcher <JobKey>> getJobListenerMatchers (final String listenerName)
  {
    synchronized (globalJobListeners)
    {
      final List <Matcher <JobKey>> matchers = globalJobListenersMatchers.get (listenerName);
      if (matchers == null)
        return null;
      return Collections.unmodifiableList (matchers);
    }
  }

  public boolean setJobListenerMatchers (final String listenerName, final List <Matcher <JobKey>> matchers)
  {
    if (matchers == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalJobListeners)
    {
      final List <Matcher <JobKey>> oldMatchers = globalJobListenersMatchers.get (listenerName);
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

  public List <JobListener> getJobListeners ()
  {
    synchronized (globalJobListeners)
    {
      return java.util.Collections.unmodifiableList (new LinkedList<> (globalJobListeners.values ()));
    }
  }

  public JobListener getJobListener (final String name)
  {
    synchronized (globalJobListeners)
    {
      return globalJobListeners.get (name);
    }
  }

  public void addTriggerListener (final TriggerListener triggerListener, final Matcher <TriggerKey>... matchers)
  {
    addTriggerListener (triggerListener, Arrays.asList (matchers));
  }

  public void addTriggerListener (final TriggerListener triggerListener, final List <Matcher <TriggerKey>> matchers)
  {
    if (triggerListener.getName () == null || triggerListener.getName ().length () == 0)
    {
      throw new IllegalArgumentException ("TriggerListener name cannot be empty.");
    }

    synchronized (globalTriggerListeners)
    {
      globalTriggerListeners.put (triggerListener.getName (), triggerListener);

      final LinkedList <Matcher <TriggerKey>> matchersL = new LinkedList<> ();
      if (matchers != null && matchers.size () > 0)
        matchersL.addAll (matchers);
      else
        matchersL.add (EverythingMatcher.allTriggers ());

      globalTriggerListenersMatchers.put (triggerListener.getName (), matchersL);
    }
  }

  public void addTriggerListener (final TriggerListener triggerListener)
  {
    addTriggerListener (triggerListener, EverythingMatcher.allTriggers ());
  }

  public void addTriggerListener (final TriggerListener triggerListener, final Matcher <TriggerKey> matcher)
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
      final List <Matcher <TriggerKey>> matchers = new LinkedList<> ();
      matchers.add (matcher);
      globalTriggerListenersMatchers.put (triggerListener.getName (), matchers);
    }
  }

  public boolean addTriggerListenerMatcher (final String listenerName, final Matcher <TriggerKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalTriggerListeners)
    {
      final List <Matcher <TriggerKey>> matchers = globalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      matchers.add (matcher);
      return true;
    }
  }

  public boolean removeTriggerListenerMatcher (final String listenerName, final Matcher <TriggerKey> matcher)
  {
    if (matcher == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalTriggerListeners)
    {
      final List <Matcher <TriggerKey>> matchers = globalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return false;
      return matchers.remove (matcher);
    }
  }

  public List <Matcher <TriggerKey>> getTriggerListenerMatchers (final String listenerName)
  {
    synchronized (globalTriggerListeners)
    {
      final List <Matcher <TriggerKey>> matchers = globalTriggerListenersMatchers.get (listenerName);
      if (matchers == null)
        return null;
      return Collections.unmodifiableList (matchers);
    }
  }

  public boolean setTriggerListenerMatchers (final String listenerName, final List <Matcher <TriggerKey>> matchers)
  {
    if (matchers == null)
      throw new IllegalArgumentException ("Non-null value not acceptable.");

    synchronized (globalTriggerListeners)
    {
      final List <Matcher <TriggerKey>> oldMatchers = globalTriggerListenersMatchers.get (listenerName);
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

  public List <TriggerListener> getTriggerListeners ()
  {
    synchronized (globalTriggerListeners)
    {
      return java.util.Collections.unmodifiableList (new LinkedList<> (globalTriggerListeners.values ()));
    }
  }

  public TriggerListener getTriggerListener (final String name)
  {
    synchronized (globalTriggerListeners)
    {
      return globalTriggerListeners.get (name);
    }
  }

  public void addSchedulerListener (final SchedulerListener schedulerListener)
  {
    synchronized (schedulerListeners)
    {
      schedulerListeners.add (schedulerListener);
    }
  }

  public boolean removeSchedulerListener (final SchedulerListener schedulerListener)
  {
    synchronized (schedulerListeners)
    {
      return schedulerListeners.remove (schedulerListener);
    }
  }

  public List <SchedulerListener> getSchedulerListeners ()
  {
    synchronized (schedulerListeners)
    {
      return java.util.Collections.unmodifiableList (new ArrayList<> (schedulerListeners));
    }
  }
}
