package org.example.game.avatar;

import static org.example.game.avatar.IntId.intId;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ArrayUtils;
import org.example.common.model.AvatarId;
import org.example.common.model.ReqMove;
import org.example.common.model.ReqMoveSerde;
import org.example.common.model.ResMove;
import org.example.common.model.ResMoveSerde;
import org.example.common.net.generated.invoker.AvatarFacadeInvoker;
import org.example.net.AsyncFuture;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.DefaultDispatcher;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.CallBackFacade;
import org.example.net.handler.DispatcherNettyInboundHandler;
import org.example.serde.DefaultSerializersRegister;
import org.example.serde.Serdes;
import org.example.util.NettyByteBufUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

public class AvatarFacadeTest {

  private static EmbeddedChannel embeddedChannel;
  private static ConnectionManager connectionManager;
  private static Serdes serdes;
  private static AvatarFacadeInvoker invoker;

  @BeforeAll
  public static void beforeAll() {
    connectionManager = new ConnectionManager();
    serdes = new Serdes();
    new DefaultSerializersRegister().register(serdes);
    serdes.registerSerializer(ReqMove.class, new ReqMoveSerde());
    serdes.registerSerializer(ResMove.class, new ResMoveSerde());

    invoker = new AvatarFacadeInvoker(connectionManager, serdes);

    DefaultDispatcher handlerRegistry = new DefaultDispatcher();
    AvatarFacade facade = new AvatarFacade(invoker);
    AvatarFacadeHandler handler = new AvatarFacadeHandler(facade, serdes);
    handler.register(handlerRegistry);

    CallBackFacade gameFacadeCallBack = new CallBackFacade(connectionManager, serdes);
    handlerRegistry.registeHandler(gameFacadeCallBack.callBackId(), gameFacadeCallBack);

    embeddedChannel = new EmbeddedChannel();
    embeddedChannel.attr(AttributeKey.valueOf("AvatarId")).set(new AvatarId(1));
    connectionManager.bindChannel(intId(1), embeddedChannel);
    embeddedChannel.pipeline()
        .addLast(new MessageCodec())
        .addLast(new DispatcherNettyInboundHandler(handlerRegistry));

  }

  @RepeatedTest(100)
  public void echo() throws Exception {
    String str = String.valueOf(ThreadLocalRandom.current().nextLong());
    invoker.of(embeddedChannel.attr(Connection.CONNECTION).get())
        .echo(str);

    //验证请求的信息
    {
      ByteBuf reqBuf = embeddedChannel.readOutbound();
      reqBuf.markReaderIndex();

      reqBuf.skipBytes(Integer.BYTES);
      NettyByteBufUtil.readVarInt32(reqBuf);
      ByteBuf buf = Unpooled.buffer();
      serdes.writeObject(buf, str);
      Assertions.assertArrayEquals(NettyByteBufUtil.readBytes(buf),
          NettyByteBufUtil.readBytes(reqBuf));
      Assertions.assertFalse(reqBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());

      reqBuf.resetReaderIndex();
      embeddedChannel.writeInbound(reqBuf);
    }

    //验证返回的结果
    while (true) {
      ByteBuf resBuf = embeddedChannel.readOutbound();
      if (resBuf == null) {
        TimeUnit.NANOSECONDS.sleep(1);
        continue;
      }
      Assertions.assertNotNull(resBuf);
      resBuf.skipBytes(Integer.BYTES);
      int protoId = NettyByteBufUtil.readVarInt32(resBuf);
      Assertions.assertNotEquals(0, protoId);
      Assertions.assertEquals(str, serdes.readObject(resBuf));

      Assertions.assertFalse(resBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());
      ReferenceCountUtil.release(resBuf);
      break;
    }
  }

  @RepeatedTest(100)
  public void echoLoop() throws Exception {
    int loops = 1000;
    String[] strs = IntStream.range(0, loops)
        .mapToObj(ignore -> String.valueOf(ThreadLocalRandom.current().nextLong()))
        .toArray(String[]::new);
    for (String str : strs) {
      invoker.of(embeddedChannel.attr(Connection.CONNECTION).get())
          .echo(str);
    }

    //模拟网络消息
    TimeUnit.NANOSECONDS.sleep(1);
    for (int i = 0; i < strs.length; i++) {
      {
        ByteBuf reqBuf = embeddedChannel.readOutbound();
        embeddedChannel.writeInbound(reqBuf);
      }
    }

    for (String str : strs) {
      //验证返回的结果
      while (true) {
        ByteBuf resBuf = embeddedChannel.readOutbound();
        if (resBuf == null) {
          TimeUnit.NANOSECONDS.sleep(1);
          continue;
        }
        Assertions.assertNotEquals(0, resBuf.readInt());
        int protoId = NettyByteBufUtil.readVarInt32(resBuf);
        Assertions.assertNotEquals(0, protoId);
        Assertions.assertEquals(str, serdes.readObject(resBuf));

        Assertions.assertFalse(resBuf.isReadable());
        ReferenceCountUtil.release(resBuf);
        break;
      }
    }

    Assertions.assertNull(embeddedChannel.readOutbound());
  }

  @RepeatedTest(10)
  public void nothing() throws Exception {
    invoker.of(embeddedChannel.attr(Connection.CONNECTION).get())
        .nothing();

    //验证请求的信息
    {
      ByteBuf reqBuf = embeddedChannel.readOutbound();
      reqBuf.markReaderIndex();
      reqBuf.skipBytes(Integer.BYTES);
      Assertions.assertNotEquals(0, NettyByteBufUtil.readVarInt32(reqBuf));
      Assertions.assertArrayEquals(ArrayUtils.EMPTY_BYTE_ARRAY, NettyByteBufUtil.readBytes(reqBuf));
      Assertions.assertFalse(reqBuf.isReadable());
      Assertions.assertNull(embeddedChannel.readOutbound());

      reqBuf.resetReaderIndex();
      embeddedChannel.writeInbound(reqBuf);
    }

    //验证返回的结果
    {
      TimeUnit.MILLISECONDS.sleep(10);
      ByteBuf resBuf = embeddedChannel.readOutbound();
      Assertions.assertNull(resBuf);
    }
  }

  @RepeatedTest(10)
  public void callBack() throws Exception {
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
            embeddedChannel.attr(Connection.CONNECTION).get())
        .callback(boolean1, byte1, short1, char1, int1, long1, float1, double1, reqMove, resMove);

    {
      //第一次向callback发送消息，然后callback返回结果
      ByteBuf reqBuf1 = embeddedChannel.readOutbound();
      embeddedChannel.writeInbound(reqBuf1);

      //然后在将结果发送一次
      while (true) {
        ByteBuf resBuf2 = embeddedChannel.readOutbound();
        if (resBuf2 == null) {
          TimeUnit.NANOSECONDS.sleep(1);
          continue;
        }
        embeddedChannel.writeInbound(resBuf2);
        break;
      }
    }

    Assertions.assertEquals(hashcode, callback.get(100, TimeUnit.MILLISECONDS));
  }


}
