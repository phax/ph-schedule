/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
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
package com.helger.quartz.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.lang.NonBlockingProperties;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.simpl.RAMJobStore;
import com.helger.quartz.simpl.SimpleThreadPool;
import com.helger.quartz.spi.IThreadPool;

public class SchedulerDetailsSetterTest
{
  @Test
  public void testSetter () throws SchedulerException, IOException
  {
    final NonBlockingProperties props = new NonBlockingProperties ();
    props.load (ClassPathResource.getInputStream ("quartz/quartz.properties"));
    props.setProperty (StdSchedulerFactory.PROP_THREAD_POOL_CLASS, MyThreadPool.class.getName ());
    props.setProperty (StdSchedulerFactory.PROP_JOB_STORE_CLASS, MyJobStore.class.getName ());
    // Separate scheduler instance name is important otherwises tests fail on
    // the commandline because the scheduler is already instantiated and simply
    // reused
    props.setProperty (StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "MyTestScheduler");

    final StdSchedulerFactory factory = new StdSchedulerFactory ().initialize (props);
    // this will initialize all the test fixtures.
    assertNotNull (factory.getScheduler ());

    assertEquals (3, instanceIdCalls.get ());
    assertEquals (3, instanceNameCalls.get ());

    final DirectSchedulerFactory directFactory = DirectSchedulerFactory.getInstance ();
    directFactory.createScheduler (new MyThreadPool (), new MyJobStore ());

    assertEquals (5, instanceIdCalls.get ());
    assertEquals (5, instanceNameCalls.get ());
  }

  @Test
  public void testMissingSetterMethods () throws SchedulerException
  {
    SchedulerDetailsSetter.setDetails (new Object (), "name", "id");
  }

  @Test
  public void testUnimplementedMethods () throws Exception
  {
    final IThreadPool tp = _makeIncompleteThreadPool ();
    try
    {
      tp.setInstanceName ("name");
      fail ();
    }
    catch (final AbstractMethodError ame)
    {
      // expected
    }

    SchedulerDetailsSetter.setDetails (tp, "name", "id");
  }

  private IThreadPool _makeIncompleteThreadPool () throws InstantiationException, IllegalAccessException
  {
    final String name = "IncompleteThreadPool";
    final ClassWriter cw = new ClassWriter (0);
    cw.visit (Opcodes.V1_5,
              Opcodes.ACC_PUBLIC,
              name,
              null,
              "java/lang/Object",
              new String [] { "com/helger/quartz/spi/IThreadPool" });

    final MethodVisitor mv = cw.visitMethod (Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode ();
    mv.visitVarInsn (Opcodes.ALOAD, 0);
    mv.visitMethodInsn (Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    mv.visitInsn (Opcodes.RETURN);
    mv.visitMaxs (1, 1);
    mv.visitEnd ();

    cw.visitEnd ();

    return (IThreadPool) new ClassLoader ()
    {
      Class <?> defineClass (final String clname, final byte [] b)
      {
        return defineClass (clname, b, 0, b.length);
      }
    }.defineClass (name, cw.toByteArray ()).newInstance ();
  }

  private static final AtomicInteger instanceIdCalls = new AtomicInteger ();
  private static final AtomicInteger instanceNameCalls = new AtomicInteger ();

  public static class MyThreadPool extends SimpleThreadPool
  {
    @Override
    public void initialize ()
    {}

    @Override
    public void setInstanceId (final String schedInstId)
    {
      super.setInstanceId (schedInstId);
      instanceIdCalls.incrementAndGet ();
    }

    @Override
    public void setInstanceName (final String schedName)
    {
      super.setInstanceName (schedName);
      instanceNameCalls.incrementAndGet ();
    }
  }

  public static class MyJobStore extends RAMJobStore
  {
    @Override
    public void setInstanceId (final String schedInstId)
    {
      super.setInstanceId (schedInstId);
      instanceIdCalls.incrementAndGet ();
    }

    @Override
    public void setInstanceName (final String schedName)
    {
      super.setInstanceName (schedName);
      instanceNameCalls.incrementAndGet ();
    }
  }

}
