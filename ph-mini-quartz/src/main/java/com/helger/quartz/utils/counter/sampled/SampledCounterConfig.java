/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2023 Philip Helger (www.helger.com)
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

import com.helger.commons.ValueEnforcer;
import com.helger.quartz.utils.counter.CounterConfig;
import com.helger.quartz.utils.counter.ICounter;

/**
 * Config for a {@link ISampledCounter}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCounterConfig extends CounterConfig
{
  private final int m_nIntervalSecs;
  private final int m_nHistorySize;
  private final boolean m_bIsReset;

  /**
   * Make a new timed counter config (duh)
   *
   * @param intervalSecs
   *        the interval (in seconds) between sampling
   * @param historySize
   *        number of counter samples that will be retained in memory
   * @param isResetOnSample
   *        true if the counter should be reset to 0 upon each sample
   */
  public SampledCounterConfig (final int intervalSecs,
                               final int historySize,
                               final boolean isResetOnSample,
                               final long initialValue)
  {
    super (initialValue);
    ValueEnforcer.isGT0 (intervalSecs, "Interval");
    ValueEnforcer.isGT0 (historySize, "HistorySize");

    m_nIntervalSecs = intervalSecs;
    m_nHistorySize = historySize;
    m_bIsReset = isResetOnSample;
  }

  /**
   * Returns the history size
   *
   * @return The history size
   */
  public int getHistorySize ()
  {
    return m_nHistorySize;
  }

  /**
   * Returns the interval time (seconds)
   *
   * @return Interval of the sampling thread in seconds
   */
  public int getIntervalSecs ()
  {
    return m_nIntervalSecs;
  }

  /**
   * Returns true if counters created from this config will reset on each sample
   *
   * @return true if values are reset to the initial value after each sample
   */
  public boolean isResetOnSample ()
  {
    return m_bIsReset;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ICounter createCounter ()
  {
    return new SampledCounter (this);
  }
}
