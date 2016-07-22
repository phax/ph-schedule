/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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

import com.helger.quartz.ITrigger;
import com.helger.quartz.JobKey;
import com.helger.quartz.SchedulerException;

/**
 * An interface to be used by <code>JobStore</code> instances in order to
 * communicate signals back to the <code>QuartzScheduler</code>.
 *
 * @author jhouse
 */
public interface ISchedulerSignaler
{
  void notifyTriggerListenersMisfired (ITrigger trigger);

  void notifySchedulerListenersFinalized (ITrigger trigger);

  void notifySchedulerListenersJobDeleted (JobKey jobKey);

  void signalSchedulingChange (long candidateNewNextFireTime);

  void notifySchedulerListenersError (String string, SchedulerException jpe);
}
