package org.example.common.model;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.example.serde.CollectionSerializer;
import org.example.serde.Serdes;
import org.example.serde.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class SerdeTest {


  private static Serdes codeSerde;


  @BeforeAll
  public static void prepare() {
    codeSerde = new Serdes();
    new ReqMoveSerde().register(codeSerde);
    new ResMoveSerde().register(codeSerde);
    new ComposeObjectSerde().register(codeSerde);
    new CommonResSerde().register(codeSerde);

    codeSerde.registerSerializer(String.class, new StringSerializer());
    codeSerde.registerSerializer(List.class, new CollectionSerializer());
  }

  private ReqMove req;
  private ResMove res;
  private ComposeObject composeObject;

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

    composeObject = new ComposeObject();
    composeObject.setId(new AvatarId(current.nextInt()));
    composeObject.setMove(req);
    composeObject.setRes(
        new CommonRes<>(current.nextBoolean(), String.valueOf(current.nextLong())));

  }

  @Test
  public void resTest() {
    ByteBuf byteBuf = Unpooled.buffer();

    codeSerde.serialize(byteBuf, res);
    ResMove move = codeSerde.deserialize(byteBuf);

    Assertions.assertEquals(res, move);
    Assertions.assertFalse(byteBuf.isReadable());
  }

  @Test
  public void reqTest() {
    ByteBuf byteBuf = Unpooled.buffer();

    codeSerde.serialize(byteBuf, req);
    ReqMove move = codeSerde.deserialize(byteBuf);

    Assertions.assertEquals(req, move);
    Assertions.assertFalse(byteBuf.isReadable());
  }

  @Test
  public void composeTest() {
    ByteBuf byteBuf = Unpooled.buffer();

    codeSerde.serialize(byteBuf, composeObject);
    ComposeObject res = codeSerde.deserialize(byteBuf);

    Assertions.assertEquals(composeObject, res);
    Assertions.assertFalse(byteBuf.isReadable());
  }

  @RepeatedTest(10)
  public void mixTest() {
    ByteBuf byteBuf = Unpooled.buffer();

    codeSerde.serialize(byteBuf, req);
    codeSerde.serialize(byteBuf, res);
    codeSerde.serialize(byteBuf, composeObject);
    ReqMove reqMove = codeSerde.deserialize(byteBuf);
    ResMove resMove = codeSerde.deserialize(byteBuf);
    ComposeObject composeObject = codeSerde.deserialize(byteBuf);

    Assertions.assertEquals(req, reqMove);
    Assertions.assertEquals(res, resMove);
    Assertions.assertEquals(this.composeObject, composeObject);
    Assertions.assertFalse(byteBuf.isReadable());
  }


}
