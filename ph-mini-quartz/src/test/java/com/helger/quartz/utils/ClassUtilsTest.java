package com.helger.quartz.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.PersistJobDataAfterExecution;
import com.helger.quartz.utils.ClassUtils;

/**
 * @author Alex Snaps
 */
public class ClassUtilsTest
{
  @Test
  public void testIsAnnotationPresentOnSuperClass () throws Exception
  {
    assertTrue (ClassUtils.isAnnotationPresent (BaseJob.class, DisallowConcurrentExecution.class));
    assertFalse (ClassUtils.isAnnotationPresent (BaseJob.class, PersistJobDataAfterExecution.class));
    assertTrue (ClassUtils.isAnnotationPresent (ExtendedJob.class, DisallowConcurrentExecution.class));
    assertFalse (ClassUtils.isAnnotationPresent (ExtendedJob.class, PersistJobDataAfterExecution.class));
    assertTrue (ClassUtils.isAnnotationPresent (ReallyExtendedJob.class, DisallowConcurrentExecution.class));
    assertTrue (ClassUtils.isAnnotationPresent (ReallyExtendedJob.class, PersistJobDataAfterExecution.class));
  }

  @DisallowConcurrentExecution
  private static class BaseJob implements IJob
  {
    public void execute (final IJobExecutionContext context) throws JobExecutionException
    {
      System.out.println (this.getClass ().getSimpleName ());
    }
  }

  private static class ExtendedJob extends BaseJob
  {}

  @PersistJobDataAfterExecution
  private static class ReallyExtendedJob extends ExtendedJob
  {

  }
}
