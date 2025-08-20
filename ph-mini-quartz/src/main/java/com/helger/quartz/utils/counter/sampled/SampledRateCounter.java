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

import jakarta.annotation.Nonnull;

/**
 * An implementation of {@link ISampledRateCounter}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public class SampledRateCounter extends SampledCounter implements ISampledRateCounter
{
  private static final String OPERATION_NOT_SUPPORTED_MSG = "This operation is not supported. Use SampledCounter Or Counter instead";

  private long m_nNumeratorValue;
  private long m_nDenominatorValue;

  /**
   * Constructor accepting the config
   *
   * @param config
   *        config
   */
  public SampledRateCounter (@Nonnull final SampledRateCounterConfig config)
  {
    super (config);
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void setValue (final long numerator, final long denominator)
  {
    m_nNumeratorValue = numerator;
    m_nDenominatorValue = denominator;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void increment (final long numerator, final long denominator)
  {
    m_nNumeratorValue += numerator;
    m_nDenominatorValue += denominator;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void decrement (final long numerator, final long denominator)
  {
    m_nNumeratorValue -= numerator;
    m_nDenominatorValue -= denominator;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void setDenominatorValue (final long newValue)
  {
    m_nDenominatorValue = newValue;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void setNumeratorValue (final long newValue)
  {
    m_nNumeratorValue = newValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized long getValue ()
  {
    return m_nDenominatorValue == 0 ? 0 : (m_nNumeratorValue / m_nDenominatorValue);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized long getAndReset ()
  {
    final long prevVal = getValue ();
    setValue (0, 0);
    return prevVal;
  }

  // ====== unsupported operations. These operations need multiple params for
  // this class
  /**
   * throws {@link UnsupportedOperationException}
   */
  @Override
  public long getAndSet (final long newValue)
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  @Override
  public synchronized void setValue (final long newValue)
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  @Override
  public long decrement ()
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  @Override
  public long decrement (final long amount)
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  public long getMaxValue ()
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  public long getMinValue ()
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  @Override
  public long increment ()
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

  /**
   * throws {@link UnsupportedOperationException}
   */
  @Override
  public long increment (final long amount)
  {
    throw new UnsupportedOperationException (OPERATION_NOT_SUPPORTED_MSG);
  }

}
