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
package com.helger.quartz.spi;

import org.jspecify.annotations.Nullable;

/**
 * @author lorban
 */
public class TriggerFiredResult
{
  private final TriggerFiredBundle m_aTriggerFiredBundle;
  private final Exception m_aException;

  public TriggerFiredResult (@Nullable final TriggerFiredBundle triggerFiredBundle)
  {
    m_aTriggerFiredBundle = triggerFiredBundle;
    m_aException = null;
  }

  public TriggerFiredResult (@Nullable final Exception exception)
  {
    m_aTriggerFiredBundle = null;
    m_aException = exception;
  }

  @Nullable
  public TriggerFiredBundle getTriggerFiredBundle ()
  {
    return m_aTriggerFiredBundle;
  }

  @Nullable
  public Exception getException ()
  {
    return m_aException;
  }
}
