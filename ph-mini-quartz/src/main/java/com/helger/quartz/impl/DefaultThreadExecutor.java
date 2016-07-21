package com.helger.quartz.impl;

import com.helger.quartz.spi.IThreadExecutor;

/**
 * Schedules work on a newly spawned thread. This is the default Quartz
 * behavior.
 *
 * @author matt.accola
 * @version $Revision$ $Date$
 */
public class DefaultThreadExecutor implements IThreadExecutor
{

  public void initialize ()
  {}

  public void execute (final Thread thread)
  {
    thread.start ();
  }

}
