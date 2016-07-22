package com.helger.quartz.impl;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.helger.quartz.impl.JobDetail;

public class JobDetailTest
{
  @Test
  public void testHashCode ()
  {
    final JobDetail job = new JobDetail ();
    Assert.assertThat (job.hashCode (), Matchers.is (0));

    job.setName ("test");
    Assert.assertThat (job.hashCode (), Matchers.not (Matchers.is (0)));

    job.setGroup ("test");
    Assert.assertThat (job.hashCode (), Matchers.not (Matchers.is (0)));
  }
}
