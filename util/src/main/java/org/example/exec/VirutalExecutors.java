package org.example.exec;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.example.util.Identity;

public class VirutalExecutors {

  private static final VirutalExecutors common = new VirutalExecutors();

  private final ScheduledExecutorService scheduledExecutorService;
  private final LoadingCache<Identity, VirtualExecutor> temporalExecutor;
  private final ThreadFactory defaultVirFactory;

  public VirutalExecutors() {
    scheduledExecutorService = Executors.newScheduledThreadPool(1);
    temporalExecutor = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .evictionListener((Identity k, VirtualExecutor v, RemovalCause c) -> {
          if (!v.isEmpty()) {
            //TODO 这里处理下，剩下的任务要切换到新的executor去
            //TODO 如果当前的executor发生了死锁或者执行了耗时的操作
          }
        })
        .build(VirtualExecutor::new);
    defaultVirFactory = Thread.ofVirtual().name("VIRTUALS").factory();
  }

  /**
   * 直接用虚拟线程执行
   *
   * @since 2024/12/8 18:31
   */
  public Thread execute(Runnable command) {
    Thread thread = defaultVirFactory.newThread(command);
    thread.start();
    return thread;
  }

  /**
   * 使用与此{@code id}绑定{@link VirtualExecutor}来执行任务
   *
   * @param id 指定执行器所属ID
   * @since 2024/12/8 18:31
   */
  public void executeOn(Identity id, Runnable command) {
    temporalExecutor.get(id).exec(command);
  }

  /**
   * {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}，当任务触发时。会交由虚拟线程来执行
   *
   * @since 2024/12/8 18:31
   */
  public ScheduledFuture<?> schedule(Runnable command, Duration duration) {
    return scheduledExecutorService.schedule(() -> {
      execute(command);
    }, duration.toNanos(), TimeUnit.NANOSECONDS);
  }

  /**
   * 使用提供{@code id}来执行定时任务
   *
   * @param identity 执行器ID
   * @since 2024/12/8 18:31
   */
  public ScheduledFuture<?> scheduleOn(Identity identity, Runnable command, Duration duration) {
    Identity id = Objects.requireNonNull(identity, "identity不能为空");
    return scheduledExecutorService.schedule(() -> {
      executeOn(id, command);
    }, duration.toNanos(), TimeUnit.NANOSECONDS);
  }


  public VirtualExecutor getExecutor(Identity id) {
    return temporalExecutor.get(id);
  }

  public static VirutalExecutors commonPool() {
    return common;
  }
}

