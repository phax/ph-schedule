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
package com.helger.quartz.impl.matchers;

import static com.helger.quartz.JobKey.jobKey;
import static com.helger.quartz.TriggerKey.triggerKey;
import static com.helger.quartz.impl.matchers.GroupMatcher.anyJobGroup;
import static com.helger.quartz.impl.matchers.GroupMatcher.anyTriggerGroup;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.quartz.JobKey;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.GroupMatcher;

/**
 * Unit test for CronScheduleBuilder.
 *
 * @author jhouse
 */
public class GroupMatcherTest
{

  @Test
  public void testAnyGroupMatchers ()
  {

    final TriggerKey tKey = triggerKey ("booboo", "baz");
    final JobKey jKey = jobKey ("frumpwomp", "bazoo");

    final GroupMatcher <TriggerKey> tgm = anyTriggerGroup ();
    final GroupMatcher <JobKey> jgm = anyJobGroup ();

    assertTrue ("Expected match on trigger group", tgm.isMatch (tKey));
    assertTrue ("Expected match on job group", jgm.isMatch (jKey));

  }

}