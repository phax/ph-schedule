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
 * A counter value at a particular time instance
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public class TimeStampedCounterValue
{
  private final long m_nCounterValue;
  private final long m_nTimestamp;

  /**
   * Constructor accepting the value of both timestamp and the counter value.
   *
   * @param nTmestamp
   *        time stamp
   * @param nValue
   *        value
   */
  public TimeStampedCounterValue (final long nTmestamp, final long nValue)
  {
    m_nTimestamp = nTmestamp;
    m_nCounterValue = nValue;
  }

  /**
   * Get the counter value
   *
   * @return The counter value
   */
  public long getCounterValue ()
  {
    return m_nCounterValue;
  }

  /**
   * Get value of the timestamp
   *
   * @return the timestamp associated with the current value
   */
  public long getTimestamp ()
  {
    return m_nTimestamp;
  }

  @Override
  public String toString ()
  {
    return "value: " + m_nCounterValue + ", timestamp: " + m_nTimestamp;
  }
}
