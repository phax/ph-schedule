/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2018 Philip Helger (www.helger.com)
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

/**
 * Unit test for PropertySettingJobFactory.
 */
public class PropertySettingJobFactoryTest
{
  private PropertySettingJobFactory m_aFactory;

  @Before
  public void setUp () throws Exception
  {
    m_aFactory = new PropertySettingJobFactory ();
    m_aFactory.setThrowIfPropertyNotFound (true);
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
    jobDataMap.put ("charValue", Character.valueOf ('a'));
    jobDataMap.put ("byteValue", Byte.valueOf ((byte) 6));
    jobDataMap.put ("stringValue", "S1");
    jobDataMap.put ("mapValue", Collections.singletonMap ("A", "B"));

    final TestBean myBean = new TestBean ();
    m_aFactory.setBeanProps (myBean, jobDataMap);

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
      m_aFactory.setBeanProps (new TestBean (), jobDataMap);
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
      m_aFactory.setBeanProps (new TestBean (), jobDataMap);
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
    m_aFactory.setBeanProps (testBean, jobDataMap);
    assertNull (testBean.getMapValue ());
  }

  @Test
  public void testSetBeanPropsWrongPrimativeType ()
  {
    final JobDataMap jobDataMap = new JobDataMap ();
    jobDataMap.put ("intValue", Float.valueOf (7));
    try
    {
      m_aFactory.setBeanProps (new TestBean (), jobDataMap);
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
    jobDataMap.put ("mapValue", Float.valueOf (7));
    try
    {
      m_aFactory.setBeanProps (new TestBean (), jobDataMap);
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
      m_aFactory.setBeanProps (new TestBean (), jobDataMap);
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
      m_aFactory.setBeanProps (new TestBean (), jobDataMap);
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
    m_aFactory.setBeanProps (myBean, jobDataMap);

    assertEquals (1, myBean.getIntValue ());
    assertEquals (2l, myBean.getLongValue ());
    assertEquals (3.0f, myBean.getFloatValue (), 0.0001);
    assertEquals (4.0, myBean.getDoubleValue (), 0.0001);
    assertTrue (myBean.getBooleanValue ());
    assertEquals (5, myBean.getShortValue ());
    assertEquals ('a', myBean.getCharValue ());
    assertEquals ((byte) 6, myBean.getByteValue ());
  }

  private static final class TestBean
  {
    private int m_nIntValue;
    private long m_nLongValue;
    private float m_fFloatValue;
    private double m_dDoubleValue;
    private boolean m_bBooleanValue;
    private byte m_nByteValue;
    private short m_nShortValue;
    private char m_cCharValue;
    private String m_sStringValue;
    private Map <?, ?> m_aMapValue;

    public boolean getBooleanValue ()
    {
      return m_bBooleanValue;
    }

    @SuppressWarnings ("unused")
    public void setBooleanValue (final boolean booleanValue)
    {
      this.m_bBooleanValue = booleanValue;
    }

    public double getDoubleValue ()
    {
      return m_dDoubleValue;
    }

    @SuppressWarnings ("unused")
    public void setDoubleValue (final double doubleValue)
    {
      this.m_dDoubleValue = doubleValue;
    }

    public float getFloatValue ()
    {
      return m_fFloatValue;
    }

    @SuppressWarnings ("unused")
    public void setFloatValue (final float floatValue)
    {
      this.m_fFloatValue = floatValue;
    }

    public int getIntValue ()
    {
      return m_nIntValue;
    }

    @SuppressWarnings ("unused")
    public void setIntValue (final int intValue)
    {
      this.m_nIntValue = intValue;
    }

    public long getLongValue ()
    {
      return m_nLongValue;
    }

    @SuppressWarnings ("unused")
    public void setLongValue (final long longValue)
    {
      this.m_nLongValue = longValue;
    }

    public Map <?, ?> getMapValue ()
    {
      return m_aMapValue;
    }

    public void setMapValue (final Map <?, ?> mapValue)
    {
      this.m_aMapValue = mapValue;
    }

    public String getStringValue ()
    {
      return m_sStringValue;
    }

    @SuppressWarnings ("unused")
    public void setStringValue (final String stringValue)
    {
      this.m_sStringValue = stringValue;
    }

    public byte getByteValue ()
    {
      return m_nByteValue;
    }

    @SuppressWarnings ("unused")
    public void setByteValue (final byte byteValue)
    {
      this.m_nByteValue = byteValue;
    }

    public char getCharValue ()
    {
      return m_cCharValue;
    }

    @SuppressWarnings ("unused")
    public void setCharValue (final char charValue)
    {
      this.m_cCharValue = charValue;
    }

    public short getShortValue ()
    {
      return m_nShortValue;
    }

    @SuppressWarnings ("unused")
    public void setShortValue (final short shortValue)
    {
      this.m_nShortValue = shortValue;
    }
  }
}
