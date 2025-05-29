package org.example.net;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.example.exec.VirtualExecutor;
import org.example.exec.VirutalExecutors;
import org.example.util.Identity;

/**
 * 异步回调，在创建时绑定当前{@link VirtualExecutor#getIdentity()}，此后所有回调都在此绑定的{@link VirtualExecutor}
 * 下执行。如果无法获取当前的{{@link VirtualExecutor}。则绑定默认执行器
 *
 * @author zhongjianping
 * @since 2024/12/8 14:22
 */
public class AsyncFuture<T> {

  private final CompletableFuture<T> f;
  private final Executor executor;
  private Future<?> timeOutFuture;

  public AsyncFuture(CompletableFuture<T> f) {
    this.f = f;
    VirtualExecutor virutalExecutor = VirtualExecutor.current();
    Identity identity;
    if (virutalExecutor == null) {
      identity = ID.AsyncFuture;
    } else {
      identity = virutalExecutor.getIdentity();
    }

    executor = r -> {
      VirutalExecutors.commonPool().executeOn(identity, r);
    };
  }

  public static <U> AsyncFuture<U> of(CompletableFuture<U> f) {
    return new AsyncFuture<>(f);
  }

  /**
   * 如果当前在VirtualThreadExecutor环境中。 则此回调则在相同VirtualTHreadExecuotr中执行。
   * {@link CompletableFuture#whenCompleteAsync(BiConsumer, Executor)}}
   * <p>
   *
   * @author zhongjianping
   * @since 2024/12/8 14:15
   */
  public AsyncFuture<T> async(
      BiConsumer<? super T, ? super Throwable> consumer) {
    f.whenCompleteAsync(consumer, executor);
    return this;
  }

  /**
   * {@link CompletableFuture#whenCompleteAsync(BiConsumer, Executor)}}
   */
  public AsyncFuture<T> async(
      BiConsumer<? super T, ? super Throwable> consumer, VirtualExecutor executor) {
    Objects.requireNonNull(executor, "executor不能为空");
    f.whenCompleteAsync(consumer, executor);
    return this;
  }

  /**
   * {@link CompletableFuture#get(long, TimeUnit)}}
   */
  public T get() throws ExecutionException, InterruptedException, TimeoutException {
    return get(3, TimeUnit.SECONDS);
  }

  /**
   * {@link CompletableFuture#get(long, TimeUnit)}}
   */
  public T get(long timeout, TimeUnit unit)
      throws ExecutionException, InterruptedException, TimeoutException {
    return f.get(timeout, unit);
  }

  /**
   * @return {@link CompletableFuture}
   * @since 2024/12/8 16:20
   */
  public CompletableFuture<T> future() {
    return f;
  }

  /**
   * {@link CompletableFuture#cancel(boolean)}
   *
   * @since 2024/12/8 21:06
   */
  public boolean cancel(boolean mayInterruptIfRunning) {
    return f.cancel(mayInterruptIfRunning);
  }

  public Future<?> timeOut(Duration duration) {
    if (timeOutFuture != null) {
      timeOutFuture.cancel(false);
    }

    timeOutFuture = VirutalExecutors.commonPool().schedule(() -> {
      f.completeExceptionally(new TimeoutException("回调函数过期"));
    }, duration);
    return timeOutFuture;
  }

  Future<?> timeOutFuture() {
    return timeOutFuture;
  }

  AsyncFuture<T> timeOutFuture(Future<?> future) {
    if (timeOutFuture != null) {
      timeOutFuture.cancel(false);
    }
    timeOutFuture = future;
    return this;
  }

  /**
   * {@link CompletableFuture#supplyAsync(Supplier, Executor)}
   *
   * @since 2024/12/8 17:31
   */
  public static <U> AsyncFuture<U> supplyAsync(Supplier<U> supplier,
      Executor executor) {
    Objects.requireNonNull(executor, "executor不能为空");
    return new AsyncFuture<>(CompletableFuture.supplyAsync(supplier, executor));
  }

  private enum ID implements Identity {
    AsyncFuture
  }
}

