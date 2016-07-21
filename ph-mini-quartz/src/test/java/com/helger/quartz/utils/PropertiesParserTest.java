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
