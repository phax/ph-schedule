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
package com.helger.quartz.simpl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

import com.helger.base.CGlobal;
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
  public Class <?> loadClass (final String name) throws ClassNotFoundException
  {
    return Class.forName (name);
  }

  /**
   * Enable sharing of the class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  public ClassLoader getClassLoader ()
  {
    // To follow the same behavior of Class.forName(...) I had to play
    // dirty (Supported by Sun, IBM & BEA JVMs)
    try
    {
      // Get a reference to this class' class-loader
      final ClassLoader cl = getClass ().getClassLoader ();

      // Create a method instance representing the protected
      // getCallerClassLoader method of class ClassLoader
      final Method mthd = ClassLoader.class.getDeclaredMethod ("getCallerClassLoader", CGlobal.EMPTY_CLASS_ARRAY);
      if (false)
      {
        // Make the method accessible.
        AccessibleObject.setAccessible (new AccessibleObject [] { mthd }, true);
      }
      // Try to get the caller's class-loader
      return (ClassLoader) mthd.invoke (cl, CGlobal.EMPTY_OBJECT_ARRAY);
    }
    catch (final Exception ex)
    {
      // Use this class' class-loader
      return getClass ().getClassLoader ();
    }
  }
}
