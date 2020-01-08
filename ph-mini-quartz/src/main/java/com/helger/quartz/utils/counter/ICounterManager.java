/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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
 * A Counter Manager that accepts a config to create counters. Creates counter's
 * based on {@link CounterConfig}. This manages the lifycycle of a counter
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.8
 */
public interface ICounterManager
{
  /**
   * Creates a Counter based on tha passed config
   *
   * @param config
   * @return The counter created and managed by this CounterManager
   */
  ICounter createCounter (CounterConfig config);

  /**
   * Shuts down this counter manager
   */
  void shutdown (boolean killTimer);

  /**
   * Shuts down the counter
   *
   * @param counter
   */
  void shutdownCounter (ICounter counter);
}
