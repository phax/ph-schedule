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

import java.util.Timer;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.utils.counter.sampled.ISampledCounter;
import com.helger.quartz.utils.counter.sampled.SampledCounter;

/**
 * An implementation of a {@link ICounterManager}.
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public class CounterManager implements ICounterManager
{
  private final Timer m_aTimer;
  private boolean m_bShutdown;
  private final ICommonsList <ICounter> m_aCounters = new CommonsArrayList <> ();

  /**
   * Constructor that accepts a timer that will be used for scheduling sampled
   * counter if any is created
   */
  public CounterManager (@Nonnull final Timer timer)
  {
    ValueEnforcer.notNull (timer, "Timer");
    m_aTimer = timer;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void shutdown (final boolean killTimer)
  {
    if (!m_bShutdown)
    {
      try
      {
        // shutdown the counters of this counterManager
        for (final ICounter counter : m_aCounters)
          if (counter instanceof ISampledCounter)
            ((ISampledCounter) counter).shutdown ();

        if (killTimer)
          m_aTimer.cancel ();
      }
      finally
      {
        m_bShutdown = true;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public synchronized ICounter createCounter (final CounterConfig config)
  {
    ValueEnforcer.notNull (config, "Config");
    ValueEnforcer.isFalse (m_bShutdown, "counter manager is shutdown");

    final ICounter aCounter = config.createCounter ();
    if (aCounter instanceof SampledCounter)
    {
      final SampledCounter sampledCounter = (SampledCounter) aCounter;
      m_aTimer.schedule (sampledCounter.getTimerTask (),
                         sampledCounter.getIntervalMillis (),
                         sampledCounter.getIntervalMillis ());
    }
    m_aCounters.add (aCounter);
    return aCounter;
  }

  /**
   * {@inheritDoc}
   */
  public void shutdownCounter (final ICounter counter)
  {
    if (counter instanceof ISampledCounter)
      ((ISampledCounter) counter).shutdown ();
  }
}
