/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package org.quartz.impl.matchers;

import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.utils.Key;

/**
 * Matches on name (ignores group) property of Keys.
 *
 * @author jhouse
 */
public class NameMatcher <T extends Key <?>> extends StringMatcher <T>
{

  private static final long serialVersionUID = -33104959459613480L;

  protected NameMatcher (final String compareTo, final StringOperatorName compareWith)
  {
    super (compareTo, compareWith);
  }

  /**
   * Create a NameMatcher that matches names equaling the given string.
   */
  public static <T extends Key <?>> NameMatcher <T> nameEquals (final String compareTo)
  {
    return new NameMatcher <> (compareTo, StringOperatorName.EQUALS);
  }

  /**
   * Create a NameMatcher that matches job names equaling the given string.
   */
  public static NameMatcher <JobKey> jobNameEquals (final String compareTo)
  {
    return NameMatcher.nameEquals (compareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names equaling the given string.
   */
  public static NameMatcher <TriggerKey> triggerNameEquals (final String compareTo)
  {
    return NameMatcher.nameEquals (compareTo);
  }

  /**
   * Create a NameMatcher that matches names starting with the given string.
   */
  public static <U extends Key <?>> NameMatcher <U> nameStartsWith (final String compareTo)
  {
    return new NameMatcher <> (compareTo, StringOperatorName.STARTS_WITH);
  }

  /**
   * Create a NameMatcher that matches job names starting with the given string.
   */
  public static NameMatcher <JobKey> jobNameStartsWith (final String compareTo)
  {
    return NameMatcher.nameStartsWith (compareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names starting with the given
   * string.
   */
  public static NameMatcher <TriggerKey> triggerNameStartsWith (final String compareTo)
  {
    return NameMatcher.nameStartsWith (compareTo);
  }

  /**
   * Create a NameMatcher that matches names ending with the given string.
   */
  public static <U extends Key <?>> NameMatcher <U> nameEndsWith (final String compareTo)
  {
    return new NameMatcher <> (compareTo, StringOperatorName.ENDS_WITH);
  }

  /**
   * Create a NameMatcher that matches job names ending with the given string.
   */
  public static NameMatcher <JobKey> jobNameEndsWith (final String compareTo)
  {
    return NameMatcher.nameEndsWith (compareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names ending with the given
   * string.
   */
  public static NameMatcher <TriggerKey> triggerNameEndsWith (final String compareTo)
  {
    return NameMatcher.nameEndsWith (compareTo);
  }

  /**
   * Create a NameMatcher that matches names containing the given string.
   */
  public static <U extends Key <?>> NameMatcher <U> nameContains (final String compareTo)
  {
    return new NameMatcher <> (compareTo, StringOperatorName.CONTAINS);
  }

  /**
   * Create a NameMatcher that matches job names containing the given string.
   */
  public static NameMatcher <JobKey> jobNameContains (final String compareTo)
  {
    return NameMatcher.nameContains (compareTo);
  }

  /**
   * Create a NameMatcher that matches trigger names containing the given
   * string.
   */
  public static NameMatcher <TriggerKey> triggerNameContains (final String compareTo)
  {
    return NameMatcher.nameContains (compareTo);
  }

  @Override
  protected String getValue (final T key)
  {
    return key.getName ();
  }

}
