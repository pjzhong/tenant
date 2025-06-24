package org.example.game.avatar;

import java.util.Arrays;
import java.util.Objects;
import org.example.common.handler.IdExecSupplier;
import org.example.common.model.AvatarId;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.common.net.generated.invoker.AvatarIdFacadeInvoker;
import org.example.net.Connection;
import org.example.net.anno.Req;
import org.example.net.anno.Rpc;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@Rpc
public class AvatarIdFacade implements IdExecSupplier<AvatarId> {

  private final AvatarIdFacadeInvoker facadeInvoker;

  public AvatarIdFacade(AvatarIdFacadeInvoker facadeInvoker) {
    this.facadeInvoker = facadeInvoker;
  }


  /**
   * 回声
   *
   * @author ZJP
   * @since 2021年09月27日 16:01:08
   **/
  @Req
  public void echo(AvatarId id, String str, Connection connection) {
    facadeInvoker.of(connection).echo(id, str);
  }

  @Req
  public void nothing(AvatarId id) {
  }

  @Req
  public int callback(AvatarId id, boolean boolean1, byte[] byte1, short short1,
      char char1, int int1, long long1,
      float float1, double double1, ReqMove reqMove, ResMove resMove) {
    return Objects.hash(id, boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1, float1,
        double1, reqMove, resMove);
  }
}


