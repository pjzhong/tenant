package org.example.serde;

import org.example.serde.array.BooleanArraySerializer;
import org.example.serde.array.ByteArraySerializer;
import org.example.serde.array.CharArraySerializer;
import org.example.serde.array.DoubleArraySerializer;
import org.example.serde.array.FloatArraySerializer;
import org.example.serde.array.IntArraySerializer;
import org.example.serde.array.LongArraySerializer;
import org.example.serde.array.MultiDimensionsArraySerializer;
import org.example.serde.array.OneDimensionArraySerializer;
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
    Pair[] pairs = {
        new Pair(byte.class, new ByteSerializer()),
        new Pair(boolean.class, new BooleanSerializer()),
        new Pair(short.class, new ShortSerializer()),
        new Pair(char.class, new CharacterSerializer()),
        new Pair(int.class, new IntegerSerializer()),
        new Pair(long.class, new LongSerializer()),
        new Pair(float.class, new FloatSerializer()),
        new Pair(double.class, new DoubleSerializer()),
        new Pair(String.class, new StringSerializer()),
        new Pair(byte[].class, new ByteArraySerializer()),
        new Pair(boolean[].class, new BooleanArraySerializer()),
        new Pair(short[].class, new ShortArraySerializer()),
        new Pair(char[].class, new CharArraySerializer()),
        new Pair(float[].class, new FloatArraySerializer()),
        new Pair(double[].class, new DoubleArraySerializer()),
        new Pair(int[].class, new IntArraySerializer()),
        new Pair(long[].class, new LongArraySerializer()),
        new Pair(String[].class, new StringArraySerializer()),
        new Pair(Object[].class, new OneDimensionArraySerializer()),
        new Pair(Object[][].class, new MultiDimensionsArraySerializer(2)),
        new Pair(Object[][][].class, new MultiDimensionsArraySerializer(3)),
        new Pair(Object[][][][].class, new MultiDimensionsArraySerializer(4)),
    };

    int id = Serdes.NULL_ID;
    for (Pair pair : pairs) {
      id += 1;
      serdes.registerSerializer(id, pair.clz, pair.ser);
    }

  }

  private record Pair(Class<?> clz, Serializer<?> ser) {

  }

}
