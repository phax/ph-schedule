/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2024 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.quartz.impl.JobDetail;

/**
 * Unit test for JobDetail.
 */
public class JobDetailTest
{

  @PersistJobDataAfterExecution
  public class SomePersistentJob implements IJob
  {
    @Override
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {}
  }

  public class SomeExtendedPersistentJob extends SomePersistentJob
  {}

  @DisallowConcurrentExecution
  public class SomeNonConcurrentJob implements IJob
  {
    @Override
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {}
  }

  public class SomeExtendedNonConcurrentJob extends SomeNonConcurrentJob
  {}

  @DisallowConcurrentExecution
  @PersistJobDataAfterExecution
  public class SomeNonConcurrentPersistentJob implements IJob
  {
    @Override
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {}
  }

  public class SomeExtendedNonConcurrentPersistentJob extends SomeNonConcurrentPersistentJob
  {}

  @PersistJobDataAfterExecution
  @DisallowConcurrentExecution
  public class SomeStatefulJob implements IJob
  {
    @Override
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {}
  }

  public class SomeExtendedStatefulJob extends SomeStatefulJob
  {}

  @Test
  public void testClone ()
  {
    final JobDetail jobDetail = new JobDetail ();
    jobDetail.setName ("hi");

    final IJobDetail clonedJobDetail = jobDetail.getClone ();
    assertEquals (clonedJobDetail, jobDetail);
  }

  @Test
  public void testAnnotationDetection ()
  {
    final JobDetail jobDetail = new JobDetail ();
    jobDetail.setName ("hi");

    jobDetail.setJobClass (SomePersistentJob.class);
    assertTrue ("Expecting SomePersistentJob to be persistent", jobDetail.isPersistJobDataAfterExecution ());
    assertFalse ("Expecting SomePersistentJob to not disallow concurrent execution",
                 jobDetail.isConcurrentExectionDisallowed ());

    jobDetail.setJobClass (SomeNonConcurrentJob.class);
    assertFalse ("Expecting SomeNonConcurrentJob to not be persistent", jobDetail.isPersistJobDataAfterExecution ());
    assertTrue ("Expecting SomeNonConcurrentJob to disallow concurrent execution",
                jobDetail.isConcurrentExectionDisallowed ());

    jobDetail.setJobClass (SomeNonConcurrentPersistentJob.class);
    assertTrue ("Expecting SomeNonConcurrentPersistentJob to be persistent",
                jobDetail.isPersistJobDataAfterExecution ());
    assertTrue ("Expecting SomeNonConcurrentPersistentJob to disallow concurrent execution",
                jobDetail.isConcurrentExectionDisallowed ());

    jobDetail.setJobClass (SomeStatefulJob.class);
    assertTrue ("Expecting SomeStatefulJob to be persistent", jobDetail.isPersistJobDataAfterExecution ());
    assertTrue ("Expecting SomeStatefulJob to disallow concurrent execution",
                jobDetail.isConcurrentExectionDisallowed ());

    jobDetail.setJobClass (SomeExtendedPersistentJob.class);
    assertTrue ("Expecting SomeExtendedPersistentJob to be persistent", jobDetail.isPersistJobDataAfterExecution ());
    assertFalse ("Expecting SomeExtendedPersistentJob to not disallow concurrent execution",
                 jobDetail.isConcurrentExectionDisallowed ());

    jobDetail.setJobClass (SomeExtendedNonConcurrentJob.class);
    assertFalse ("Expecting SomeExtendedNonConcurrentJob to not be persistent",
                 jobDetail.isPersistJobDataAfterExecution ());
    assertTrue ("Expecting SomeExtendedNonConcurrentJob to disallow concurrent execution",
                jobDetail.isConcurrentExectionDisallowed ());

    jobDetail.setJobClass (SomeExtendedNonConcurrentPersistentJob.class);
    assertTrue ("Expecting SomeExtendedNonConcurrentPersistentJob to be persistent",
                jobDetail.isPersistJobDataAfterExecution ());
    assertTrue ("Expecting SomeExtendedNonConcurrentPersistentJob to disallow concurrent execution",
                jobDetail.isConcurrentExectionDisallowed ());

    jobDetail.setJobClass (SomeExtendedStatefulJob.class);
    assertTrue ("Expecting SomeExtendedStatefulJob to be persistent", jobDetail.isPersistJobDataAfterExecution ());
    assertTrue ("Expecting SomeExtendedStatefulJob to disallow concurrent execution",
                jobDetail.isConcurrentExectionDisallowed ());
  }
}
