/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.helger.quartz.utils.counter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

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

  private final Timer timer;
  private boolean shutdown;
  private final List <ICounter> counters = new ArrayList <> ();

  /**
   * Constructor that accepts a timer that will be used for scheduling sampled
   * counter if any is created
   */
  public CounterManager (final Timer timer)
  {
    if (timer == null)
    {
      throw new IllegalArgumentException ("Timer cannot be null");
    }
    this.timer = timer;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void shutdown (final boolean killTimer)
  {
    if (shutdown)
    {
      return;
    }
    try
    {
      // shutdown the counters of this counterManager
      for (final ICounter counter : counters)
      {
        if (counter instanceof ISampledCounter)
        {
          ((ISampledCounter) counter).shutdown ();
        }
      }
      if (killTimer)
        timer.cancel ();
    }
    finally
    {
      shutdown = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  public synchronized ICounter createCounter (final CounterConfig config)
  {
    if (shutdown)
    {
      throw new IllegalStateException ("counter manager is shutdown");
    }
    if (config == null)
    {
      throw new NullPointerException ("config cannot be null");
    }
    final ICounter counter = config.createCounter ();
    if (counter instanceof SampledCounter)
    {
      final SampledCounter sampledCounter = (SampledCounter) counter;
      timer.schedule (sampledCounter.getTimerTask (),
                      sampledCounter.getIntervalMillis (),
                      sampledCounter.getIntervalMillis ());
    }
    counters.add (counter);
    return counter;
  }

  /**
   * {@inheritDoc}
   */
  public void shutdownCounter (final ICounter counter)
  {
    if (counter instanceof ISampledCounter)
    {
      final ISampledCounter sc = (ISampledCounter) counter;
      sc.shutdown ();
    }
  }

}
