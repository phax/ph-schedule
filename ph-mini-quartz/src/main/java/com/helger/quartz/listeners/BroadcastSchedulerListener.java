package com.helger.quartz.listeners;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.helger.quartz.JobDetail;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SchedulerListener;
import com.helger.quartz.Trigger;
import com.helger.quartz.TriggerKey;

/**
 * Holds a List of references to SchedulerListener instances and broadcasts all
 * events to them (in order).
 * <p>
 * This may be more convenient than registering all of the listeners directly
 * with the Scheduler, and provides the flexibility of easily changing which
 * listeners get notified.
 * </p>
 *
 * @see #addListener(com.helger.quartz.SchedulerListener)
 * @see #removeListener(com.helger.quartz.SchedulerListener)
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastSchedulerListener implements SchedulerListener
{

  private final List <SchedulerListener> listeners;

  public BroadcastSchedulerListener ()
  {
    listeners = new LinkedList <> ();
  }

  /**
   * Construct an instance with the given List of listeners.
   *
   * @param listeners
   *        the initial List of SchedulerListeners to broadcast to.
   */
  public BroadcastSchedulerListener (final List <SchedulerListener> listeners)
  {
    this ();
    this.listeners.addAll (listeners);
  }

  public void addListener (final SchedulerListener listener)
  {
    listeners.add (listener);
  }

  public boolean removeListener (final SchedulerListener listener)
  {
    return listeners.remove (listener);
  }

  public List <SchedulerListener> getListeners ()
  {
    return java.util.Collections.unmodifiableList (listeners);
  }

  public void jobAdded (final JobDetail jobDetail)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobAdded (jobDetail);
    }
  }

  public void jobDeleted (final JobKey jobKey)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobDeleted (jobKey);
    }
  }

  public void jobScheduled (final Trigger trigger)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobScheduled (trigger);
    }
  }

  public void jobUnscheduled (final TriggerKey triggerKey)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobUnscheduled (triggerKey);
    }
  }

  public void triggerFinalized (final Trigger trigger)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.triggerFinalized (trigger);
    }
  }

  public void triggerPaused (final TriggerKey key)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.triggerPaused (key);
    }
  }

  public void triggersPaused (final String triggerGroup)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.triggersPaused (triggerGroup);
    }
  }

  public void triggerResumed (final TriggerKey key)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.triggerResumed (key);
    }
  }

  public void triggersResumed (final String triggerGroup)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.triggersResumed (triggerGroup);
    }
  }

  public void schedulingDataCleared ()
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.schedulingDataCleared ();
    }
  }

  public void jobPaused (final JobKey key)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobPaused (key);
    }
  }

  public void jobsPaused (final String jobGroup)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobsPaused (jobGroup);
    }
  }

  public void jobResumed (final JobKey key)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobResumed (key);
    }
  }

  public void jobsResumed (final String jobGroup)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.jobsResumed (jobGroup);
    }
  }

  public void schedulerError (final String msg, final SchedulerException cause)
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.schedulerError (msg, cause);
    }
  }

  public void schedulerStarted ()
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.schedulerStarted ();
    }
  }

  public void schedulerStarting ()
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.schedulerStarting ();
    }
  }

  public void schedulerInStandbyMode ()
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.schedulerInStandbyMode ();
    }
  }

  public void schedulerShutdown ()
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.schedulerShutdown ();
    }
  }

  public void schedulerShuttingdown ()
  {
    final Iterator <SchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final SchedulerListener l = itr.next ();
      l.schedulerShuttingdown ();
    }
  }

}
