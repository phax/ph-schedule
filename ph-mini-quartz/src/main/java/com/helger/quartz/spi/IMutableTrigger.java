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
package com.helger.quartz.spi;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.quartz.ITrigger;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobKey;
import com.helger.quartz.TriggerKey;

public interface IMutableTrigger extends ITrigger
{
  void setKey (@Nonnull TriggerKey key);

  void setJobKey (@Nonnull JobKey key);

  /**
   * Set a description for the <code>Trigger</code> instance - may be useful for
   * remembering/displaying the purpose of the trigger, though the description
   * has no meaning to Quartz.
   */
  void setDescription (@Nullable String sDescription);

  /**
   * Associate the <code>Calendar</code> with the given name with this Trigger.
   *
   * @param sCalendarName
   *        use <code>null</code> to dis-associate a Calendar.
   */
  void setCalendarName (@Nullable String sCalendarName);

  /**
   * Set the <code>JobDataMap</code> to be associated with the
   * <code>Trigger</code>.
   *
   * @param jobDataMap
   *        The new job data map. May be <code>null</code>.
   */
  void setJobDataMap (@Nullable JobDataMap jobDataMap);

  /**
   * The priority of a <code>Trigger</code> acts as a tie breaker such that if
   * two <code>Trigger</code>s have the same scheduled fire time, then Quartz
   * will do its best to give the one with the higher priority first access to a
   * worker thread.<br>
   * If not explicitly set, the default value is <code>5</code>.
   *
   * @see #DEFAULT_PRIORITY
   */
  void setPriority (int priority);

  /**
   * The time at which the trigger's scheduling should start. May or may not be
   * the first actual fire time of the trigger, depending upon the type of
   * trigger and the settings of the other properties of the trigger. However
   * the first actual first time will not be before this date.<br>
   * Setting a value in the past may cause a new trigger to compute a first fire
   * time that is in the past, which may cause an immediate misfire of the
   * trigger.
   *
   * @param startTime
   *        start time
   */
  void setStartTime (@Nonnull Date startTime);

  /**
   * <p>
   * Set the time at which the <code>Trigger</code> should quit repeating -
   * regardless of any remaining repeats (based on the trigger's particular
   * repeat settings).
   * </p>
   *
   * @see com.helger.quartz.TriggerUtils#computeEndTimeToAllowParticularNumberOfFirings(IOperableTrigger,com.helger.quartz.ICalendar,int)
   */
  void setEndTime (@Nullable Date endTime);

  /**
   * Set the instruction the <code>Scheduler</code> should be given for handling
   * misfire situations for this <code>Trigger</code>- the concrete
   * <code>Trigger</code> type that you are using will have defined a set of
   * additional <code>MISFIRE_INSTRUCTION_XXX</code> constants that may be
   * passed to this method.<br>
   * If not explicitly set, the default value is
   * <code>MISFIRE_INSTRUCTION_SMART_POLICY</code>.
   *
   * @see com.helger.quartz.impl.triggers.AbstractTrigger#updateAfterMisfire(com.helger.quartz.ICalendar)
   * @see com.helger.quartz.ISimpleTrigger
   * @see com.helger.quartz.ICronTrigger
   */
  void setMisfireInstruction (EMisfireInstruction eMisfireInstruction);

  IMutableTrigger getClone ();
}
