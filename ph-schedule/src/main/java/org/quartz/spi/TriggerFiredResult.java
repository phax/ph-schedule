package org.quartz.spi;

/**
 * @author lorban
 */
public class TriggerFiredResult
{

  private TriggerFiredBundle triggerFiredBundle;

  private Exception exception;

  public TriggerFiredResult (final TriggerFiredBundle triggerFiredBundle)
  {
    this.triggerFiredBundle = triggerFiredBundle;
  }

  public TriggerFiredResult (final Exception exception)
  {
    this.exception = exception;
  }

  public TriggerFiredBundle getTriggerFiredBundle ()
  {
    return triggerFiredBundle;
  }

  public Exception getException ()
  {
    return exception;
  }
}