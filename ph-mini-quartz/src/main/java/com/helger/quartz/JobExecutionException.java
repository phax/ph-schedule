/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2022 Philip Helger (www.helger.com)
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
package com.helger.quartz;

/**
 * An exception that can be thrown by a
 * <code>{@link com.helger.quartz.IJob}</code> to indicate to the Quartz
 * <code>{@link IScheduler}</code> that an error occurred while executing, and
 * whether or not the <code>Job</code> requests to be re-fired immediately
 * (using the same <code>{@link IJobExecutionContext}</code>, or whether it
 * wants to be unscheduled.
 * <p>
 * Note that if the flag for 'refire immediately' is set, the flags for
 * unscheduling the Job are ignored.
 * </p>
 *
 * @see IJob
 * @see IJobExecutionContext
 * @see SchedulerException
 * @author James House
 */
public class JobExecutionException extends SchedulerException
{
  private final boolean m_bRefire;
  private final boolean m_bUnscheduleTrigg = false;
  private final boolean m_bUnscheduleAllTriggs = false;

  /**
   * Create a JobExcecutionException, with the 're-fire immediately' flag set to
   * <code>false</code>.
   */
  public JobExecutionException ()
  {
    this (null, null, false);
  }

  /**
   * Create a JobExcecutionException, with the given cause.
   */
  public JobExecutionException (final Throwable cause)
  {
    this (null, cause, false);
  }

  /**
   * Create a JobExcecutionException, with the given message.
   */
  public JobExecutionException (final String msg)
  {
    this (msg, null, false);
  }

  /**
   * Create a JobExcecutionException with the 're-fire immediately' flag set to
   * the given value.
   */
  public JobExecutionException (final boolean refireImmediately)
  {
    this (null, null, refireImmediately);
  }

  /**
   * Create a JobExcecutionException with the given underlying exception, and
   * the 're-fire immediately' flag set to the given value.
   */
  public JobExecutionException (final Throwable cause, final boolean refireImmediately)
  {
    this (null, cause, refireImmediately);
  }

  /**
   * Create a JobExcecutionException with the given message, and underlying
   * exception.
   */
  public JobExecutionException (final String msg, final Throwable cause)
  {
    this (msg, cause, false);
  }

  /**
   * Create a JobExcecutionException with the given message and the 're-fire
   * immediately' flag set to the given value.
   */
  public JobExecutionException (final String msg, final boolean refireImmediately)
  {
    this (msg, null, refireImmediately);
  }

  /**
   * Create a JobExcecutionException with the given message, and underlying
   * exception, and the 're-fire immediately' flag set to the given value.
   */
  public JobExecutionException (final String msg, final Throwable cause, final boolean refireImmediately)
  {
    super (msg, cause);

    m_bRefire = refireImmediately;
  }

  public boolean refireImmediately ()
  {
    return m_bRefire;
  }

  public boolean unscheduleFiringTrigger ()
  {
    return m_bUnscheduleTrigg;
  }

  public boolean unscheduleAllTriggers ()
  {
    return m_bUnscheduleAllTriggs;
  }
}
