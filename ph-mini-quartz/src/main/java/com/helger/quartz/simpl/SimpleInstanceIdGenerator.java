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

import java.net.InetAddress;

import com.helger.quartz.SchedulerException;
import com.helger.quartz.spi.IInstanceIdGenerator;

/**
 * The default InstanceIdGenerator used by Quartz when instance id is to be
 * automatically generated. Instance id is of the form HOSTNAME + CURRENT_TIME.
 *
 * @see IInstanceIdGenerator
 * @see HostnameInstanceIdGenerator
 */
public class SimpleInstanceIdGenerator implements IInstanceIdGenerator
{
  public String generateInstanceId () throws SchedulerException
  {
    try
    {
      return InetAddress.getLocalHost ().getHostName () + System.currentTimeMillis ();
    }
    catch (final Exception e)
    {
      throw new SchedulerException ("Couldn't get host name!", e);
    }
  }
}
