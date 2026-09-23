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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.quartz.JobKey;

/**
 * A global, bounded registry of the most recent failed job executions, per job. It is filled by
 * {@link ErrorHistoryJobListener} and exists so that the reason of a failure is available for
 * inspection without having to consult the log file.
 * <p>
 * At most {@link #getMaxErrorsPerJob()} entries are kept per job - the oldest one is dropped when
 * the limit is reached. Setting the maximum to 0 disables the collection altogether and drops
 * everything that was collected so far.
 * </p>
 *
 * @author Philip Helger
 * @since 6.1.2
 */
@ThreadSafe
public final class JobExecutionErrorRegistry
{
  /** The default number of errors that are remembered per job */
  public static final int DEFAULT_MAX_ERRORS_PER_JOB = 5;

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static final ICommonsOrderedMap <JobKey, ICommonsList <JobExecutionError>> ERRORS = new CommonsLinkedHashMap <> ();
  @GuardedBy ("RW_LOCK")
  private static int s_nMaxErrorsPerJob = DEFAULT_MAX_ERRORS_PER_JOB;

  private JobExecutionErrorRegistry ()
  {}

  /**
   * @return The maximum number of errors that are remembered per job. Always &ge; 0. 0 means that
   *         no errors are collected at all.
   */
  @Nonnegative
  public static int getMaxErrorsPerJob ()
  {
    return RW_LOCK.readLockedInt ( () -> s_nMaxErrorsPerJob);
  }

  /**
   * Set the maximum number of errors to be remembered per job. Existing entries that exceed the
   * new limit are dropped immediately.
   *
   * @param nMaxErrorsPerJob
   *        The new maximum. Must be &ge; 0. Use 0 to disable the collection entirely.
   */
  public static void setMaxErrorsPerJob (@Nonnegative final int nMaxErrorsPerJob)
  {
    ValueEnforcer.isGE0 (nMaxErrorsPerJob, "MaxErrorsPerJob");

    RW_LOCK.writeLocked ( () -> {
      s_nMaxErrorsPerJob = nMaxErrorsPerJob;
      if (nMaxErrorsPerJob == 0)
        ERRORS.clear ();
      else
        for (final ICommonsList <JobExecutionError> aList : ERRORS.values ())
          while (aList.size () > nMaxErrorsPerJob)
            aList.removeFirstOrNull ();
    });
  }

  /**
   * Remember a single failed job execution. Called by {@link ErrorHistoryJobListener}.
   *
   * @param aError
   *        The error to be remembered. May not be <code>null</code>.
   */
  public static void addError (@NonNull final JobExecutionError aError)
  {
    ValueEnforcer.notNull (aError, "Error");

    RW_LOCK.writeLocked ( () -> {
      if (s_nMaxErrorsPerJob == 0)
      {
        // Collection is disabled
        return;
      }

      final ICommonsList <JobExecutionError> aList = ERRORS.computeIfAbsent (aError.getJobKey (),
                                                                             k -> new CommonsArrayList <> ());
      aList.add (aError);
      while (aList.size () > s_nMaxErrorsPerJob)
        aList.removeFirstOrNull ();
    });
  }

  /**
   * Get all remembered errors of a single job, oldest first.
   *
   * @param aJobKey
   *        The job to get the errors for. May be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty copy of the list.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <JobExecutionError> getAllErrorsOfJob (@Nullable final JobKey aJobKey)
  {
    if (aJobKey == null)
      return new CommonsArrayList <> ();

    return RW_LOCK.readLockedGet ( () -> {
      final ICommonsList <JobExecutionError> aList = ERRORS.get (aJobKey);
      return aList == null ? new CommonsArrayList <> () : aList.getClone ();
    });
  }

  /**
   * Get the most recent error of a single job.
   *
   * @param aJobKey
   *        The job to get the last error for. May be <code>null</code>.
   * @return <code>null</code> if the job never failed, or if all its errors were removed.
   */
  @Nullable
  public static JobExecutionError getLastErrorOfJob (@Nullable final JobKey aJobKey)
  {
    if (aJobKey == null)
      return null;

    return RW_LOCK.readLockedGet ( () -> {
      final ICommonsList <JobExecutionError> aList = ERRORS.get (aJobKey);
      return aList == null ? null : aList.getLastOrNull ();
    });
  }

  /**
   * @return A copy of all remembered errors of all jobs, in the order in which the jobs failed for
   *         the first time. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsMap <JobKey, ICommonsList <JobExecutionError>> getAllErrors ()
  {
    return RW_LOCK.readLockedGet ( () -> {
      final ICommonsOrderedMap <JobKey, ICommonsList <JobExecutionError>> ret = new CommonsLinkedHashMap <> ();
      for (final var aEntry : ERRORS.entrySet ())
        ret.put (aEntry.getKey (), aEntry.getValue ().getClone ());
      return ret;
    });
  }

  /**
   * @return The total number of remembered errors over all jobs. Always &ge; 0.
   */
  @Nonnegative
  public static int getTotalErrorCount ()
  {
    return RW_LOCK.readLockedInt ( () -> {
      int ret = 0;
      for (final ICommonsList <JobExecutionError> aList : ERRORS.values ())
        ret += aList.size ();
      return ret;
    });
  }

  /**
   * Remove all remembered errors of a single job.
   *
   * @param aJobKey
   *        The job to remove the errors of. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if at least one error was removed.
   */
  @NonNull
  public static EChange removeAllErrorsOfJob (@Nullable final JobKey aJobKey)
  {
    if (aJobKey == null)
      return EChange.UNCHANGED;

    return RW_LOCK.writeLockedGet ( () -> EChange.valueOf (ERRORS.remove (aJobKey) != null));
  }

  /**
   * Remove all remembered errors of all jobs.
   *
   * @return {@link EChange#CHANGED} if at least one error was removed.
   */
  @NonNull
  public static EChange removeAllErrors ()
  {
    return RW_LOCK.writeLockedGet ( () -> ERRORS.removeAll ());
  }
}
