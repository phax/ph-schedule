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
package com.helger.quartz.utils.counter.sampled;

/**
 * Interface of a sampled rate counter -- a counter that keeps sampled values of
 * rates
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public interface ISampledRateCounter extends ISampledCounter
{

  /**
   * Increments the numerator and denominator by the passed values
   *
   * @param numerator
   * @param denominator
   */
  void increment (long numerator, long denominator);

  /**
   * Decrements the numerator and denominator by the passed values
   *
   * @param numerator
   * @param denominator
   */
  void decrement (long numerator, long denominator);

  /**
   * Sets the values of the numerator and denominator to the passed values
   *
   * @param numerator
   * @param denominator
   */
  void setValue (long numerator, long denominator);

  /**
   * Sets the value of the numerator to the passed value
   *
   * @param newValue
   */
  void setNumeratorValue (long newValue);

  /**
   * Sets the value of the denominator to the passed value
   *
   * @param newValue
   */
  void setDenominatorValue (long newValue);
}
