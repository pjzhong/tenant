package org.example.game.remote;


import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import org.example.common.model.GameId;
import org.example.common.model.ServerInfo;
import org.example.game.config.GameConfiguration;
import org.example.world.config.WorldConfiguration;
import org.example.world.server.WorldInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(GameConfiguration.class)
public class ClientServiceTest {

  /** 临时构建的远程服务区环境 */
  private static AnnotationConfigApplicationContext remote;

  @BeforeAll
  public static void beforeAll()
      throws Exception {
    AnnotationConfigApplicationContext remote = new AnnotationConfigApplicationContext();
    remote.register(WorldConfiguration.class);
    remote.refresh();
    remote.start();
    ClientServiceTest.remote = remote;
  }

  @AfterAll
  public static void afterAll() {
    if (remote != null) {
      remote.close();
    }
  }

  @Test
  public void register(@Autowired ClientService clientService)
      throws Exception {
    WorldInfo worldInfo = remote.getBean(WorldInfo.class);

    ServerInfo serverInfo = ServerInfo.serInfo(worldInfo.getId(),
        new InetSocketAddress("127.0.0.1", worldInfo.getPort()));
    ChannelFuture future = clientService.connectSync(serverInfo);
    Assertions.assertTrue(future.isSuccess());

    GameId rndId = new GameId(String.valueOf(ThreadLocalRandom.current().nextInt()));

    Assertions.assertTrue(
        clientService.registerChannelSync(rndId, serverInfo, future.channel()));
    Assertions.assertFalse(
        clientService.registerChannelSync(rndId, serverInfo, future.channel()));
  }


}
