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

/**
 * A simple counter
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public interface ICounter
{
  /**
   * Increment the counter by 1
   *
   * @return the value after incrementing
   */
  long increment ();

  /**
   * Decrement the counter by 1
   *
   * @return the value after decrementing
   */
  long decrement ();

  /**
   * Returns the value of the counter and sets it to the new value
   *
   * @param newValue
   * @return Returns the old value
   */
  long getAndSet (long newValue);

  /**
   * Gets current value of the counter
   *
   * @return current value of the counter
   */
  long getValue ();

  /**
   * Increment the counter by given amount
   *
   * @param amount
   * @return the value of the counter after incrementing
   */
  long increment (long amount);

  /**
   * Decrement the counter by given amount
   *
   * @param amount
   * @return the value of the counter after decrementing
   */
  default long decrement (final long amount)
  {
    return increment (-amount);
  }

  /**
   * Sets the value of the counter to the supplied value
   *
   * @param newValue
   */
  void setValue (long newValue);
}
