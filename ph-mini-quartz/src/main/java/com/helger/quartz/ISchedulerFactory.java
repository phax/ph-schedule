/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2024 Philip Helger (www.helger.com)
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
package com.helger.quartz;

import com.helger.commons.collection.impl.ICommonsCollection;

/**
 * Provides a mechanism for obtaining client-usable handles to
 * <code>Scheduler</code> instances.
 *
 * @see IScheduler
 * @see com.helger.quartz.impl.StdSchedulerFactory
 * @author James House
 */
public interface ISchedulerFactory
{
  /**
   * Returns a client-usable handle to a <code>Scheduler</code>.
   *
   * @throws SchedulerException
   *         if there is a problem with the underlying <code>Scheduler</code>.
   */
  IScheduler getScheduler () throws SchedulerException;

  /**
   * Returns a handle to the Scheduler with the given name, if it exists.
   */
  IScheduler getScheduler (String schedName) throws SchedulerException;

  /**
   * Returns handles to all known Schedulers (made by any SchedulerFactory
   * within this jvm.).
   */
  ICommonsCollection <IScheduler> getAllSchedulers () throws SchedulerException;
}
