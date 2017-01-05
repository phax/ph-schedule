/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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

  private final String compareTo;
  private final StringOperatorName compareWith;

  protected StringMatcher (final String compareTo, final StringOperatorName compareWith)
  {
    if (compareTo == null)
      throw new IllegalArgumentException ("CompareTo value cannot be null!");
    if (compareWith == null)
      throw new IllegalArgumentException ("CompareWith operator cannot be null!");

    this.compareTo = compareTo;
    this.compareWith = compareWith;
  }

  public String getCompareToValue ()
  {
    return compareTo;
  }

  public StringOperatorName getCompareWithOperator ()
  {
    return compareWith;
  }

  protected abstract String getValue (T key);

  public boolean isMatch (final T key)
  {
    return compareWith.evaluate (getValue (key), compareTo);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (compareTo).append (compareWith).getHashCode ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final StringMatcher <?> other = (StringMatcher <?>) o;
    return compareTo.equals (other.compareTo) && compareWith.equals (other.compareWith);
  }
}
