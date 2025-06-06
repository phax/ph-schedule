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

import com.helger.quartz.spi.IClassLoadHelper;

/**
 * A <code>ClassLoadHelper</code> that uses either the loader of it's own class
 * (<code>getClass().getClassLoader().loadClass( .. )</code>).
 *
 * @see com.helger.quartz.spi.IClassLoadHelper
 * @see com.helger.quartz.simpl.InitThreadContextClassLoadHelper
 * @see com.helger.quartz.simpl.SimpleClassLoadHelper
 * @see com.helger.quartz.simpl.CascadingClassLoadHelper
 * @author jhouse
 * @author pl47ypus
 */
public class LoadingLoaderClassLoadHelper implements IClassLoadHelper
{
  /**
   * Enable sharing of the class-loader with 3rd party.
   *
   * @return the class-loader user be the helper.
   */
  public ClassLoader getClassLoader ()
  {
    return getClass ().getClassLoader ();
  }
}
