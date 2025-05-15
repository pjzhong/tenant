package org.example.net;

/**
 * rpc服务注册(实验状态)
 *
 * @author zhongjianping
 * @since 2025/5/15 21:46
 */
@FunctionalInterface
public interface HandlerRegister {

  /**
   * 注册RPC服务至{@code dispatcher}
   *
   * @since 2025/5/15 21:45
   */
  void register(DefaultDispatcher dispatcher);
}
