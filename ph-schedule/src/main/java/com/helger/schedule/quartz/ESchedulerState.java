/*
 * Copyright (C) 2014-2026 Philip Helger (www.helger.com)
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
package com.helger.schedule.quartz;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * Defines the different scheduler states.
 *
 * @author Philip Helger
 */
public enum ESchedulerState implements IHasID <String>
{
  /** The scheduler is running and executes jobs */
  STARTED ("started"),
  /**
   * The scheduler was started and put into standby mode afterwards. No jobs are executed in this
   * state.
   */
  STANDBY ("standby"),
  /**
   * The scheduler exists, but was never started. No jobs are executed in this state. Quartz
   * reports standby mode for this state as well, so the two can only be told apart via
   * {@link com.helger.quartz.SchedulerMetaData#getRunningSince()}.
   *
   * @since 6.1.2
   */
  NOT_STARTED ("not-started"),
  /** The scheduler was shut down. No jobs are executed in this state any more. */
  SHUTDOWN ("shutdown");

  private String m_sID;

  ESchedulerState (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static ESchedulerState getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESchedulerState.class, sID);
  }
}
