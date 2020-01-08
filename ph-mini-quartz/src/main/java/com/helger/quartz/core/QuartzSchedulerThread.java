/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.JobPersistenceException;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.spi.IOperableTrigger;
import com.helger.quartz.spi.TriggerFiredBundle;
import com.helger.quartz.spi.TriggerFiredResult;

/**
 * <p>
 * The thread responsible for performing the work of firing
 * <code>{@link ITrigger}</code> s that are registered with the
 * <code>{@link QuartzScheduler}</code>.
 * </p>
 *
 * @see QuartzScheduler
 * @see com.helger.quartz.IJob
 * @see ITrigger
 * @author James House
 */
public class QuartzSchedulerThread extends Thread
{
  private static final Logger LOGGER = LoggerFactory.getLogger (QuartzSchedulerThread.class);

  private QuartzScheduler m_aQS;
  private QuartzSchedulerResources m_aQSRsrcs;
  private final Object m_aSigLock = new Object ();
  private boolean m_bSignaled;
  private long m_nSignaledNextFireTime;
  private boolean m_bPaused;
  private final AtomicBoolean m_aHalted;
  private final Random m_aRandom = new Random ();
  // When the scheduler finds there is no current trigger to fire, how long
  // it should wait until checking again...
  private static long DEFAULT_IDLE_WAIT_TIME = 30L * 1000L;
  private long m_nIdleWaitTime = DEFAULT_IDLE_WAIT_TIME;
  private int m_nIdleWaitVariablness = 7 * 1000;

  /**
   * <p>
   * Construct a new <code>QuartzSchedulerThread</code> for the given
   * <code>QuartzScheduler</code> as a non-daemon <code>Thread</code> with
   * normal priority.
   * </p>
   */
  QuartzSchedulerThread (final QuartzScheduler qs, final QuartzSchedulerResources qsRsrcs)
  {
    this (qs, qsRsrcs, qsRsrcs.getMakeSchedulerThreadDaemon (), Thread.NORM_PRIORITY);
  }

  /**
   * <p>
   * Construct a new <code>QuartzSchedulerThread</code> for the given
   * <code>QuartzScheduler</code> as a <code>Thread</code> with the given
   * attributes.
   * </p>
   */
  QuartzSchedulerThread (final QuartzScheduler qs,
                         final QuartzSchedulerResources qsRsrcs,
                         final boolean setDaemon,
                         final int threadPrio)
  {
    super (qs.getSchedulerThreadGroup (), qsRsrcs.getThreadName ());
    m_aQS = qs;
    m_aQSRsrcs = qsRsrcs;
    setDaemon (setDaemon);
    if (qsRsrcs.isThreadsInheritInitializersClassLoadContext ())
    {
      LOGGER.info ("QuartzSchedulerThread Inheriting ContextClassLoader of thread: " +
                   Thread.currentThread ().getName ());
      setContextClassLoader (Thread.currentThread ().getContextClassLoader ());
    }

    setPriority (threadPrio);

    // start the underlying thread, but put this object into the 'paused'
    // state
    // so processing doesn't start yet...
    m_bPaused = true;
    m_aHalted = new AtomicBoolean (false);
  }

  void setIdleWaitTime (final long waitTime)
  {
    m_nIdleWaitTime = waitTime;
    m_nIdleWaitVariablness = (int) (waitTime * 0.2);
  }

  private long getRandomizedIdleWaitTime ()
  {
    return m_nIdleWaitTime - m_aRandom.nextInt (m_nIdleWaitVariablness);
  }

  /**
   * <p>
   * Signals the main processing loop to pause at the next possible point.
   * </p>
   */
  void togglePause (final boolean pause)
  {
    synchronized (m_aSigLock)
    {
      m_bPaused = pause;

      if (m_bPaused)
      {
        signalSchedulingChange (0);
      }
      else
      {
        m_aSigLock.notifyAll ();
      }
    }
  }

  /**
   * <p>
   * Signals the main processing loop to pause at the next possible point.
   * </p>
   */
  void halt (final boolean wait)
  {
    synchronized (m_aSigLock)
    {
      m_aHalted.set (true);

      if (m_bPaused)
      {
        m_aSigLock.notifyAll ();
      }
      else
      {
        signalSchedulingChange (0);
      }
    }

    if (wait)
    {
      boolean interrupted = false;
      try
      {
        while (true)
        {
          try
          {
            join ();
            break;
          }
          catch (final InterruptedException ex)
          {
            interrupted = true;
          }
        }
      }
      finally
      {
        if (interrupted)
        {
          Thread.currentThread ().interrupt ();
        }
      }
    }
  }

  boolean isPaused ()
  {
    return m_bPaused;
  }

  /**
   * <p>
   * Signals the main processing loop that a change in scheduling has been made
   * - in order to interrupt any sleeping that may be occuring while waiting for
   * the fire time to arrive.
   * </p>
   *
   * @param candidateNewNextFireTime
   *        the time (in millis) when the newly scheduled trigger will fire. If
   *        this method is being called do to some other even (rather than
   *        scheduling a trigger), the caller should pass zero (0).
   */
  public void signalSchedulingChange (final long candidateNewNextFireTime)
  {
    synchronized (m_aSigLock)
    {
      m_bSignaled = true;
      m_nSignaledNextFireTime = candidateNewNextFireTime;
      m_aSigLock.notifyAll ();
    }
  }

