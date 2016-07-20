package org.quartz.utils;

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
