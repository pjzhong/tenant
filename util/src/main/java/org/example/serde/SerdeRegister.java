package org.example.serde;

/**
 * 序列化模块注册
 *
 * @author zhongjianping
 * @since 2025/5/14 11:22
 */
@FunctionalInterface
public interface SerdeRegister {

  /**
   * 使用{@code serdes}注册{@link Serializer}实现
   *
   * @param serdes
   * @since 2025/5/14 11:23
   */
  void register(Serdes serdes);
}
