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
package com.helger.quartz.impl.matchers;

import org.jspecify.annotations.NonNull;

import com.helger.quartz.JobKey;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.utils.Key;

/**
 * Matches on name (ignores group) property of Keys.
 *
 * @author jhouse
 */
public class NameMatcher <T extends Key <T>> extends StringMatcher <T>
{
  protected NameMatcher (@NonNull final String sCompareTo, @NonNull final EStringOperatorName eCompareWith)
  {
    super (sCompareTo, eCompareWith);
  }

  @Override
  @NonNull
  protected String getValue (@NonNull final T aKey)
  {
    return aKey.getName ();
  }

  /**
   * Create a NameMatcher that matches names equaling the given string.
   */
  @NonNull
  public static <T extends Key <T>> NameMatcher <T> nameEquals (@NonNull final String sCompareTo)
  {
    return new NameMatcher <> (sCompareTo, EStringOperatorName.EQUALS);
  }

  /**
   * Create a NameMatcher that matches job names equaling the given string.
   */
  @NonNull
  public static NameMatcher <JobKey> jobNameEquals (@NonNull final String sCompareTo)
  {
    return nameEquals (sCompareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names equaling the given string.
   */
  @NonNull
  public static NameMatcher <TriggerKey> triggerNameEquals (@NonNull final String sCompareTo)
  {
    return nameEquals (sCompareTo);
  }

  /**
   * Create a NameMatcher that matches names starting with the given string.
   */
  @NonNull
  public static <U extends Key <U>> NameMatcher <U> nameStartsWith (@NonNull final String sCompareTo)
  {
    return new NameMatcher <> (sCompareTo, EStringOperatorName.STARTS_WITH);
  }

  /**
   * Create a NameMatcher that matches job names starting with the given string.
   */
  @NonNull
  public static NameMatcher <JobKey> jobNameStartsWith (@NonNull final String sCompareTo)
  {
    return nameStartsWith (sCompareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names starting with the given string.
   */
  @NonNull
  public static NameMatcher <TriggerKey> triggerNameStartsWith (@NonNull final String sCompareTo)
  {
    return nameStartsWith (sCompareTo);
  }

  /**
   * Create a NameMatcher that matches names ending with the given string.
   */
  @NonNull
  public static <U extends Key <U>> NameMatcher <U> nameEndsWith (@NonNull final String sCompareTo)
  {
    return new NameMatcher <> (sCompareTo, EStringOperatorName.ENDS_WITH);
  }

  /**
   * Create a NameMatcher that matches job names ending with the given string.
   */
  @NonNull
  public static NameMatcher <JobKey> jobNameEndsWith (@NonNull final String sCompareTo)
  {
    return nameEndsWith (sCompareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names ending with the given string.
   */
  @NonNull
  public static NameMatcher <TriggerKey> triggerNameEndsWith (@NonNull final String sCompareTo)
  {
    return nameEndsWith (sCompareTo);
  }

  /**
   * Create a NameMatcher that matches names containing the given string.
   */
  @NonNull
  public static <U extends Key <U>> NameMatcher <U> nameContains (@NonNull final String sCompareTo)
  {
    return new NameMatcher <> (sCompareTo, EStringOperatorName.CONTAINS);
  }

  /**
   * Create a NameMatcher that matches job names containing the given string.
   */
  @NonNull
  public static NameMatcher <JobKey> jobNameContains (@NonNull final String sCompareTo)
  {
    return nameContains (sCompareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names containing the given string.
   */
  @NonNull
  public static NameMatcher <TriggerKey> triggerNameContains (@NonNull final String sCompareTo)
  {
    return nameContains (sCompareTo);
  }
}
