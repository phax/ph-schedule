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
package com.helger.quartz.xml;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.StringHelper;

/**
 * Reports JobSchedulingDataLoader validation exceptions.
 *
 * @author <a href="mailto:bonhamcm@thirdeyeconsulting.com">Chris Bonham</a>
 */
public class ValidationException extends Exception
{
  private final ICommonsList <Exception> m_aValidationExceptions;

  /**
   * Constructor for ValidationException.
   *
   * @param message
   *        exception message.
   * @param errors
   *        collection of validation exceptions.
   */
  public ValidationException (final String message, @Nonnull final Collection <Exception> errors)
  {
    super (message, CollectionHelper.getFirstElement (errors));
    m_aValidationExceptions = new CommonsArrayList <> (errors);
  }

  /**
   * Returns collection of errors.
   *
   * @return collection of errors.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <Exception> getValidationExceptions ()
  {
    return m_aValidationExceptions.getClone ();
  }

  /**
   * Returns the detail message string.
   *
   * @return the detail message string.
   */
  @Override
  public String getMessage ()
  {
    if (m_aValidationExceptions.isEmpty ())
      return super.getMessage ();

    return StringHelper.getImplodedMapped ('\n', m_aValidationExceptions, Exception::getMessage);
  }
}
