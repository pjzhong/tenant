package org.example.serde;

import org.example.serde.array.BooleanArraySerializer;
import org.example.serde.array.ByteArraySerializer;
import org.example.serde.array.CharArraySerializer;
import org.example.serde.array.DoubleArraySerializer;
import org.example.serde.array.FloatArraySerializer;
import org.example.serde.array.IntArraySerializer;
import org.example.serde.array.LongArraySerializer;
import org.example.serde.array.ShortArraySerializer;
import org.example.serde.array.StringArraySerializer;

public class DefaultSerializersRegister {


  /**
   * 注册常用的类型解析器
   *
   * @since 2021年07月19日 23:00:35
   */
  public void register(Serdes serdes) {
    serdes.registerSerializer(Serdes.NULL_ID, NullSerializer.class, new NullSerializer());
    serdes.registerSerializer(1, Byte.class, new ByteSerializer());
    serdes.registerSerializer(2, Boolean.class, new BooleanSerializer());
    serdes.registerSerializer(3, Short.class, new ShortSerializer());
    serdes.registerSerializer(4, Integer.class, new IntegerSerializer());
    serdes.registerSerializer(5, Long.class, new LongSerializer());
    serdes.registerSerializer(6, Float.class, new FloatSerializer());
    serdes.registerSerializer(7, Double.class, new DoubleSerializer());
    serdes.registerSerializer(9, Character.class, new CharacterSerializer());
    serdes.registerSerializer(10, String.class, new StringSerializer());
    serdes.registerSerializer(12, byte[].class, new ByteArraySerializer());
    serdes.registerSerializer(13, boolean[].class, new BooleanArraySerializer());
    serdes.registerSerializer(14, short[].class, new ShortArraySerializer());
    serdes.registerSerializer(15, char[].class, new CharArraySerializer());
    serdes.registerSerializer(16, float[].class, new FloatArraySerializer());
    serdes.registerSerializer(17, double[].class, new DoubleArraySerializer());
    serdes.registerSerializer(18, int[].class, new IntArraySerializer());
    serdes.registerSerializer(19, long[].class, new LongArraySerializer());
    serdes.registerSerializer(20, String[].class, new StringArraySerializer());
  }

}
