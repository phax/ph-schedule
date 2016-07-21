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
 * Matches using an OR operator on two Matcher operands.
 *
 * @author jhouse
 */
public class OrMatcher <T extends Key <T>> implements Matcher <T>
{
  private final Matcher <T> leftOperand;
  private final Matcher <T> rightOperand;

  public OrMatcher (final Matcher <T> leftOperand, final Matcher <T> rightOperand)
  {
    if (leftOperand == null || rightOperand == null)
      throw new IllegalArgumentException ("Two non-null operands required!");

    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
  }

  public boolean isMatch (final T key)
  {

    return leftOperand.isMatch (key) || rightOperand.isMatch (key);
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
    return new HashCodeGenerator (this).append (leftOperand).append (rightOperand).getHashCode ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final OrMatcher <?> other = (OrMatcher <?>) o;
    return leftOperand.equals (other.leftOperand) && rightOperand.equals (other.rightOperand);
  }
}
