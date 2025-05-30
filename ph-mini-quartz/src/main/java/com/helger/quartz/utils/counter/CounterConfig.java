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
package com.helger.quartz.utils.counter;

import javax.annotation.Nonnull;

/**
 * Config for a simple Counter
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public class CounterConfig
{
  private final long m_nInitialValue;

  /**
   * Creates a config with the initial value
   *
   * @param initialValue
   *        initial value
   */
  public CounterConfig (final long initialValue)
  {
    m_nInitialValue = initialValue;
  }

  /**
   * Gets the initial value
   *
   * @return the initial value of counters created by this config
   */
  public final long getInitialValue ()
  {
    return m_nInitialValue;
  }

  /**
   * Creates and returns a Counter based on the initial value
   *
   * @return The counter created by this config
   */
  @Nonnull
  public ICounter createCounter ()
  {
    return new Counter (m_nInitialValue);
  }
}
