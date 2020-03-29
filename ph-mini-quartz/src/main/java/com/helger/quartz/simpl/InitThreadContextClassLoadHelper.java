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

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

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
  private WeakReference <ClassLoader> m_aInitClassLoader;

  /**
   * Called to give the ClassLoadHelper a chance to initialize itself, including
   * the opportunity to "steal" the class loader off of the calling thread,
   * which is the thread that is initializing Quartz.
   */
  @Override
  public void initialize ()
  {
    m_aInitClassLoader = new WeakReference <> (Thread.currentThread ().getContextClassLoader ());
  }

  /**
   * Enable sharing of the class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  @Nonnull
  public ClassLoader getClassLoader ()
  {
    final ClassLoader ret = m_aInitClassLoader.get ();
    if (ret == null)
      throw new IllegalStateException ("Classload vanished :(");
    return ret;
  }
}
