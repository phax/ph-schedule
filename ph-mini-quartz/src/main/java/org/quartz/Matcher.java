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

package org.quartz;

import java.io.Serializable;

import org.quartz.impl.matchers.AndMatcher;
import org.quartz.impl.matchers.NotMatcher;
import org.quartz.impl.matchers.OrMatcher;
import org.quartz.utils.Key;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;

/**
 * Matchers can be used in various {@link Scheduler} API methods to select the
 * entities that should be operated upon.
 *
 * @author jhouse
 * @since 2.0
 */
@MustImplementEqualsAndHashcode
public interface Matcher <T extends Key <T>> extends Serializable
{
  boolean isMatch (T key);

  boolean equals (Object obj);

  int hashCode ();

  /**
   * Create an AndMatcher that depends upon the result of both of the given
   * matchers.
   */
  static <U extends Key <U>> AndMatcher <U> and (final Matcher <U> leftOperand, final Matcher <U> rightOperand)
  {
    return new AndMatcher<> (leftOperand, rightOperand);
  }

  default AndMatcher <T> and (final Matcher <T> rightOperand)
  {
    return and (this, rightOperand);
  }

  /**
   * Create an OrMatcher that depends upon the result of at least one of the
   * given matchers.
   */
  public static <U extends Key <U>> OrMatcher <U> or (final Matcher <U> leftOperand, final Matcher <U> rightOperand)
  {
    return new OrMatcher<> (leftOperand, rightOperand);
  }

  default OrMatcher <T> or (final Matcher <T> rightOperand)
  {
    return or (this, rightOperand);
  }

  /**
   * Create a NotMatcher that reverses the result of the given matcher.
   */
  public static <U extends Key <U>> NotMatcher <U> not (final Matcher <U> operand)
  {
    return new NotMatcher<> (operand);
  }

  default NotMatcher <T> not ()
  {
    return not (this);
  }
}
