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

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.quartz.IMatcher;
import com.helger.quartz.utils.Key;

/**
 * An abstract base class for some types of matchers.
 *
 * @author jhouse
 */
public abstract class StringMatcher <T extends Key <T>> implements IMatcher <T>
{
  public enum EStringOperatorName
  {
    EQUALS
    {
      @Override
      public boolean evaluate (@Nonnull final String value, @Nonnull final String sCompareTo)
      {
        return value.equals (sCompareTo);
      }
    },

    STARTS_WITH
    {
      @Override
      public boolean evaluate (@Nonnull final String value, @Nonnull final String sCompareTo)
      {
        return value.startsWith (sCompareTo);
      }
    },

    ENDS_WITH
    {
      @Override
      public boolean evaluate (@Nonnull final String value, @Nonnull final String sCompareTo)
      {
        return value.endsWith (sCompareTo);
      }
    },

    CONTAINS
    {
      @Override
      public boolean evaluate (@Nonnull final String value, @Nonnull final String sCompareTo)
      {
        return value.contains (sCompareTo);
      }
    },

    ANYTHING
    {
      @Override
      public boolean evaluate (@Nonnull final String value, @Nonnull final String sCompareTo)
      {
        return true;
      }
    };

    public abstract boolean evaluate (@Nonnull String value, @Nonnull String sCompareTo);
  }

  private final String m_sCompareTo;
  private final EStringOperatorName m_eCompareWith;

  protected StringMatcher (@Nonnull final String sCompareTo, @Nonnull final EStringOperatorName eCompareWith)
  {
    ValueEnforcer.notNull (sCompareTo, "CompareTo");
    ValueEnforcer.notNull (eCompareWith, "CompareWith");

    m_sCompareTo = sCompareTo;
    m_eCompareWith = eCompareWith;
  }

  @Nonnull
  public final String getCompareToValue ()
  {
    return m_sCompareTo;
  }

  @Nonnull
  public final EStringOperatorName getCompareWithOperator ()
  {
    return m_eCompareWith;
  }

  @Nonnull
  protected abstract String getValue (T key);

  public final boolean isMatch (final T key)
  {
    return m_eCompareWith.evaluate (getValue (key), m_sCompareTo);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final StringMatcher <?> other = (StringMatcher <?>) o;
    return m_sCompareTo.equals (other.m_sCompareTo) && m_eCompareWith.equals (other.m_eCompareWith);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sCompareTo).append (m_eCompareWith).getHashCode ();
  }
}
