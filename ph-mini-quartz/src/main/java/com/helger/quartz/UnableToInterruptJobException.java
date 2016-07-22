/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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
 * An exception that is thrown to indicate that a call to
 * InterruptableJob.interrupt() failed without interrupting the Job.
 *
 * @see com.helger.quartz.IInterruptableJob#interrupt()
 * @author James House
 */
public class UnableToInterruptJobException extends SchedulerException
{
  /**
   * <p>
   * Create a <code>UnableToInterruptJobException</code> with the given message.
   * </p>
   */
  public UnableToInterruptJobException (final String msg)
  {
    super (msg);
  }

  /**
   * <p>
   * Create a <code>UnableToInterruptJobException</code> with the given cause.
   * </p>
   */
  public UnableToInterruptJobException (final Throwable cause)
  {
    super (cause);
  }

}
