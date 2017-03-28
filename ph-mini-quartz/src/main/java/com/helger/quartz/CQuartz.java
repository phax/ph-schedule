package com.helger.quartz;

import javax.annotation.concurrent.Immutable;

import com.helger.commons.datetime.PDTFactory;

/**
 * Quartz constants
 * 
 * @author Philip Helger
 */
@Immutable
public final class CQuartz
{
  public static final int MAX_YEAR = PDTFactory.getCurrentYear () + 100;

  private CQuartz ()
  {}
}
