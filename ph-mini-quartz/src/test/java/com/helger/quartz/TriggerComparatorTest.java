/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2021 Philip Helger (www.helger.com)
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

import static com.helger.quartz.DateBuilder.futureDate;
import static com.helger.quartz.EIntervalUnit.MINUTE;
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.helger.quartz.spi.IOperableTrigger;

public class TriggerComparatorTest
{
  @Test
  public void testTriggerSort ()
  {

    // build trigger in expected sort order
    final ITrigger t1 = newTrigger ().withIdentity ("a").build ();
    final ITrigger t2 = newTrigger ().withIdentity ("b").build ();
    final ITrigger t3 = newTrigger ().withIdentity ("c").build ();
    final ITrigger t4 = newTrigger ().withIdentity ("a", "a").build ();
    final ITrigger t5 = newTrigger ().withIdentity ("a", "b").build ();
    final ITrigger t6 = newTrigger ().withIdentity ("a", "c").build ();

    final List <ITrigger> ts = new LinkedList <> ();
    // add triggers to list in somewhat randomized order
    ts.add (t5);
    ts.add (t6);
    ts.add (t4);
    ts.add (t3);
    ts.add (t1);
    ts.add (t2);

    // sort the list
    Collections.sort (ts);

    // check the order of the list
    assertEquals (t1, ts.get (0));
    assertEquals (t2, ts.get (1));
    assertEquals (t3, ts.get (2));
    assertEquals (t4, ts.get (3));
    assertEquals (t5, ts.get (4));
    assertEquals (t6, ts.get (5));
  }

  @Test
  public void testTriggerTimeSort ()
  {

    // build trigger in expected sort order
    final ITrigger t1 = newTrigger ().withIdentity ("a").startAt (futureDate (1, MINUTE)).build ();
    ((IOperableTrigger) t1).computeFirstFireTime (null);
    final ITrigger t2 = newTrigger ().withIdentity ("b").startAt (futureDate (2, MINUTE)).build ();
    ((IOperableTrigger) t2).computeFirstFireTime (null);
    final ITrigger t3 = newTrigger ().withIdentity ("c").startAt (futureDate (3, MINUTE)).build ();
    ((IOperableTrigger) t3).computeFirstFireTime (null);
    final ITrigger t4 = newTrigger ().withIdentity ("d").startAt (futureDate (5, MINUTE)).withPriority (7).build ();
    ((IOperableTrigger) t4).computeFirstFireTime (null);
    final ITrigger t5 = newTrigger ().withIdentity ("e").startAt (futureDate (5, MINUTE)).build ();
    ((IOperableTrigger) t5).computeFirstFireTime (null);
    final ITrigger t6 = newTrigger ().withIdentity ("g").startAt (futureDate (5, MINUTE)).build ();
    ((IOperableTrigger) t6).computeFirstFireTime (null);
    final ITrigger t7 = newTrigger ().withIdentity ("h").startAt (futureDate (5, MINUTE)).withPriority (2).build ();
    ((IOperableTrigger) t7).computeFirstFireTime (null);
    final ITrigger t8 = newTrigger ().withIdentity ("i").startAt (futureDate (6, MINUTE)).build ();
    ((IOperableTrigger) t8).computeFirstFireTime (null);
    final ITrigger t9 = newTrigger ().withIdentity ("j").startAt (futureDate (7, MINUTE)).build ();
    ((IOperableTrigger) t9).computeFirstFireTime (null);

    final List <ITrigger> ts = new LinkedList <> ();
    // add triggers to list in somewhat randomized order
    ts.add (t5);
    ts.add (t9);
    ts.add (t6);
    ts.add (t8);
    ts.add (t4);
    ts.add (t3);
    ts.add (t1);
    ts.add (t7);
    ts.add (t2);

    // sort the list
    Collections.sort (ts);

    // check the order of the list
    assertEquals (t1, ts.get (0));
    assertEquals (t2, ts.get (1));
    assertEquals (t3, ts.get (2));
    assertEquals (t4, ts.get (3));
    assertEquals (t5, ts.get (4));
    assertEquals (t6, ts.get (5));
    assertEquals (t7, ts.get (6));
    assertEquals (t8, ts.get (7));
    assertEquals (t9, ts.get (8));
  }

}
