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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;

/**
 * <p>
 * An implementation of <code>Map</code> that wraps another <code>Map</code> and
 * flags itself 'dirty' when it is modified, enforces that all keys are Strings.
 * </p>
 * <p>
 * All allowsTransientData flag related methods are deprecated as of version
 * 1.6.
 * </p>
 */
public class StringKeyDirtyFlagMap extends DirtyFlagMap <String, Object>
{
  public StringKeyDirtyFlagMap ()
  {
    super ();
  }

  public StringKeyDirtyFlagMap (final int nInitialCapacity)
  {
    super (nInitialCapacity);
  }

  public StringKeyDirtyFlagMap (final int nInitialCapacity, final float loadFactor)
  {
    super (nInitialCapacity, loadFactor);
  }

  public StringKeyDirtyFlagMap (@Nonnull final StringKeyDirtyFlagMap aOther)
  {
    super (aOther);
  }

  @Override
  public boolean equals (final Object obj)
  {
    return super.equals (obj);
  }

  @Override
  public int hashCode ()
  {
    return getWrappedMap ().hashCode ();
  }

  /**
   * @return a copy of the Map's String keys in an array of Strings.
   */
  public String [] getKeys ()
  {
    return keySet ().toArray (new String [size ()]);
  }

  /**
   * <p>
   * Adds the given <code>int</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put (final String key, final int value)
  {
    super.put (key, Integer.valueOf (value));
  }

  /**
   * <p>
   * Adds the given <code>long</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put (final String key, final long value)
  {
    super.put (key, Long.valueOf (value));
  }

  /**
   * <p>
   * Adds the given <code>float</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put (final String key, final float value)
  {
    super.put (key, Float.valueOf (value));
  }

  /**
   * <p>
   * Adds the given <code>double</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put (final String key, final double value)
  {
    super.put (key, Double.valueOf (value));
  }

  /**
   * <p>
   * Adds the given <code>boolean</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put (final String key, final boolean value)
  {
    super.put (key, Boolean.valueOf (value));
  }

  /**
   * <p>
   * Adds the given <code>char</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put (final String key, final char value)
  {
    super.put (key, Character.valueOf (value));
  }

  /**
   * <p>
   * Adds the given <code>String</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put (final String key, final String value)
  {
    super.put (key, value);
  }

  /**
   * <p>
   * Adds the given <code>Object</code> value to the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  @Override
  public Object put (final String key, final Object value)
  {
    return super.put (key, value);
  }

  /**
   * <p>
   * Retrieve the identified <code>int</code> value from the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not an Integer.
   */
  public int getInt (final String key)
  {
    final Object obj = get (key);

    try
    {
      if (obj instanceof Integer)
        return ((Integer) obj).intValue ();
      return Integer.parseInt ((String) obj);
    }
    catch (final Exception e)
    {
      throw new ClassCastException ("Identified object is not an Integer.");
    }
  }

  /**
   * <p>
   * Retrieve the identified <code>long</code> value from the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a Long.
   */
  public long getLong (final String key)
  {
    final Object obj = get (key);

    try
    {
      if (obj instanceof Long)
        return ((Long) obj).longValue ();
      return Long.parseLong ((String) obj);
    }
    catch (final Exception e)
    {
      throw new ClassCastException ("Identified object is not a Long.");
    }
  }

  /**
   * <p>
   * Retrieve the identified <code>float</code> value from the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a Float.
   */
  public float getFloat (final String key)
  {
    final Object obj = get (key);

    try
    {
      if (obj instanceof Float)
        return ((Float) obj).floatValue ();
      return Float.parseFloat ((String) obj);
    }
    catch (final Exception e)
    {
      throw new ClassCastException ("Identified object is not a Float.");
    }
  }

  /**
   * <p>
   * Retrieve the identified <code>double</code> value from the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a Double.
   */
  public double getDouble (final String key)
  {
    final Object obj = get (key);

    try
    {
      if (obj instanceof Double)
        return ((Double) obj).doubleValue ();
      return Double.parseDouble ((String) obj);
    }
    catch (final Exception e)
    {
      throw new ClassCastException ("Identified object is not a Double.");
    }
  }

  /**
   * <p>
   * Retrieve the identified <code>boolean</code> value from the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a Boolean.
   */
  public boolean getBoolean (final String key)
  {
    final Object obj = get (key);

    try
    {
      if (obj instanceof Boolean)
        return ((Boolean) obj).booleanValue ();
      return Boolean.parseBoolean ((String) obj);
    }
    catch (final Exception e)
    {
      throw new ClassCastException ("Identified object is not a Boolean.");
    }
  }

  /**
   * <p>
   * Retrieve the identified <code>char</code> value from the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a Character.
   */
  public char getChar (final String key)
  {
    final Object obj = get (key);

    try
    {
      if (obj instanceof Character)
        return ((Character) obj).charValue ();
      return ((String) obj).charAt (0);
    }
    catch (final Exception e)
    {
      throw new ClassCastException ("Identified object is not a Character.");
    }
  }

  /**
   * <p>
   * Retrieve the identified <code>String</code> value from the
   * <code>StringKeyDirtyFlagMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public String getString (final String key)
  {
    final Object obj = get (key);

    try
    {
      return (String) obj;
    }
    catch (final Exception e)
    {
      throw new ClassCastException ("Identified object is not a String.");
    }
  }

  @Override
  @Nonnull
  @ReturnsMutableCopy
  public StringKeyDirtyFlagMap getClone ()
  {
    return new StringKeyDirtyFlagMap (this);
  }
}
