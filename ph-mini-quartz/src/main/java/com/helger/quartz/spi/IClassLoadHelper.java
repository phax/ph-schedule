/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2018 Philip Helger (www.helger.com)
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
package com.helger.quartz.spi;

import java.io.InputStream;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An interface for classes wishing to provide the service of loading classes
 * and resources within the scheduler...
 *
 * @author jhouse
 * @author pl47ypus
 */
public interface IClassLoadHelper
{
  /**
   * Called to give the ClassLoadHelper a chance to initialize itself, including
   * the opportunity to "steal" the class loader off of the calling thread,
   * which is the thread that is initializing Quartz.
   */
  default void initialize ()
  {}

  /**
   * Return the class with the given name.
   *
   * @param sClassName
   *        the fqcn of the class to load.
   * @return the requested class.
   * @throws ClassNotFoundException
   *         if the class can be found in the classpath.
   */
  default Class <?> loadClass (final String sClassName) throws ClassNotFoundException
  {
    return getClassLoader ().loadClass (sClassName);
  }

  /**
   * Return the class of the given type with the given name.
   *
   * @param name
   *        the fqcn of the class to load.
   * @param dummy
   *        For casting.
   * @return the requested class.
   * @throws ClassNotFoundException
   *         if the class can be found in the classpath.
   */
  @SuppressWarnings ("unchecked")
  default <T> Class <? extends T> loadClass (final String name, final Class <T> dummy) throws ClassNotFoundException
  {
    return (Class <? extends T>) loadClass (name);
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource
   * with this name is found.
   *
   * @param sName
   *        name of the desired resource
   * @return a java.net.URL object
   */
  @Nullable
  default URL getResource (final String sName)
  {
    return getClassLoader ().getResource (sName);
  }

  /**
   * Finds a resource with a given name. This method returns null if no resource
   * with this name is found.
   *
   * @param sName
   *        name of the desired resource
   * @return a java.io.InputStream object
   */
  @Nullable
  default InputStream getResourceAsStream (final String sName)
  {
    return getClassLoader ().getResourceAsStream (sName);
  }

  /**
   * Enable sharing of the class-loader with 3rd party (e.g. digester).
   *
   * @return the class-loader user be the helper.
   */
  @Nonnull
  ClassLoader getClassLoader ();
}
