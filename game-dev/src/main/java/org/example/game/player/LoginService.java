package org.example.game.player;

import java.util.concurrent.Executor;
import org.example.common.model.AvatarId;
import org.example.common.supplier.AvatarIdAttr;
import org.example.exec.VirutalExecutors;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.anno.Req;
import org.example.net.anno.Rpc;
import org.example.net.handler.SysArgsExecSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Rpc
public class LoginService implements SysArgsExecSupplier<VirutalExecutors, Connection, AvatarId> {

  private static final Logger logger = LoggerFactory.getLogger(LoginService.class);
  @Autowired
  private ConnectionManager connectionManager;


  /**
   * 登录角色
   *
   * @since 2025/6/24 10:41
   */
  @Req
  public boolean login(Connection connection, AvatarId id) {
    connection.channel().attr(AvatarIdAttr.KEY).set(id);
    connectionManager.bindChannel(id, connection.channel());
    logger.info("玩家：{}，登录成功", id);
    return true;
  }

  @Override
  public Executor get(VirutalExecutors executors, Connection connection, AvatarId avatarId) {
    AvatarId id = connection.channel().attr(AvatarIdAttr.KEY).get();
    if (id != null) {
      throw new IllegalArgumentException("链接：%s, 已注册ID:%s".formatted(connection, id));
    }
    return executors.getExecutor(avatarId);
  }
}
