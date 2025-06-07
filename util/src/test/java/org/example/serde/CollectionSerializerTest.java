package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;


/**
 * 集合序列化测试
 *
 * @author ZJP
 * @since 2021年07月18日 14:17:04
 **/
public class CollectionSerializerTest {

  /** 业务入口 */
  private Serdes serializer;
  /** 临时buff */
  private ByteBuf buf;

  @BeforeEach
  public void beforeEach() {
    serializer = new Serdes();
    buf = Unpooled.buffer();

    new DefaultSerializersRegister().register(serializer);
    CollectionSerializer collectSer = new CollectionSerializer();

    serializer.registerSerializer(collectSer.getType(), collectSer);
    serializer.registerSerializer(LinkedList.class,
        new CollectionSerializer(LinkedList.class, i -> new LinkedList<>()));
  }

  @RepeatedTest(3)
  public void intCollectionTest() {
    Random random = ThreadLocalRandom.current();
    List<Integer> col = new ArrayList<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(random.nextInt());
    }

    serializer.writeObject(buf, col);
    List<Integer> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void doubleCollectionLinkedTest() {
    Random random = ThreadLocalRandom.current();
    List<Double> col = new LinkedList<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(random.nextDouble());
    }

    serializer.writeObject(buf, col);
    List<Double> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void strCollectionTest() {
    Random random = ThreadLocalRandom.current();
    List<String> col = new ArrayList<>();
    int size = random.nextInt(Short.MAX_VALUE);
    for (int i = 0; i < size; i++) {
      col.add(Integer.toString(random.nextInt()));
    }

    col.set(random.nextInt(size), null);

    serializer.writeObject(buf, col);
    List<String> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

  @Test
  public void colCollectionTest() {
    Random random = ThreadLocalRandom.current();
    List<List<String>> col = new ArrayList<>();
    int size = Byte.MAX_VALUE;
    for (int i = 0; i < size; i++) {
      int subSize = random.nextInt(size);
      List<String> strs = new ArrayList<>(subSize);
      for (int j = 0; j < subSize; j++) {
        strs.add("1q23412341234");
      }
      col.add(strs);
    }

    col.set(random.nextInt(size), null);

    serializer.writeObject(buf, col);
    List<List<String>> res = serializer.readObject(buf);
    assertEquals(col, res);
  }

}
