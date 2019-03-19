/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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
package com.helger.quartz.listeners;

import com.helger.quartz.ITriggerListener;

/**
 * A helpful abstract base class for implementors of
 * <code>{@link com.helger.quartz.ITriggerListener}</code>.
 * <p>
 * The methods in this class are empty so you only need to override the subset
 * for the <code>{@link com.helger.quartz.ITriggerListener}</code> events you
 * care about.
 * </p>
 * <p>
 * You are required to implement
 * <code>{@link com.helger.quartz.ITriggerListener#getName()}</code> to return
 * the unique name of your <code>TriggerListener</code>.
 * </p>
 *
 * @see com.helger.quartz.ITriggerListener
 */
public abstract class AbstractTriggerListenerSupport implements ITriggerListener
{
  /* empty */
}
