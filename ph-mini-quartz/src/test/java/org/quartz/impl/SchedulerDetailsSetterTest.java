package org.quartz.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.quartz.SchedulerException;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ThreadPool;

public class SchedulerDetailsSetterTest
{
  @Test
  public void testSetter () throws SchedulerException, IOException
  {
    final Properties props = new Properties ();
    props.load (getClass ().getResourceAsStream ("/org/quartz/quartz.properties"));
    props.setProperty (StdSchedulerFactory.PROP_THREAD_POOL_CLASS, MyThreadPool.class.getName ());
    props.setProperty (StdSchedulerFactory.PROP_JOB_STORE_CLASS, MyJobStore.class.getName ());

    final StdSchedulerFactory factory = new StdSchedulerFactory (props);
    factory.getScheduler (); // this will initialize all the test fixtures.

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
    final ThreadPool tp = makeIncompleteThreadPool ();
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

  private ThreadPool makeIncompleteThreadPool () throws InstantiationException, IllegalAccessException
  {
    final String name = "IncompleteThreadPool";
    final ClassWriter cw = new ClassWriter (0);
    cw.visit (Opcodes.V1_5,
              Opcodes.ACC_PUBLIC,
              name,
              null,
              "java/lang/Object",
              new String [] { "org/quartz/spi/ThreadPool" });

    final MethodVisitor mv = cw.visitMethod (Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode ();
    mv.visitVarInsn (Opcodes.ALOAD, 0);
    mv.visitMethodInsn (Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    mv.visitInsn (Opcodes.RETURN);
    mv.visitMaxs (1, 1);
    mv.visitEnd ();

    cw.visitEnd ();

    return (ThreadPool) new ClassLoader ()
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
