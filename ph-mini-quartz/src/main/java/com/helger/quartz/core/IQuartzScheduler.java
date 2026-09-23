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
package com.helger.quartz.core;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.quartz.ICalendar;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.ETriggerState;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerContext;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.UnableToInterruptJobException;
import com.helger.quartz.impl.matchers.GroupMatcher;
import com.helger.quartz.spi.IOperableTrigger;

/**
 * @author James House
 */
public interface IQuartzScheduler
{
  @NonNull
  @Nonempty
  String getSchedulerName ();

  @NonNull
  @Nonempty
  String getSchedulerInstanceId ();

  @NonNull
  SchedulerContext getSchedulerContext () throws SchedulerException;

  void start () throws SchedulerException;

  void startDelayed (@Nonnegative int seconds) throws SchedulerException;

  void standby ();

  boolean isInStandbyMode ();

  void shutdown ();

  void shutdown (boolean waitForJobsToComplete);

  boolean isShutdown ();

  @Nullable
  Date runningSince ();

  @NonNull
  @Nonempty
  String getVersion ();

  @Nonnegative
  int numJobsExecuted ();

  @NonNull
  Class <?> getJobStoreClass ();

  boolean supportsPersistence ();

  boolean isClustered ();

  @NonNull
  Class <?> getThreadPoolClass ();

  @Nonnegative
  int getThreadPoolSize ();

  void clear () throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsList <IJobExecutionContext> getCurrentlyExecutingJobs () throws SchedulerException;

  @NonNull
  Date scheduleJob (@NonNull IJobDetail jobDetail, @NonNull ITrigger trigger) throws SchedulerException;

  @NonNull
  Date scheduleJob (@NonNull ITrigger trigger) throws SchedulerException;

  void addJob (@NonNull IJobDetail jobDetail, boolean replace) throws SchedulerException;

  void addJob (@NonNull IJobDetail jobDetail,
               boolean replace,
               boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException;

  boolean deleteJob (@NonNull JobKey jobKey) throws SchedulerException;

  boolean unscheduleJob (@NonNull TriggerKey triggerKey) throws SchedulerException;

  @Nullable
  Date rescheduleJob (@NonNull TriggerKey triggerKey, @NonNull ITrigger newTrigger) throws SchedulerException;

  void triggerJob (@NonNull JobKey jobKey, @Nullable JobDataMap data) throws SchedulerException;

  void triggerJob (@NonNull IOperableTrigger trig) throws SchedulerException;

  void pauseTrigger (@NonNull TriggerKey triggerKey) throws SchedulerException;

  void pauseTriggers (@Nullable GroupMatcher <TriggerKey> matcher) throws SchedulerException;

  void pauseJob (@NonNull JobKey jobKey) throws SchedulerException;

  void pauseJobs (@Nullable GroupMatcher <JobKey> matcher) throws SchedulerException;

  void resumeTrigger (@NonNull TriggerKey triggerKey) throws SchedulerException;

  void resumeTriggers (@Nullable GroupMatcher <TriggerKey> matcher) throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsSet <String> getPausedTriggerGroups () throws SchedulerException;

  void resumeJob (@NonNull JobKey jobKey) throws SchedulerException;

  void resumeJobs (@Nullable GroupMatcher <JobKey> matcher) throws SchedulerException;

  void pauseAll () throws SchedulerException;

  void resumeAll () throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsList <String> getJobGroupNames () throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsSet <JobKey> getJobKeys (@Nullable GroupMatcher <JobKey> matcher) throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsList <? extends ITrigger> getTriggersOfJob (@NonNull JobKey jobKey) throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsList <String> getTriggerGroupNames () throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsSet <TriggerKey> getTriggerKeys (@Nullable GroupMatcher <TriggerKey> matcher) throws SchedulerException;

  @Nullable
  IJobDetail getJobDetail (@NonNull JobKey jobKey) throws SchedulerException;

  @Nullable
  ITrigger getTrigger (@NonNull TriggerKey triggerKey) throws SchedulerException;

  @NonNull
  ETriggerState getTriggerState (@NonNull TriggerKey triggerKey) throws SchedulerException;

  void addCalendar (@NonNull String calName,
                    @NonNull ICalendar calendar,
                    boolean replace,
                    boolean updateTriggers) throws SchedulerException;

  boolean deleteCalendar (@NonNull String calName) throws SchedulerException;

  @Nullable
  ICalendar getCalendar (@NonNull String calName) throws SchedulerException;

  @NonNull
  @ReturnsMutableCopy
  ICommonsList <String> getCalendarNames () throws SchedulerException;

  boolean interrupt (@NonNull JobKey jobKey) throws UnableToInterruptJobException;

  boolean interrupt (@NonNull String fireInstanceId) throws UnableToInterruptJobException;

  boolean checkExists (@NonNull JobKey jobKey) throws SchedulerException;

  boolean checkExists (@NonNull TriggerKey triggerKey) throws SchedulerException;

  boolean deleteJobs (@NonNull List <JobKey> jobKeys) throws SchedulerException;

  void scheduleJobs (@NonNull Map <IJobDetail, Set <? extends ITrigger>> triggersAndJobs,
                     boolean replace) throws SchedulerException;

  void scheduleJob (@NonNull IJobDetail jobDetail,
                    @NonNull Set <? extends ITrigger> triggersForJob,
                    boolean replace) throws SchedulerException;

  boolean unscheduleJobs (@NonNull List <TriggerKey> triggerKeys) throws SchedulerException;
}
