/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2022 Philip Helger (www.helger.com)
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
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.ITrigger.ECompletedExecutionInstruction;
import com.helger.quartz.ITriggerListener;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.impl.matchers.EverythingMatcher;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.ISchedulerPlugin;

/**
 * Logs a history of all trigger firings via the Jakarta Commons-Logging
 * framework.
 * <p>
 * The logged message is customizable by setting one of the following message
 * properties to a String that conforms to the syntax of
 * <code>java.util.MessageFormat</code>.
 * </p>
 * <p>
 * TriggerFiredMessage - available message data are:
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
 * <td>The Trigger's Name.</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>String</td>
 * <td>The Trigger's Group.</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Date</td>
 * <td>The scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Date</td>
 * <td>The next scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Date</td>
 * <td>The actual fire time.</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>String</td>
 * <td>The Job's name.</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>String</td>
 * <td>The Job's group.</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>The re-fire count from the JobExecutionContext.</td>
 * </tr>
 * </table>
 * <p>
 * The default message text is <i>"Trigger {1}.{0} fired job {6}.{5} at: {4,
 * date, HH:mm:ss MM/dd/yyyy}"</i>
 * </p>
 * <p>
 * TriggerMisfiredMessage - available message data are:
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
 * <td>The Trigger's Name.</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>String</td>
 * <td>The Trigger's Group.</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Date</td>
 * <td>The scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Date</td>
 * <td>The next scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Date</td>
 * <td>The actual fire time. (the time the misfire was detected/handled)</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>String</td>
 * <td>The Job's name.</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>String</td>
 * <td>The Job's group.</td>
 * </tr>
 * </table>
 * <p>
 * The default message text is <i>"Trigger {1}.{0} misfired job {6}.{5} at: {4,
 * date, HH:mm:ss MM/dd/yyyy}. Should have fired at: {3, date, HH:mm:ss
 * MM/dd/yyyy}"</i>
 * </p>
 * <p>
 * TriggerCompleteMessage - available message data are:
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
 * <td>The Trigger's Name.</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>String</td>
 * <td>The Trigger's Group.</td>
 * </tr>
 * <tr>
 * <td>2</td>
 * <td>Date</td>
 * <td>The scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>3</td>
 * <td>Date</td>
 * <td>The next scheduled fire time.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Date</td>
 * <td>The job completion time.</td>
 * </tr>
 * <tr>
 * <td>5</td>
 * <td>String</td>
 * <td>The Job's name.</td>
 * </tr>
 * <tr>
 * <td>6</td>
 * <td>String</td>
 * <td>The Job's group.</td>
 * </tr>
 * <tr>
 * <td>7</td>
 * <td>Integer</td>
 * <td>The re-fire count from the JobExecutionContext.</td>
 * </tr>
 * <tr>
 * <td>8</td>
 * <td>Integer</td>
 * <td>The trigger's resulting instruction code.</td>
 * </tr>
 * <tr>
 * <td>9</td>
 * <td>String</td>
 * <td>A human-readable translation of the trigger's resulting instruction
 * code.</td>
 * </tr>
 * </table>
 * <p>
 * The default message text is <i>"Trigger {1}.{0} completed firing job {6}.{5}
 * at {4, date, HH:mm:ss MM/dd/yyyy} with resulting trigger instruction code:
 * {9}"</i>
 * </p>
 *
 * @author James House
 */
