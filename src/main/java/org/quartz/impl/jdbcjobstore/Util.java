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

package org.quartz.impl.jdbcjobstore;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Locale;

import org.quartz.JobPersistenceException;

/**
 * <p>
 * This class contains utility functions for use in all delegate classes.
 * </p>
 *
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 */
public final class Util
{

  /**
   * Private constructor because this is a pure utility class.
   */
  private Util ()
  {}

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * <p>
   * Replace the table prefix in a query by replacing any occurrences of "{0}"
   * with the table prefix.
   * </p>
   *
   * @param query
   *        the unsubstitued query
   * @param tablePrefix
   *        the table prefix
   * @return the query, with proper table prefix substituted
   */
  public static String rtp (final String query, final String tablePrefix, final String schedNameLiteral)
  {
    return MessageFormat.format (query, new Object [] { tablePrefix, schedNameLiteral });
  }

  /**
   * <p>
   * Obtain a unique key for a given job.
   * </p>
   *
   * @param jobName
   *        the job name
   * @param groupName
   *        the group containing the job
   * @return a unique <code>String</code> key
   */
  static String getJobNameKey (final String jobName, final String groupName)
  {
    return (groupName + "_$x$x$_" + jobName).intern ();
  }

  /**
   * <p>
   * Obtain a unique key for a given trigger.
   * </p>
   *
   * @param triggerName
   *        the trigger name
   * @param groupName
   *        the group containing the trigger
   * @return a unique <code>String</code> key
   */
  static String getTriggerNameKey (final String triggerName, final String groupName)
  {
    return (groupName + "_$x$x$_" + triggerName).intern ();
  }

  /**
   * Cleanup helper method that closes the given <code>ResultSet</code> while
   * ignoring any errors.
   */
  public static void closeResultSet (final ResultSet rs)
  {
    if (null != rs)
    {
      try
      {
        rs.close ();
      }
      catch (final SQLException ignore)
      {}
    }
  }

  /**
   * Cleanup helper method that closes the given <code>Statement</code> while
   * ignoring any errors.
   */
  public static void closeStatement (final Statement statement)
  {
    if (null != statement)
    {
      try
      {
        statement.close ();
      }
      catch (final SQLException ignore)
      {}
    }
  }

  public static void setBeanProps (final Object obj,
                                   final String [] propNames,
                                   final Object [] propValues) throws JobPersistenceException
  {

    if (propNames == null || propNames.length == 0)
      return;
    if (propNames.length != propValues.length)
      throw new IllegalArgumentException ("propNames[].lenght != propValues[].length");

    String name = null;

    try
    {
      final BeanInfo bi = Introspector.getBeanInfo (obj.getClass ());
      final PropertyDescriptor [] propDescs = bi.getPropertyDescriptors ();

      for (int i = 0; i < propNames.length; i++)
      {
        name = propNames[i];
        final String c = name.substring (0, 1).toUpperCase (Locale.US);
        final String methName = "set" + c + name.substring (1);

        final java.lang.reflect.Method setMeth = getSetMethod (methName, propDescs);

        if (setMeth == null)
        {
          throw new NoSuchMethodException ("No setter for property '" + name + "'");
        }

        final Class <?> [] params = setMeth.getParameterTypes ();
        if (params.length != 1)
        {
          throw new NoSuchMethodException ("No 1-argument setter for property '" + name + "'");
        }

        setMeth.invoke (obj, new Object [] { propValues[i] });
      }
    }
    catch (final Exception e)
    {
      throw new JobPersistenceException ("Unable to set property named: " +
                                         name +
                                         " of object of type: " +
                                         obj.getClass ().getCanonicalName (),
                                         e);
    }
  }

  private static java.lang.reflect.Method getSetMethod (final String name, final PropertyDescriptor [] props)
  {
    for (final PropertyDescriptor prop : props)
    {
      final java.lang.reflect.Method wMeth = prop.getWriteMethod ();

      if (wMeth != null && wMeth.getName ().equals (name))
      {
        return wMeth;
      }
    }

    return null;
  }

}

// EOF
