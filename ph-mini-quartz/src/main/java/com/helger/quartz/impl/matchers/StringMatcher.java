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
 * An abstract base class for some types of matchers.
 *
 * @author jhouse
 */
public abstract class StringMatcher <T extends Key <T>> implements IMatcher <T>
{
  public enum StringOperatorName
  {
    EQUALS
    {
      @Override
      public boolean evaluate (final String value, final String compareTo)
      {
        return value.equals (compareTo);
      }
    },

    STARTS_WITH
    {
      @Override
      public boolean evaluate (final String value, final String compareTo)
      {
        return value.startsWith (compareTo);
      }
    },

    ENDS_WITH
    {
      @Override
      public boolean evaluate (final String value, final String compareTo)
      {
        return value.endsWith (compareTo);
      }
    },

    CONTAINS
    {
      @Override
      public boolean evaluate (final String value, final String compareTo)
      {
        return value.contains (compareTo);
      }
    },

    ANYTHING
    {
      @Override
      public boolean evaluate (final String value, final String compareTo)
      {
        return true;
      }
    };

    public abstract boolean evaluate (String value, String compareTo);
  }

  private final String m_sCompareTo;
  private final StringOperatorName m_sCompareWith;

  protected StringMatcher (@Nonnull final String compareTo, @Nonnull final StringOperatorName compareWith)
  {
    ValueEnforcer.notNull (compareTo, "CompareTo");
    ValueEnforcer.notNull (compareWith, "CompareWith");

    m_sCompareTo = compareTo;
    m_sCompareWith = compareWith;
  }

  @Nonnull
  public String getCompareToValue ()
  {
    return m_sCompareTo;
  }

  @Nonnull
  public StringOperatorName getCompareWithOperator ()
  {
    return m_sCompareWith;
  }

  protected abstract String getValue (T key);

  public boolean isMatch (final T key)
  {
    return m_sCompareWith.evaluate (getValue (key), m_sCompareTo);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final StringMatcher <?> other = (StringMatcher <?>) o;
    return m_sCompareTo.equals (other.m_sCompareTo) && m_sCompareWith.equals (other.m_sCompareWith);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sCompareTo).append (m_sCompareWith).getHashCode ();
  }
}
