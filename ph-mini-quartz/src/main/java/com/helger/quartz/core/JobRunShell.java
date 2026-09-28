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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ISchedulerListener;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.impl.JobExecutionContext;
import com.helger.quartz.spi.IOperableTrigger;
import com.helger.quartz.spi.TriggerFiredBundle;

/**
 * <p>
 * JobRunShell instances are responsible for providing the 'safe' environment for <code>Job</code> s
 * to run in, and for performing all of the work of executing the <code>Job</code>, catching ANY
 * thrown exceptions, updating the <code>Trigger</code> with the <code>Job</code>'s completion code,
 * etc.
 * </p>
 * <p>
 * A <code>JobRunShell</code> instance is created by a <code>JobRunShellFactory</code> on behalf of
 * the <code>QuartzSchedulerThread</code> which then runs the shell in a thread from the configured
 * <code>ThreadPool</code> when the scheduler determines that a <code>Job</code> has been triggered.
 * </p>
 *
 * @see IJobRunShellFactory
 * @see com.helger.quartz.core.QuartzSchedulerThread
 * @see com.helger.quartz.IJob
 * @see com.helger.quartz.ITrigger
 * @author James House
 */
public class JobRunShell implements Runnable, ISchedulerListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger (JobRunShell.class);

  protected JobExecutionContext m_aJEC;
  protected QuartzScheduler m_aQS;
  protected TriggerFiredBundle m_aFiredTriggerBundle;
  protected IScheduler m_aScheduler;
  protected volatile boolean m_bShutdownRequested = false;

  /**
   * <p>
   * Create a JobRunShell instance with the given settings.
   * </p>
   *
   * @param scheduler
   *        The <code>Scheduler</code> instance that should be made available within the
   *        <code>JobExecutionContext</code>.
   */
  public JobRunShell (@NonNull final IScheduler scheduler, @NonNull final TriggerFiredBundle bndle)
  {
    ValueEnforcer.notNull (scheduler, "Scheduler");
    ValueEnforcer.notNull (bndle, "FiredTriggerBundle");

    m_aScheduler = scheduler;
    m_aFiredTriggerBundle = bndle;
  }

  @Override
  public void schedulerShuttingdown ()
  {
    requestShutdown ();
  }

  public void initialize (@NonNull final QuartzScheduler sched) throws SchedulerException
  {
    ValueEnforcer.notNull (sched, "Scheduler");

    m_aQS = sched;

    IJob job = null;
    final IJobDetail jobDetail = m_aFiredTriggerBundle.getJobDetail ();

    try
    {
      job = sched.getJobFactory ().newJob (m_aFiredTriggerBundle, m_aScheduler);
    }
    catch (final SchedulerException se)
    {
      sched.notifySchedulerListenersError ("An error occured instantiating job to be executed. job= '" +
                                           jobDetail.getKey () +
                                           "'",
                                           se);
      throw se;
    }
    catch (final Throwable ncdfe)
    {
      // Catch Throwable (not just Exception) because the typical failure here is an Error - such
      // as NoClassDefFoundError - and an Error escaping this method is not handled by the caller
      final SchedulerException se = new SchedulerException ("Problem instantiating class '" +
                                                            jobDetail.getJobClass ().getName () +
                                                            "' - ",
                                                            ncdfe);
      sched.notifySchedulerListenersError ("An error occured instantiating job to be executed. job= '" +
                                           jobDetail.getKey () +
                                           "'",
                                           se);
      throw se;
    }

    m_aJEC = new JobExecutionContext (m_aScheduler, m_aFiredTriggerBundle, job);
  }

  public void requestShutdown ()
  {
    m_bShutdownRequested = true;
  }

  public void run ()
  {
    m_aQS.addInternalSchedulerListener (this);

    final IOperableTrigger aTrigger = (IOperableTrigger) m_aJEC.getTrigger ();
    final IJobDetail aJobDetail = m_aJEC.getJobDetail ();

    // The JobStore must be informed that this execution ended, no matter how this method is left.
    // For a job with @DisallowConcurrentExecution, triggeredJobComplete is the only thing that
    // clears the "blocked" marker that triggersFired set, and a trigger left in state BLOCKED is
    // never acquired again, is ignored by the misfire handling and cannot be resumed - so the job
    // silently stops running until the next restart.
    boolean jobStoreNotified = false;

    try
    {
      while (true)
      {
        JobExecutionException aJobExEx = null;
        final IJob aJob = m_aJEC.getJobInstance ();

        try
        {
          begin ();
        }
        catch (final SchedulerException se)
        {
          m_aQS.notifySchedulerListenersError ("Error executing Job (" +
                                               m_aJEC.getJobDetail ().getKey () +
                                               ": couldn't begin execution.",
                                               se);
          break;
        }

        // notify job & trigger listeners...
        try
        {
          if (!_notifyListenersBeginning (m_aJEC))
            break;
        }
        catch (final VetoedException ve)
        {
          try
          {
            final ECompletedExecutionInstruction instCode = aTrigger.executionComplete (m_aJEC,
                                                                                        (JobExecutionException) null);
            m_aQS.notifyJobStoreJobVetoed (aTrigger, aJobDetail, instCode);
            jobStoreNotified = true;

            // QTZ-205
            // Even if trigger got vetoed, we still needs to check to see if
            // it's the trigger's finalized run or not.
            if (m_aJEC.getTrigger ().getNextFireTime () == null)
            {
              m_aQS.notifySchedulerListenersFinalized (m_aJEC.getTrigger ());
            }

            complete (true);
          }
          catch (final SchedulerException se)
          {
            m_aQS.notifySchedulerListenersError ("Error during veto of Job (" +
                                                 m_aJEC.getJobDetail ().getKey () +
                                                 ": couldn't finalize execution.",
                                                 se);
          }
          break;
        }

        final long startTime = System.currentTimeMillis ();
        long endTime = startTime;

        // execute the job
        try
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Calling execute on job " + aJobDetail.getKey ());
          aJob.execute (m_aJEC);
          endTime = System.currentTimeMillis ();
        }
        catch (final JobExecutionException jee)
        {
          endTime = System.currentTimeMillis ();
          aJobExEx = jee;
          LOGGER.info ("Job " + aJobDetail.getKey () + " threw a JobExecutionException: ", aJobExEx);
        }
        catch (final Throwable ex)
        {
          // Catch Throwable (not just Exception) so that an Error - like the OutOfMemoryError of a
          // job that ran out of heap - is turned into a regular failed execution. Otherwise it
          // escapes this method and the JobStore is never told that the execution ended.
          endTime = System.currentTimeMillis ();
          LOGGER.error ("Job " + aJobDetail.getKey () + " threw an unhandled Throwable: ", ex);
          final SchedulerException se = new SchedulerException ("Job threw an unhandled throwable.", ex);
          m_aQS.notifySchedulerListenersError ("Job (" + m_aJEC.getJobDetail ().getKey () + " threw an exception.", se);
          aJobExEx = new JobExecutionException (se, false);
        }

        m_aJEC.setJobRunTime (endTime - startTime);

        // notify all job listeners
        if (!_notifyJobListenersComplete (m_aJEC, aJobExEx))
        {
          break;
        }

        ECompletedExecutionInstruction instCode = ECompletedExecutionInstruction.NOOP;

        // update the trigger
        try
        {
          instCode = aTrigger.executionComplete (m_aJEC, aJobExEx);
        }
        catch (final Exception e)
        {
          // If this happens, there's a bug in the trigger...
          final SchedulerException se = new SchedulerException ("Trigger threw an unhandled exception.", e);
          m_aQS.notifySchedulerListenersError ("Please report this error to the Quartz developers.", se);
        }

        // notify all trigger listeners
        if (!_notifyTriggerListenersComplete (m_aJEC, instCode))
        {
          break;
        }

        // update job/trigger or re-execute job
        if (instCode == ECompletedExecutionInstruction.RE_EXECUTE_JOB)
        {
          m_aJEC.incrementRefireCount ();
          try
          {
            complete (false);
          }
          catch (final SchedulerException se)
          {
            m_aQS.notifySchedulerListenersError ("Error executing Job (" +
                                                 m_aJEC.getJobDetail ().getKey () +
                                                 ": couldn't finalize execution.",
                                                 se);
          }
          continue;
        }

        try
        {
          complete (true);
        }
        catch (final SchedulerException se)
        {
          m_aQS.notifySchedulerListenersError ("Error executing Job (" +
                                               m_aJEC.getJobDetail ().getKey () +
                                               ": couldn't finalize execution.",
                                               se);
          continue;
        }

        m_aQS.notifyJobStoreJobComplete (aTrigger, aJobDetail, instCode);
        jobStoreNotified = true;
        break;
      }
    }
    finally
    {
      m_aQS.removeInternalSchedulerListener (this);

      if (!jobStoreNotified)
      {
        // Either one of the "break" paths above was taken - a failed begin(), a listener that could
        // not be notified - or the plumbing threw an Error. Use NOOP, so that the trigger simply
        // keeps its existing schedule and fires again: the causes that end up here are typically
        // transient, and parking the trigger in the ERROR state would stop the job until somebody
        // calls IScheduler.resetTriggerFromErrorState (...) by hand. The failure is logged here and
        // is collected by the job listeners anyway.
        LOGGER.error ("The execution of job " +
                      aJobDetail.getKey () +
                      " ended without the JobStore being notified - doing it now, so that the job is not blocked forever");
        m_aQS.notifyJobStoreJobComplete (aTrigger, aJobDetail, ECompletedExecutionInstruction.NOOP);
      }
    }
  }

  /**
   * @throws SchedulerException
   *         on error
   */
  protected void begin () throws SchedulerException
  {}

  /**
   * @param successfulExecution
   *        was the execution successful?
   * @throws SchedulerException
   *         in case of error
   */
  protected void complete (final boolean successfulExecution) throws SchedulerException
  {}

  public void passivate ()
  {
    m_aJEC = null;
    m_aQS = null;
  }

  private boolean _notifyListenersBeginning (@NonNull final IJobExecutionContext jobExCtxt) throws VetoedException
  {
    boolean vetoed = false;

    // notify all trigger listeners
    try
    {
      vetoed = m_aQS.notifyTriggerListenersFired (jobExCtxt);
    }
    catch (final SchedulerException se)
    {
      m_aQS.notifySchedulerListenersError ("Unable to notify TriggerListener(s) while firing trigger " +
                                           "(Trigger and Job will NOT be fired!). trigger= " +
                                           jobExCtxt.getTrigger ().getKey () +
                                           " job= " +
                                           jobExCtxt.getJobDetail ().getKey (),
                                           se);

      return false;
    }

    if (vetoed)
    {
      try
      {
        m_aQS.notifyJobListenersWasVetoed (jobExCtxt);
      }
      catch (final SchedulerException se)
      {
        m_aQS.notifySchedulerListenersError ("Unable to notify JobListener(s) of vetoed execution " +
                                             "while firing trigger (Trigger and Job will NOT be " +
                                             "fired!). trigger= " +
                                             jobExCtxt.getTrigger ().getKey () +
                                             " job= " +
                                             jobExCtxt.getJobDetail ().getKey (),
                                             se);

      }
      throw new VetoedException ();
    }

    // notify all job listeners
    try
    {
      m_aQS.notifyJobListenersToBeExecuted (jobExCtxt);
    }
    catch (final SchedulerException se)
    {
      m_aQS.notifySchedulerListenersError ("Unable to notify JobListener(s) of Job to be executed: " +
                                           "(Job will NOT be executed!). trigger= " +
                                           jobExCtxt.getTrigger ().getKey () +
                                           " job= " +
                                           jobExCtxt.getJobDetail ().getKey (),
                                           se);

      return false;
    }

    return true;
  }

  private boolean _notifyJobListenersComplete (@NonNull final IJobExecutionContext jobExCtxt,
                                               @Nullable final JobExecutionException jobExEx)
  {
    try
    {
      m_aQS.notifyJobListenersWasExecuted (jobExCtxt, jobExEx);
      return true;
    }
    catch (final SchedulerException se)
    {
      m_aQS.notifySchedulerListenersError ("Unable to notify JobListener(s) of Job that was executed: " +
                                           "(error will be ignored). trigger= " +
                                           jobExCtxt.getTrigger ().getKey () +
                                           " job= " +
                                           jobExCtxt.getJobDetail ().getKey (),
                                           se);

      return false;
    }
  }

  private boolean _notifyTriggerListenersComplete (@NonNull final IJobExecutionContext jobExCtxt,
                                                   @NonNull final ECompletedExecutionInstruction instCode)
  {
    try
    {
      m_aQS.notifyTriggerListenersComplete (jobExCtxt, instCode);

    }
    catch (final SchedulerException se)
    {
      m_aQS.notifySchedulerListenersError ("Unable to notify TriggerListener(s) of Job that was executed: " +
                                           "(error will be ignored). trigger= " +
                                           jobExCtxt.getTrigger ().getKey () +
                                           " job= " +
                                           jobExCtxt.getJobDetail ().getKey (),
                                           se);

      return false;
    }
    if (jobExCtxt.getTrigger ().getNextFireTime () == null)
    {
      m_aQS.notifySchedulerListenersFinalized (jobExCtxt.getTrigger ());
    }

    return true;
  }

  static class VetoedException extends Exception
  {
    public VetoedException ()
    {}
  }

}
