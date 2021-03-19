/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
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
  String getSchedulerName ();

  String getSchedulerInstanceId ();

  SchedulerContext getSchedulerContext () throws SchedulerException;

  void start () throws SchedulerException;

  void startDelayed (int seconds) throws SchedulerException;

  void standby ();

  boolean isInStandbyMode ();

  void shutdown ();

  void shutdown (boolean waitForJobsToComplete);

  boolean isShutdown ();

  Date runningSince ();

  String getVersion ();

  int numJobsExecuted ();

  Class <?> getJobStoreClass ();

  boolean supportsPersistence ();

  boolean isClustered ();

  Class <?> getThreadPoolClass ();

  int getThreadPoolSize ();

  void clear () throws SchedulerException;

  ICommonsList <IJobExecutionContext> getCurrentlyExecutingJobs () throws SchedulerException;

  Date scheduleJob (IJobDetail jobDetail, ITrigger trigger) throws SchedulerException;

  Date scheduleJob (ITrigger trigger) throws SchedulerException;

  void addJob (IJobDetail jobDetail, boolean replace) throws SchedulerException;

  void addJob (IJobDetail jobDetail,
               boolean replace,
               boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException;

  boolean deleteJob (JobKey jobKey) throws SchedulerException;

  boolean unscheduleJob (TriggerKey triggerKey) throws SchedulerException;

  Date rescheduleJob (TriggerKey triggerKey, ITrigger newTrigger) throws SchedulerException;

  void triggerJob (JobKey jobKey, JobDataMap data) throws SchedulerException;

  void triggerJob (IOperableTrigger trig) throws SchedulerException;

  void pauseTrigger (TriggerKey triggerKey) throws SchedulerException;

  void pauseTriggers (GroupMatcher <TriggerKey> matcher) throws SchedulerException;

  void pauseJob (JobKey jobKey) throws SchedulerException;

  void pauseJobs (GroupMatcher <JobKey> matcher) throws SchedulerException;

  void resumeTrigger (TriggerKey triggerKey) throws SchedulerException;

  void resumeTriggers (GroupMatcher <TriggerKey> matcher) throws SchedulerException;

  ICommonsSet <String> getPausedTriggerGroups () throws SchedulerException;

  void resumeJob (JobKey jobKey) throws SchedulerException;

  void resumeJobs (GroupMatcher <JobKey> matcher) throws SchedulerException;

  void pauseAll () throws SchedulerException;

  void resumeAll () throws SchedulerException;

  ICommonsList <String> getJobGroupNames () throws SchedulerException;

  ICommonsSet <JobKey> getJobKeys (GroupMatcher <JobKey> matcher) throws SchedulerException;

  ICommonsList <? extends ITrigger> getTriggersOfJob (JobKey jobKey) throws SchedulerException;

  ICommonsList <String> getTriggerGroupNames () throws SchedulerException;

  ICommonsSet <TriggerKey> getTriggerKeys (GroupMatcher <TriggerKey> matcher) throws SchedulerException;

  IJobDetail getJobDetail (JobKey jobKey) throws SchedulerException;

  ITrigger getTrigger (TriggerKey triggerKey) throws SchedulerException;

  ETriggerState getTriggerState (TriggerKey triggerKey) throws SchedulerException;

  void addCalendar (String calName,
                    ICalendar calendar,
                    boolean replace,
                    boolean updateTriggers) throws SchedulerException;

  boolean deleteCalendar (String calName) throws SchedulerException;

  ICalendar getCalendar (String calName) throws SchedulerException;

  ICommonsList <String> getCalendarNames () throws SchedulerException;

  boolean interrupt (JobKey jobKey) throws UnableToInterruptJobException;

  boolean interrupt (String fireInstanceId) throws UnableToInterruptJobException;

  boolean checkExists (JobKey jobKey) throws SchedulerException;

  boolean checkExists (TriggerKey triggerKey) throws SchedulerException;

  boolean deleteJobs (List <JobKey> jobKeys) throws SchedulerException;

  void scheduleJobs (Map <IJobDetail, Set <? extends ITrigger>> triggersAndJobs,
                     boolean replace) throws SchedulerException;

  void scheduleJob (IJobDetail jobDetail,
                    Set <? extends ITrigger> triggersForJob,
                    boolean replace) throws SchedulerException;

  boolean unscheduleJobs (List <TriggerKey> triggerKeys) throws SchedulerException;
}
