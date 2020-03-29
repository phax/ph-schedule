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
package com.helger.quartz;

/**
 * Base class for exceptions thrown by the Quartz
 * <code>{@link IScheduler}</code>.
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
}
