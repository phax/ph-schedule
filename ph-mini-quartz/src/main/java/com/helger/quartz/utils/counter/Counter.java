/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple counter implementation
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public class Counter implements ICounter, Serializable
{
  private final AtomicLong value;

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
   */
  public Counter (final long initialValue)
  {
    this.value = new AtomicLong (initialValue);
  }

  /**
   * {@inheritDoc}
   */
  public long increment ()
  {
    return value.incrementAndGet ();
  }

  /**
   * {@inheritDoc}
   */
  public long decrement ()
  {
    return value.decrementAndGet ();
  }

  /**
   * {@inheritDoc}
   */
  public long getAndSet (final long newValue)
  {
    return value.getAndSet (newValue);
  }

  /**
   * {@inheritDoc}
   */
  public long getValue ()
  {
    return value.get ();
  }

  /**
   * {@inheritDoc}
   */
  public long increment (final long amount)
  {
    return value.addAndGet (amount);
  }

  /**
   * {@inheritDoc}
   */
  public long decrement (final long amount)
  {
    return value.addAndGet (amount * -1);
  }

  /**
   * {@inheritDoc}
   */
  public void setValue (final long newValue)
  {
    value.set (newValue);
  }
}