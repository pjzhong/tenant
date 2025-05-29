package org.example.executor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.example.exec.VirutalExecutors;
import org.example.util.Identity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

public class VirtualThreadTest {


  protected static VirutalExecutors service;

  private static Identity identity = new Id();

  @BeforeAll
  public static void beforeAll() {
    service = VirutalExecutors.commonPool();
  }


  /**
   * 虚拟线程
   *
   * @since 2021年10月07日 16:37:58
   */
  @RepeatedTest(ExecutorTest.REPEAT)
  public void mutli() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(ExecutorTest.TASKS);
    Runnable task = latch::countDown;
    IntStream.range(0, ExecutorTest.TASKS).parallel().forEach(i -> service.execute(task));

    Assertions.assertTrue(latch.await(1, TimeUnit.MINUTES));
  }

  /**
   * 虚拟线程
   *
   * @since 2021年10月07日 16:37:58
   */
  @RepeatedTest(ExecutorTest.REPEAT)
  public void single() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(ExecutorTest.TASKS);
    Runnable task = latch::countDown;
    IntStream.range(0, ExecutorTest.TASKS).parallel()
        .forEach(i -> service.executeOn(identity, task));

    Assertions.assertTrue(latch.await(1, TimeUnit.MINUTES));
  }

  public record Id() implements Identity {

  }

}
