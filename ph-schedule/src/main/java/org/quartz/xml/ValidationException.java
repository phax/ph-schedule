/*
 * Copyright 2001-2009 Terracotta, Inc.
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

package org.quartz.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Reports JobSchedulingDataLoader validation exceptions.
 *
 * @author <a href="mailto:bonhamcm@thirdeyeconsulting.com">Chris Bonham</a>
 */
public class ValidationException extends Exception
{

  private static final long serialVersionUID = -1697832087051681357L;

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Data members.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private Collection <Exception> validationExceptions = new ArrayList <> ();

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Constructors.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Constructor for ValidationException.
   */
  public ValidationException ()
  {
    super ();
  }

  /**
   * Constructor for ValidationException.
   *
   * @param message
   *        exception message.
   */
  public ValidationException (final String message)
  {
    super (message);
  }

  /**
   * Constructor for ValidationException.
   *
   * @param errors
   *        collection of validation exceptions.
   */
  public ValidationException (final Collection <Exception> errors)
  {
    this ();
    this.validationExceptions = Collections.unmodifiableCollection (validationExceptions);
    initCause (errors.iterator ().next ());
  }

  /**
   * Constructor for ValidationException.
   *
   * @param message
   *        exception message.
   * @param errors
   *        collection of validation exceptions.
   */
  public ValidationException (final String message, final Collection <Exception> errors)
  {
    this (message);
    this.validationExceptions = Collections.unmodifiableCollection (validationExceptions);
    initCause (errors.iterator ().next ());
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Returns collection of errors.
   *
   * @return collection of errors.
   */
  public Collection <Exception> getValidationExceptions ()
  {
    return validationExceptions;
  }

  /**
   * Returns the detail message string.
   *
   * @return the detail message string.
   */
  @Override
  public String getMessage ()
  {
    if (getValidationExceptions ().size () == 0)
    {
      return super.getMessage ();
    }

    final StringBuffer sb = new StringBuffer ();

    boolean first = true;

    for (final Exception e : getValidationExceptions ())
    {
      if (!first)
      {
        sb.append ('\n');
        first = false;
      }

      sb.append (e.getMessage ());
    }

    return sb.toString ();
  }

}