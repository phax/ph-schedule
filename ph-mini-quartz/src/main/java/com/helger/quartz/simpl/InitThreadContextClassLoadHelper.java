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
package com.helger.quartz.simpl;

import java.io.InputStream;
import java.net.URL;

import com.helger.quartz.spi.IClassLoadHelper;

/**
 * A <code>ClassLoadHelper</code> that uses either the context class loader of
 * the thread that initialized Quartz.
 *
 * @see com.helger.quartz.spi.IClassLoadHelper
 * @see com.helger.quartz.simpl.ThreadContextClassLoadHelper
 * @see com.helger.quartz.simpl.SimpleClassLoadHelper
 * @see com.helger.quartz.simpl.CascadingClassLoadHelper
 * @see com.helger.quartz.simpl.LoadingLoaderClassLoadHelper
 * @author jhouse
 * @author pl47ypus
 */
public class InitThreadContextClassLoadHelper implements IClassLoadHelper
{

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private ClassLoader initClassLoader;

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
    initClassLoader = Thread.currentThread ().getContextClassLoader ();
  }

  /**
   * Return the class with the given name.
   */
  public Class <?> loadClass (final String name) throws ClassNotFoundException
  {
    return initClassLoader.loadClass (name);
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
    return initClassLoader.getResource (name);
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
    return initClassLoader.getResourceAsStream (name);
  }

  /**
   * Enable sharing of the class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  public ClassLoader getClassLoader ()
  {
    return this.initClassLoader;
  }
}
