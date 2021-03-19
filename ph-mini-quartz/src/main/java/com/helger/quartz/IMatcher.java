/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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
package com.helger.quartz;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.quartz.impl.matchers.AndMatcher;
import com.helger.quartz.impl.matchers.NotMatcher;
import com.helger.quartz.impl.matchers.OrMatcher;
import com.helger.quartz.utils.Key;

/**
 * Matchers can be used in various {@link IScheduler} API methods to select the
 * entities that should be operated upon.
 *
 * @author jhouse
 * @since 2.0
 */
@MustImplementEqualsAndHashcode
public interface IMatcher <T extends Key <T>>
{
  boolean isMatch (T key);

  boolean equals (Object obj);

  int hashCode ();

  /**
   * Create an AndMatcher that depends upon the result of both of the given
   * matchers.
   */
  @Nonnull
  static <U extends Key <U>> AndMatcher <U> and (@Nonnull final IMatcher <U> leftOperand,
                                                 @Nonnull final IMatcher <U> rightOperand)
  {
    return new AndMatcher <> (leftOperand, rightOperand);
  }

  @Nonnull
  default AndMatcher <T> and (@Nonnull final IMatcher <T> rightOperand)
  {
    return and (this, rightOperand);
  }

  /**
   * Create an OrMatcher that depends upon the result of at least one of the
   * given matchers.
   */
  @Nonnull
  static <U extends Key <U>> OrMatcher <U> or (@Nonnull final IMatcher <U> leftOperand,
                                               @Nonnull final IMatcher <U> rightOperand)
  {
    return new OrMatcher <> (leftOperand, rightOperand);
  }

  @Nonnull
  default OrMatcher <T> or (@Nonnull final IMatcher <T> rightOperand)
  {
    return or (this, rightOperand);
  }

  /**
   * Create a NotMatcher that reverses the result of the given matcher.
   */
  @Nonnull
  static <U extends Key <U>> NotMatcher <U> not (@Nonnull final IMatcher <U> operand)
  {
    return new NotMatcher <> (operand);
  }

  @Nonnull
  default NotMatcher <T> not ()
  {
    return not (this);
  }
}
