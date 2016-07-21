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

import org.quartz.Matcher;
import org.quartz.utils.Key;

import com.helger.commons.hashcode.HashCodeGenerator;

/**
 * Matches using an NOT operator on another Matcher.
 *
 * @author jhouse
 */
public class NotMatcher <T extends Key <T>> implements Matcher <T>
{
  private final Matcher <T> operand;

  public NotMatcher (final Matcher <T> operand)
  {
    if (operand == null)
      throw new IllegalArgumentException ("Non-null operand required!");

    this.operand = operand;
  }

  public boolean isMatch (final T key)
  {
    return !operand.isMatch (key);
  }

  public Matcher <T> getOperand ()
  {
    return operand;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (operand).getHashCode ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final NotMatcher <?> other = (NotMatcher <?>) o;
    return operand.equals (other.operand);
  }
}
