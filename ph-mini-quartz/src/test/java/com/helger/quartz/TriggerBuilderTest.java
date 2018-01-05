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
package com.helger.quartz;

import static com.helger.quartz.DateBuilder.evenSecondDateAfterNow;
import static com.helger.quartz.DateBuilder.futureDate;
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import com.helger.quartz.utils.Key;

/**
 * Test TriggerBuilder functionality
 */
public class TriggerBuilderTest
{
  public static class TestJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {}
  }

  @DisallowConcurrentExecution
  @PersistJobDataAfterExecution
  public static class TestAnnotatedJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {}
  }

  @Test
  public void testTriggerBuilder () throws Exception
  {

    ITrigger trigger = newTrigger ().build ();

    assertTrue ("Expected non-null trigger name ", trigger.getKey ().getName () != null);
    assertTrue ("Unexpected trigger group: " +
                trigger.getKey ().getGroup (),
                trigger.getKey ().getGroup ().equals (Key.DEFAULT_GROUP));
    assertTrue ("Unexpected job key: " + trigger.getJobKey (), trigger.getJobKey () == null);
    assertTrue ("Unexpected job description: " + trigger.getDescription (), trigger.getDescription () == null);
    assertTrue ("Unexpected trigger priortiy: " +
                trigger.getPriority (),
                trigger.getPriority () == ITrigger.DEFAULT_PRIORITY);
    assertTrue ("Unexpected start-time: " + trigger.getStartTime (), trigger.getStartTime () != null);
    assertTrue ("Unexpected end-time: " + trigger.getEndTime (), trigger.getEndTime () == null);

    final Date stime = evenSecondDateAfterNow ();

    trigger = newTrigger ().withIdentity ("t1")
                           .withDescription ("my description")
                           .withPriority (2)
                           .endAt (futureDate (10, EIntervalUnit.WEEK))
                           .startAt (stime)
                           .build ();

    assertTrue ("Unexpected trigger name " + trigger.getKey ().getName (), trigger.getKey ().getName ().equals ("t1"));
    assertTrue ("Unexpected trigger group: " +
                trigger.getKey ().getGroup (),
                trigger.getKey ().getGroup ().equals (Key.DEFAULT_GROUP));
    assertTrue ("Unexpected job key: " + trigger.getJobKey (), trigger.getJobKey () == null);
    assertTrue ("Unexpected job description: " +
                trigger.getDescription (),
                trigger.getDescription ().equals ("my description"));
    assertTrue ("Unexpected trigger priortiy: " + trigger, trigger.getPriority () == 2);
    assertTrue ("Unexpected start-time: " + trigger.getStartTime (), trigger.getStartTime ().equals (stime));
    assertTrue ("Unexpected end-time: " + trigger.getEndTime (), trigger.getEndTime () != null);

  }

  /** QTZ-157 */
  @Test
  public void testTriggerBuilderWithEndTimePriorCurrrentTime () throws Exception
  {
    TriggerBuilder.newTrigger ()
                  .withIdentity ("some trigger name", "some trigger group")
                  .forJob ("some job name", "some job group")
                  .startAt (new Date (System.currentTimeMillis () - 200000000))
                  .endAt (new Date (System.currentTimeMillis () - 100000000))
                  .withSchedule (CronScheduleBuilder.cronSchedule ("0 0 0 * * ?"))
                  .build ();
  }

}
