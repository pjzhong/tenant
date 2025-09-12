package org.example.net.handler;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器获取接口
 * <p>
 *
 * @author zhongjianping
 * @since 2025/8/14 17:15
 */
@FunctionalInterface
public interface ExecSupplier {

  /**
   * 根据具体场景，返回对应的异步任务执行器
   *
   * <p>实现者需保证线程安全<p/>
   *
   * @since 2024/12/4 10:31
   */
  Executor get();
}
