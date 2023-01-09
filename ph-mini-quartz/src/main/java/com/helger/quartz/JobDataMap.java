/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2023 Philip Helger (www.helger.com)
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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.attr.AttributeContainerAny;

/**
 * Holds state information for <code>Job</code> instances.
 * <p>
 * <code>JobDataMap</code> instances are stored once when the <code>Job</code>
 * is added to a scheduler. They are also re-persisted after every execution of
 * jobs annotated with <code>@PersistJobDataAfterExecution</code>.
 * </p>
 * <p>
 * <code>JobDataMap</code> instances can also be stored with a
 * <code>Trigger</code>. This can be useful in the case where you have a Job
 * that is stored in the scheduler for regular/repeated use by multiple
 * Triggers, yet with each independent triggering, you want to supply the Job
 * with different data inputs.
 * </p>
 * <p>
 * The <code>JobExecutionContext</code> passed to a Job at execution time also
 * contains a convenience <code>JobDataMap</code> that is the result of merging
 * the contents of the trigger's JobDataMap (if any) over the Job's JobDataMap
 * (if any).
 * </p>
 *
 * @see IJob
 * @see PersistJobDataAfterExecution
 * @see ITrigger
 * @see IJobExecutionContext
 * @author James House
 */
public class JobDataMap extends AttributeContainerAny <String>
{
  /**
   * Create an empty <code>JobDataMap</code>.
   */
  public JobDataMap ()
  {}

  /**
   * Create a <code>JobDataMap</code> with the given map data.
   *
   * @param map
   *        The map to copy from. May be <code>null</code>.
   */
  public JobDataMap (@Nullable final Map <String, ? extends Object> map)
  {
    super (map);
  }

  @Override
  @Nonnull
  @ReturnsMutableCopy
  public JobDataMap getClone ()
  {
    return new JobDataMap (this);
  }
}
