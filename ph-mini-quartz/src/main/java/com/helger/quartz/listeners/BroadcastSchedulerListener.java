package com.helger.quartz.listeners;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.helger.quartz.IJobDetail;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.ITrigger;
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
 * @see #addListener(com.helger.quartz.ISchedulerListener)
 * @see #removeListener(com.helger.quartz.ISchedulerListener)
 * @author James House (jhouse AT revolition DOT net)
 */
public class BroadcastSchedulerListener implements ISchedulerListener
{

  private final List <ISchedulerListener> listeners;

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
  public BroadcastSchedulerListener (final List <ISchedulerListener> listeners)
  {
    this ();
    this.listeners.addAll (listeners);
  }

  public void addListener (final ISchedulerListener listener)
  {
    listeners.add (listener);
  }

  public boolean removeListener (final ISchedulerListener listener)
  {
    return listeners.remove (listener);
  }

  public List <ISchedulerListener> getListeners ()
  {
    return java.util.Collections.unmodifiableList (listeners);
  }

  public void jobAdded (final IJobDetail jobDetail)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobAdded (jobDetail);
    }
  }

  public void jobDeleted (final JobKey jobKey)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobDeleted (jobKey);
    }
  }

  public void jobScheduled (final ITrigger trigger)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobScheduled (trigger);
    }
  }

  public void jobUnscheduled (final TriggerKey triggerKey)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobUnscheduled (triggerKey);
    }
  }

  public void triggerFinalized (final ITrigger trigger)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.triggerFinalized (trigger);
    }
  }

  public void triggerPaused (final TriggerKey key)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.triggerPaused (key);
    }
  }

  public void triggersPaused (final String triggerGroup)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.triggersPaused (triggerGroup);
    }
  }

  public void triggerResumed (final TriggerKey key)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.triggerResumed (key);
    }
  }

  public void triggersResumed (final String triggerGroup)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.triggersResumed (triggerGroup);
    }
  }

  public void schedulingDataCleared ()
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.schedulingDataCleared ();
    }
  }

  public void jobPaused (final JobKey key)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobPaused (key);
    }
  }

  public void jobsPaused (final String jobGroup)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobsPaused (jobGroup);
    }
  }

  public void jobResumed (final JobKey key)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobResumed (key);
    }
  }

  public void jobsResumed (final String jobGroup)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.jobsResumed (jobGroup);
    }
  }

  public void schedulerError (final String msg, final SchedulerException cause)
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.schedulerError (msg, cause);
    }
  }

  public void schedulerStarted ()
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.schedulerStarted ();
    }
  }

  public void schedulerStarting ()
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.schedulerStarting ();
    }
  }

  public void schedulerInStandbyMode ()
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.schedulerInStandbyMode ();
    }
  }

  public void schedulerShutdown ()
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.schedulerShutdown ();
    }
  }

  public void schedulerShuttingdown ()
  {
    final Iterator <ISchedulerListener> itr = listeners.iterator ();
    while (itr.hasNext ())
    {
      final ISchedulerListener l = itr.next ();
      l.schedulerShuttingdown ();
    }
  }

}
