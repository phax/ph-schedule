/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016 Philip Helger (www.helger.com)
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
package com.helger.quartz.utils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class ClassUtils
{

  public static boolean isAnnotationPresent (final Class <?> clazz, final Class <? extends Annotation> a)
  {
    for (Class <?> c = clazz; c != null; c = c.getSuperclass ())
    {
      if (c.isAnnotationPresent (a))
        return true;
      if (isAnnotationPresentOnInterfaces (c, a))
        return true;
    }
    return false;
  }

  private static boolean isAnnotationPresentOnInterfaces (final Class <?> clazz, final Class <? extends Annotation> a)
  {
    for (final Class <?> i : clazz.getInterfaces ())
    {
      if (i.isAnnotationPresent (a))
        return true;
      if (isAnnotationPresentOnInterfaces (i, a))
        return true;
    }

    return false;
  }

  public static <T extends Annotation> T getAnnotation (final Class <?> clazz, final Class <T> aClazz)
  {
    // Check class hierarchy
    for (Class <?> c = clazz; c != null; c = c.getSuperclass ())
    {
      final T anno = c.getAnnotation (aClazz);
      if (anno != null)
      {
        return anno;
      }
    }

    // Check interfaces (breadth first)
    final Queue <Class <?>> q = new LinkedList <> ();
    q.add (clazz);
    while (!q.isEmpty ())
    {
      final Class <?> c = q.remove ();
      if (c != null)
      {
        if (c.isInterface ())
        {
          final T anno = c.getAnnotation (aClazz);
          if (anno != null)
          {
            return anno;
          }
        }
        else
        {
          q.add (c.getSuperclass ());
        }
        q.addAll (Arrays.asList (c.getInterfaces ()));
      }
    }

    return null;
  }
}
