package org.example.exec;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.example.util.Identity;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualExecutor implements Executor {

  private static final Logger logger = LoggerFactory.getLogger(VirtualExecutor.class);

  /**
   * 当前线程对应的Identity，为了适配虚拟线程所以使用Cmp代替ThreadLocal
   */
  private static final ConcurrentHashMap<Thread, Identity> CURRENT = new ConcurrentHashMap<>();
  /** 运行标记修改Handle */
  private static final VarHandle VALUE;
  /** 任务队列，块长度 */
  private static final int CHUNK = 128;

  static {
    try {
      MethodHandles.Lookup l = MethodHandles.lookup();
      VALUE = l.findVarHandle(VirtualExecutor.class, "value", int.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final Queue<Runnable> queue;
  private final ThreadFactory factory;
  private final Identity identity;
  private volatile int value;

  public VirtualExecutor(Identity identity) {
    this.identity = identity;
    queue = new MpscUnboundedArrayQueue<>(CHUNK);
    factory = Thread.ofVirtual().name(identity.toString())
        .factory();
  }

  public Identity getIdentity() {
    return identity;
  }

  private void run() {
    boolean failed = !compareAndSet(false, true);
    if (failed) {
      return;
    }

    Thread thread = Thread.currentThread();
    try {
      CURRENT.put(thread, identity);
      doRun();
    } catch (Throwable e) {
      logger.error("VirtualExecutor uncaughtException", e);
    } finally {
      CURRENT.remove(thread);
      setRunning(false);
    }

    if (!isEmpty()) {
      trySchedule();
    }
  }

  @Override
  public void execute(Runnable command) {
    exec(command);
  }

  public void exec(Runnable command) {
    queue.add(command);
    trySchedule();
  }

  private void trySchedule() {
    if (isRunning()) {
      return;
    }

    Thread thread = factory.newThread(this::run);
    thread.start();
  }

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public static VirtualExecutor current() {
    Identity identity = CURRENT.get(Thread.currentThread());
    if (identity == null) {
      return null;
    }

    return VirutalExecutors.commonPool().getExecutor(identity);
  }

  private void doRun() {
    for (int i = 0; i < CHUNK; i++) {
      Runnable runnable = queue.poll();
      if (runnable != null) {
        runnable.run();
      }
    }
  }

  /**
   * {@link AtomicBoolean#compareAndSet(boolean, boolean)}
   *
   * @since 2025/5/24 12:12
   */
  private final boolean compareAndSet(boolean expectedValue, boolean newValue) {
    return VALUE.compareAndSet(this,
        expectedValue ? 1 : 0,
        newValue ? 1 : 0);
  }

  /**
   * {@link AtomicBoolean#set(boolean)}
   *
   * @since 2025/5/24 12:12
   */
  private final void setRunning(boolean newValue) {
    value = newValue ? 1 : 0;
  }

  /**
   * {@link AtomicBoolean#get()}
   *
   * @since 2025/5/24 12:13
   */
  private final boolean isRunning() {
    return value != 0;
  }

}
