package org.example.net.proxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程调用方法标记
 *
 * @author ZJP
 * @since 2021年07月25日 15:00:25
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcMethod {

  /**
   * 方法ID,范围：模块ID +[0...99]
   * <p>
   * 配合{@link RpcMethod#value()} 组成唯一调用ID
   * <p>A模块ID:0, 方法ID:1</p>
   * <p>唯一调用ID: 0 + 1 = 1</p>
   *
   * </p>
   *
   * @return 方法ID
   * @since 2021年07月25日 14:29:21
   */
  int value() default 0;

  /**
   * {@link RpcMethod#value()}的负数形式，表示用来接收返回结果
   *
   * @since 2021年07月25日 20:57:58
   */
  int res() default 0;

}
