/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2025 Philip Helger (www.helger.com)
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

import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.rt.NonBlockingProperties;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;

/**
 * <p>
 * This is an utility class used to parse the properties.
 * </p>
 *
 * @author James House
 */
public class PropertiesParser
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PropertiesParser.class);

  private final NonBlockingProperties m_aProps;

  public PropertiesParser (final NonBlockingProperties props)
  {
    m_aProps = props;
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("PropertiesParser ctor: " + props);
  }

  public NonBlockingProperties getUnderlyingProperties ()
  {
    return m_aProps;
  }

  /**
   * Get the trimmed String value of the property with the given <code>name</code>. If the value the
   * empty String (after trimming), then it returns null.
   */
  public String getStringProperty (final String name)
  {
    return getStringProperty (name, null);
  }

  /**
   * Get the trimmed String value of the property with the given <code>name</code> or the given
   * default value if the value is null or empty after trimming.
   */
  public String getStringProperty (final String sName, final String sDefault)
  {
    String val = m_aProps.getProperty (sName);
    if (val != null)
    {
      val = val.trim ();
      if (val.length () > 0)
        return val;
    }
    return StringHelper.trim (sDefault);
  }

  public String [] getStringArrayProperty (final String sName)
  {
    return getStringArrayProperty (sName, null);
  }

  public String [] getStringArrayProperty (final String sName, final String [] aDefault)
  {
    final String sVals = getStringProperty (sName);
    if (sVals == null)
      return aDefault;

    final StringTokenizer aTokenizer = new StringTokenizer (sVals, ",");
    final ICommonsList <String> aList = new CommonsArrayList <> ();
    try
    {
      while (aTokenizer.hasMoreTokens ())
      {
        aList.add (aTokenizer.nextToken ().trim ());
      }
      return aList.toArray (new String [aList.size ()]);
    }
    catch (final Exception e)
    {
      return aDefault;
    }
  }

  public boolean getBooleanProperty (final String sName)
  {
    return getBooleanProperty (sName, false);
  }

  public boolean getBooleanProperty (final String sName, final boolean bDefault)
  {
    final String val = getStringProperty (sName);
    return val == null ? bDefault : Boolean.parseBoolean (val);
  }

  public byte getByteProperty (final String sName)
  {
    final String sValue = getStringProperty (sName);
    if (sValue == null)
      throw new NumberFormatException (" null string");

    try
    {
      return Byte.parseByte (sValue);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + sValue + "'");
    }
  }

  public byte getByteProperty (final String name, final byte def)
  {
    final String val = getStringProperty (name);
    if (val == null)
      return def;

    try
    {
      return Byte.parseByte (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public char getCharProperty (final String name)
  {
    return getCharProperty (name, '\0');
  }

  public char getCharProperty (final String name, final char def)
  {
    final String sParam = getStringProperty (name);
    return sParam == null ? def : sParam.charAt (0);
  }

  public double getDoubleProperty (final String name)
  {
    final String val = getStringProperty (name);
    if (val == null)
      throw new NumberFormatException (" null string");

    try
    {
      return Double.parseDouble (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public double getDoubleProperty (final String name, final double def)
  {
    final String val = getStringProperty (name);
    if (val == null)
      return def;

    try
    {
      return Double.parseDouble (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public float getFloatProperty (final String name)
  {
    final String val = getStringProperty (name);
    if (val == null)
      throw new NumberFormatException (" null string");

    try
    {
      return Float.parseFloat (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public float getFloatProperty (final String name, final float def)
  {
    final String val = getStringProperty (name);
    if (val == null)
      return def;

    try
    {
      return Float.parseFloat (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public int getIntProperty (final String name)
  {
    final String val = getStringProperty (name);
    if (val == null)
      throw new NumberFormatException (" null string");

    try
    {
      return Integer.parseInt (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public int getIntProperty (final String name, final int def)
  {
    final String val = getStringProperty (name);
    if (val == null)
      return def;

    try
    {
      return Integer.parseInt (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public int [] getIntArrayProperty (final String name)
  {
    return getIntArrayProperty (name, null);
  }

  public int [] getIntArrayProperty (final String name, final int [] def)
  {
    final String vals = getStringProperty (name);
    if (vals == null)
      return def;

    final StringTokenizer stok = new StringTokenizer (vals, ",");
    final ICommonsList <Integer> ints = new CommonsArrayList <> ();
    try
    {
      while (stok.hasMoreTokens ())
      {
        try
        {
          ints.add (Integer.valueOf (stok.nextToken ().trim ()));
        }
        catch (final NumberFormatException nfe)
        {
          throw new NumberFormatException (" '" + vals + "'");
        }
      }

      final int [] outInts = new int [ints.size ()];
      for (int i = 0; i < ints.size (); i++)
        outInts[i] = ints.get (i).intValue ();
      return outInts;
    }
    catch (final Exception e)
    {
      return def;
    }
  }

  public long getLongProperty (final String name)
  {
    final String val = getStringProperty (name);
    if (val == null)
      throw new NumberFormatException (" null string");

    try
    {
      return Long.parseLong (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public long getLongProperty (final String name, final long def)
  {
    final String val = getStringProperty (name);
    if (val == null)
      return def;

    try
    {
      return Long.parseLong (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public short getShortProperty (final String name)
  {
    final String val = getStringProperty (name);
    if (val == null)
      throw new NumberFormatException (" null string");

    try
    {
      return Short.parseShort (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public short getShortProperty (final String name, final short def)
  {
    final String val = getStringProperty (name);
    if (val == null)
      return def;

    try
    {
      return Short.parseShort (val);
    }
    catch (final NumberFormatException nfe)
    {
      throw new NumberFormatException (" '" + val + "'");
    }
  }

  public ICommonsList <String> getPropertyGroups (final String sPrefix)
  {
    final ICommonsSet <String> groups = new CommonsLinkedHashSet <> (10);
    String sRealPrefix = sPrefix;
    if (!sRealPrefix.endsWith ("."))
      sRealPrefix += ".";

    for (final String sKey : m_aProps.keySet ())
    {
      if (sKey.startsWith (sRealPrefix))
      {
        final String groupName = sKey.substring (sRealPrefix.length (), sKey.indexOf ('.', sRealPrefix.length ()));
        groups.add (groupName);
      }
    }
    return groups.getCopyAsList ();
  }

  public NonBlockingProperties getPropertyGroup (final String prefix)
  {
    return getPropertyGroup (prefix, false, null);
  }

  public NonBlockingProperties getPropertyGroup (final String prefix, final boolean stripPrefix)
  {
    return getPropertyGroup (prefix, stripPrefix, null);
  }

  /**
   * Get all properties that start with the given prefix.
   *
   * @param sPrefix
   *        The prefix for which to search. If it does not end in a "." then one will be added to it
   *        for search purposes.
   * @param bStripPrefix
   *        Whether to strip off the given <code>prefix</code> in the result's keys.
   * @param aExcludedPrefixes
   *        Optional array of fully qualified prefixes to exclude. For example if
   *        <code>prefix</code> is "a.b.c", then <code>excludedPrefixes</code> might be
   *        "a.b.c.ignore".
   * @return Group of <code>NonBlockingProperties</code> that start with the given prefix,
   *         optionally have that prefix removed, and do not include properties that start with one
   *         of the given excluded prefixes.
   */
  public NonBlockingProperties getPropertyGroup (final String sPrefix,
                                                 final boolean bStripPrefix,
                                                 final String [] aExcludedPrefixes)
  {
    final NonBlockingProperties group = new NonBlockingProperties ();

    String sRealPrefix = sPrefix;
    if (!sRealPrefix.endsWith ("."))
      sRealPrefix += '.';

    for (final String sKey : m_aProps.keySet ())
      if (sKey.startsWith (sRealPrefix))
      {
        boolean bExclude = false;
        if (aExcludedPrefixes != null)
        {
          for (int i = 0; i < aExcludedPrefixes.length && !bExclude; i++)
            if (sKey.startsWith (aExcludedPrefixes[i]))
            {
              bExclude = true;
              break;
            }
        }

        if (!bExclude)
        {
          final String sValue = getStringProperty (sKey, "");
          if (bStripPrefix)
            group.put (sKey.substring (sRealPrefix.length ()), sValue);
          else
            group.put (sKey, sValue);
        }
      }

    return group;
  }
}
