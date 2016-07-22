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
package com.helger.quartz.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.helger.quartz.utils.DirtyFlagMap;

/**
 * Unit test for DirtyFlagMap. These tests focus on making sure the isDirty flag
 * is set correctly.
 */
public class DirtyFlagMapTest
{
  @Test
  public void testClear ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    assertFalse (dirtyFlagMap.isDirty ());

    dirtyFlagMap.clear ();
    assertFalse (dirtyFlagMap.isDirty ());
    dirtyFlagMap.put ("X", "Y");
    dirtyFlagMap.clearDirtyFlag ();
    dirtyFlagMap.clear ();
    assertTrue (dirtyFlagMap.isDirty ());
  }

  @Test
  public void testPut ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    dirtyFlagMap.put ("a", "Y");
    assertTrue (dirtyFlagMap.isDirty ());
  }

  @Test
  public void testRemove ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    dirtyFlagMap.put ("a", "Y");
    dirtyFlagMap.clearDirtyFlag ();

    dirtyFlagMap.remove ("b");
    assertFalse (dirtyFlagMap.isDirty ());

    dirtyFlagMap.remove ("a");
    assertTrue (dirtyFlagMap.isDirty ());
  }

  @Test
  public void testEntrySetRemove ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <Map.Entry <String, String>> entrySet = dirtyFlagMap.entrySet ();
    dirtyFlagMap.remove ("a");
    assertFalse (dirtyFlagMap.isDirty ());
    dirtyFlagMap.put ("a", "Y");
    dirtyFlagMap.clearDirtyFlag ();
    entrySet.remove ("b");
    assertFalse (dirtyFlagMap.isDirty ());
    entrySet.remove (entrySet.iterator ().next ());
    assertTrue (dirtyFlagMap.isDirty ());
  }

  @Test
  public void testEntrySetRetainAll ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <Map.Entry <String, String>> entrySet = dirtyFlagMap.entrySet ();
    entrySet.retainAll (Collections.EMPTY_LIST);
    assertFalse (dirtyFlagMap.isDirty ());
    dirtyFlagMap.put ("a", "Y");
    dirtyFlagMap.clearDirtyFlag ();
    entrySet.retainAll (Collections.singletonList (entrySet.iterator ().next ()));
    assertFalse (dirtyFlagMap.isDirty ());
    entrySet.retainAll (Collections.EMPTY_LIST);
    assertTrue (dirtyFlagMap.isDirty ());
  }

  @Test
  public void testEntrySetRemoveAll ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <Map.Entry <String, String>> entrySet = dirtyFlagMap.entrySet ();
    entrySet.removeAll (Collections.EMPTY_LIST);
    assertFalse (dirtyFlagMap.isDirty ());
    dirtyFlagMap.put ("a", "Y");
    dirtyFlagMap.clearDirtyFlag ();
    entrySet.removeAll (Collections.EMPTY_LIST);
    assertFalse (dirtyFlagMap.isDirty ());
    entrySet.removeAll (Collections.singletonList (entrySet.iterator ().next ()));
    assertTrue (dirtyFlagMap.isDirty ());
  }

  @Test
  public void testEntrySetClear ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <Map.Entry <String, String>> entrySet = dirtyFlagMap.entrySet ();
    entrySet.clear ();
    assertFalse (dirtyFlagMap.isDirty ());
    dirtyFlagMap.put ("a", "Y");
    dirtyFlagMap.clearDirtyFlag ();
    entrySet.clear ();
    assertTrue (dirtyFlagMap.isDirty ());
  }

  @SuppressWarnings ("unchecked")
  @Test
  public void testEntrySetIterator ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <Map.Entry <String, String>> entrySet = dirtyFlagMap.entrySet ();
    dirtyFlagMap.put ("a", "A");
    dirtyFlagMap.put ("b", "B");
    dirtyFlagMap.put ("c", "C");
    dirtyFlagMap.clearDirtyFlag ();
    final Iterator <?> entrySetIter = entrySet.iterator ();
    final Map.Entry <?, ?> entryToBeRemoved = (Map.Entry <?, ?>) entrySetIter.next ();
    final String removedKey = (String) entryToBeRemoved.getKey ();
    entrySetIter.remove ();
    assertEquals (2, dirtyFlagMap.size ());
    assertTrue (dirtyFlagMap.isDirty ());
    assertFalse (dirtyFlagMap.containsKey (removedKey));
    dirtyFlagMap.clearDirtyFlag ();
    final Map.Entry <?, String> entry = (Map.Entry <?, String>) entrySetIter.next ();
    entry.setValue ("BB");
    assertTrue (dirtyFlagMap.isDirty ());
    assertTrue (dirtyFlagMap.containsValue ("BB"));
  }

  @SuppressWarnings ("unchecked")
  @Test
  public void testEntrySetToArray ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <Map.Entry <String, String>> entrySet = dirtyFlagMap.entrySet ();
    dirtyFlagMap.put ("a", "A");
    dirtyFlagMap.put ("b", "B");
    dirtyFlagMap.put ("c", "C");
    dirtyFlagMap.clearDirtyFlag ();
    final Object [] array = entrySet.toArray ();
    assertEquals (3, array.length);
    final Map.Entry <?, String> entry = (Map.Entry <?, String>) array[0];
    entry.setValue ("BB");
    assertTrue (dirtyFlagMap.isDirty ());
    assertTrue (dirtyFlagMap.containsValue ("BB"));
  }

  @SuppressWarnings ("unchecked")
  @Test
  public void testEntrySetToArrayWithArg ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <Map.Entry <String, String>> entrySet = dirtyFlagMap.entrySet ();
    dirtyFlagMap.put ("a", "A");
    dirtyFlagMap.put ("b", "B");
    dirtyFlagMap.put ("c", "C");
    dirtyFlagMap.clearDirtyFlag ();
    final Object [] array = entrySet.toArray (new Map.Entry [] {});
    assertEquals (3, array.length);
    final Map.Entry <?, String> entry = (Map.Entry <?, String>) array[0];
    entry.setValue ("BB");
    assertTrue (dirtyFlagMap.isDirty ());
    assertTrue (dirtyFlagMap.containsValue ("BB"));
  }

  @Test
  public void testKeySetClear ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Set <?> keySet = dirtyFlagMap.keySet ();
    keySet.clear ();
    assertFalse (dirtyFlagMap.isDirty ());
    dirtyFlagMap.put ("a", "Y");
    dirtyFlagMap.clearDirtyFlag ();
    keySet.clear ();
    assertTrue (dirtyFlagMap.isDirty ());
    assertEquals (0, dirtyFlagMap.size ());
  }

  @Test
  public void testValuesClear ()
  {
    final DirtyFlagMap <String, String> dirtyFlagMap = new DirtyFlagMap<> ();
    final Collection <?> values = dirtyFlagMap.values ();
    values.clear ();
    assertFalse (dirtyFlagMap.isDirty ());
    dirtyFlagMap.put ("a", "Y");
    dirtyFlagMap.clearDirtyFlag ();
    values.clear ();
    assertTrue (dirtyFlagMap.isDirty ());
    assertEquals (0, dirtyFlagMap.size ());
  }
}