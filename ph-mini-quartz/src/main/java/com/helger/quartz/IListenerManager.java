/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2026 Philip Helger (www.helger.com)
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

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.ICommonsList;

/**
 * Client programs may be interested in the 'listener' interfaces that are available from Quartz.
 * The <code>{@link IJobListener}</code> interface provides notifications of <code>Job</code>
 * executions. The <code>{@link ITriggerListener}</code> interface provides notifications of
 * <code>Trigger</code> firings. The <code>{@link ISchedulerListener}</code> interface provides
 * notifications of <code>Scheduler</code> events and errors. Listeners can be associated with local
 * schedulers through the {@link IListenerManager} interface.
 * <p>
 * Listener registration order is preserved, and hence notification of listeners will be in the
 * order in which they were registered.
 * </p>
 *
 * @author jhouse
 * @since 2.0 - previously listeners were managed directly on the Scheduler interface.
 */
public interface IListenerManager
{
  /**
   * Add the given <code>{@link IJobListener}</code> to the <code>Scheduler</code>, and register it
   * to receive events for all Jobs. Because no matchers are provided, the
   * <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addJobListener (@NonNull IJobListener jobListener);

  /**
   * Add the given <code>{@link IJobListener}</code> to the <code>Scheduler</code>, and register it
   * to receive events for Jobs that are matched by the given Matcher. If no matchers are provided,
   * the <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addJobListener (@NonNull IJobListener jobListener, @Nullable IMatcher <JobKey> matcher);

  /**
   * Add the given <code>{@link IJobListener}</code> to the <code>Scheduler</code>, and register it
   * to receive events for Jobs that are matched by ANY of the given Matchers. If no matchers are
   * provided, the <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addJobListener (@NonNull IJobListener jobListener,
                       @SuppressWarnings ("unchecked") @Nullable IMatcher <JobKey>... matchers);

  /**
   * Add the given <code>{@link IJobListener}</code> to the <code>Scheduler</code>, and register it
   * to receive events for Jobs that are matched by ANY of the given Matchers. If no matchers are
   * provided, the <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addJobListener (@NonNull IJobListener jobListener, @Nullable List <IMatcher <JobKey>> matchers);

  /**
   * Add the given Matcher to the set of matchers for which the listener will receive events if ANY
   * of the matchers match.
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @param matcher
   *        the additional matcher to apply for selecting events
   * @return true if the identified listener was found and updated
   */
  boolean addJobListenerMatcher (@NonNull String listenerName, @NonNull IMatcher <JobKey> matcher);

  /**
   * Remove the given Matcher to the set of matchers for which the listener will receive events if
   * ANY of the matchers match.
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @param matcher
   *        the additional matcher to apply for selecting events
   * @return true if the given matcher was found and removed from the listener's list of matchers
   */
  boolean removeJobListenerMatcher (@NonNull String listenerName, @NonNull IMatcher <JobKey> matcher);

  /**
   * Set the set of Matchers for which the listener will receive events if ANY of the matchers
   * match.
   * <p>
   * Removes any existing matchers for the identified listener!
   * </p>
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @param matchers
   *        the matchers to apply for selecting events
   * @return true if the given matcher was found and removed from the listener's list of matchers
   */
  boolean setJobListenerMatchers (@NonNull String listenerName, @NonNull List <IMatcher <JobKey>> matchers);

  /**
   * Get the set of Matchers for which the listener will receive events if ANY of the matchers
   * match.
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @return the matchers registered for selecting events for the identified listener
   */
  @Nullable
  @ReturnsMutableCopy
  ICommonsList <IMatcher <JobKey>> getJobListenerMatchers (@Nullable String listenerName);

  /**
   * Remove the identified <code>{@link IJobListener}</code> from the <code>Scheduler</code>.
   *
   * @return true if the identified listener was found in the list, and removed.
   */
  boolean removeJobListener (@Nullable String name);

  /**
   * Get a List containing all of the <code>{@link IJobListener}</code>s in the
   * <code>Scheduler</code>, in the order in which they were registered.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <IJobListener> getJobListeners ();

  /**
   * Get the <code>{@link IJobListener}</code> that has the given name.
   */
  @Nullable
  IJobListener getJobListener (@Nullable String name);

