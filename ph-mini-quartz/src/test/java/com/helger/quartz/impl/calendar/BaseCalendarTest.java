package com.helger.quartz.impl.calendar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.quartz.impl.calendar.BaseCalendar;

public class BaseCalendarTest
{

  @Test
  public void testClone ()
  {
    final BaseCalendar base = new BaseCalendar ();
    final BaseCalendar clone = (BaseCalendar) base.clone ();

    assertEquals (base.getDescription (), clone.getDescription ());
    assertEquals (base.getBaseCalendar (), clone.getBaseCalendar ());
    assertEquals (base.getTimeZone (), clone.getTimeZone ());
  }

}
