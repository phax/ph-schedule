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

import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple counter implementation
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public class Counter implements ICounter
{
  private final AtomicLong m_aValue;

  /**
   * Default Constructor
   */
  public Counter ()
  {
    this (0L);
  }

  /**
   * Constructor with initial value
   *
   * @param initialValue
   *        initial value
   */
  public Counter (final long initialValue)
  {
    m_aValue = new AtomicLong (initialValue);
  }

  public long increment ()
  {
    return m_aValue.incrementAndGet ();
  }

  public long decrement ()
  {
    return m_aValue.decrementAndGet ();
  }

  public long getAndSet (final long newValue)
  {
    return m_aValue.getAndSet (newValue);
  }

  public long getValue ()
  {
    return m_aValue.get ();
  }

  public long increment (final long amount)
  {
    return m_aValue.addAndGet (amount);
  }

  public void setValue (final long newValue)
  {
    m_aValue.set (newValue);
  }
}
