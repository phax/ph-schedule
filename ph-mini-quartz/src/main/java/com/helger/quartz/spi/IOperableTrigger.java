/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2018 Philip Helger (www.helger.com)
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
package com.helger.quartz.spi;

import java.util.Date;

import com.helger.quartz.ICalendar;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.SchedulerException;

public interface IOperableTrigger extends IMutableTrigger
{
  /**
   * <p>
   * This method should not be used by the Quartz client.
   * </p>
   * <p>
   * Called when the <code>{@link IScheduler}</code> has decided to 'fire' the
   * trigger (execute the associated <code>Job</code>), in order to give the
   * <code>Trigger</code> a chance to update itself for its next triggering (if
   * any).
   * </p>
   *
   * @see #executionComplete(IJobExecutionContext, JobExecutionException)
   */
  void triggered (ICalendar calendar);

  /**
   * <p>
   * This method should not be used by the Quartz client.
   * </p>
   * <p>
   * Called by the scheduler at the time a <code>Trigger</code> is first added to
   * the scheduler, in order to have the <code>Trigger</code> compute its first
   * fire time, based on any associated calendar.
   * </p>
   * <p>
   * After this method has been called, <code>getNextFireTime()</code> should
   * return a valid answer.
   * </p>
   *
   * @return the first time at which the <code>Trigger</code> will be fired by the
   *         scheduler, which is also the same value
   *         <code>getNextFireTime()</code> will return (until after the first
   *         firing of the <code>Trigger</code>).
   */
  Date computeFirstFireTime (ICalendar calendar);

  /**
   * <p>
   * This method should not be used by the Quartz client.
   * </p>
   * <p>
   * Called after the <code>{@link IScheduler}</code> has executed the
   * <code>{@link com.helger.quartz.IJobDetail}</code> associated with the
   * <code>Trigger</code> in order to get the final instruction code from the
   * trigger.
   * </p>
   *
   * @param context
   *        is the <code>JobExecutionContext</code> that was used by the
   *        <code>Job</code>'s<code>execute(xx)</code> method.
   * @param result
   *        is the <code>JobExecutionException</code> thrown by the
   *        <code>Job</code>, if any (may be null).
   * @return one of the <code>CompletedExecutionInstruction</code> constants.
   * @see #triggered(ICalendar)
   */
  ITrigger.ECompletedExecutionInstruction executionComplete (IJobExecutionContext context,
                                                             JobExecutionException result);

  /**
   * <p>
   * This method should not be used by the Quartz client.
   * </p>
   * <p>
   * To be implemented by the concrete classes that extend this class.
   * </p>
   * <p>
   * The implementation should update the <code>Trigger</code>'s state based on
   * the MISFIRE_INSTRUCTION_XXX that was selected when the <code>Trigger</code>
   * was created.
   * </p>
   */
  void updateAfterMisfire (ICalendar cal);

  /**
   * <p>
   * This method should not be used by the Quartz client.
   * </p>
   * <p>
   * To be implemented by the concrete class.
   * </p>
   * <p>
   * The implementation should update the <code>Trigger</code>'s state based on
   * the given new version of the associated <code>Calendar</code> (the state
   * should be updated so that it's next fire time is appropriate given the
   * Calendar's new settings).
   * </p>
   *
   * @param cal
   */
  void updateWithNewCalendar (ICalendar cal, long misfireThreshold);

  /**
   * <p>
   * Validates whether the properties of the <code>JobDetail</code> are valid for
   * submission into a <code>Scheduler</code>.
   *
   * @throws IllegalStateException
   *         if a required property (such as Name, Group, Class) is not set.
   */
  void validate () throws SchedulerException;

  /**
   * <p>
   * This method should not be used by the Quartz client.
   * </p>
   * <p>
   * Usable by <code>{@link com.helger.quartz.spi.IJobStore}</code>
   * implementations, in order to facilitate 'recognizing' instances of fired
   * <code>Trigger</code> s as their jobs complete execution.
   * </p>
   */
  void setFireInstanceId (String id);

  /**
   * <p>
   * This method should not be used by the Quartz client.
   * </p>
   */
  String getFireInstanceId ();

  void setNextFireTime (Date nextFireTime);

  void setPreviousFireTime (Date previousFireTime);
}
