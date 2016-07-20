package org.quartz;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;

import junit.framework.TestCase;

/**
 * DefaultSchedulerTest
 */
public class DefaultSchedulerTest extends TestCase
{

  public void testAddJobNoTrigger () throws Exception
  {
    final Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler ();
    final JobDetailImpl jobDetail = new JobDetailImpl ();
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
