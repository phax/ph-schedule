
/*
 * Copyright 2001-2009 Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package com.helger.quartz;

import java.util.Map;

import com.helger.quartz.utils.StringKeyDirtyFlagMap;

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
 * @see Job
 * @see PersistJobDataAfterExecution
 * @see Trigger
 * @see JobExecutionContext
 * @author James House
 */
public class JobDataMap extends StringKeyDirtyFlagMap
{
  /**
   * <p>
   * Create an empty <code>JobDataMap</code>.
   * </p>
   */
  public JobDataMap ()
  {
    super (15);
  }

  /**
   * <p>
   * Create a <code>JobDataMap</code> with the given data.
   * </p>
   */
  public JobDataMap (final Map <?, ?> map)
  {
    this ();
    @SuppressWarnings ("unchecked") // casting to keep API compatible and avoid
    // compiler errors/warnings.
    final Map <String, Object> mapTyped = (Map <String, Object>) map;
    putAll (mapTyped);
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Adds the given <code>boolean</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final boolean value)
  {
    final String strValue = Boolean.valueOf (value).toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>Boolean</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final Boolean value)
  {
    final String strValue = value.toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>char</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final char value)
  {
    final String strValue = Character.valueOf (value).toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>Character</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final Character value)
  {
    final String strValue = value.toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>double</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final double value)
  {
    final String strValue = Double.toString (value);

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>Double</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final Double value)
  {
    final String strValue = value.toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>float</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final float value)
  {
    final String strValue = Float.toString (value);

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>Float</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final Float value)
  {
    final String strValue = value.toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>int</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final int value)
  {
    final String strValue = Integer.valueOf (value).toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>Integer</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final Integer value)
  {
    final String strValue = value.toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>long</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final long value)
  {
    final String strValue = Long.valueOf (value).toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Adds the given <code>Long</code> value as a string version to the
   * <code>Job</code>'s data map.
   * </p>
   */
  public void putAsString (final String key, final Long value)
  {
    final String strValue = value.toString ();

    super.put (key, strValue);
  }

  /**
   * <p>
   * Retrieve the identified <code>int</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public int getIntFromString (final String key)
  {
    final Object obj = get (key);

    return new Integer ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>int</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String or Integer.
   */
  public int getIntValue (final String key)
  {
    final Object obj = get (key);

    if (obj instanceof String)
      return getIntFromString (key);
    return getInt (key);
  }

  /**
   * <p>
   * Retrieve the identified <code>int</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public Integer getIntegerFromString (final String key)
  {
    final Object obj = get (key);

    return new Integer ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>boolean</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public boolean getBooleanValueFromString (final String key)
  {
    final Object obj = get (key);

    return Boolean.valueOf ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>boolean</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String or Boolean.
   */
  public boolean getBooleanValue (final String key)
  {
    final Object obj = get (key);

    if (obj instanceof String)
      return getBooleanValueFromString (key);
    return getBoolean (key);
  }

  /**
   * <p>
   * Retrieve the identified <code>Boolean</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public Boolean getBooleanFromString (final String key)
  {
    final Object obj = get (key);

    return Boolean.valueOf ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>char</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public char getCharFromString (final String key)
  {
    final Object obj = get (key);

    return ((String) obj).charAt (0);
  }

  /**
   * <p>
   * Retrieve the identified <code>Character</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public Character getCharacterFromString (final String key)
  {
    final Object obj = get (key);

    return ((String) obj).charAt (0);
  }

  /**
   * <p>
   * Retrieve the identified <code>double</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public double getDoubleValueFromString (final String key)
  {
    final Object obj = get (key);

    return Double.valueOf ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>double</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String or Double.
   */
  public double getDoubleValue (final String key)
  {
    final Object obj = get (key);

    if (obj instanceof String)
      return getDoubleValueFromString (key);
    return getDouble (key);
  }

  /**
   * <p>
   * Retrieve the identified <code>Double</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public Double getDoubleFromString (final String key)
  {
    final Object obj = get (key);

    return new Double ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>float</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public float getFloatValueFromString (final String key)
  {
    final Object obj = get (key);

    return new Float ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>float</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String or Float.
   */
  public float getFloatValue (final String key)
  {
    final Object obj = get (key);

    if (obj instanceof String)
      return getFloatValueFromString (key);
    return getFloat (key);
  }

  /**
   * <p>
   * Retrieve the identified <code>Float</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public Float getFloatFromString (final String key)
  {
    final Object obj = get (key);

    return new Float ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>long</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public long getLongValueFromString (final String key)
  {
    final Object obj = get (key);

    return new Long ((String) obj);
  }

  /**
   * <p>
   * Retrieve the identified <code>long</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String or Long.
   */
  public long getLongValue (final String key)
  {
    final Object obj = get (key);

    if (obj instanceof String)
      return getLongValueFromString (key);
    return getLong (key);
  }

  /**
   * <p>
   * Retrieve the identified <code>Long</code> value from the
   * <code>JobDataMap</code>.
   * </p>
   *
   * @throws ClassCastException
   *         if the identified object is not a String.
   */
  public Long getLongFromString (final String key)
  {
    final Object obj = get (key);

    return new Long ((String) obj);
  }
}
