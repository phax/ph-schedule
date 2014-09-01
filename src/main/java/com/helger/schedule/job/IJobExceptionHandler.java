/**
 * Copyright (C) 2014 Philip Helger (www.helger.com)
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
package com.helger.schedule.job;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Callback interface to handle thrown exception objects from the
 * {@link AbstractScopeAwareJob}.
 * 
 * @author Philip Helger
 */
public interface IJobExceptionHandler
{
  /**
   * Called when an exception of the specified type occurred
   * 
   * @param t
   *        The exception. Never <code>null</code>.
   * @param sJobClassName
   *        The name of the job class
   * @param bIsLongRunning
   *        <code>true</code> if it is a long running job
   */
  void onScheduledJobException (@Nonnull Throwable t, @Nullable String sJobClassName, boolean bIsLongRunning);
}
