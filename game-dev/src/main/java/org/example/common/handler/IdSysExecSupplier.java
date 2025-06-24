package org.example.common.handler;

import java.util.concurrent.Executor;
import org.example.exec.VirutalExecutors;
import org.example.net.handler.SysArgExecSupplier;
import org.example.util.Identity;

/**
 * 根据{@code T}参数和{@link VirutalExecutors}系统计算运行环境(线程)。
 * 实现了此接口，所有被{@link org.example.net.anno.Req}标记的方法首个参数的类型必须相同
 *
 * @author zhongjianping
 * @since 2024/12/4 15:51
 */
public interface IdSysExecSupplier<T extends Identity> extends
    SysArgExecSupplier<VirutalExecutors, T> {

  @Override
  default Executor get(VirutalExecutors executors, T t) {
    return executors.getExecutor(t);
  }
}
