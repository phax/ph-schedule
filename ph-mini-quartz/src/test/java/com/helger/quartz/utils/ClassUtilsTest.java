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
package com.helger.quartz.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.PersistJobDataAfterExecution;

/**
 * @author Alex Snaps
 */
public class ClassUtilsTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ClassUtilsTest.class);

  @Test
  public void testIsAnnotationPresentOnSuperClass () throws Exception
  {
    assertTrue (ClassUtils.isAnnotationPresent (BaseJob.class, DisallowConcurrentExecution.class));
    assertFalse (ClassUtils.isAnnotationPresent (BaseJob.class, PersistJobDataAfterExecution.class));
    assertTrue (ClassUtils.isAnnotationPresent (ExtendedJob.class, DisallowConcurrentExecution.class));
    assertFalse (ClassUtils.isAnnotationPresent (ExtendedJob.class, PersistJobDataAfterExecution.class));
    assertTrue (ClassUtils.isAnnotationPresent (ReallyExtendedJob.class, DisallowConcurrentExecution.class));
    assertTrue (ClassUtils.isAnnotationPresent (ReallyExtendedJob.class, PersistJobDataAfterExecution.class));
  }

  @DisallowConcurrentExecution
  private static class BaseJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      s_aLogger.info (this.getClass ().getSimpleName ());
    }
  }

  private static class ExtendedJob extends BaseJob
  {}

  @PersistJobDataAfterExecution
  private static class ReallyExtendedJob extends ExtendedJob
  {

  }
}
