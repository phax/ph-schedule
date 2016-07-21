package com.helger.quartz;

import java.util.Properties;

import com.helger.quartz.impl.StdSchedulerFactory;
import com.helger.quartz.simpl.SimpleThreadPool;

public class RAMSchedulerTest extends AbstractSchedulerTest
{

  @Override
  protected IScheduler createScheduler (final String name, final int threadPoolSize) throws SchedulerException
  {
    final Properties config = new Properties ();
    config.setProperty ("org.quartz.scheduler.instanceName", name + "Scheduler");
    config.setProperty ("org.quartz.scheduler.instanceId", "AUTO");
    config.setProperty ("org.quartz.threadPool.threadCount", Integer.toString (threadPoolSize));
    config.setProperty ("org.quartz.threadPool.class", SimpleThreadPool.class.getName ());
    return new StdSchedulerFactory (config).getScheduler ();
  }
}
