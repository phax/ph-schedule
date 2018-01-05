/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.quartz.plugins.history;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.IJobListener;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobExecutionException;
import com.helger.quartz.SchedulerConfigException;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.impl.matchers.EverythingMatcher;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.ISchedulerPlugin;

/**
 * Logs a history of all job executions (and execution vetos) via the Jakarta
 * Commons-Logging framework.
 * <p>
 * The logged message is customizable by setting one of the following message
 * properties to a String that conforms to the syntax of
 * <code>java.util.MessageFormat</code>.
 * </p>
 * <p>
 * JobToBeFiredMessage - available message data are:
 * </p>
 * <table summary="">
 * <tr>
 * <th>Element</th>
 * <th>Data Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>0</td>
 * <td>String</td>
 * <td>The Job's Name.</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>String</td>
 * <td>The Job's Group.</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Date</td>
 * <td>The current time.</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>String</td>
 * <td>The Trigger's name.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>String</td>
 * <td>The Triggers's group.</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Date</td>
 * <td>The scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>Date</td>
 * <td>The next scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>The re-fire count from the JobExecutionContext.</td>
 * </tr>
 * </table>
 * <p>
 * The default message text is <i>"Job {1}.{0} fired (by trigger {4}.{3}) at:
 * {2, date, HH:mm:ss MM/dd/yyyy}"</i>
 * </p>
 * <p>
 * JobSuccessMessage - available message data are:
 * </p>
 * <table summary="">
 * <tr>
 * <th>Element</th>
 * <th>Data Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>0</td>
 * <td>String</td>
 * <td>The Job's Name.</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>String</td>
 * <td>The Job's Group.</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Date</td>
 * <td>The current time.</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>String</td>
 * <td>The Trigger's name.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>String</td>
 * <td>The Triggers's group.</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Date</td>
 * <td>The scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>Date</td>
 * <td>The next scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>The re-fire count from the JobExecutionContext.</td>
 * </tr>
 * <tr>
 * <td>8</td>
 * <td>Object</td>
 * <td>The string value (toString() having been called) of the result (if any)
 * that the Job set on the JobExecutionContext, with on it. "NULL" if no result
 * was set.</td>
 * </tr>
 * </table>
 * <p>
 * The default message text is <i>"Job {1}.{0} execution complete at {2, date,
 * HH:mm:ss MM/dd/yyyy} and reports: {8}"</i>
 * </p>
 * <p>
 * JobFailedMessage - available message data are:
 * </p>
 * <table summary="">
 * <tr>
 * <th>Element</th>
 * <th>Data Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>0</td>
 * <td>String</td>
 * <td>The Job's Name.</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>String</td>
 * <td>The Job's Group.</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Date</td>
 * <td>The current time.</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>String</td>
 * <td>The Trigger's name.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>String</td>
 * <td>The Triggers's group.</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Date</td>
 * <td>The scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>Date</td>
 * <td>The next scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>The re-fire count from the JobExecutionContext.</td>
 * </tr>
 * <tr>
 * <td>8</td>
 * <td>String</td>
 * <td>The message from the thrown JobExecution Exception.</td>
 * </tr>
 * </table>
 * <p>
 * The default message text is <i>"Job {1}.{0} execution failed at {2, date,
 * HH:mm:ss MM/dd/yyyy} and reports: {8}"</i>
 * </p>
 * <p>
 * JobWasVetoedMessage - available message data are:
 * </p>
 * <table summary="">
 * <tr>
 * <th>Element</th>
 * <th>Data Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>0</td>
 * <td>String</td>
 * <td>The Job's Name.</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>String</td>
 * <td>The Job's Group.</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Date</td>
 * <td>The current time.</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>String</td>
 * <td>The Trigger's name.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>String</td>
 * <td>The Triggers's group.</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>Date</td>
 * <td>The scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>Date</td>
 * <td>The next scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>The re-fire count from the JobExecutionContext.</td>
 * </tr>
 * </table>
 * <p>
 * The default message text is <i>"Job {1}.{0} was vetoed. It was to be fired
 * (by trigger {4}.{3}) at: {2, date, HH:mm:ss MM/dd/yyyy}"</i>
 * </p>
 *
 * @author James House
 */
