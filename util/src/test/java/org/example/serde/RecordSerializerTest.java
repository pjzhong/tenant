package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

public class RecordSerializerTest {


  @Test
  public void emptyTest() {
    Serdes serdes = new Serdes();
    serdes.registerObject(Empty.class);

    Empty empty = new Empty();
    ByteBuf buf = Unpooled.buffer();
    serdes.serialize(buf, empty);

    Empty res = serdes.deserialize(buf);
    assertEquals(empty, res);
  }

  @Test
  public void primityTest() {
    Serdes serdes = new Serdes();
    new DefaultSerializersRegister().register(serdes);
    serdes.registerObject(Empty.class);
    serdes.registerObject(Compose.class);

    Compose<Double> abc = new Compose<Double>((byte) 0, 'c', (short) 1, 1, 2L, 3F, 3.0,false, "EEEEEE", new Empty());
    ByteBuf buf = Unpooled.buffer();
    serdes.serialize(buf, abc);

    Compose<Double> res = serdes.deserialize(buf);
    assertEquals(abc, res);
  }

  @Test
  public void composeTest() {
    Serdes serdes = new Serdes();
    new DefaultSerializersRegister().register(serdes);
    serdes.registerSerializer(Empty.class, new EmptySerde());
    serdes.registerSerializer(Compose.class, new ComposeSerde());

    {
      ByteBuf buf = Unpooled.buffer();
      Empty empty = new Empty();
      serdes.serialize(buf, empty);
      Empty res = serdes.deserialize(buf);
      assertEquals(empty, res);
      assertFalse(buf.isReadable());
    }

    {
      ByteBuf buf = Unpooled.buffer();
      Compose<Double> abc = new Compose<Double>((byte) 0, 'c', (short) 1, 1, 2L, 3F, 3.0,false, "EEEEEE", new Empty());
      serdes.serialize(buf, abc);
      Compose<Double> res = serdes.deserialize(buf);
      assertEquals(abc, res);
      assertFalse(buf.isReadable());
    }

  }


  @Serde
  public record Empty() {

  }

  @Serde
  public record Compose<T>(byte b, char c, short s, int a, long l, float f, T t, boolean bool, String d,
                        Empty e) {
  }

}