  /**
   * Add the given <code>{@link ITriggerListener}</code> to the <code>Scheduler</code>, and register
   * it to receive events for all Triggers. Because no matcher is provided, the
   * <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addTriggerListener (@NonNull ITriggerListener triggerListener);

  /**
   * Add the given <code>{@link ITriggerListener}</code> to the <code>Scheduler</code>, and register
   * it to receive events for Triggers that are matched by the given Matcher. If no matcher is
   * provided, the <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addTriggerListener (@NonNull ITriggerListener triggerListener, @NonNull IMatcher <TriggerKey> matcher);

  /**
   * Add the given <code>{@link ITriggerListener}</code> to the <code>Scheduler</code>, and register
   * it to receive events for Triggers that are matched by ANY of the given Matchers. If no matcher
   * is provided, the <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addTriggerListener (@NonNull ITriggerListener triggerListener,
                           @SuppressWarnings ("unchecked") @Nullable IMatcher <TriggerKey>... matchers);

  /**
   * Add the given <code>{@link ITriggerListener}</code> to the <code>Scheduler</code>, and register
   * it to receive events for Triggers that are matched by ANY of the given Matchers. If no matcher
   * is provided, the <code>EverythingMatcher</code> will be used.
   *
   * @see IMatcher
   * @see com.helger.quartz.impl.matchers.EverythingMatcher
   */
  void addTriggerListener (@NonNull ITriggerListener triggerListener, @Nullable List <IMatcher <TriggerKey>> matchers);

  /**
   * Add the given Matcher to the set of matchers for which the listener will receive events if ANY
   * of the matchers match.
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @param matcher
   *        the additional matcher to apply for selecting events
   * @return true if the identified listener was found and updated
   */
  boolean addTriggerListenerMatcher (@NonNull String listenerName, @NonNull IMatcher <TriggerKey> matcher);

  /**
   * Remove the given Matcher to the set of matchers for which the listener will receive events if
   * ANY of the matchers match.
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @param matcher
   *        the additional matcher to apply for selecting events
   * @return true if the given matcher was found and removed from the listener's list of matchers
   */
  boolean removeTriggerListenerMatcher (@NonNull String listenerName, @NonNull IMatcher <TriggerKey> matcher);

  /**
   * Set the set of Matchers for which the listener will receive events if ANY of the matchers
   * match.
   * <p>
   * Removes any existing matchers for the identified listener!
   * </p>
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @param matchers
   *        the matchers to apply for selecting events
   * @return true if the given matcher was found and removed from the listener's list of matchers
   */
  boolean setTriggerListenerMatchers (@NonNull String listenerName, @NonNull List <IMatcher <TriggerKey>> matchers);

  /**
   * Get the set of Matchers for which the listener will receive events if ANY of the matchers
   * match.
   *
   * @param listenerName
   *        the name of the listener to add the matcher to
   * @return the matchers registered for selecting events for the identified listener
   */
  @Nullable
  @ReturnsMutableCopy
  ICommonsList <IMatcher <TriggerKey>> getTriggerListenerMatchers (@Nullable String listenerName);

  /**
   * Remove the identified <code>{@link ITriggerListener}</code> from the <code>Scheduler</code>.
   *
   * @return true if the identified listener was found in the list, and removed.
   */
  boolean removeTriggerListener (@Nullable String name);

  /**
   * Get a List containing all of the <code>{@link ITriggerListener}</code>s in the
   * <code>Scheduler</code>, in the order in which they were registered.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ITriggerListener> getTriggerListeners ();

  /**
   * Get the <code>{@link ITriggerListener}</code> that has the given name.
   */
  @Nullable
  ITriggerListener getTriggerListener (@Nullable String name);

  /**
   * Register the given <code>{@link ISchedulerListener}</code> with the <code>Scheduler</code>.
   */
  void addSchedulerListener (@NonNull ISchedulerListener schedulerListener);

  /**
   * Remove the given <code>{@link ISchedulerListener}</code> from the <code>Scheduler</code>.
   *
   * @return true if the identified listener was found in the list, and removed.
   */
  boolean removeSchedulerListener (@Nullable ISchedulerListener schedulerListener);

  /**
   * Get a List containing all of the <code>{@link ISchedulerListener}</code>s registered with the
   * <code>Scheduler</code>, in the order in which they were registered.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISchedulerListener> getSchedulerListeners ();
}
