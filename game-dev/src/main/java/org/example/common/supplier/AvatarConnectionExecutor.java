package org.example.common.supplier;

import java.util.Objects;
import java.util.concurrent.Executor;
import org.example.common.model.AvatarId;
import org.example.exec.VirutalExecutors;
import org.example.net.Connection;
import org.example.net.handler.SysArgExecSupplier;

/**
 * 根据网络通道提供的信息，返回玩家对应的Executor
 *
 * @author zhongjianping
 * @since 2024/12/4 13:37
 */
public interface AvatarConnectionExecutor extends SysArgExecSupplier<VirutalExecutors, Connection> {

  @Override
  default Executor get(VirutalExecutors executors, Connection c) {
    AvatarId avatarIdentity = Objects
        .requireNonNull(c.channel().attr(AvatarIdAttr.KEY).get(), "玩家ID为空，请检查");
    return executors.getExecutor(avatarIdentity);
  }

}
