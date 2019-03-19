/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2019 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.string.StringHelper;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.spi.IInstanceIdGenerator;

/**
 * InstanceIdGenerator that will use a
 * {@link SystemPropertyInstanceIdGenerator#SYSTEM_PROPERTY system property} to
 * configure the scheduler. The default system property name to use the value of
 * {@link #SYSTEM_PROPERTY}, but can be specified via the "systemPropertyName"
 * property. You can also set the properties "postpend" and "prepend" to String
 * values that will be added to the beginning or end (respectively) of the value
 * found in the system property. If no value set for the property, a
 * {@link com.helger.quartz.SchedulerException} is thrown
 *
 * @author Alex Snaps
 */
public class SystemPropertyInstanceIdGenerator implements IInstanceIdGenerator
{
  /**
   * System property to read the instanceId from
   */
  public static final String SYSTEM_PROPERTY = "org.quartz.scheduler.instanceId";

  private String m_sPrepend;
  private String m_sPostpend;
  private String m_sSystemPropertyName = SYSTEM_PROPERTY;

  /**
   * Returns the cluster wide value for this scheduler instance's id, based on a
   * system property
   *
   * @return the value of the system property named by the value of
   *         {@link #getSystemPropertyName()} - which defaults to
   *         {@link #SYSTEM_PROPERTY}.
   * @throws SchedulerException
   *         Shouldn't a value be found
   */
  public String generateInstanceId () throws SchedulerException
  {
    String property = System.getProperty (getSystemPropertyName ());
    if (property == null)
      throw new SchedulerException ("No value for '" +
                                    SYSTEM_PROPERTY +
                                    "' system property found, please configure your environment accordingly!");

    if (getPrepend () != null)
      property = getPrepend () + property;
    if (getPostpend () != null)
      property = property + getPostpend ();

    return property;
  }

  /**
   * A String of text to prepend (add to the beginning) to the instanceId found
   * in the system property.
   */
  @Nullable
  public String getPrepend ()
  {
    return m_sPrepend;
  }

  /**
   * A String of text to prepend (add to the beginning) to the instanceId found
   * in the system property.
   *
   * @param prepend
   *        the value to prepend, or null if none is desired.
   */
  public void setPrepend (@Nullable final String prepend)
  {
    m_sPrepend = StringHelper.trim (prepend);
  }

  /**
   * A String of text to postpend (add to the end) to the instanceId found in
   * the system property.
   */
  @Nullable
  public String getPostpend ()
  {
    return m_sPostpend;
  }

  /**
   * A String of text to postpend (add to the end) to the instanceId found in
   * the system property.
   *
   * @param postpend
   *        the value to postpend, or null if none is desired.
   */
  public void setPostpend (@Nullable final String postpend)
  {
    m_sPostpend = StringHelper.trim (postpend);
  }

  /**
   * The name of the system property from which to obtain the instanceId.
   * Defaults to {@link #SYSTEM_PROPERTY}.
   */
  @Nonnull
  public String getSystemPropertyName ()
  {
    return m_sSystemPropertyName;
  }

  /**
   * The name of the system property from which to obtain the instanceId.
   * Defaults to {@link #SYSTEM_PROPERTY}.
   *
   * @param systemPropertyName
   *        the system property name
   */
  public void setSystemPropertyName (@Nullable final String systemPropertyName)
  {
    m_sSystemPropertyName = systemPropertyName == null ? SYSTEM_PROPERTY : systemPropertyName.trim ();
  }
}
