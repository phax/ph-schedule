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
package com.helger.quartz.impl;

import com.helger.quartz.spi.IThreadExecutor;

/**
 * Schedules work on a newly spawned thread. This is the default Quartz
 * behavior.
 *
 * @author matt.accola
 * @version $Revision$ $Date$
 */
public class DefaultThreadExecutor implements IThreadExecutor
{
  public void initialize ()
  {}

  public void execute (final Thread thread)
  {
    thread.start ();
  }
}
