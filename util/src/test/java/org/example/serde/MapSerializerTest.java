package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapSerializerTest {

  /** 业务入口 */
  private Serdes serializer;
  /** 临时buff */
  private ByteBuf buf;

  @BeforeEach
  public void beforeEach() {
    serializer = new Serdes();
    buf = Unpooled.buffer();

    new DefaultSerializersRegister().register(serializer);

    MapSerializer<?, ?> map = new MapSerializer<>();
    serializer.registerSerializer(map.getType(), map);
    serializer.registerSerializer(LinkedHashMap.class,
        new MapSerializer<>(LinkedHashMap.class, LinkedHashMap::new));
    serializer.registerSerializer(TreeMap.class,
        new MapSerializer<>(TreeMap.class, ignore -> new TreeMap<>()));
  }

  @Test
  public void intDoubleHashMapTest() {
    Random random = ThreadLocalRandom.current();
    Map<Integer, Double> col = new HashMap<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.put(random.nextInt(), random.nextDouble());
    }

    serializer.writeObject(buf, col);
    Map<Integer, Double> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void strCollectionLinkedHashMapTest() {
    Random random = ThreadLocalRandom.current();
    Map<String, Double> col = new LinkedHashMap<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.put(Long.toString(random.nextLong()), random.nextDouble());
    }

    serializer.writeObject(buf, col);
    Map<String, Double> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void colCollectionTreeMapTest() {
    Random random = ThreadLocalRandom.current();
    Map<Integer, Map<String, Double>> col = new TreeMap<>();
    int size = Byte.MAX_VALUE;
    int old = 0;
    for (int i = 0; i < size; i++) {
      int subSize = random.nextInt(size);
      Map<String, Double> strs = new HashMap<>();
      for (int j = 0; j < subSize; j++) {
        strs.put(Long.toString(random.nextLong()), random.nextDouble());
      }
      int key = random.nextInt();
      if (col.put(key, strs) != null) {
        old = key;
      }
    }

    col.put(old, null);

    serializer.writeObject(buf, col);
    Map<Integer, Map<String, Double>> res = serializer.readObject(buf);
    assertEquals(col, res);
  }
}
