package com.helger.quartz.impl;

import com.helger.quartz.spi.ThreadExecutor;

/**
 * Schedules work on a newly spawned thread. This is the default Quartz
 * behavior.
 *
 * @author matt.accola
 * @version $Revision$ $Date$
 */
public class DefaultThreadExecutor implements ThreadExecutor
{

  public void initialize ()
  {}

  public void execute (final Thread thread)
  {
    thread.start ();
  }

}
