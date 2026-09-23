/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;

import com.helger.quartz.spi.IClassLoadHelper;

/**
 * A <code>ClassLoadHelper</code> that simply calls <code>Class.forName(..)</code>.
 *
 * @see com.helger.quartz.spi.IClassLoadHelper
 * @see com.helger.quartz.simpl.ThreadContextClassLoadHelper
 * @see com.helger.quartz.simpl.CascadingClassLoadHelper
 * @see com.helger.quartz.simpl.LoadingLoaderClassLoadHelper
 * @author jhouse
 * @author pl47ypus
 */
public class SimpleClassLoadHelper implements IClassLoadHelper
{
  /**
   * Called to give the ClassLoadHelper a chance to initialize itself, including the opportunity to
   * "steal" the class loader off of the calling thread, which is the thread that is initializing
   * Quartz.
   */
  @Override
  public void initialize ()
  {}

  /**
   * Return the class with the given name.
   */
  @Override
  @NonNull
  public Class <?> loadClass (@NonNull final String name) throws ClassNotFoundException
  {
    return Class.forName (name);
  }

  /**
   * Enable sharing of the class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  @NonNull
  public ClassLoader getClassLoader ()
  {
    // Note: up to v6.1.1 this method tried to reflectively call the JVM internal method
    // "ClassLoader.getCallerClassLoader" first. That method was removed from the JDK years ago, so
    // the reflective lookup always failed and this class-loader was used anyway.
    return getClass ().getClassLoader ();
  }
}
