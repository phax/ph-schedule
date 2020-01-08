/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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

import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.quartz.IMatcher;
import com.helger.quartz.JobKey;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.utils.Key;

/**
 * Matches on the complete key being equal (both name and group).
 *
 * @author jhouse
 */
public class EverythingMatcher <T extends Key <T>> implements IMatcher <T>
{
  protected EverythingMatcher ()
  {}

  /**
   * Create an EverythingMatcher that matches all jobs.
   */
  public static EverythingMatcher <JobKey> allJobs ()
  {
    return new EverythingMatcher <> ();
  }

  /**
   * Create an EverythingMatcher that matches all triggers.
   */
  public static EverythingMatcher <TriggerKey> allTriggers ()
  {
    return new EverythingMatcher <> ();
  }

  public boolean isMatch (final T key)
  {
    return true;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    return true;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).getHashCode ();
  }
}
