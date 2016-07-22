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

import java.util.Properties;

import org.junit.Test;

import com.helger.quartz.utils.PropertiesParser;

/**
 * Unit tests for PropertiesParser.
 */
public class PropertiesParserTest
{
  /**
   * Unit test for full getPropertyGroup() method.
   */
  @Test
  public void testGetPropertyGroupStringBooleanStringArray ()
  {
    // Test that an empty property does not cause an exception
    final Properties props = new Properties ();
    props.put ("x.y.z", "");

    final PropertiesParser propertiesParser = new PropertiesParser (props);
    final Properties propGroup = propertiesParser.getPropertyGroup ("x.y", true, new String [] {});
    assertEquals ("", propGroup.getProperty ("z"));
  }
}
