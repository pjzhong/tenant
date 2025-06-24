package org.example.net.handler;

import java.util.concurrent.Executor;

/**
 * 根据{@code FIRST}和{@code SECOND}参数和{@code SYSTEM}系统计算运行环境(线程)。
 * 实现了此接口，所有被{@link org.example.net.anno.Req}标记的方法首个参数的类型必须相同
 *
 * @author zhongjianping
 * @since 2024/12/4 15:51
 */
@FunctionalInterface
public interface SysArgsExecSupplier<SYSTEM, FIRST, SECOND> {

  Executor get(SYSTEM system, FIRST first, SECOND second);
}
