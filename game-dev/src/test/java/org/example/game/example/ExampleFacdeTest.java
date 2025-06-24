package org.example.game.example;


import io.netty.channel.ChannelFuture;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.example.common.model.GameId;
import org.example.common.model.ReqMove;
import org.example.common.model.ResMove;
import org.example.common.model.ServerInfo;
import org.example.common.net.generated.invoker.ExampleFacadeInvoker;
import org.example.game.config.GameConfiguration;
import org.example.game.remote.ClientService;
import org.example.game.server.GameInfo;
import org.example.game.server.GameServer;
import org.example.net.AsyncFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(GameConfiguration.class)
public class ExampleFacdeTest {

  private static GameId rndGameId;

  @BeforeAll
  public static void beforeAll(@Autowired GameServer server, @Autowired ClientService service,
      @Autowired GameInfo info) throws Exception {
    server.startTest();

    rndGameId = new GameId(String.valueOf(ThreadLocalRandom.current().nextInt()));

    ServerInfo serverInfo = ServerInfo.serInfo(info.getId(),
        new InetSocketAddress("127.0.0.1", info.getPort()));
    ChannelFuture future = service.connectSync(serverInfo);
    Assertions.assertTrue(service.registerChannelSync(rndGameId, serverInfo, future.channel()));
  }

  @BeforeAll
  public static void afterAll(@Autowired GameServer server) throws Exception {
    server.close();
  }

  @RepeatedTest(100)
  public void callBack(@Autowired ExampleFacadeInvoker invoker)
      throws Exception {
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

    AsyncFuture<Integer> callback = invoker.of(rndGameId)
        .callback(boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove, resMove);

    Assertions.assertEquals(hashcode, callback.get());
  }

  @Test
  public void batchCallBack(@Autowired ExampleFacadeInvoker invoker)
      throws Exception {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    Assertions.assertEquals(ResourceLeakDetector.getLevel(), Level.SIMPLE);

    List<Runnable> callbacks = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
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

      AsyncFuture<Integer> callback = invoker.of(rndGameId)
          .callback(boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove, resMove);

      callbacks.add(() -> {
        try {
          Assertions.assertEquals(hashcode, callback.get());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }

    for (Runnable r : callbacks) {
      r.run();
    }
  }


}
