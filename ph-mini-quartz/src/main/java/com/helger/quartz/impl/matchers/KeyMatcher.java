/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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
 * Matches on the complete key being equal (both name and group).
 *
 * @author jhouse
 */
public class KeyMatcher <T extends Key <T>> implements IMatcher <T>
{
  private final T m_aCompareTo;

  protected KeyMatcher (@Nonnull final T compareTo)
  {
    ValueEnforcer.notNull (compareTo, "CompareTo");
    m_aCompareTo = compareTo;
  }

  /**
   * Create a KeyMatcher that matches Keys that equal the given key.
   */
  public static <U extends Key <U>> KeyMatcher <U> keyEquals (final U compareTo)
  {
    return new KeyMatcher <> (compareTo);
  }

  public boolean isMatch (final T key)
  {
    return m_aCompareTo.equals (key);
  }

  @Nonnull
  public T getCompareToValue ()
  {
    return m_aCompareTo;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aCompareTo).getHashCode ();
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
}
