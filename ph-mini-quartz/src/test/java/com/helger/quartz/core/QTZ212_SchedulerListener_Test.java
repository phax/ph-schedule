package com.helger.quartz.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.ISchedulerFactory;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.listeners.BroadcastSchedulerListener;
import com.helger.quartz.listeners.AbstractSchedulerListenerSupport;

/**
 * Test that verifies that schedulerStarting() is called before the
 * schedulerStarted()
 *
 * @author adahanne
 */
public class QTZ212_SchedulerListener_Test
{

  private static final String SCHEDULER_STARTED = "SCHEDULER_STARTED";
  private static final String SCHEDULER_STARTING = "SCHEDULER_STARTING";
  private static List <String> methodsCalledInSchedulerListener = new ArrayList<> ();

  @Test
  public void stdSchedulerCallsStartingBeforeStartedTest () throws SchedulerException
  {
    final ISchedulerFactory sf = new StdSchedulerFactory ();
    final IScheduler sched = sf.getScheduler ();
    sched.getListenerManager ().addSchedulerListener (new TestSchedulerListener ());
    sched.start ();

    assertEquals (SCHEDULER_STARTING, methodsCalledInSchedulerListener.get (0));
    assertEquals (SCHEDULER_STARTED, methodsCalledInSchedulerListener.get (1));

    sched.shutdown ();
  }

  @Test
  public void broadcastSchedulerListenerCallsSchedulerStartingOnAllItsListeners () throws SchedulerException
  {

    methodsCalledInSchedulerListener = new ArrayList<> ();
    final ISchedulerFactory sf = new StdSchedulerFactory ();
    final IScheduler sched = sf.getScheduler ();
    final List <ISchedulerListener> listeners = new ArrayList<> ();
    listeners.add (new TestSchedulerListener ());

    sched.getListenerManager ().addSchedulerListener (new BroadcastSchedulerListener (listeners));
    sched.start ();

    assertEquals (SCHEDULER_STARTING, methodsCalledInSchedulerListener.get (0));
    assertEquals (SCHEDULER_STARTED, methodsCalledInSchedulerListener.get (1));

    sched.shutdown ();
  }

  public static class TestSchedulerListener extends AbstractSchedulerListenerSupport
  {

    @Override
    public void schedulerStarted ()
    {
      methodsCalledInSchedulerListener.add (SCHEDULER_STARTED);
      System.out.println ("schedulerStarted was called");
    }

    @Override
    public void schedulerStarting ()
    {
      methodsCalledInSchedulerListener.add (SCHEDULER_STARTING);
      System.out.println ("schedulerStarting was called");
    }

  }

}
