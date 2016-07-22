/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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
package com.helger.quartz.impl;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.helger.quartz.impl.JobDetail;

public class JobDetailTest
{
  @Test
  public void testHashCode ()
  {
    final JobDetail job = new JobDetail ();
    Assert.assertThat (job.hashCode (), Matchers.is (0));

    job.setName ("test");
    Assert.assertThat (job.hashCode (), Matchers.not (Matchers.is (0)));

    job.setGroup ("test");
    Assert.assertThat (job.hashCode (), Matchers.not (Matchers.is (0)));
  }
}