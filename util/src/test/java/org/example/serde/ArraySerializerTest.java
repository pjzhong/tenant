package org.example.serde;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class ArraySerializerTest {

  private Serdes write;
  private Serdes read;
  private ByteBuf buf;

  @BeforeEach
  public void beforeEach() {
    write = new Serdes();
    new DefaultSerializersRegister().register(write);

    write.registerObject(ArrayWrapper.class);

    read = new Serdes();
    new DefaultSerializersRegister().register(read);

    read.registerObject(ArrayWrapper.class);
    buf = Unpooled.buffer();
  }

  @Test
  public void emptyArrayTest() {
    int[] test = {};
    write.serialize(buf, test);

    int[] res = read.deserialize(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void simpleArrayTest() {
    {
      int[] test = new int[1000];
      for (int i = 0; i < test.length; i++) {
        test[i] = ThreadLocalRandom.current().nextInt();
      }
      write.serialize(buf, test);

      int[] res = read.deserialize(buf);
      assertArrayEquals(test, res);
    }
  }

  @Test
  public void twoDimensionIntArrayTest() {
    int[][] test = {{1, 10}, {10, 1}};
    write.serialize(buf, test);
    int[][] res = read.deserialize(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void threeDimensionDoubleArrayTest() {
    Random random = ThreadLocalRandom.current();
    double[][][] test = new double[2][3][4];
    for (int o = 0; o < 2; o++) {
      for (int t = 0; t < 3; t++) {
        for (int th = 0; th < 4; th++) {
          test[o][t][th] = random.nextDouble();
        }
      }
    }
    write.serialize(buf, test);
    double[][][] res = read.deserialize(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void fourDimensionStringArrayTest() {
    Random random = ThreadLocalRandom.current();
    String[][][][] test = new String[1][2][3][4];
    for (int f = 0; f < 1; f++) {
      for (int o = 0; o < 2; o++) {
        for (int t = 0; t < 3; t++) {
          for (int th = 0; th < 4; th++) {
            if (random.nextBoolean()) {
              test[f][o][t][th] = "a" + random.nextInt();
            } else if (random.nextBoolean()) {
              test[f][o][t][th] = "";
            }
          }
        }
      }
    }

    write.serialize(buf, test);
    String[][][][] res = read.deserialize(buf);
    assertArrayEquals(test, res);
  }

  @RepeatedTest(100)
  public void fourDimensionObjectArrayTest() {
    Random random = ThreadLocalRandom.current();
    int one = 5, two = 2, three = 3, four = 4;
    ArrayWrapper[][][][] test = new ArrayWrapper[one][two][three][four];
    int special = 2;
    for (int f = 0; f < one; f++) {
      for (int o = 0; o < two; o++) {
        for (int t = 0; t < three; t++) {
          for (int th = 0; th < four; th++) {
            ArrayWrapper wrap = new ArrayWrapper();
            if (th != special) {
              wrap.str = "a" + random.nextInt();
            } else {
              int length = random.nextInt(100);
              wrap.test = new int[length];
              for (int i = 0; i < length; i++) {
                wrap.test[i] = random.nextInt();
              }
            }
            test[f][o][t][th] = wrap;
          }
        }
      }
    }

    write.serialize(buf, test);
    ArrayWrapper[][][][] res = read.deserialize(buf);
    assertArrayEquals(test, res);
  }

  @Test
  public void objectArraySerializerTest() {
    Object[] objects = {1, 2L, "asdfasdf", new ArrayWrapper(), null, 'a'};

    write.serialize(buf, objects);
    Object[] res = read.deserialize(buf);
    assertArrayEquals(objects, res);
  }

  private static class ArrayWrapper {

    private String str;
    private int[] test;

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ArrayWrapper that = (ArrayWrapper) o;
      return Objects.equals(str, that.str) &&
          Arrays.equals(test, that.test);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(str);
      result = 31 * result + Arrays.hashCode(test);
      return result;
    }

    @Override
    public String toString() {
      return "StringWrapper{" +
          "str='" + str + '\'' +
          '}';
    }
  }

}
