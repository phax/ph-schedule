package org.quartz.impl.jdbcjobstore;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.spi.OperableTrigger;

public class CronTriggerPersistenceDelegate implements TriggerPersistenceDelegate, StdJDBCConstants
{

  protected String tablePrefix;
  protected String schedNameLiteral;

  public void initialize (final String theTablePrefix, final String schedName)
  {
    this.tablePrefix = theTablePrefix;
    this.schedNameLiteral = "'" + schedName + "'";
  }

  public String getHandledTriggerTypeDiscriminator ()
  {
    return TTYPE_CRON;
  }

  public boolean canHandleTriggerType (final OperableTrigger trigger)
  {
    return ((trigger instanceof CronTriggerImpl) && !((CronTriggerImpl) trigger).hasAdditionalProperties ());
  }

  public int deleteExtendedTriggerProperties (final Connection conn, final TriggerKey triggerKey) throws SQLException
  {

    PreparedStatement ps = null;

    try
    {
      ps = conn.prepareStatement (Util.rtp (DELETE_CRON_TRIGGER, tablePrefix, schedNameLiteral));
      ps.setString (1, triggerKey.getName ());
      ps.setString (2, triggerKey.getGroup ());

      return ps.executeUpdate ();
    }
    finally
    {
      Util.closeStatement (ps);
    }
  }

  public int insertExtendedTriggerProperties (final Connection conn,
                                              final OperableTrigger trigger,
                                              final String state,
                                              final JobDetail jobDetail) throws SQLException, IOException
  {

    final CronTrigger cronTrigger = (CronTrigger) trigger;

    PreparedStatement ps = null;

    try
    {
      ps = conn.prepareStatement (Util.rtp (INSERT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
      ps.setString (1, trigger.getKey ().getName ());
      ps.setString (2, trigger.getKey ().getGroup ());
      ps.setString (3, cronTrigger.getCronExpression ());
      ps.setString (4, cronTrigger.getTimeZone ().getID ());

      return ps.executeUpdate ();
    }
    finally
    {
      Util.closeStatement (ps);
    }
  }

  public TriggerPropertyBundle loadExtendedTriggerProperties (final Connection conn,
                                                              final TriggerKey triggerKey) throws SQLException
  {

    PreparedStatement ps = null;
    ResultSet rs = null;

    try
    {
      ps = conn.prepareStatement (Util.rtp (SELECT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
      ps.setString (1, triggerKey.getName ());
      ps.setString (2, triggerKey.getGroup ());
      rs = ps.executeQuery ();

      if (rs.next ())
      {
        final String cronExpr = rs.getString (COL_CRON_EXPRESSION);
        final String timeZoneId = rs.getString (COL_TIME_ZONE_ID);

        final CronScheduleBuilder cb = CronScheduleBuilder.cronSchedule (cronExpr);

        if (timeZoneId != null)
          cb.inTimeZone (TimeZone.getTimeZone (timeZoneId));

        return new TriggerPropertyBundle (cb, null, null);
      }

      throw new IllegalStateException ("No record found for selection of Trigger with key: '" +
                                       triggerKey +
                                       "' and statement: " +
                                       Util.rtp (SELECT_CRON_TRIGGER, tablePrefix, schedNameLiteral));
    }
    finally
    {
      Util.closeResultSet (rs);
      Util.closeStatement (ps);
    }
  }

  public int updateExtendedTriggerProperties (final Connection conn,
                                              final OperableTrigger trigger,
                                              final String state,
                                              final JobDetail jobDetail) throws SQLException, IOException
  {

    final CronTrigger cronTrigger = (CronTrigger) trigger;

    PreparedStatement ps = null;

    try
    {
      ps = conn.prepareStatement (Util.rtp (UPDATE_CRON_TRIGGER, tablePrefix, schedNameLiteral));
      ps.setString (1, cronTrigger.getCronExpression ());
      ps.setString (2, cronTrigger.getTimeZone ().getID ());
      ps.setString (3, trigger.getKey ().getName ());
      ps.setString (4, trigger.getKey ().getGroup ());

      return ps.executeUpdate ();
    }
    finally
    {
      Util.closeStatement (ps);
    }
  }

}
