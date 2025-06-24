package org.example.game.avatar;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.example.common.model.AvatarId;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.common.model.ServerInfo;
import org.example.common.net.generated.invoker.AvatarFacadeInvoker;
import org.example.common.net.generated.invoker.LoginServiceInvoker;
import org.example.game.config.GameConfiguration;
import org.example.game.remote.ClientService;
import org.example.game.server.GameInfo;
import org.example.game.server.GameServer;
import org.example.net.AsyncFuture;
import org.example.net.Connection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(GameConfiguration.class)
public class AvatarFacadeTest {

  private static Channel channel;

  @BeforeAll
  public static void beforeAll(@Autowired GameServer server,
      @Autowired ClientService clientService,
      @Autowired GameInfo info,
      @Autowired LoginServiceInvoker loginServiceInvoker
  ) throws Exception {
    server.startTest();

    AvatarId id = new AvatarId(1);

    ServerInfo serverInfo = ServerInfo.serInfo(info.getId(),
        new InetSocketAddress("127.0.0.1", info.getPort()));
    channel = clientService.connectSync(serverInfo).channel();
    Assertions.assertTrue(
        loginServiceInvoker.of(channel.attr(Connection.CONNECTION).get()).login(id).get());
  }

  @BeforeAll
  public static void afterAll(@Autowired GameServer server) throws Exception {
    server.close();
  }

  @RepeatedTest(10)
  public void nothing(@Autowired AvatarFacadeInvoker invoker) throws Exception {
    Assertions.assertTrue(
        invoker.of(channel.attr(Connection.CONNECTION).get())
            .nothing()
            .get());
  }

  @RepeatedTest(10)
  public void callBack(@Autowired AvatarFacadeInvoker invoker) throws Exception {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    boolean boolean1 = random.nextBoolean();
    byte[] byte1 = new byte[random.nextInt(10)];
    random.nextBytes(byte1);
    short short1 = (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
    char char1 = (char) random.nextInt();
    int int1 = random.nextInt();
    long long1 = random.nextLong();
    float float1 = random.nextFloat();
    double double1 = random.nextDouble();

    ReqMove reqMove = new ReqMove();
    reqMove.setId(random.nextInt());
    reqMove.setX(random.nextFloat());
    reqMove.setY(random.nextFloat());

    ResMove resMove = new ResMove();
    resMove.setId(random.nextInt());
    resMove.setX(random.nextFloat());
    resMove.setY(random.nextFloat());
    resMove.setDir(random.nextInt());

    int hashcode = Objects.hash(boolean1, Arrays.hashCode(byte1), short1, char1, int1, long1,
        float1, double1, reqMove, resMove);

    AsyncFuture<Integer> callback = invoker.of(
            channel.attr(Connection.CONNECTION).get())
        .callback(boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove, resMove);

    Assertions.assertEquals(hashcode, callback.get());
  }


}
