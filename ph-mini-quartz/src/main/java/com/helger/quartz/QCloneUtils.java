/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2024 Philip Helger (www.helger.com)
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
package com.helger.quartz;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.TimeZone;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.lang.ICloneable;

/**
 * Internal clone helper class.
 *
 * @author Philip Helger
 */
@Immutable
public final class QCloneUtils
{
  private QCloneUtils ()
  {}

  @Nullable
  public static <T extends ICloneable <T>> T getClone (@Nullable final T x)
  {
    return x == null ? null : x.getClone ();
  }

  @Nullable
  public static JobDataMap getClone (@Nullable final JobDataMap x)
  {
    return x == null ? null : x.getClone ();
  }

  @Nullable
  public static <T extends Enum <T>> EnumSet <T> getClone (@Nullable final EnumSet <T> x)
  {
    return x == null ? null : EnumSet.copyOf (x);
  }

  @Nullable
  public static Calendar getClone (@Nullable final Calendar x)
  {
    return x == null ? null : (Calendar) x.clone ();
  }

  @Nullable
  public static Date getClone (@Nullable final Date x)
  {
    return x == null ? null : (Date) x.clone ();
  }

  @Nullable
  public static TimeZone getClone (@Nullable final TimeZone x)
  {
    return x == null ? null : (TimeZone) x.clone ();
  }
}
