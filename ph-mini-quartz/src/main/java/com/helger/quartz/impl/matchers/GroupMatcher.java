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
package com.helger.quartz.impl.matchers;

import javax.annotation.Nonnull;

import com.helger.quartz.JobKey;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.utils.Key;

/**
 * Matches on group (ignores name) property of Keys.
 *
 * @author jhouse
 */
public class GroupMatcher <T extends Key <T>> extends StringMatcher <T>
{
  protected GroupMatcher (@Nonnull final String sCompareTo, @Nonnull final EStringOperatorName eCompareWith)
  {
    super (sCompareTo, eCompareWith);
  }

  @Override
  protected String getValue (final T key)
  {
    return key.getGroup ();
  }

  /**
   * Create a GroupMatcher that matches groups equaling the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupEquals (final String sCompareTo)
  {
    return new GroupMatcher <> (sCompareTo, EStringOperatorName.EQUALS);
  }

  /**
   * Create a GroupMatcher that matches job groups equaling the given string.
   */
  public static GroupMatcher <JobKey> jobGroupEquals (final String sCompareTo)
  {
    return groupEquals (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups equaling the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupEquals (final String sCompareTo)
  {
    return groupEquals (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches groups starting with the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupStartsWith (final String sCompareTo)
  {
    return new GroupMatcher <> (sCompareTo, EStringOperatorName.STARTS_WITH);
  }

  /**
   * Create a GroupMatcher that matches job groups starting with the given
   * string.
   */
  public static GroupMatcher <JobKey> jobGroupStartsWith (final String sCompareTo)
  {
    return groupStartsWith (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups starting with the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupStartsWith (final String sCompareTo)
  {
    return groupStartsWith (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches groups ending with the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupEndsWith (final String sCompareTo)
  {
    return new GroupMatcher <> (sCompareTo, EStringOperatorName.ENDS_WITH);
  }

  /**
   * Create a GroupMatcher that matches job groups ending with the given string.
   */
  public static GroupMatcher <JobKey> jobGroupEndsWith (final String sCompareTo)
  {
    return groupEndsWith (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups ending with the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupEndsWith (final String sCompareTo)
  {
    return groupEndsWith (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches groups containing the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupContains (final String sCompareTo)
  {
    return new GroupMatcher <> (sCompareTo, EStringOperatorName.CONTAINS);
  }

  /**
   * Create a GroupMatcher that matches job groups containing the given string.
   */
  public static GroupMatcher <JobKey> jobGroupContains (final String sCompareTo)
  {
    return groupContains (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups containing the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupContains (final String sCompareTo)
  {
    return groupContains (sCompareTo);
  }

  /**
   * Create a GroupMatcher that matches groups starting with the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> anyGroup ()
  {
    return new GroupMatcher <> ("", EStringOperatorName.ANYTHING);
  }

  /**
   * Create a GroupMatcher that matches job groups starting with the given
   * string.
   */
  public static GroupMatcher <JobKey> anyJobGroup ()
  {
    return anyGroup ();
  }

  /**
   * Create a GroupMatcher that matches trigger groups starting with the given
   * string.
   */
  public static GroupMatcher <TriggerKey> anyTriggerGroup ()
  {
    return anyGroup ();
  }
}
