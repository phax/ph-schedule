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
 */
package com.helger.quartz.simpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.helger.quartz.JobDataMap;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.simpl.PropertySettingJobFactory;

/**
 * Unit test for PropertySettingJobFactory.
 */
public class PropertySettingJobFactoryTest
{

  private PropertySettingJobFactory factory;

  @Before
  public void setUp () throws Exception
  {
    factory = new PropertySettingJobFactory ();
    factory.setThrowIfPropertyNotFound (true);
  }

  @Test
  public void testSetBeanPropsPrimatives () throws SchedulerException
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("intValue", Integer.valueOf (1));
    jobDataMap.put ("longValue", Long.valueOf (2l));
    jobDataMap.put ("floatValue", Float.valueOf (3.0f));
    jobDataMap.put ("doubleValue", Double.valueOf (4.0));
    jobDataMap.put ("booleanValue", Boolean.TRUE);
    jobDataMap.put ("shortValue", Short.valueOf (((short) 5)));
    jobDataMap.put ("charValue", 'a');
    jobDataMap.put ("byteValue", Byte.valueOf ((byte) 6));
    jobDataMap.put ("stringValue", "S1");
    jobDataMap.put ("mapValue", Collections.singletonMap ("A", "B"));

    final TestBean myBean = new TestBean ();
    factory.setBeanProps (myBean, jobDataMap);

    assertEquals (1, myBean.getIntValue ());
    assertEquals (2l, myBean.getLongValue ());
    assertEquals (3.0f, myBean.getFloatValue (), 0.0001);
    assertEquals (4.0, myBean.getDoubleValue (), 0.0001);
    assertTrue (myBean.getBooleanValue ());
    assertEquals (5, myBean.getShortValue ());
    assertEquals ('a', myBean.getCharValue ());
    assertEquals ((byte) 6, myBean.getByteValue ());
    assertEquals ("S1", myBean.getStringValue ());
    assertTrue (myBean.getMapValue ().containsKey ("A"));
  }

  @Test
  public void testSetBeanPropsUnknownProperty ()
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("bogusValue", Integer.valueOf (1));
    try
    {
      factory.setBeanProps (new TestBean (), jobDataMap);
      fail ();
    }
    catch (final SchedulerException ignore)
    { // ignore
    }
  }

  @Test
  public void testSetBeanPropsNullPrimative ()
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("intValue", null);
    try
    {
      factory.setBeanProps (new TestBean (), jobDataMap);
      fail ();
    }
    catch (final SchedulerException ignore)
    {
      // ignore
    }
  }

  @Test
  public void testSetBeanPropsNullNonPrimative () throws SchedulerException
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("mapValue", null);
    final TestBean testBean = new TestBean ();
    testBean.setMapValue (Collections.singletonMap ("A", "B"));
    factory.setBeanProps (testBean, jobDataMap);
    assertNull (testBean.getMapValue ());
  }

  @Test
  public void testSetBeanPropsWrongPrimativeType ()
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("intValue", new Float (7));
    try
    {
      factory.setBeanProps (new TestBean (), jobDataMap);
      fail ();
    }
    catch (final SchedulerException ignore)
    {
      // ignore
    }
  }

  @Test
  public void testSetBeanPropsWrongNonPrimativeType ()
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("mapValue", new Float (7));
    try
    {
      factory.setBeanProps (new TestBean (), jobDataMap);
      fail ();
    }
    catch (final SchedulerException ignore)
    {
      // ignore
    }
  }

  @Test
  public void testSetBeanPropsCharStringTooShort ()
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("charValue", "");
    try
    {
      factory.setBeanProps (new TestBean (), jobDataMap);
      fail ();
    }
    catch (final SchedulerException ignore)
    {
      // ignroe
    }
  }

  @Test
  public void testSetBeanPropsCharStringTooLong ()
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("charValue", "abba");
    try
    {
      factory.setBeanProps (new TestBean (), jobDataMap);
      fail ();
    }
    catch (final SchedulerException ignore)
    {
      // ignore
    }
  }

  @Test
  public void testSetBeanPropsFromStrings () throws SchedulerException
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("intValue", "1");
    jobDataMap.put ("longValue", "2");
    jobDataMap.put ("floatValue", "3.0");
    jobDataMap.put ("doubleValue", "4.0");
    jobDataMap.put ("booleanValue", "true");
    jobDataMap.put ("shortValue", "5");
    jobDataMap.put ("charValue", "a");
    jobDataMap.put ("byteValue", "6");

    final TestBean myBean = new TestBean ();
    factory.setBeanProps (myBean, jobDataMap);

    assertEquals (1, myBean.getIntValue ());
    assertEquals (2l, myBean.getLongValue ());
    assertEquals (3.0f, myBean.getFloatValue (), 0.0001);
    assertEquals (4.0, myBean.getDoubleValue (), 0.0001);
    assertEquals (true, myBean.getBooleanValue ());
    assertEquals (5, myBean.getShortValue ());
    assertEquals ('a', myBean.getCharValue ());
    assertEquals ((byte) 6, myBean.getByteValue ());
  }

  private static final class TestBean
  {
    private int intValue;
    private long longValue;
    private float floatValue;
    private double doubleValue;
    private boolean booleanValue;
    private byte byteValue;
    private short shortValue;
    private char charValue;
    private String stringValue;
    private Map <?, ?> mapValue;

    public boolean getBooleanValue ()
    {
      return booleanValue;
    }

    @SuppressWarnings ("unused")
    public void setBooleanValue (final boolean booleanValue)
    {
      this.booleanValue = booleanValue;
    }

    public double getDoubleValue ()
    {
      return doubleValue;
    }

    @SuppressWarnings ("unused")
    public void setDoubleValue (final double doubleValue)
    {
      this.doubleValue = doubleValue;
    }

    public float getFloatValue ()
    {
      return floatValue;
    }

    @SuppressWarnings ("unused")
    public void setFloatValue (final float floatValue)
    {
      this.floatValue = floatValue;
    }

    public int getIntValue ()
    {
      return intValue;
    }

    @SuppressWarnings ("unused")
    public void setIntValue (final int intValue)
    {
      this.intValue = intValue;
    }

    public long getLongValue ()
    {
      return longValue;
    }

    @SuppressWarnings ("unused")
    public void setLongValue (final long longValue)
    {
      this.longValue = longValue;
    }

    public Map <?, ?> getMapValue ()
    {
      return mapValue;
    }

    public void setMapValue (final Map <?, ?> mapValue)
    {
      this.mapValue = mapValue;
    }

    public String getStringValue ()
    {
      return stringValue;
    }

    @SuppressWarnings ("unused")
    public void setStringValue (final String stringValue)
    {
      this.stringValue = stringValue;
    }

    public byte getByteValue ()
    {
      return byteValue;
    }

    @SuppressWarnings ("unused")
    public void setByteValue (final byte byteValue)
    {
      this.byteValue = byteValue;
    }

    public char getCharValue ()
    {
      return charValue;
    }

    @SuppressWarnings ("unused")
    public void setCharValue (final char charValue)
    {
      this.charValue = charValue;
    }

    public short getShortValue ()
    {
      return shortValue;
    }

    @SuppressWarnings ("unused")
    public void setShortValue (final short shortValue)
    {
      this.shortValue = shortValue;
    }
  }
}
