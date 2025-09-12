package org.example.net.handler;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器获取接口
 * <p>
 *
 * 根据{@code T}参数和{@code SYSTEM}系统获取异步任务执行器。
 * 实现了此接口，所有被{@link org.example.net.anno.Req}标记的方法首个参数的类型必须相同
 *
 * @author zhongjianping
 * @since 2024/12/4 15:51
 */
@FunctionalInterface
public interface SysArgExecSupplier<SYSTEM, T> {

  Executor get(SYSTEM system, T t);
}
