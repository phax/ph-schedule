/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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

import com.helger.quartz.utils.counter.ICounter;

/**
 * An implementation of {@link SampledCounterConfig}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public class SampledRateCounterConfig extends SampledCounterConfig
{
  private final long m_nInitialNumeratorValue;
  private final long m_nInitialDenominatorValue;

  /**
   * Constructor accepting the interval time in seconds, history-size and
   * whether counters should reset on each sample or not. Initial values of both
   * numerator and denominator are zeroes
   *
   * @param intervalSecs
   *        seconds
   * @param historySize
   *        size
   * @param isResetOnSample
   *        reset on sample?
   */
  public SampledRateCounterConfig (final int intervalSecs, final int historySize, final boolean isResetOnSample)
  {
    this (intervalSecs, historySize, isResetOnSample, 0, 0);
  }

  /**
   * Constructor accepting the interval time in seconds, history-size and
   * whether counters should reset on each sample or not. Also the initial
   * values for the numerator and the denominator
   *
   * @param intervalSecs
   *        seconds
   * @param historySize
   *        size
   * @param isResetOnSample
   *        reset on sample
   * @param initialNumeratorValue
   *        initial value
   * @param initialDenominatorValue
   *        initial value
   */
  public SampledRateCounterConfig (final int intervalSecs,
                                   final int historySize,
                                   final boolean isResetOnSample,
                                   final long initialNumeratorValue,
                                   final long initialDenominatorValue)
  {
    super (intervalSecs, historySize, isResetOnSample, 0);
    m_nInitialNumeratorValue = initialNumeratorValue;
    m_nInitialDenominatorValue = initialDenominatorValue;
  }

  @Override
  public ICounter createCounter ()
  {
    final SampledRateCounter sampledRateCounter = new SampledRateCounter (this);
    sampledRateCounter.setValue (m_nInitialNumeratorValue, m_nInitialDenominatorValue);
    return sampledRateCounter;
  }
}
