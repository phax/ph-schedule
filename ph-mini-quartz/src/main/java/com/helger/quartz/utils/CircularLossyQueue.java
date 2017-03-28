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
package com.helger.quartz.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of a CircularQueue data-structure. When the number of items
 * added exceeds the maximum capacity, items that were added first are lost.
 *
 * @param <T>
 *        Type of the item's to add in this queue
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class CircularLossyQueue <T>
{
  private final AtomicReference <T> [] m_aCircularArray;
  private final int m_nMaxSize;
  private final AtomicLong m_aCurrentIndex = new AtomicLong (-1);

  /**
   * Constructs the circular queue with the specified capacity
   *
   * @param size
   */
  @SuppressWarnings ("unchecked")
  public CircularLossyQueue (final int size)
  {
    m_aCircularArray = new AtomicReference [size];
    for (int i = 0; i < size; i++)
      m_aCircularArray[i] = new AtomicReference <> ();
    m_nMaxSize = size;
  }

  /**
   * Adds a new item
   *
   * @param newVal
   */
  public void push (final T newVal)
  {
    final int index = (int) (m_aCurrentIndex.incrementAndGet () % m_nMaxSize);
    m_aCircularArray[index].set (newVal);
  }

  /**
   * Returns an array of the current elements in the queue. The order of
   * elements is in reverse order of the order items were added.
   *
   * @param type
   * @return An array containing the current elements in the queue. The first
   *         element of the array is the tail of the queue and the last element
   *         is the head of the queue
   */
  public T [] toArray (final T [] type)
  {
    System.getProperties ();

    if (type.length > m_nMaxSize)
    {
      throw new IllegalArgumentException ("Size of array passed in cannot be greater than " + m_nMaxSize);
    }

    final int curIndex = _getCurrentIndex ();
    for (int k = 0; k < type.length; k++)
    {
      final int index = _getIndex (curIndex - k);
      type[k] = m_aCircularArray[index].get ();
    }
    return type;
  }

  private int _getIndex (final int index)
  {
    return index < 0 ? index + m_nMaxSize : index;
  }

  /**
   * Returns value at the tail of the queue
   *
   * @return Value at the tail of the queue
   */
  public T peek ()
  {
    if (depth () == 0)
      return null;
    return m_aCircularArray[_getIndex (_getCurrentIndex ())].get ();
  }

  /**
   * Returns true if the queue is empty, otherwise false
   *
   * @return true if the queue is empty, false otherwise
   */
  public boolean isEmtpy ()
  {
    return depth () == 0;
  }

  private int _getCurrentIndex ()
  {
    return (int) (m_aCurrentIndex.get () % m_nMaxSize);
  }

  /**
   * Returns the number of items currently in the queue
   *
   * @return the number of items in the queue
   */
  public int depth ()
  {
    final long currInd = m_aCurrentIndex.get () + 1;
    return currInd >= m_nMaxSize ? m_nMaxSize : (int) currInd;
  }
}
