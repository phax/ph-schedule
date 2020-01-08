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
package com.helger.quartz.simpl;

import com.helger.quartz.AbstractJobStoreTest;
import com.helger.quartz.spi.IJobStore;

public class RAMJobStoreTest extends AbstractJobStoreTest
{
  @Override
  protected IJobStore createJobStore (final String name)
  {
    final RAMJobStore rs = new RAMJobStore ();
    return rs;
  }

  @Override
  protected void destroyJobStore (final String name)
  {}
}
