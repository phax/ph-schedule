package com.helger.quartz.core;

public interface ISampledStatistics
{
  long getJobsScheduledMostRecentSample ();

  long getJobsExecutingMostRecentSample ();

  long getJobsCompletedMostRecentSample ();

  void shutdown ();
}
