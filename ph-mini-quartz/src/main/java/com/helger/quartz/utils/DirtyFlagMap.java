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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsCollection;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;

/**
 * <p>
 * An implementation of <code>Map</code> that wraps another <code>Map</code> and
 * flags itself 'dirty' when it is modified.
 * </p>
 *
 * @author James House
 */
public class DirtyFlagMap <K, V> implements ICommonsMap <K, V>, Cloneable
{
  private boolean m_bDirty = false;
  private final ICommonsMap <K, V> m_aMap;

  /**
   * <p>
   * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code>.
   * </p>
   *
   * @see java.util.HashMap
   */
  public DirtyFlagMap ()
  {
    m_aMap = new CommonsHashMap <> ();
  }

  /**
   * <p>
   * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code> that has the
   * given initial capacity.
   * </p>
   *
   * @see java.util.HashMap
   */
  public DirtyFlagMap (final int nInitialCapacity)
  {
    m_aMap = new CommonsHashMap <> (nInitialCapacity);
  }

  /**
   * <p>
   * Create a DirtyFlagMap that 'wraps' a <code>HashMap</code> that has the
   * given initial capacity and load factor.
   * </p>
   *
   * @see java.util.HashMap
   */
  public DirtyFlagMap (final int nInitialCapacity, final float loadFactor)
  {
    m_aMap = new CommonsHashMap <> (nInitialCapacity, loadFactor);
  }

  /**
   * Copy constructor
   *
   * @param aOther
   *        The map to copy from. May not be <code>null</code>.
   */
  public DirtyFlagMap (@Nonnull final DirtyFlagMap <K, V> aOther)
  {
    m_bDirty = aOther.m_bDirty;
    m_aMap = aOther.m_aMap.getClone ();
  }

  /**
   * <p>
   * Clear the 'dirty' flag (set dirty flag to <code>false</code>).
   * </p>
   */
  public void clearDirtyFlag ()
  {
    m_bDirty = false;
  }

  /**
   * <p>
   * Determine whether the <code>Map</code> is flagged dirty.
   * </p>
   */
  public boolean isDirty ()
  {
    return m_bDirty;
  }

  /**
   * <p>
   * Get a direct handle to the underlying Map.
   * </p>
   */
  public ICommonsMap <K, V> getWrappedMap ()
  {
    return m_aMap;
  }

  public void clear ()
  {
    if (!m_aMap.isEmpty ())
    {
      m_bDirty = true;
    }
    m_aMap.clear ();
  }

  public boolean containsKey (final Object key)
  {
    return m_aMap.containsKey (key);
  }

  public boolean containsValue (final Object val)
  {
    return m_aMap.containsValue (val);
  }

  public Set <Entry <K, V>> entrySet ()
  {
    return new DirtyFlagMapEntrySet (m_aMap.entrySet ());
  }

  @Override
  public boolean equals (final Object obj)
  {
    if (obj == null || !(obj instanceof DirtyFlagMap))
      return false;

    return m_aMap.equals (((DirtyFlagMap <?, ?>) obj).getWrappedMap ());
  }

  @Override
  public int hashCode ()
  {
    return m_aMap.hashCode ();
  }

  public V get (final Object key)
  {
    return m_aMap.get (key);
  }

  public boolean isEmpty ()
  {
    return m_aMap.isEmpty ();
  }

  @Nonnull
  public ICommonsSet <K> keySet ()
  {
    return new DirtyFlagSet <> (m_aMap.keySet ());
  }

  public V put (final K key, final V val)
  {
    m_bDirty = true;

    return m_aMap.put (key, val);
  }

  public void putAll (@Nonnull final Map <? extends K, ? extends V> t)
  {
    if (!t.isEmpty ())
      m_bDirty = true;

    m_aMap.putAll (t);
  }

  public V remove (final Object key)
  {
    final V obj = m_aMap.remove (key);

    if (obj != null)
    {
      m_bDirty = true;
    }

    return obj;
  }

  public int size ()
  {
    return m_aMap.size ();
  }

