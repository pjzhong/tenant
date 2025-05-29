package org.example.game.example;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.common.net.generated.invoker.ExampleFacadeInvoker;
import org.example.exec.VirutalExecutors;
import org.example.net.anno.Req;
import org.example.net.anno.Rpc;
import org.example.net.handler.ExecSupplier;

/**
 * 游戏门面(文档生产插件测试)
 *
 * @author ZJP
 * @since 2021年09月27日 15:54:54
 **/
@Rpc
public class ExampleFacade implements ExecSupplier {

  private static final ExampleIdentity IDENTITY = new ExampleIdentity();

  private final ExampleFacadeInvoker facadeInvoker;

  public ExampleFacade(ExampleFacadeInvoker facadeInvoker) {
    this.facadeInvoker = facadeInvoker;
  }

  @Req
  public int callback(boolean boolean1, byte[] byte1, short short1,
      char char1, int int1, long long1,
      float float1, double double1, ReqMove reqMove, ResMove resMove) {
    return Objects.hash(boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1, float1,
        double1, reqMove, resMove);
  }

  @Override
  public Executor get() {
    return VirutalExecutors.commonPool().getExecutor(IDENTITY);
  }
}