public class LoggingJobHistoryPlugin implements ISchedulerPlugin, IJobListener
{
  private String name;
  private String jobToBeFiredMessage = "Job {1}.{0} fired (by trigger {4}.{3}) at: {2, date, HH:mm:ss MM/dd/yyyy}";
  private String jobSuccessMessage = "Job {1}.{0} execution complete at {2, date, HH:mm:ss MM/dd/yyyy} and reports: {8}";
  private String jobFailedMessage = "Job {1}.{0} execution failed at {2, date, HH:mm:ss MM/dd/yyyy} and reports: {8}";
  private String jobWasVetoedMessage = "Job {1}.{0} was vetoed.  It was to be fired (by trigger {4}.{3}) at: {2, date, HH:mm:ss MM/dd/yyyy}";
  private static final Logger log = LoggerFactory.getLogger (LoggingJobHistoryPlugin.class);

  public LoggingJobHistoryPlugin ()
  {}

  protected Logger getLog ()
  {
    return log;
  }

  /**
   * Get the message that is logged when a Job successfully completes its
   * execution.
   */
  public String getJobSuccessMessage ()
  {
    return jobSuccessMessage;
  }

  /**
   * Get the message that is logged when a Job fails its execution.
   */
  public String getJobFailedMessage ()
  {
    return jobFailedMessage;
  }

  /**
   * Get the message that is logged when a Job is about to execute.
   */
  public String getJobToBeFiredMessage ()
  {
    return jobToBeFiredMessage;
  }

  /**
   * Set the message that is logged when a Job successfully completes its
   * execution.
   *
   * @param jobSuccessMessage
   *        String in java.text.MessageFormat syntax.
   */
  public void setJobSuccessMessage (final String jobSuccessMessage)
  {
    this.jobSuccessMessage = jobSuccessMessage;
  }

  /**
   * Set the message that is logged when a Job fails its execution.
   *
   * @param jobFailedMessage
   *        String in java.text.MessageFormat syntax.
   */
  public void setJobFailedMessage (final String jobFailedMessage)
  {
    this.jobFailedMessage = jobFailedMessage;
  }

  /**
   * Set the message that is logged when a Job is about to execute.
   *
   * @param jobToBeFiredMessage
   *        String in java.text.MessageFormat syntax.
   */
  public void setJobToBeFiredMessage (final String jobToBeFiredMessage)
  {
    this.jobToBeFiredMessage = jobToBeFiredMessage;
  }

  /**
   * Get the message that is logged when a Job execution is vetoed by a trigger
   * listener.
   */
  public String getJobWasVetoedMessage ()
  {
    return jobWasVetoedMessage;
  }

  /**
   * Set the message that is logged when a Job execution is vetoed by a trigger
   * listener.
   *
   * @param jobWasVetoedMessage
   *        String in java.text.MessageFormat syntax.
   */
  public void setJobWasVetoedMessage (final String jobWasVetoedMessage)
  {
    this.jobWasVetoedMessage = jobWasVetoedMessage;
  }

  /**
   * <p>
   * Called during creation of the <code>Scheduler</code> in order to give the
   * <code>SchedulerPlugin</code> a chance to initialize.
   * </p>
   *
   * @throws SchedulerConfigException
   *         if there is an error initializing.
   */
  public void initialize (final String pname,
                          final IScheduler scheduler,
                          final IClassLoadHelper classLoadHelper) throws SchedulerException
  {
    this.name = pname;
    scheduler.getListenerManager ().addJobListener (this, EverythingMatcher.allJobs ());
  }

  public void start ()
  {
    // do nothing...
  }

