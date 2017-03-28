/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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
  private boolean refire = false;

  private boolean unscheduleTrigg = false;

  private boolean unscheduleAllTriggs = false;

  /**
   * <p>
   * Create a JobExcecutionException, with the 're-fire immediately' flag set to
   * <code>false</code>.
   * </p>
   */
  public JobExecutionException ()
  {}

  /**
   * <p>
   * Create a JobExcecutionException, with the given cause.
   * </p>
   */
  public JobExecutionException (final Throwable cause)
  {
    super (cause);
  }

  /**
   * <p>
   * Create a JobExcecutionException, with the given message.
   * </p>
   */
  public JobExecutionException (final String msg)
  {
    super (msg);
  }

  /**
   * <p>
   * Create a JobExcecutionException with the 're-fire immediately' flag set to
   * the given value.
   * </p>
   */
  public JobExecutionException (final boolean refireImmediately)
  {
    refire = refireImmediately;
  }

  /**
   * <p>
   * Create a JobExcecutionException with the given underlying exception, and
   * the 're-fire immediately' flag set to the given value.
   * </p>
   */
  public JobExecutionException (final Throwable cause, final boolean refireImmediately)
  {
    super (cause);

    refire = refireImmediately;
  }

  /**
   * <p>
   * Create a JobExcecutionException with the given message, and underlying
   * exception.
   * </p>
   */
  public JobExecutionException (final String msg, final Throwable cause)
  {
    super (msg, cause);
  }

  /**
   * <p>
   * Create a JobExcecutionException with the given message, and underlying
   * exception, and the 're-fire immediately' flag set to the given value.
   * </p>
   */
  public JobExecutionException (final String msg, final Throwable cause, final boolean refireImmediately)
  {
    super (msg, cause);

    refire = refireImmediately;
  }

  /**
   * Create a JobExcecutionException with the given message and the 're-fire
   * immediately' flag set to the given value.
   */
  public JobExecutionException (final String msg, final boolean refireImmediately)
  {
    super (msg);

    refire = refireImmediately;
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  public void setRefireImmediately (final boolean refire)
  {
    this.refire = refire;
  }

  public boolean refireImmediately ()
  {
    return refire;
  }

  public void setUnscheduleFiringTrigger (final boolean unscheduleTrigg)
  {
    this.unscheduleTrigg = unscheduleTrigg;
  }

  public boolean unscheduleFiringTrigger ()
  {
    return unscheduleTrigg;
  }

  public void setUnscheduleAllTriggers (final boolean unscheduleAllTriggs)
  {
    this.unscheduleAllTriggs = unscheduleAllTriggs;
  }

  public boolean unscheduleAllTriggers ()
  {
    return unscheduleAllTriggs;
  }

}
