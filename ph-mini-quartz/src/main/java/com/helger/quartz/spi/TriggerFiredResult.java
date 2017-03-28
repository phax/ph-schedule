/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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

import java.io.Serializable;

/**
 * @author lorban
 */
public class TriggerFiredResult implements Serializable
{
  private TriggerFiredBundle triggerFiredBundle;
  private Exception exception;

  public TriggerFiredResult (final TriggerFiredBundle triggerFiredBundle)
  {
    this.triggerFiredBundle = triggerFiredBundle;
  }

  public TriggerFiredResult (final Exception exception)
  {
    this.exception = exception;
  }

  public TriggerFiredBundle getTriggerFiredBundle ()
  {
    return triggerFiredBundle;
  }

  public Exception getException ()
  {
    return exception;
  }
}
