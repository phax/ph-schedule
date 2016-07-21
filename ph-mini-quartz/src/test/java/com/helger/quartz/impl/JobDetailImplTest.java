package com.helger.quartz.impl;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.helger.quartz.impl.JobDetailImpl;

public class JobDetailImplTest
{
  @Test
  public void testHashCode ()
  {
    final JobDetailImpl job = new JobDetailImpl ();
    Assert.assertThat (job.hashCode (), Matchers.is (0));

    job.setName ("test");
    Assert.assertThat (job.hashCode (), Matchers.not (Matchers.is (0)));

    job.setGroup ("test");
    Assert.assertThat (job.hashCode (), Matchers.not (Matchers.is (0)));
  }
}
