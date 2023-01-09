/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2023 Philip Helger (www.helger.com)
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.compare.IComparable;
import com.helger.commons.hashcode.HashCodeGenerator;

/**
 * <p>
 * Object representing a job or trigger key.
 * </p>
 *
 * @author <a href="mailto:jeff@binaryfeed.org">Jeffrey Wescott</a>
 */
@Immutable
public class Key <T> implements IComparable <Key <T>>
{
  /**
   * The default group for scheduling entities, with the value "DEFAULT".
   */
  public static final String DEFAULT_GROUP = "DEFAULT";

  private final String m_sName;
  private final String m_sGroup;

  /**
   * Construct a new key with the given name and group.
   *
   * @param name
   *        the name
   * @param group
   *        the group
   */
  public Key (@Nonnull final String name, @Nullable final String group)
  {
    ValueEnforcer.notNull (name, "Name");
    m_sName = name;
    if (group != null)
      m_sGroup = group;
    else
      m_sGroup = DEFAULT_GROUP;
  }

  /**
   * Get the name portion of the key.
   *
   * @return the name
   */
  @Nonnull
  public final String getName ()
  {
    return m_sName;
  }

  /**
   * Get the group portion of the key.
   *
   * @return the group
   */
  @Nonnull
  public final String getGroup ()
  {
    return m_sGroup;
  }

  /**
   * Return the string representation of the key. The format will be:
   * &lt;group&gt;.&lt;name&gt;.
   *
   * @return the string representation of the key
   */
  public final String getAsString ()
  {
    return getGroup () + '.' + getName ();
  }

  /**
   * <p>
   * Return the string representation of the key. The format will be:
   * &lt;group&gt;.&lt;name&gt;.
   * </p>
   *
   * @return the string representation of the key
   */
  @Override
  public String toString ()
  {
    return getAsString ();
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName).append (m_sGroup).getHashCode ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (this == o)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final Key <?> rhs = (Key <?>) o;
    return m_sName.equals (rhs.m_sName) && m_sGroup.equals (rhs.m_sGroup);
  }

  public int compareTo (final Key <T> o)
  {
    if (m_sGroup.equals (DEFAULT_GROUP) && !o.m_sGroup.equals (DEFAULT_GROUP))
      return -1;
    if (!m_sGroup.equals (DEFAULT_GROUP) && o.m_sGroup.equals (DEFAULT_GROUP))
      return 1;

    int ret = m_sGroup.compareTo (o.getGroup ());
    if (ret == 0)
      ret = m_sName.compareTo (o.getName ());
    return ret;
  }

  @Nonnull
  public static String createUniqueName (final String sGroup)
  {
    final String n1 = UUID.randomUUID ().toString ();
    final String n2 = UUID.nameUUIDFromBytes ((sGroup != null ? sGroup
                                                              : DEFAULT_GROUP).getBytes (StandardCharsets.ISO_8859_1))
                          .toString ();

    return n2.substring (24) + "-" + n1;
  }
}
