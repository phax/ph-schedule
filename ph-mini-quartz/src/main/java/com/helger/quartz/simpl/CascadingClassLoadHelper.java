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

package com.helger.quartz.simpl;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
  private List <IClassLoadHelper> loadHelpers;
  private IClassLoadHelper bestCandidate;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Called to give the ClassLoadHelper a chance to initialize itself, including
   * the opportunity to "steal" the class loader off of the calling thread,
   * which is the thread that is initializing Quartz.
   */
  public void initialize ()
  {
    loadHelpers = new ArrayList<> ();
    loadHelpers.add (new LoadingLoaderClassLoadHelper ());
    loadHelpers.add (new SimpleClassLoadHelper ());
    loadHelpers.add (new ThreadContextClassLoadHelper ());
    loadHelpers.add (new InitThreadContextClassLoadHelper ());

    for (final IClassLoadHelper loadHelper : loadHelpers)
    {
      loadHelper.initialize ();
    }
  }

  /**
   * Return the class with the given name.
   */
  public Class <?> loadClass (final String name) throws ClassNotFoundException
  {

    if (bestCandidate != null)
    {
      try
      {
        return bestCandidate.loadClass (name);
      }
      catch (final Throwable t)
      {
        bestCandidate = null;
      }
    }

    Throwable throwable = null;
    Class <?> clazz = null;
    IClassLoadHelper loadHelper = null;

    final Iterator <IClassLoadHelper> iter = loadHelpers.iterator ();
    while (iter.hasNext ())
    {
      loadHelper = iter.next ();

      try
      {
        clazz = loadHelper.loadClass (name);
        break;
      }
      catch (final Throwable t)
      {
        throwable = t;
      }
    }

    if (clazz == null)
    {
      if (throwable instanceof ClassNotFoundException)
      {
        throw (ClassNotFoundException) throwable;
      }
      throw new ClassNotFoundException (String.format ("Unable to load class %s by any known loaders.", name),
                                        throwable);
    }

    bestCandidate = loadHelper;

    return clazz;
  }

  @SuppressWarnings ("unchecked")
  public <T> Class <? extends T> loadClass (final String name, final Class <T> clazz) throws ClassNotFoundException
  {
    return (Class <? extends T>) loadClass (name);
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource
   * with this name is found.
   *
   * @param name
   *        name of the desired resource
   * @return a java.net.URL object
   */
  public URL getResource (final String name)
  {

    URL result = null;

    if (bestCandidate != null)
    {
      result = bestCandidate.getResource (name);
      if (result == null)
      {
        bestCandidate = null;
      }
      else
      {
        return result;
      }
    }

    IClassLoadHelper loadHelper = null;

    final Iterator <IClassLoadHelper> iter = loadHelpers.iterator ();
    while (iter.hasNext ())
    {
      loadHelper = iter.next ();

      result = loadHelper.getResource (name);
      if (result != null)
      {
        break;
      }
    }

    bestCandidate = loadHelper;
    return result;
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource
   * with this name is found.
   *
   * @param name
   *        name of the desired resource
   * @return a java.io.InputStream object
   */
  public InputStream getResourceAsStream (final String name)
  {

    InputStream result = null;

    if (bestCandidate != null)
    {
      result = bestCandidate.getResourceAsStream (name);
      if (result == null)
      {
        bestCandidate = null;
      }
      else
      {
        return result;
      }
    }

    IClassLoadHelper loadHelper = null;

    final Iterator <IClassLoadHelper> iter = loadHelpers.iterator ();
    while (iter.hasNext ())
    {
      loadHelper = iter.next ();

      result = loadHelper.getResourceAsStream (name);
      if (result != null)
      {
        break;
      }
    }

    bestCandidate = loadHelper;
    return result;
  }

  /**
   * Enable sharing of the "best" class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  public ClassLoader getClassLoader ()
  {
    return (this.bestCandidate == null) ? Thread.currentThread ().getContextClassLoader ()
                                        : this.bestCandidate.getClassLoader ();
  }

}
