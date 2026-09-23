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
package com.helger.schedule.quartz.listener;

import java.time.LocalDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.rt.StackTraceHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobKey;

/**
 * Represents a single failed job execution, as collected by {@link ErrorHistoryJobListener}.
 * <p>
 * The causing {@link Throwable} is deliberately <b>not</b> retained. A stored exception keeps its
 * whole reference graph alive - which for a job that failed while holding a large payload can be a
 * significant amount of memory - and the only thing a diagnostic view needs is the text. The class
 * name, the message and the stack trace are therefore extracted eagerly.
 * </p>
 *
 * @author Philip Helger
 * @since 6.2.0
 */
@Immutable
public class JobExecutionError
{
  private final LocalDateTime m_aErrorDT;
  private final JobKey m_aJobKey;
  private final String m_sJobClassName;
  private final String m_sFireInstanceID;
  private final String m_sThrowableClassName;
  private final String m_sThrowableMessage;
  private final String m_sStackTrace;

  public JobExecutionError (@NonNull final LocalDateTime aErrorDT,
                            @NonNull final JobKey aJobKey,
                            @NonNull @Nonempty final String sJobClassName,
                            @Nullable final String sFireInstanceID,
                            @NonNull final Throwable aThrowable)
  {
    m_aErrorDT = ValueEnforcer.notNull (aErrorDT, "ErrorDT");
    m_aJobKey = ValueEnforcer.notNull (aJobKey, "JobKey");
    m_sJobClassName = ValueEnforcer.notEmpty (sJobClassName, "JobClassName");
    m_sFireInstanceID = sFireInstanceID;
    ValueEnforcer.notNull (aThrowable, "Throwable");
    m_sThrowableClassName = aThrowable.getClass ().getName ();
    m_sThrowableMessage = aThrowable.getMessage ();
    m_sStackTrace = StackTraceHelper.getStackAsString (aThrowable);
  }

  /**
   * @return The date and time at which the failure was recorded. Never <code>null</code>.
   */
  @NonNull
  public LocalDateTime getErrorDateTime ()
  {
    return m_aErrorDT;
  }

  /**
   * @return The key of the job that failed. Never <code>null</code>.
   */
  @NonNull
  public JobKey getJobKey ()
  {
    return m_aJobKey;
  }

  /**
   * @return The name of the job class that failed. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getJobClassName ()
  {
    return m_sJobClassName;
  }

  /**
   * @return The unique ID of the firing instance that failed, as provided by
   *         {@link IJobExecutionContext#getFireInstanceId()}. May be <code>null</code>.
   */
  @Nullable
  public String getFireInstanceID ()
  {
    return m_sFireInstanceID;
  }

  /**
   * @return The class name of the causing exception. Never <code>null</code>.
   */
  @NonNull
  public String getThrowableClassName ()
  {
    return m_sThrowableClassName;
  }

  /**
   * @return The message of the causing exception. May be <code>null</code>.
   */
  @Nullable
  public String getThrowableMessage ()
  {
    return m_sThrowableMessage;
  }

  /**
   * @return The stack trace of the causing exception. Never <code>null</code>.
   */
  @NonNull
  public String getStackTrace ()
  {
    return m_sStackTrace;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ErrorDT", m_aErrorDT)
                                       .append ("JobKey", m_aJobKey)
                                       .append ("JobClassName", m_sJobClassName)
                                       .append ("FireInstanceID", m_sFireInstanceID)
                                       .append ("ThrowableClassName", m_sThrowableClassName)
                                       .append ("ThrowableMessage", m_sThrowableMessage)
                                       .getToString ();
  }
}
