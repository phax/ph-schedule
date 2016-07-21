package org.quartz.impl.calendar;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
