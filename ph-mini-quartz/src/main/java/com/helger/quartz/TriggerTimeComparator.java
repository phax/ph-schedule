/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2022 Philip Helger (www.helger.com)
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

import java.util.Date;

import com.helger.commons.compare.IComparator;

/**
 * A Comparator that compares trigger's next fire times, or in other words,
 * sorts them according to earliest next fire time. If the fire times are the
 * same, then the triggers are sorted according to priority (highest value
 * first), if the priorities are the same, then they are sorted by key.
 */
public class TriggerTimeComparator implements IComparator <ITrigger>
{
  // This static method exists for comparator in TC clustered quartz
  static int compare (final Date nextFireTime1,
                      final int priority1,
                      final TriggerKey key1,
                      final Date nextFireTime2,
                      final int priority2,
                      final TriggerKey key2)
  {
    if (nextFireTime1 != null || nextFireTime2 != null)
    {
      if (nextFireTime1 == null)
        return 1;
      if (nextFireTime2 == null)
        return -1;
      if (nextFireTime1.before (nextFireTime2))
        return -1;
      if (nextFireTime1.after (nextFireTime2))
        return 1;
    }

    int comp = priority2 - priority1;
    if (comp == 0)
      comp = key1.compareTo (key2);
    return comp;
  }

  public int compare (final ITrigger t1, final ITrigger t2)
  {
    return compare (t1.getNextFireTime (),
                    t1.getPriority (),
                    t1.getKey (),
                    t2.getNextFireTime (),
                    t2.getPriority (),
                    t2.getKey ());
  }
}
