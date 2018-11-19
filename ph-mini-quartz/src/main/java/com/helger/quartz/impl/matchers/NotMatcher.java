/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2018 Philip Helger (www.helger.com)
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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.quartz.IMatcher;
import com.helger.quartz.utils.Key;

/**
 * Matches using an NOT operator on another Matcher.
 *
 * @author jhouse
 */
public class NotMatcher <T extends Key <T>> implements IMatcher <T>
{
  private final IMatcher <T> m_aOperand;

  public NotMatcher (@Nonnull final IMatcher <T> operand)
  {
    ValueEnforcer.notNull (operand, "Operand");
    this.m_aOperand = operand;
  }

  public boolean isMatch (final T key)
  {
    return !m_aOperand.isMatch (key);
  }

  @Nonnull
  public IMatcher <T> getOperand ()
  {
    return m_aOperand;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final NotMatcher <?> other = (NotMatcher <?>) o;
    return m_aOperand.equals (other.m_aOperand);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aOperand).getHashCode ();
  }
}
