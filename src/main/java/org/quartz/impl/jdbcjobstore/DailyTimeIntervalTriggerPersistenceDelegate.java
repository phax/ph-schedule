/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package org.quartz.impl.jdbcjobstore;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.TimeOfDay;
import org.quartz.impl.triggers.DailyTimeIntervalTriggerImpl;
import org.quartz.spi.OperableTrigger;

/**
 * Persist a DailyTimeIntervalTrigger by converting internal fields to and from
 * SimplePropertiesTriggerProperties.
 *
 * @see DailyTimeIntervalScheduleBuilder
 * @see DailyTimeIntervalTrigger
 * @since 2.1.0
 * @author Zemian Deng <saltnlight5@gmail.com>
 */
public class DailyTimeIntervalTriggerPersistenceDelegate extends SimplePropertiesTriggerPersistenceDelegateSupport
{

  public boolean canHandleTriggerType (final OperableTrigger trigger)
  {
    return ((trigger instanceof DailyTimeIntervalTrigger) &&
            !((DailyTimeIntervalTriggerImpl) trigger).hasAdditionalProperties ());
  }

  public String getHandledTriggerTypeDiscriminator ()
  {
    return TTYPE_DAILY_TIME_INT;
  }

  @Override
  protected SimplePropertiesTriggerProperties getTriggerProperties (final OperableTrigger trigger)
  {
    final DailyTimeIntervalTriggerImpl dailyTrigger = (DailyTimeIntervalTriggerImpl) trigger;
    final SimplePropertiesTriggerProperties props = new SimplePropertiesTriggerProperties ();

    props.setInt1 (dailyTrigger.getRepeatInterval ());
    props.setString1 (dailyTrigger.getRepeatIntervalUnit ().name ());
    props.setInt2 (dailyTrigger.getTimesTriggered ());

    final Set <Integer> days = dailyTrigger.getDaysOfWeek ();
    final String daysStr = join (days, ",");
    props.setString2 (daysStr);

    final StringBuilder timeOfDayBuffer = new StringBuilder ();
    final TimeOfDay startTimeOfDay = dailyTrigger.getStartTimeOfDay ();
    if (startTimeOfDay != null)
    {
      timeOfDayBuffer.append (startTimeOfDay.getHour ()).append (",");
      timeOfDayBuffer.append (startTimeOfDay.getMinute ()).append (",");
      timeOfDayBuffer.append (startTimeOfDay.getSecond ()).append (",");
    }
    else
    {
      timeOfDayBuffer.append (",,,");
    }
    final TimeOfDay endTimeOfDay = dailyTrigger.getEndTimeOfDay ();
    if (endTimeOfDay != null)
    {
      timeOfDayBuffer.append (endTimeOfDay.getHour ()).append (",");
      timeOfDayBuffer.append (endTimeOfDay.getMinute ()).append (",");
      timeOfDayBuffer.append (endTimeOfDay.getSecond ());
    }
    else
    {
      timeOfDayBuffer.append (",,,");
    }
    props.setString3 (timeOfDayBuffer.toString ());

    props.setLong1 (dailyTrigger.getRepeatCount ());

    return props;
  }

  private String join (final Set <Integer> days, final String sep)
  {
    final StringBuilder sb = new StringBuilder ();
    if (days == null || days.size () <= 0)
      return "";

    final Iterator <Integer> itr = days.iterator ();
    sb.append (itr.next ());
    while (itr.hasNext ())
    {
      sb.append (sep).append (itr.next ());
    }
    return sb.toString ();
  }

  @Override
  protected TriggerPropertyBundle getTriggerPropertyBundle (final SimplePropertiesTriggerProperties props)
  {
    final int repeatCount = (int) props.getLong1 ();
    final int interval = props.getInt1 ();
    final String intervalUnitStr = props.getString1 ();
    final String daysOfWeekStr = props.getString2 ();
    final String timeOfDayStr = props.getString3 ();

    final IntervalUnit intervalUnit = IntervalUnit.valueOf (intervalUnitStr);
    final DailyTimeIntervalScheduleBuilder scheduleBuilder = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule ()
                                                                                             .withInterval (interval,
                                                                                                            intervalUnit)
                                                                                             .withRepeatCount (repeatCount);

    if (daysOfWeekStr != null)
    {
      final Set <Integer> daysOfWeek = new HashSet <> ();
      final String [] nums = daysOfWeekStr.split (",");
      if (nums.length > 0)
      {
        for (final String num : nums)
        {
          daysOfWeek.add (Integer.parseInt (num));
        }
        scheduleBuilder.onDaysOfTheWeek (daysOfWeek);
      }
    }
    else
    {
      scheduleBuilder.onDaysOfTheWeek (DailyTimeIntervalScheduleBuilder.ALL_DAYS_OF_THE_WEEK);
    }

    if (timeOfDayStr != null)
    {
      final String [] nums = timeOfDayStr.split (",");
      TimeOfDay startTimeOfDay;
      if (nums.length >= 3)
      {
        final int hour = Integer.parseInt (nums[0]);
        final int min = Integer.parseInt (nums[1]);
        final int sec = Integer.parseInt (nums[2]);
        startTimeOfDay = new TimeOfDay (hour, min, sec);
      }
      else
      {
        startTimeOfDay = TimeOfDay.hourMinuteAndSecondOfDay (0, 0, 0);
      }
      scheduleBuilder.startingDailyAt (startTimeOfDay);

      TimeOfDay endTimeOfDay;
      if (nums.length >= 6)
      {
        final int hour = Integer.parseInt (nums[3]);
        final int min = Integer.parseInt (nums[4]);
        final int sec = Integer.parseInt (nums[5]);
        endTimeOfDay = new TimeOfDay (hour, min, sec);
      }
      else
      {
        endTimeOfDay = TimeOfDay.hourMinuteAndSecondOfDay (23, 59, 59);
      }
      scheduleBuilder.endingDailyAt (endTimeOfDay);
    }
    else
    {
      scheduleBuilder.startingDailyAt (TimeOfDay.hourMinuteAndSecondOfDay (0, 0, 0));
      scheduleBuilder.endingDailyAt (TimeOfDay.hourMinuteAndSecondOfDay (23, 59, 59));
    }

    final int timesTriggered = props.getInt2 ();
    final String [] statePropertyNames = { "timesTriggered" };
    final Object [] statePropertyValues = { timesTriggered };

    return new TriggerPropertyBundle (scheduleBuilder, statePropertyNames, statePropertyValues);
  }
}
