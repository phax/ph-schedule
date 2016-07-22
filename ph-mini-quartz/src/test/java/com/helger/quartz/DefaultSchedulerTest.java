package com.helger.quartz;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.helger.quartz.IScheduler;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.impl.JobDetail;
import com.helger.quartz.impl.StdSchedulerFactory;

/**
 * DefaultSchedulerTest
 */
public class DefaultSchedulerTest
{
  @Test
  public void testAddJobNoTrigger () throws Exception
  {
    final IScheduler scheduler = StdSchedulerFactory.getDefaultScheduler ();
    final JobDetail jobDetail = new JobDetail ();
    jobDetail.setName ("testjob");

    try
    {
      scheduler.addJob (jobDetail, false);
    }
    catch (final SchedulerException e)
    {
      assertThat (e.getMessage (), containsString ("durable"));
    }

    jobDetail.setDurability (true);
    scheduler.addJob (jobDetail, false);
  }
}
