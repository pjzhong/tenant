package org.example.game.avatar;

import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.example.common.model.AvatarId;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.common.model.ServerInfo;
import org.example.common.net.generated.invoker.AvatarIdFacadeInvoker;
import org.example.game.config.GameConfiguration;
import org.example.game.remote.ClientService;
import org.example.game.server.GameInfo;
import org.example.game.server.GameServer;
import org.example.net.AsyncFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(GameConfiguration.class)
public class AvatarIdFacadeTest {


  @BeforeAll
  public static void beforeAll(
      @Autowired GameServer server,
      @Autowired GameInfo info,
      @Autowired ClientService clientService)
      throws Exception {
    server.startTest();

    ServerInfo serverInfo = ServerInfo.serInfo(info.getId(),
        new InetSocketAddress("127.0.0.1", info.getPort()));
    Channel channel = clientService.connectSync(serverInfo).channel();
    clientService.registerChannelSync(new AvatarId(1), serverInfo, channel);
  }

  @BeforeAll
  public static void afterAll(@Autowired GameServer server) throws Exception {
    server.close();
  }

  @RepeatedTest(100)
  public void echo(@Autowired AvatarIdFacadeInvoker invoker, @Autowired GameInfo info)
      throws Exception {
    AvatarId avatarId = new AvatarId(ThreadLocalRandom.current().nextLong());
    String str = String.valueOf(ThreadLocalRandom.current().nextLong());
    int hash = Objects.hash(avatarId, str);
    Assertions.assertEquals(hash, invoker.of(info.getId()).echo(avatarId, str).get());
  }

  @RepeatedTest(100)
  public void callBack(@Autowired AvatarIdFacadeInvoker invoker, @Autowired GameInfo info)
      throws Exception {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    AvatarId avatarId = new AvatarId(random.nextInt());
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

    int hashcode = Objects.hash(avatarId, boolean1, Arrays.hashCode(byte1), short1, char1, int1,
        long1,
        float1, double1, reqMove, resMove);

    AsyncFuture<Integer> callback = invoker.of(info.getId())
        .callback(avatarId, boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove,
            resMove);

    Assertions.assertEquals(hashcode, callback.get(100, TimeUnit.MILLISECONDS));
  }


}
