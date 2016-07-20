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

/**
 * Matches using an AND operator on two Matcher operands.
 *
 * @author jhouse
 */
public class AndMatcher <T extends Key <?>> implements Matcher <T>
{
  protected Matcher <T> leftOperand;
  protected Matcher <T> rightOperand;

  protected AndMatcher (final Matcher <T> leftOperand, final Matcher <T> rightOperand)
  {
    if (leftOperand == null || rightOperand == null)
      throw new IllegalArgumentException ("Two non-null operands required!");

    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
  }

  /**
   * Create an AndMatcher that depends upon the result of both of the given
   * matchers.
   */
  public static <U extends Key <?>> AndMatcher <U> and (final Matcher <U> leftOperand, final Matcher <U> rightOperand)
  {
    return new AndMatcher<> (leftOperand, rightOperand);
  }

  public boolean isMatch (final T key)
  {

    return leftOperand.isMatch (key) && rightOperand.isMatch (key);
  }

  public Matcher <T> getLeftOperand ()
  {
    return leftOperand;
  }

  public Matcher <T> getRightOperand ()
  {
    return rightOperand;
  }

  @Override
  public int hashCode ()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((leftOperand == null) ? 0 : leftOperand.hashCode ());
    result = prime * result + ((rightOperand == null) ? 0 : rightOperand.hashCode ());
    return result;
  }

  @Override
  public boolean equals (final Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass () != obj.getClass ())
      return false;
    final AndMatcher <?> other = (AndMatcher <?>) obj;
    if (leftOperand == null)
    {
      if (other.leftOperand != null)
        return false;
    }
    else
      if (!leftOperand.equals (other.leftOperand))
        return false;
    if (rightOperand == null)
    {
      if (other.rightOperand != null)
        return false;
    }
    else
      if (!rightOperand.equals (other.rightOperand))
        return false;
    return true;
  }

}
