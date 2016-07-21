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
 * Matches on group (ignores name) property of Keys.
 *
 * @author jhouse
 */
public class GroupMatcher <T extends Key <T>> extends StringMatcher <T>
{
  protected GroupMatcher (final String compareTo, final StringOperatorName compareWith)
  {
    super (compareTo, compareWith);
  }

  /**
   * Create a GroupMatcher that matches groups equaling the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupEquals (final String compareTo)
  {
    return new GroupMatcher<> (compareTo, StringOperatorName.EQUALS);
  }

  /**
   * Create a GroupMatcher that matches job groups equaling the given string.
   */
  public static GroupMatcher <JobKey> jobGroupEquals (final String compareTo)
  {
    return GroupMatcher.groupEquals (compareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups equaling the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupEquals (final String compareTo)
  {
    return GroupMatcher.groupEquals (compareTo);
  }

  /**
   * Create a GroupMatcher that matches groups starting with the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupStartsWith (final String compareTo)
  {
    return new GroupMatcher<> (compareTo, StringOperatorName.STARTS_WITH);
  }

  /**
   * Create a GroupMatcher that matches job groups starting with the given
   * string.
   */
  public static GroupMatcher <JobKey> jobGroupStartsWith (final String compareTo)
  {
    return GroupMatcher.groupStartsWith (compareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups starting with the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupStartsWith (final String compareTo)
  {
    return GroupMatcher.groupStartsWith (compareTo);
  }

  /**
   * Create a GroupMatcher that matches groups ending with the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupEndsWith (final String compareTo)
  {
    return new GroupMatcher<> (compareTo, StringOperatorName.ENDS_WITH);
  }

  /**
   * Create a GroupMatcher that matches job groups ending with the given string.
   */
  public static GroupMatcher <JobKey> jobGroupEndsWith (final String compareTo)
  {
    return GroupMatcher.groupEndsWith (compareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups ending with the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupEndsWith (final String compareTo)
  {
    return GroupMatcher.groupEndsWith (compareTo);
  }

  /**
   * Create a GroupMatcher that matches groups containing the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> groupContains (final String compareTo)
  {
    return new GroupMatcher<> (compareTo, StringOperatorName.CONTAINS);
  }

  /**
   * Create a GroupMatcher that matches job groups containing the given string.
   */
  public static GroupMatcher <JobKey> jobGroupContains (final String compareTo)
  {
    return GroupMatcher.groupContains (compareTo);
  }

  /**
   * Create a GroupMatcher that matches trigger groups containing the given
   * string.
   */
  public static GroupMatcher <TriggerKey> triggerGroupContains (final String compareTo)
  {
    return GroupMatcher.groupContains (compareTo);
  }

  /**
   * Create a GroupMatcher that matches groups starting with the given string.
   */
  public static <T extends Key <T>> GroupMatcher <T> anyGroup ()
  {
    return new GroupMatcher<> ("", StringOperatorName.ANYTHING);
  }

  /**
   * Create a GroupMatcher that matches job groups starting with the given
   * string.
   */
  public static GroupMatcher <JobKey> anyJobGroup ()
  {
    return GroupMatcher.anyGroup ();
  }

  /**
   * Create a GroupMatcher that matches trigger groups starting with the given
   * string.
   */
  public static GroupMatcher <TriggerKey> anyTriggerGroup ()
  {
    return GroupMatcher.anyGroup ();
  }

  @Override
  protected String getValue (final T key)
  {
    return key.getGroup ();
  }

}