  public void clearSignaledSchedulingChange ()
  {
    synchronized (m_aSigLock)
    {
      m_bSignaled = false;
      m_nSignaledNextFireTime = 0;
    }
  }

  public boolean isScheduleChanged ()
  {
    synchronized (m_aSigLock)
    {
      return m_bSignaled;
    }
  }

  public long getSignaledNextFireTime ()
  {
    synchronized (m_aSigLock)
    {
      return m_nSignaledNextFireTime;
    }
  }

  /**
   * <p>
   * The main processing loop of the <code>QuartzSchedulerThread</code>.
   * </p>
   */
  @Override
  public void run ()
  {
    boolean lastAcquireFailed = false;

    while (!m_aHalted.get ())
    {
      try
      {
        // check if we're supposed to pause...
        synchronized (m_aSigLock)
        {
          while (m_bPaused && !m_aHalted.get ())
          {
            try
            {
              // wait until togglePause(false) is called...
              m_aSigLock.wait (1000L);
            }
            catch (final InterruptedException ignore)
            {}
          }

          if (m_aHalted.get ())
          {
            break;
          }
        }

        final int availThreadCount = m_aQSRsrcs.getThreadPool ().blockForAvailableThreads ();
        if (availThreadCount > 0)
        { // will always be true, due to semantics of
          // blockForAvailableThreads...

          ICommonsList <IOperableTrigger> triggers = null;

          long now = System.currentTimeMillis ();

          clearSignaledSchedulingChange ();
          try
          {
            triggers = m_aQSRsrcs.getJobStore ()
                                 .acquireNextTriggers (now + m_nIdleWaitTime,
                                                       Math.min (availThreadCount, m_aQSRsrcs.getMaxBatchSize ()),
                                                       m_aQSRsrcs.getBatchTimeWindow ());
            lastAcquireFailed = false;
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("batch acquisition of " + (triggers == null ? 0 : triggers.size ()) + " triggers");
          }
          catch (final JobPersistenceException jpe)
          {
            if (!lastAcquireFailed)
            {
              m_aQS.notifySchedulerListenersError ("An error occurred while scanning for the next triggers to fire.",
                                                   jpe);
            }
            lastAcquireFailed = true;
            continue;
          }
          catch (final RuntimeException e)
          {
            if (!lastAcquireFailed)
            {
              LOGGER.error ("quartzSchedulerThreadLoop: RuntimeException " + e.getMessage (), e);
            }
            lastAcquireFailed = true;
            continue;
          }

          if (triggers != null && !triggers.isEmpty ())
          {

            now = System.currentTimeMillis ();
            final long triggerTime = triggers.get (0).getNextFireTime ().getTime ();
            long timeUntilTrigger = triggerTime - now;
            while (timeUntilTrigger > 2)
            {
              synchronized (m_aSigLock)
              {
                if (m_aHalted.get ())
                {
                  break;
                }
                if (!isCandidateNewTimeEarlierWithinReason (triggerTime, false))
                {
                  try
                  {
                    // we could have blocked a long while
                    // on 'synchronize', so we must recompute
                    now = System.currentTimeMillis ();
                    timeUntilTrigger = triggerTime - now;
                    if (timeUntilTrigger >= 1)
                      m_aSigLock.wait (timeUntilTrigger);
                  }
                  catch (final InterruptedException ignore)
                  {}
                }
              }
              if (releaseIfScheduleChangedSignificantly (triggers, triggerTime))
              {
                break;
              }
              now = System.currentTimeMillis ();
              timeUntilTrigger = triggerTime - now;
            }

            // this happens if releaseIfScheduleChangedSignificantly decided to
            // release triggers
            if (triggers.isEmpty ())
              continue;

            // set triggers to 'executing'
            ICommonsList <TriggerFiredResult> bndles = new CommonsArrayList <> ();

            boolean goAhead = true;
            synchronized (m_aSigLock)
            {
              goAhead = !m_aHalted.get ();
            }
            if (goAhead)
            {
              try
              {
                final ICommonsList <TriggerFiredResult> res = m_aQSRsrcs.getJobStore ().triggersFired (triggers);
                if (res != null)
                  bndles = res;
              }
              catch (final SchedulerException se)
              {
                m_aQS.notifySchedulerListenersError ("An error occurred while firing triggers '" + triggers + "'", se);
                // QTZ-179 : a problem occurred interacting with the triggers
                // from the db
                // we release them and loop again
                for (int i = 0; i < triggers.size (); i++)
                {
                  m_aQSRsrcs.getJobStore ().releaseAcquiredTrigger (triggers.get (i));
                }
                continue;
              }

            }

            for (int i = 0; i < bndles.size (); i++)
            {
              final TriggerFiredResult result = bndles.get (i);
              final TriggerFiredBundle bndle = result.getTriggerFiredBundle ();
              final Exception exception = result.getException ();

              if (exception instanceof RuntimeException)
              {
                LOGGER.error ("RuntimeException while firing trigger " + triggers.get (i), exception);
                m_aQSRsrcs.getJobStore ().releaseAcquiredTrigger (triggers.get (i));
                continue;
              }

              // it's possible to get 'null' if the triggers was paused,
              // blocked, or other similar occurrences that prevent it being
              // fired at this time... or if the scheduler was shutdown (halted)
              if (bndle == null)
              {
                m_aQSRsrcs.getJobStore ().releaseAcquiredTrigger (triggers.get (i));
                continue;
              }

              JobRunShell shell = null;
              try
              {
                shell = m_aQSRsrcs.getJobRunShellFactory ().createJobRunShell (bndle);
                shell.initialize (m_aQS);
              }
              catch (final SchedulerException se)
              {
                m_aQSRsrcs.getJobStore ()
                          .triggeredJobComplete (triggers.get (i),
                                                 bndle.getJobDetail (),
                                                 ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
                continue;
              }

              if (m_aQSRsrcs.getThreadPool ().runInThread (shell) == false)
              {
                // this case should never happen, as it is indicative of the
                // scheduler being shutdown or a bug in the thread pool or
                // a thread pool being used concurrently - which the docs
                // say not to do...
                LOGGER.error ("ThreadPool.runInThread() return false!");
                m_aQSRsrcs.getJobStore ()
                          .triggeredJobComplete (triggers.get (i),
                                                 bndle.getJobDetail (),
                                                 ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR);
              }

            }

            continue; // while (!halted)
          }
        }
        else
        { // if(availThreadCount > 0)
          // should never happen, if threadPool.blockForAvailableThreads()
          // follows contract
          continue; // while (!halted)
        }

        final long now = System.currentTimeMillis ();
        final long waitTime = now + getRandomizedIdleWaitTime ();
        final long timeUntilContinue = waitTime - now;
        synchronized (m_aSigLock)
        {
          try
          {
            if (!m_aHalted.get ())
            {
              // QTZ-336 A job might have been completed in the mean time and we
              // might have
              // missed the scheduled changed signal by not waiting for the
              // notify() yet
              // Check that before waiting for too long in case this very job
              // needs to be
              // scheduled very soon
              if (!isScheduleChanged ())
              {
                m_aSigLock.wait (timeUntilContinue);
              }
            }
          }
          catch (final InterruptedException ignore)
          {}
        }

      }
      catch (final RuntimeException re)
      {
        LOGGER.error ("Runtime error occurred in main trigger firing loop.", re);
      }
    } // while (!halted)

    // drop references to scheduler stuff to aid garbage collection...
    m_aQS = null;
    m_aQSRsrcs = null;
  }

  private boolean releaseIfScheduleChangedSignificantly (final List <IOperableTrigger> triggers, final long triggerTime)
  {
    if (isCandidateNewTimeEarlierWithinReason (triggerTime, true))
    {
      // above call does a clearSignaledSchedulingChange()
      for (final IOperableTrigger trigger : triggers)
      {
        m_aQSRsrcs.getJobStore ().releaseAcquiredTrigger (trigger);
      }
      triggers.clear ();
      return true;
    }
    return false;
  }

  private boolean isCandidateNewTimeEarlierWithinReason (final long oldTime, final boolean clearSignal)
  {

    // So here's the deal: We know due to being signaled that 'the schedule'
    // has changed. We may know (if getSignaledNextFireTime() != 0) the
    // new earliest fire time. We may not (in which case we will assume
    // that the new time is earlier than the trigger we have acquired).
    // In either case, we only want to abandon our acquired trigger and
    // go looking for a new one if "it's worth it". It's only worth it if
    // the time cost incurred to abandon the trigger and acquire a new one
    // is less than the time until the currently acquired trigger will fire,
    // otherwise we're just "thrashing" the job store (e.g. database).
    //
    // So the question becomes when is it "worth it"? This will depend on
    // the job store implementation (and of course the particular database
    // or whatever behind it). Ideally we would depend on the job store
    // implementation to tell us the amount of time in which it "thinks"
    // it can abandon the acquired trigger and acquire a new one. However
    // we have no current facility for having it tell us that, so we make
    // a somewhat educated but arbitrary guess ;-).

    synchronized (m_aSigLock)
    {

      if (!isScheduleChanged ())
        return false;

      boolean earlier = false;

      if (getSignaledNextFireTime () == 0)
        earlier = true;
      else
        if (getSignaledNextFireTime () < oldTime)
          earlier = true;

      if (earlier)
      {
        // so the new time is considered earlier, but is it enough earlier?
        final long diff = oldTime - System.currentTimeMillis ();
        if (diff < (m_aQSRsrcs.getJobStore ().supportsPersistence () ? 70L : 7L))
          earlier = false;
      }

      if (clearSignal)
      {
        clearSignaledSchedulingChange ();
      }

      return earlier;
    }
  }
}
