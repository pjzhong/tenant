package org.example.game.avatar;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.example.common.model.AvatarId;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.exec.VirutalExecutors;
import org.example.net.anno.LocalReq;
import org.example.net.anno.Rpc;
import org.example.net.handler.SysArgExecSupplier;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@Rpc
public class AvatarIdService implements SysArgExecSupplier<VirutalExecutors, AvatarId> {

  public AvatarId id;

  public AvatarIdService() {
  }

  @LocalReq
  public AvatarId echo(AvatarId id) {
    return id;
  }

  @LocalReq
  public boolean set(AvatarId id) {
    this.id = id;
    return true;
  }

  @LocalReq
  public int callback(AvatarId id, boolean boolean1, byte[] byte1, short short1,
      char char1, int int1, long long1,
      float float1, double double1, ReqMove reqMove, ResMove resMove) {
    return Objects.hash(id, boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1, float1,
        double1, reqMove, resMove);
  }

  @Override
  public Executor get(VirutalExecutors executors, AvatarId avatarId) {
    return executors.getExecutor(avatarId);
  }
}


