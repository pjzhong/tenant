package org.example.common.model;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.example.serde.CollectionSerializer;
import org.example.serde.Serde;
import org.example.serde.Serdes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class FastSerdeTest {


  private static Serdes codeSerde;


  @BeforeAll
  public static void prepare() {
    codeSerde = new Serdes();
    codeSerde.registerSerializer(List.class, new CollectionSerializer());
    new ReqMoveSerde().register(codeSerde);
    new ResMoveSerde().register(codeSerde);
    new ComposeSerde().register(codeSerde);
  }

  private ReqMove req;
  private ResMove res;

  @BeforeEach
  public void createObj() {
    req = new ReqMove();
    ThreadLocalRandom current = ThreadLocalRandom.current();
    req.setId(current.nextInt());
    req.setX(current.nextInt());
    req.setY(current.nextInt());

    res = new ResMove();
    res.setId(current.nextInt());
    res.setX(current.nextInt());
    res.setY(current.nextInt());
    res.setDir(current.nextInt());
  }

  @Test
  public void resTest() {
    ByteBuf one = Unpooled.buffer();
    ByteBuf two = Unpooled.buffer();

    codeSerde.serialize(one, res);
    ResMoveSerde.fastSerializer(codeSerde, two, res);

    ResMove move1 = codeSerde.deserialize(two);
    ResMove move2 = ResMoveSerde.fastDeserialzier(codeSerde, one);

    Assertions.assertEquals(res, move1);
    Assertions.assertEquals(res, move2);
    Assertions.assertFalse(one.isReadable());
    Assertions.assertFalse(two.isReadable());
  }

  @Test
  public void reqTest() {
    ByteBuf one = Unpooled.buffer();
    ByteBuf two = Unpooled.buffer();

    codeSerde.serialize(one, req);
    ReqMoveSerde.fastSerializer(codeSerde, two, req);

    ReqMove move1 = codeSerde.deserialize(two);
    ReqMove move2 = ReqMoveSerde.fastDeserialzier(codeSerde, one);

    Assertions.assertEquals(req, move1);
    Assertions.assertEquals(req, move2);
    Assertions.assertFalse(one.isReadable());
    Assertions.assertFalse(two.isReadable());
  }

  @RepeatedTest(10)
  public void mixTest() {
    ByteBuf one = Unpooled.buffer();
    ByteBuf two = Unpooled.buffer();

    Compose compose = new Compose(req, res);
    {
      codeSerde.serialize(one, compose);
      ComposeSerde.fastSerializer(codeSerde, two, compose);
    }

    Compose resCompose1 = codeSerde.deserialize(two);
    Compose resCompose2 = ComposeSerde.fastDeserialzier(codeSerde, one);

    Assertions.assertEquals(resCompose1, resCompose2);
    Assertions.assertEquals(req, resCompose1.req);
    Assertions.assertEquals(res, resCompose1.res);
    Assertions.assertEquals(req, resCompose2.req);
    Assertions.assertEquals(res, resCompose2.res);
    Assertions.assertFalse(one.isReadable());
    Assertions.assertFalse(two.isReadable());
  }


  @Serde
  public record Compose(ReqMove req, ResMove res) {

  }


}