  /**
   * <p>
   * Called in order to inform the <code>SchedulerPlugin</code> that it should
   * free up all of it's resources because the scheduler is shutting down.
   * </p>
   */
  public void shutdown ()
  {
    // nothing to do...
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * JobListener Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /*
   * Object[] arguments = { new Integer(7), new
   * Date(System.currentTimeMillis()), "a disturbance in the Force" }; String
   * result = MessageFormat.format( "At {1,time} on {1,date}, there was {2} on
   * planet {0,number,integer}.", arguments);
   */

  public String getName ()
  {
    return name;
  }

  /**
   * @see com.helger.quartz.IJobListener#jobToBeExecuted(IJobExecutionContext)
   */
  public void jobToBeExecuted (final IJobExecutionContext context)
  {
    if (!getLog ().isInfoEnabled ())
    {
      return;
    }

    final ITrigger trigger = context.getTrigger ();

    final Object [] args = { context.getJobDetail ().getKey ().getName (),
                             context.getJobDetail ().getKey ().getGroup (),
                             new Date (),
                             trigger.getKey ().getName (),
                             trigger.getKey ().getGroup (),
                             trigger.getPreviousFireTime (),
                             trigger.getNextFireTime (),
                             Integer.valueOf (context.getRefireCount ()) };

    getLog ().info (new MessageFormat (getJobToBeFiredMessage (), Locale.US).format (args));
  }

  /**
   * @see com.helger.quartz.IJobListener#jobWasExecuted(IJobExecutionContext,
   *      JobExecutionException)
   */
  public void jobWasExecuted (final IJobExecutionContext context, final JobExecutionException jobException)
  {

    final ITrigger trigger = context.getTrigger ();

    Object [] args = null;

    if (jobException != null)
    {
      if (!getLog ().isWarnEnabled ())
      {
        return;
      }

      final String errMsg = jobException.getMessage ();
      args = new Object [] { context.getJobDetail ().getKey ().getName (),
                             context.getJobDetail ().getKey ().getGroup (),
                             new Date (),
                             trigger.getKey ().getName (),
                             trigger.getKey ().getGroup (),
                             trigger.getPreviousFireTime (),
                             trigger.getNextFireTime (),
                             Integer.valueOf (context.getRefireCount ()),
                             errMsg };

      getLog ().warn (new MessageFormat (getJobFailedMessage (), Locale.US).format (args), jobException);
    }
    else
    {
      if (!getLog ().isInfoEnabled ())
      {
        return;
      }

      final String result = String.valueOf (context.getResult ());
      args = new Object [] { context.getJobDetail ().getKey ().getName (),
                             context.getJobDetail ().getKey ().getGroup (),
                             new Date (),
                             trigger.getKey ().getName (),
                             trigger.getKey ().getGroup (),
                             trigger.getPreviousFireTime (),
                             trigger.getNextFireTime (),
                             Integer.valueOf (context.getRefireCount ()),
                             result };

      getLog ().info (new MessageFormat (getJobSuccessMessage (), Locale.US).format (args));
    }
  }

  /**
   * @see com.helger.quartz.IJobListener#jobExecutionVetoed(com.helger.quartz.IJobExecutionContext)
   */
  public void jobExecutionVetoed (final IJobExecutionContext context)
  {

    if (!getLog ().isInfoEnabled ())
    {
      return;
    }

    final ITrigger trigger = context.getTrigger ();

    final Object [] args = { context.getJobDetail ().getKey ().getName (),
                             context.getJobDetail ().getKey ().getGroup (),
                             new Date (),
                             trigger.getKey ().getName (),
                             trigger.getKey ().getGroup (),
                             trigger.getPreviousFireTime (),
                             trigger.getNextFireTime (),
                             Integer.valueOf (context.getRefireCount ()) };

    getLog ().info (new MessageFormat (getJobWasVetoedMessage (), Locale.US).format (args));
  }

}

// EOF
