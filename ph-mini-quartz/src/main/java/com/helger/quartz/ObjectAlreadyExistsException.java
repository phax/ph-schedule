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
 * An exception that is thrown to indicate that an attempt to store a new object
 * (i.e.
 * <code>{@link com.helger.quartz.IJobDetail}</code>,<code>{@link ITrigger}</code>
 * or <code>{@link ICalendar}</code>) in a <code>{@link IScheduler}</code>
 * failed, because one with the same name &amp; group already exists.
 *
 * @author James House
 */
public class ObjectAlreadyExistsException extends JobPersistenceException
{
  /**
   * <p>
   * Create a <code>ObjectAlreadyExistsException</code> with the given message.
   * </p>
   */
  public ObjectAlreadyExistsException (final String msg)
  {
    super (msg);
  }

  /**
   * <p>
   * Create a <code>ObjectAlreadyExistsException</code> and auto-generate a
   * message using the name/group from the given <code>JobDetail</code>.
   * </p>
   * <p>
   * The message will read: <BR>
   * "Unable to store Job with name: '__' and group: '__', because one already
   * exists with this identification."
   * </p>
   */
  public ObjectAlreadyExistsException (final IJobDetail offendingJob)
  {
    super ("Unable to store Job : '" +
           offendingJob.getKey () +
           "', because one already exists with this identification.");
  }

  /**
   * <p>
   * Create a <code>ObjectAlreadyExistsException</code> and auto-generate a
   * message using the name/group from the given <code>Trigger</code>.
   * </p>
   * <p>
   * The message will read: <BR>
   * "Unable to store Trigger with name: '__' and group: '__', because one
   * already exists with this identification."
   * </p>
   */
  public ObjectAlreadyExistsException (final ITrigger offendingTrigger)
  {
    super ("Unable to store Trigger with name: '" +
           offendingTrigger.getKey ().getName () +
           "' and group: '" +
           offendingTrigger.getKey ().getGroup () +
           "', because one already exists with this identification.");
  }

}
