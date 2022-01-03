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
 * A <code>{@link ITrigger}</code> that is used to fire a <code>Job</code> at a
 * given moment in time, and optionally repeated at a specified interval.
 *
 * @see TriggerBuilder
 * @see SimpleScheduleBuilder
 * @author James House
 * @author contributions by Lieven Govaerts of Ebitec Nv, Belgium.
 */
public interface ISimpleTrigger extends ITrigger
{
  /**
   * Used to indicate the 'repeat count' of the trigger is indefinite. Or in
   * other words, the trigger should repeat continually until the trigger's
   * ending timestamp.
   */
  int REPEAT_INDEFINITELY = -1;

  /**
   * <p>
   * Get the the number of times the <code>SimpleTrigger</code> should repeat,
   * after which it will be automatically deleted.
   * </p>
   *
   * @see #REPEAT_INDEFINITELY
   */
  int getRepeatCount ();

  /**
   * <p>
   * Get the the time interval (in milliseconds) at which the
   * <code>SimpleTrigger</code> should repeat.
   * </p>
   */
  long getRepeatInterval ();

  /**
   * <p>
   * Get the number of times the <code>SimpleTrigger</code> has already fired.
   * </p>
   */
  int getTimesTriggered ();

  TriggerBuilder <? extends ISimpleTrigger> getTriggerBuilder ();
}
