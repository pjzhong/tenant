package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SerializerTest {

  /** 写入buf */
  private final ByteBuf buf = Unpooled.buffer();
  /** 测试对象 */
  private final Serdes serializer = new Serdes();


  @BeforeEach
  public void before() {
    new DefaultSerializersRegister().register(serializer);
    buf.clear();
  }

  @Test
  public void serdeNullTest() {
    serializer.serialize(buf, null);
    assertNull(serializer.deserialize(buf));
  }

  @Test
  public void byteSerdeTest() {
    for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
      buf.clear();
      Byte v = (byte) i;
      serializer.serialize(buf, v);
      assertEquals(v, serializer.deserialize(buf));
    }
  }

  @Test
  public void shortSerdeTest() {
    for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      buf.clear();
      Short v = (short) i;
      serializer.serialize(buf, v);
      assertEquals(v, serializer.deserialize(buf));
    }
  }

  @Test
  public void intSerdeTest() {
    for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      buf.clear();
      Integer v = i;
      serializer.serialize(buf, v);
      assertEquals(v, serializer.deserialize(buf));
    }

    buf.clear();
    serializer.serialize(buf, Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, (int) serializer.deserialize(buf));

    buf.clear();
    serializer.serialize(buf, Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, (int) serializer.deserialize(buf));
  }

  @Test
  public void longSerdeTest() {
    for (long i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      buf.clear();
      Long v = i;
      serializer.serialize(buf, v);
      assertEquals(v, serializer.deserialize(buf));
    }
  }

  @Test
  public void floatSerdeTest() {
    for (float i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      buf.clear();
      Float v = i;
      serializer.serialize(buf, v);
      assertEquals(v, serializer.deserialize(buf));
    }
  }

  @Test
  public void doubleSerdeTest() {
    for (double i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
      buf.clear();
      Double v = i;
      serializer.serialize(buf, v);
      assertEquals(v, serializer.deserialize(buf));
    }
  }

  @Test
  public void charSerdeTest() {
    Character str = '中';
    serializer.serialize(buf, str);
    assertEquals(str, serializer.deserialize(buf));
  }

  @Test
  public void strSerdeTest() throws NoSuchFieldException {
    String str = "Hello World!";
    serializer.serialize(buf, str);
    assertEquals(str, serializer.deserialize(buf));
  }
}
