package org.example.net.handler;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器获取接口
 * <p>
 * 根据首个参数，获取异步任务执行器。
 * <p>
 * 所有被{@link org.example.net.anno.Req}标记的方法首个参数的类型必须相同
 *
 * @author zhongjianping
 * @since 2024/12/4 15:51
 */
@FunctionalInterface
public interface ArgExecSupplier<T> {

  Executor get(T t);
}
