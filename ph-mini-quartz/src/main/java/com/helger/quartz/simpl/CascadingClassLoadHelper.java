/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2020 Philip Helger (www.helger.com)
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

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.spi.IClassLoadHelper;

/**
 * A <code>ClassLoadHelper</code> uses all of the <code>ClassLoadHelper</code>
 * types that are found in this package in its attempts to load a class, when
 * one scheme is found to work, it is promoted to the scheme that will be used
 * first the next time a class is loaded (in order to improve performance).
 * <p>
 * This approach is used because of the wide variance in class loader behavior
 * between the various environments in which Quartz runs (e.g. disparate
 * application servers, stand-alone, mobile devices, etc.). Because of this
 * disparity, Quartz ran into difficulty with a one class-load style fits-all
 * design. Thus, this class loader finds the approach that works, then
 * 'remembers' it.
 * </p>
 *
 * @see com.helger.quartz.spi.IClassLoadHelper
 * @see com.helger.quartz.simpl.LoadingLoaderClassLoadHelper
 * @see com.helger.quartz.simpl.SimpleClassLoadHelper
 * @see com.helger.quartz.simpl.ThreadContextClassLoadHelper
 * @see com.helger.quartz.simpl.InitThreadContextClassLoadHelper
 * @author jhouse
 * @author pl47ypus
 */
public class CascadingClassLoadHelper implements IClassLoadHelper
{
  private ICommonsList <IClassLoadHelper> m_aLoadHelpers;
  private IClassLoadHelper m_aBestCandidate;

  /**
   * Called to give the ClassLoadHelper a chance to initialize itself, including
   * the opportunity to "steal" the class loader off of the calling thread,
   * which is the thread that is initializing Quartz.
   */
  public void initialize ()
  {
    m_aLoadHelpers = new CommonsArrayList <> ();
    m_aLoadHelpers.add (new LoadingLoaderClassLoadHelper ());
    m_aLoadHelpers.add (new SimpleClassLoadHelper ());
    m_aLoadHelpers.add (new ThreadContextClassLoadHelper ());
    m_aLoadHelpers.add (new InitThreadContextClassLoadHelper ());

    for (final IClassLoadHelper aLoadHelper : m_aLoadHelpers)
      aLoadHelper.initialize ();
  }

  /**
   * Return the class with the given name.
   */
  @Nonnull
  public Class <?> loadClass (final String sClassName) throws ClassNotFoundException
  {
    if (m_aBestCandidate != null)
    {
      try
      {
        return m_aBestCandidate.loadClass (sClassName);
      }
      catch (final Throwable t)
      {
        m_aBestCandidate = null;
      }
    }

    Throwable aThrowable = null;
    Class <?> ret = null;
    IClassLoadHelper aLoadHelper = null;

    final Iterator <IClassLoadHelper> iter = m_aLoadHelpers.iterator ();
    while (iter.hasNext ())
    {
      aLoadHelper = iter.next ();
      try
      {
        ret = aLoadHelper.loadClass (sClassName);
        break;
      }
      catch (final Throwable t)
      {
        aThrowable = t;
      }
    }

    if (ret == null)
    {
      if (aThrowable instanceof ClassNotFoundException)
        throw (ClassNotFoundException) aThrowable;
      throw new ClassNotFoundException ("Unable to load class " + sClassName + " by any known loaders.", aThrowable);
    }

    // Remember
    m_aBestCandidate = aLoadHelper;
    return ret;
  }

  @SuppressWarnings ("unchecked")
  @Nonnull
  public <T> Class <? extends T> loadClass (final String sClassName,
                                            final Class <T> dummy) throws ClassNotFoundException
  {
    return (Class <? extends T>) loadClass (sClassName);
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource
   * with this name is found.
   *
   * @param name
   *        name of the desired resource
   * @return a java.net.URL object
   */
  @Nullable
  public URL getResource (final String name)
  {
    URL ret = null;

    if (m_aBestCandidate != null)
    {
      ret = m_aBestCandidate.getResource (name);
      if (ret != null)
        return ret;
      m_aBestCandidate = null;
    }

    IClassLoadHelper aLoadHelper = null;

    final Iterator <IClassLoadHelper> iter = m_aLoadHelpers.iterator ();
    while (iter.hasNext ())
    {
      aLoadHelper = iter.next ();
      ret = aLoadHelper.getResource (name);
      if (ret != null)
        break;
    }

    m_aBestCandidate = aLoadHelper;
    return ret;
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource
   * with this name is found.
   *
   * @param name
   *        name of the desired resource
   * @return a java.io.InputStream object
   */
  @Nullable
  public InputStream getResourceAsStream (final String name)
  {
    InputStream ret = null;
    if (m_aBestCandidate != null)
    {
      ret = m_aBestCandidate.getResourceAsStream (name);
      if (ret != null)
        return ret;
      m_aBestCandidate = null;
    }

    IClassLoadHelper aLoadHelper = null;
    final Iterator <IClassLoadHelper> iter = m_aLoadHelpers.iterator ();
    while (iter.hasNext ())
    {
      aLoadHelper = iter.next ();
      ret = aLoadHelper.getResourceAsStream (name);
      if (ret != null)
        break;
    }

    m_aBestCandidate = aLoadHelper;
    return ret;
  }

  /**
   * Enable sharing of the "best" class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  @Nonnull
  public ClassLoader getClassLoader ()
  {
    return m_aBestCandidate == null ? Thread.currentThread ().getContextClassLoader ()
                                    : m_aBestCandidate.getClassLoader ();
  }
}
