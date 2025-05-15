package org.example.net.handler;

import org.example.net.Connection;
import org.example.net.DefaultDispatcher;
import org.example.net.Message;

/**
 * 业务请求处理
 *
 * @author zhongjianping
 * @since 2024/8/8 14:44
 */
public interface Handler {

  /**
   * 处理{@param connection}和{@param message}
   *
   * @since 2025/5/15 11:30
   */
  void invoke(Connection connection, Message message) throws Exception;

  /**
   * @since 2025/5/15 11:01
   */
  void register(DefaultDispatcher defaultDispatcher);
}
