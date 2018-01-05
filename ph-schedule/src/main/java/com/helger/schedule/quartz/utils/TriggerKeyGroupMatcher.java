/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
package com.helger.schedule.quartz.utils;

import javax.annotation.Nonnull;

import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.GroupMatcher;

/**
 * Type-safe implementation of GroupMatcher for {@link TriggerKey}
 *
 * @author Philip Helger
 */
public class TriggerKeyGroupMatcher extends GroupMatcher <TriggerKey>
{
  public TriggerKeyGroupMatcher (@Nonnull final String sCompareTo, @Nonnull final StringOperatorName eCompareWith)
  {
    super (sCompareTo, eCompareWith);
  }

  @Nonnull
  public static TriggerKeyGroupMatcher createEquals (@Nonnull final String sCompareTo)
  {
    return new TriggerKeyGroupMatcher (sCompareTo, StringOperatorName.EQUALS);
  }
}
