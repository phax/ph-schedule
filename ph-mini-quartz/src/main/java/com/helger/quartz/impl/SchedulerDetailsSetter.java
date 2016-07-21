package com.helger.quartz.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.SchedulerException;

/**
 * This utility calls methods reflectively on the given objects even though the
 * methods are likely on a proper interface (ThreadPool, JobStore, etc). The
 * motivation is to be tolerant of older implementations that have not been
 * updated for the changes in the interfaces (eg. LocalTaskExecutorThreadPool in
 * spring quartz helpers)
 *
 * @author teck
 */
class SchedulerDetailsSetter
{

  private static final Logger LOGGER = LoggerFactory.getLogger (SchedulerDetailsSetter.class);

  private SchedulerDetailsSetter ()
  {
    //
  }

  static void setDetails (final Object target,
                          final String schedulerName,
                          final String schedulerId) throws SchedulerException
  {
    set (target, "setInstanceName", schedulerName);
    set (target, "setInstanceId", schedulerId);
  }

  private static void set (final Object target, final String method, final String value) throws SchedulerException
  {
    final Method setter;

    try
    {
      setter = target.getClass ().getMethod (method, String.class);
    }
    catch (final SecurityException e)
    {
      LOGGER.error ("A SecurityException occured: " + e.getMessage (), e);
      return;
    }
    catch (final NoSuchMethodException e)
    {
      // This probably won't happen since the interface has the method
      LOGGER.warn (target.getClass ().getName () + " does not contain public method " + method + "(String)");
      return;
    }

    if (Modifier.isAbstract (setter.getModifiers ()))
    {
      // expected if method not implemented (but is present on
      // interface)
      LOGGER.warn (target.getClass ().getName () + " does not implement " + method + "(String)");
      return;
    }

    try
    {
      setter.invoke (target, value);
    }
    catch (final InvocationTargetException ite)
    {
      throw new SchedulerException (ite.getTargetException ());
    }
    catch (final Exception e)
    {
      throw new SchedulerException (e);
    }
  }

}
