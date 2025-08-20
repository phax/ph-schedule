/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2025 Philip Helger (www.helger.com)
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

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.quartz.IMatcher;
import com.helger.quartz.utils.Key;

import jakarta.annotation.Nonnull;

/**
 * Matches using an AND operator on two Matcher operands.
 *
 * @author jhouse
 */
public class AndMatcher <T extends Key <T>> implements IMatcher <T>
{
  private final IMatcher <T> m_aLeftOperand;
  private final IMatcher <T> m_aRightOperand;

  public AndMatcher (@Nonnull final IMatcher <T> aLeftOperand, @Nonnull final IMatcher <T> aRightOperand)
  {
    ValueEnforcer.notNull (aLeftOperand, "LeftOperand");
    ValueEnforcer.notNull (aRightOperand, "RightOperand");

    m_aLeftOperand = aLeftOperand;
    m_aRightOperand = aRightOperand;
  }

  @Nonnull
  public IMatcher <T> getLeftOperand ()
  {
    return m_aLeftOperand;
  }

  @Nonnull
  public IMatcher <T> getRightOperand ()
  {
    return m_aRightOperand;
  }

  public boolean isMatch (final T key)
  {
    return m_aLeftOperand.isMatch (key) && m_aRightOperand.isMatch (key);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final AndMatcher <?> other = (AndMatcher <?>) o;
    return m_aLeftOperand.equals (other.m_aLeftOperand) && m_aRightOperand.equals (other.m_aRightOperand);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aLeftOperand).append (m_aRightOperand).getHashCode ();
  }
}