  public ICommonsCollection <V> values ()
  {
    return new DirtyFlagCollection <> (m_aMap.values ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public DirtyFlagMap <K, V> getClone ()
  {
    return new DirtyFlagMap <> (this);
  }

  @Override
  public Object clone ()
  {
    return getClone ();
  }

  /**
   * Wrap a Collection so we can mark the DirtyFlagMap as dirty if the
   * underlying Collection is modified.
   */
  private class DirtyFlagCollection <T> implements ICommonsCollection <T>
  {
    private final Collection <T> m_aCollection;

    public DirtyFlagCollection (@Nonnull final Collection <T> c)
    {
      m_aCollection = c;
    }

    @Nonnull
    protected Collection <T> getWrappedCollection ()
    {
      return m_aCollection;
    }

    @Nonnull
    public Iterator <T> iterator ()
    {
      return new DirtyFlagIterator <> (m_aCollection.iterator ());
    }

    public boolean remove (final Object o)
    {
      final boolean removed = m_aCollection.remove (o);
      if (removed)
      {
        m_bDirty = true;
      }
      return removed;
    }

    public boolean removeAll (final Collection <?> c)
    {
      final boolean bChanged = m_aCollection.removeAll (c);
      if (bChanged)
        m_bDirty = true;
      return bChanged;
    }

    public boolean retainAll (final Collection <?> c)
    {
      final boolean bChanged = m_aCollection.retainAll (c);
      if (bChanged)
        m_bDirty = true;
      return bChanged;
    }

    public void clear ()
    {
      if (m_aCollection.isEmpty () == false)
        m_bDirty = true;
      m_aCollection.clear ();
    }

    // Pure wrapper methods
    public int size ()
    {
      return m_aCollection.size ();
    }

    public boolean isEmpty ()
    {
      return m_aCollection.isEmpty ();
    }

    public boolean contains (final Object o)
    {
      return m_aCollection.contains (o);
    }

    public boolean add (final T o)
    {
      return m_aCollection.add (o);
    } // Not supported

    public boolean addAll (final Collection <? extends T> c)
    {
      return m_aCollection.addAll (c);
    } // Not supported

    public boolean containsAll (final Collection <?> c)
    {
      return m_aCollection.containsAll (c);
    }

    public Object [] toArray ()
    {
      return m_aCollection.toArray ();
    }

    public <U> U [] toArray (final U [] array)
    {
      return m_aCollection.toArray (array);
    }
  }

  /**
   * Wrap a Set so we can mark the DirtyFlagMap as dirty if the underlying
   * Collection is modified.
   */
  private class DirtyFlagSet <T> extends DirtyFlagCollection <T> implements ICommonsSet <T>
  {
    public DirtyFlagSet (@Nonnull final Set <T> set)
    {
      super (set);
    }

    @Nonnull
    protected Set <T> getWrappedSet ()
    {
      return (Set <T>) getWrappedCollection ();
    }

    @Nonnull
    public DirtyFlagSet <T> getClone ()
    {
      return new DirtyFlagSet <> (getWrappedSet ());
    }
  }

  /**
   * Wrap an Iterator so that we can mark the DirtyFlagMap as dirty if an
   * element is removed.
   */
  private class DirtyFlagIterator <T> implements Iterator <T>
  {
    private final Iterator <T> m_aIterator;

    public DirtyFlagIterator (final Iterator <T> iterator)
    {
      m_aIterator = iterator;
    }

    public void remove ()
    {
      m_bDirty = true;
      m_aIterator.remove ();
    }

    // Pure wrapper methods
    public boolean hasNext ()
    {
      return m_aIterator.hasNext ();
    }

    public T next ()
    {
      return m_aIterator.next ();
    }
  }

  /**
   * Wrap a Map.Entry Set so we can mark the Map as dirty if the Set is
   * modified, and return Map.Entry objects wrapped in the
   * <code>DirtyFlagMapEntry</code> class.
   */
  private class DirtyFlagMapEntrySet extends DirtyFlagSet <Map.Entry <K, V>>
  {
    public DirtyFlagMapEntrySet (final Set <Map.Entry <K, V>> set)
    {
      super (set);
    }

    @Override
    public Iterator <Map.Entry <K, V>> iterator ()
    {
      return new DirtyFlagMapEntryIterator (getWrappedSet ().iterator ());
    }

    @Override
    public Object [] toArray ()
    {
      return toArray (new Object [super.size ()]);
    }

    @SuppressWarnings ("unchecked") // suppress warnings on both U[] and U
                                    // casting.
    @Override
    public <U> U [] toArray (final U [] array)
    {
      if (array.getClass ().getComponentType ().isAssignableFrom (Map.Entry.class) == false)
      {
        throw new IllegalArgumentException ("Array must be of type assignable from Map.Entry");
      }

      final int size = super.size ();

      final U [] result = array.length < size ? (U []) Array.newInstance (array.getClass ().getComponentType (), size)
                                              : array;

      final Iterator <Map.Entry <K, V>> entryIter = iterator (); // Will return
      // DirtyFlagMapEntry
      // objects
      for (int i = 0; i < size; i++)
      {
        result[i] = (U) entryIter.next ();
      }

      if (result.length > size)
      {
        result[size] = null;
      }

      return result;
    }
  }

  /**
   * Wrap an Iterator over Map.Entry objects so that we can mark the Map as
   * dirty if an element is removed or modified.
   */
  private class DirtyFlagMapEntryIterator extends DirtyFlagIterator <Map.Entry <K, V>>
  {
    public DirtyFlagMapEntryIterator (final Iterator <Map.Entry <K, V>> iterator)
    {
      super (iterator);
    }

    @Override
    public DirtyFlagMapEntry next ()
    {
      return new DirtyFlagMapEntry (super.next ());
    }
  }

  /**
   * Wrap a Map.Entry so we can mark the Map as dirty if a value is set.
   */
  private class DirtyFlagMapEntry implements Map.Entry <K, V>
  {
    private final Map.Entry <K, V> m_aEntry;

    public DirtyFlagMapEntry (@Nonnull final Map.Entry <K, V> entry)
    {
      m_aEntry = entry;
    }

    public V setValue (final V o)
    {
      m_bDirty = true;
      return m_aEntry.setValue (o);
    }

    // Pure wrapper methods
    public K getKey ()
    {
      return m_aEntry.getKey ();
    }

    public V getValue ()
    {
      return m_aEntry.getValue ();
    }

    @Override
    public boolean equals (final Object o)
    {
      return m_aEntry.equals (o);
    }

    @Override
    public int hashCode ()
    {
      return m_aEntry.hashCode ();
    }
  }
}
