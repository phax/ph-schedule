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
 * Matches on the complete key being equal (both name and group).
 *
 * @author jhouse
 */
public class KeyMatcher <T extends Key <T>> implements IMatcher <T>
{
  private final T m_aCompareTo;

  protected KeyMatcher (@Nonnull final T aCompareTo)
  {
    ValueEnforcer.notNull (aCompareTo, "CompareTo");
    m_aCompareTo = aCompareTo;
  }

  @Nonnull
  public T getCompareToValue ()
  {
    return m_aCompareTo;
  }

  public boolean isMatch (final T aKey)
  {
    return m_aCompareTo.equals (aKey);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final KeyMatcher <?> other = (KeyMatcher <?>) o;
    return m_aCompareTo.equals (other.m_aCompareTo);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aCompareTo).getHashCode ();
  }

  /**
   * Create a KeyMatcher that matches Keys that equal the given key.
   */
  public static <U extends Key <U>> KeyMatcher <U> keyEquals (@Nonnull final U aCompareTo)
  {
    return new KeyMatcher <> (aCompareTo);
  }
}
