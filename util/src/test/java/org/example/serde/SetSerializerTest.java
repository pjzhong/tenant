package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * 集合序列化测试
 *
 * @author ZJP
 * @since 2021年07月18日 14:17:04
 **/
public class SetSerializerTest {

  /** 业务入口 */
  private Serdes serializer;
  /** 临时buff */
  private ByteBuf buf;

  @BeforeEach
  public void beforeEach() {
    serializer = new Serdes();
    buf = Unpooled.buffer();

    new DefaultSerializersRegister().register(serializer);
    CollectionSerializer collectSer = new CollectionSerializer(Set.class, HashSet::new);
    serializer.registerSerializer(collectSer.getType(), collectSer);
    serializer.registerSerializer(LinkedHashSet.class,
        new CollectionSerializer(LinkedHashSet.class, LinkedHashSet::new));
    serializer.registerSerializer(TreeSet.class,
        new CollectionSerializer(TreeSet.class, ignore -> new TreeSet<>()));
  }

  @Test
  public void intCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<Integer> col = new HashSet<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(random.nextInt());
    }

    serializer.writeObject(buf, col);
    Set<Integer> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void doubleCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<Double> col = new TreeSet<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(random.nextDouble());
    }

    serializer.writeObject(buf, col);
    Set<Double> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void strCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<String> col = new HashSet<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(Integer.toString(random.nextInt()));
    }

    serializer.writeObject(buf, col);
    Set<String> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void colCollectionTest() {
    Random random = ThreadLocalRandom.current();
    Set<Set<String>> col = new HashSet<>();
    int size = Byte.MAX_VALUE;
    for (int i = 0; i < size; i++) {
      int subSize = random.nextInt(size);
      Set<String> strs = new TreeSet<>();
      for (int j = 0; j < subSize; j++) {
        strs.add(Integer.toString(random.nextInt()));
      }
      col.add(strs);
    }

    serializer.writeObject(buf, col);
    Set<Set<String>> res = serializer.readObject(buf);
    assertEquals(col, res);
  }


}