public class LoggingTriggerHistoryPlugin implements ISchedulerPlugin, ITriggerListener
{
  private String m_sName;
  private String m_sTriggerFiredMessage = "Trigger {1}.{0} fired job {6}.{5} at: {4, date, HH:mm:ss MM/dd/yyyy}";
  private String m_sTriggerMisfiredMessage = "Trigger {1}.{0} misfired job {6}.{5}  at: {4, date, HH:mm:ss MM/dd/yyyy}.  Should have fired at: {3, date, HH:mm:ss MM/dd/yyyy}";
  private String m_sTriggerCompleteMessage = "Trigger {1}.{0} completed firing job {6}.{5} at {4, date, HH:mm:ss MM/dd/yyyy} with resulting trigger instruction code: {9}";

  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingTriggerHistoryPlugin.class);

  public LoggingTriggerHistoryPlugin ()
  {}

  /**
   * Get the message that is printed upon the completion of a trigger's firing.
   *
   * @return String
   */
  public String getTriggerCompleteMessage ()
  {
    return m_sTriggerCompleteMessage;
  }

  /**
   * Get the message that is printed upon a trigger's firing.
   *
   * @return String
   */
  public String getTriggerFiredMessage ()
  {
    return m_sTriggerFiredMessage;
  }

  /**
   * Get the message that is printed upon a trigger's mis-firing.
   *
   * @return String
   */
  public String getTriggerMisfiredMessage ()
  {
    return m_sTriggerMisfiredMessage;
  }

  /**
   * Set the message that is printed upon the completion of a trigger's firing.
   *
   * @param triggerCompleteMessage
   *        String in java.text.MessageFormat syntax.
   */
  public void setTriggerCompleteMessage (final String triggerCompleteMessage)
  {
    m_sTriggerCompleteMessage = triggerCompleteMessage;
  }

  /**
   * Set the message that is printed upon a trigger's firing.
   *
   * @param triggerFiredMessage
   *        String in java.text.MessageFormat syntax.
   */
  public void setTriggerFiredMessage (final String triggerFiredMessage)
  {
    m_sTriggerFiredMessage = triggerFiredMessage;
  }

  /**
   * Set the message that is printed upon a trigger's firing.
   *
   * @param triggerMisfiredMessage
   *        String in java.text.MessageFormat syntax.
   */
  public void setTriggerMisfiredMessage (final String triggerMisfiredMessage)
  {
    m_sTriggerMisfiredMessage = triggerMisfiredMessage;
  }

  /**
   * <p>
   * Called during creation of the <code>Scheduler</code> in order to give the
   * <code>SchedulerPlugin</code> a chance to initialize.
   * </p>
   *
   * @throws SchedulerException
   *         if there is an error initializing.
   */
  public void initialize (final String pname,
                          final IScheduler scheduler,
                          final IClassLoadHelper classLoadHelper) throws SchedulerException
  {
    m_sName = pname;

    scheduler.getListenerManager ().addTriggerListener (this, EverythingMatcher.allTriggers ());
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

  public String getName ()
  {
    return m_sName;
  }

  @Override
  public void triggerFired (final ITrigger trigger, final IJobExecutionContext context)
  {
    if (LOGGER.isInfoEnabled ())
    {
      final Object [] args = { trigger.getKey ().getName (),
                               trigger.getKey ().getGroup (),
                               trigger.getPreviousFireTime (),
                               trigger.getNextFireTime (),
                               new Date (),
                               context.getJobDetail ().getKey ().getName (),
                               context.getJobDetail ().getKey ().getGroup (),
                               Integer.valueOf (context.getRefireCount ()) };
      LOGGER.info (new MessageFormat (getTriggerFiredMessage (), Locale.US).format (args));
    }
  }

  @Override
  public void triggerMisfired (final ITrigger trigger)
  {
    if (LOGGER.isInfoEnabled ())
    {
      final Object [] args = { trigger.getKey ().getName (),
                               trigger.getKey ().getGroup (),
                               trigger.getPreviousFireTime (),
                               trigger.getNextFireTime (),
                               new Date (),
                               trigger.getJobKey ().getName (),
                               trigger.getJobKey ().getGroup () };
      LOGGER.info (new MessageFormat (getTriggerMisfiredMessage (), Locale.US).format (args));
    }
  }

  @Override
  public void triggerComplete (final ITrigger trigger,
                               final IJobExecutionContext context,
                               final ECompletedExecutionInstruction triggerInstructionCode)
  {
    if (LOGGER.isInfoEnabled ())
    {
      String instrCode = "UNKNOWN";
      if (triggerInstructionCode == ECompletedExecutionInstruction.DELETE_TRIGGER)
      {
        instrCode = "DELETE TRIGGER";
      }
      else
        if (triggerInstructionCode == ECompletedExecutionInstruction.NOOP)
        {
          instrCode = "DO NOTHING";
        }
        else
          if (triggerInstructionCode == ECompletedExecutionInstruction.RE_EXECUTE_JOB)
          {
            instrCode = "RE-EXECUTE JOB";
          }
          else
            if (triggerInstructionCode == ECompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE)
            {
              instrCode = "SET ALL OF JOB'S TRIGGERS COMPLETE";
            }
            else
              if (triggerInstructionCode == ECompletedExecutionInstruction.SET_TRIGGER_COMPLETE)
              {
                instrCode = "SET THIS TRIGGER COMPLETE";
              }

      final Object [] args = { trigger.getKey ().getName (),
                               trigger.getKey ().getGroup (),
                               trigger.getPreviousFireTime (),
                               trigger.getNextFireTime (),
                               new Date (),
                               context.getJobDetail ().getKey ().getName (),
                               context.getJobDetail ().getKey ().getGroup (),
                               Integer.valueOf (context.getRefireCount ()),
                               triggerInstructionCode.toString (),
                               instrCode };
      LOGGER.info (new MessageFormat (getTriggerCompleteMessage (), Locale.US).format (args));
    }
  }

  @Override
  public boolean vetoJobExecution (final ITrigger trigger, final IJobExecutionContext context)
  {
    return false;
  }
}
