/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2024 Philip Helger (www.helger.com)
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

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for SystemPropertyInstanceIdGenerator.
 */
public class SystemPropertyInstanceIdGeneratorTest
{
  @Before
  public void setUp () throws Exception
  {
    System.setProperty (SystemPropertyInstanceIdGenerator.SYSTEM_PROPERTY, "foo");
    System.setProperty ("blah.blah", "goo");
  }

  @Test
  public void testGetInstanceId () throws Exception
  {
    final SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator ();

    final String instId = gen.generateInstanceId ();

    assertEquals ("foo", instId);
  }

  @Test
  public void testGetInstanceIdWithPrepend () throws Exception
  {
    final SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator ();
    gen.setPrepend ("1");

    final String instId = gen.generateInstanceId ();

    assertEquals ("1foo", instId);
  }

  @Test
  public void testGetInstanceIdWithPostpend () throws Exception
  {
    final SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator ();
    gen.setPostpend ("2");

    final String instId = gen.generateInstanceId ();

    assertEquals ("foo2", instId);
  }

  @Test
  public void testGetInstanceIdWithPrependAndPostpend () throws Exception
  {
    final SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator ();
    gen.setPrepend ("1");
    gen.setPostpend ("2");

    final String instId = gen.generateInstanceId ();

    assertEquals ("1foo2", instId);
  }

  @Test
  public void testGetInstanceIdFromCustomSystemProperty () throws Exception
  {
    final SystemPropertyInstanceIdGenerator gen = new SystemPropertyInstanceIdGenerator ();
    gen.setSystemPropertyName ("blah.blah");

    final String instId = gen.generateInstanceId ();

    assertEquals ("goo", instId);
  }
}
