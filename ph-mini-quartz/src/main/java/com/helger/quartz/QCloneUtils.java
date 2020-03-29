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
