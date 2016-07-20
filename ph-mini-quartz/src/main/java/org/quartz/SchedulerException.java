
/*
 * Copyright 2001-2009 Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package org.quartz;

/**
 * Base class for exceptions thrown by the Quartz
 * <code>{@link Scheduler}</code>.
 * <p>
 * <code>SchedulerException</code>s may contain a reference to another
 * <code>Exception</code>, which was the underlying cause of the
 * <code>SchedulerException</code>.
 * </p>
 *
 * @author James House
 */
public class SchedulerException extends Exception
{
  public SchedulerException ()
  {
    super ();
  }

  public SchedulerException (final String msg)
  {
    super (msg);
  }

  public SchedulerException (final Throwable cause)
  {
    super (cause);
  }

  public SchedulerException (final String msg, final Throwable cause)
  {
    super (msg, cause);
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Return the exception that is the underlying cause of this exception.
   * </p>
   * <p>
   * This may be used to find more detail about the cause of the error.
   * </p>
   *
   * @return the underlying exception, or <code>null</code> if there is not one.
   */
  public Throwable getUnderlyingException ()
  {
    return super.getCause ();
  }

  @Override
  public String toString ()
  {
    final Throwable cause = getUnderlyingException ();
    if (cause == null || cause == this)
    {
      return super.toString ();
    }
    else
    {
      return super.toString () + " [See nested exception: " + cause + "]";
    }
  }

}
