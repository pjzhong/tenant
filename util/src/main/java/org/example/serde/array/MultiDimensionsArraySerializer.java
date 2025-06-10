package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import org.example.serde.Serdes;
import org.example.serde.Serdes.SerializerPair;
import org.example.serde.Serializer;

/**
 * 多维数组序列化实现
 * <p>
 * 与{@link Serdes} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/

public class MultiDimensionsArraySerializer implements Serializer<Object> {

  private final int dimension;

  public MultiDimensionsArraySerializer(int dimension) {
    if (dimension <= 0) {
      throw new IllegalArgumentException("dimension 必须大于0");
    }

    this.dimension = dimension;
  }

  @Override
  public Object readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);

    Class<?> componentType;
    int typeId = serializer.readVarInt32(buf);
    if (serializer.isNullId(typeId)) {
      componentType = Object.class;
    } else {
      SerializerPair componentPair = serializer.getSerializerPair(typeId);
      if (componentPair == null) {
        throw new UnsupportedOperationException("类型ID:%s,未注册, 多维数组序列化失败");
      }

      componentType = componentPair.clz();
    }

    int[] dimensions = new int[dimension];
    dimensions[0] = length;
    Object array = Array.newInstance(componentType, dimensions);

    for (int i = 0; i < length; i++) {
      Array.set(array, i, serializer.readObject(buf));
    }
    return array;
  }


  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Object object) {
    final int length = Array.getLength(object);
    serializer.writeVarInt32(buf, length);

    Class<?> componentType = getComponentType(object);

    if (componentType == Object.class) {
      serializer.writeNull(buf);
    } else {
      SerializerPair componentPair = serializer.trySerachAndBindSerializer(componentType);
      if (componentPair == null || componentType != componentPair.clz()) {
        throw new UnsupportedOperationException(
            """
                类型:%s,未注册, 多维数组【%s】序列化失败。\s
                修复提示：
                1.包装类数组(Integer[],Long[], FLoat[]), 请使用基础类型数组代替(int[], long[], float[])"""
                .formatted(componentType, object.getClass()));
      }

      serializer.writeVarInt32(buf, componentPair.typeId());
    }

    for (int i = 0; i < length; i++) {
      serializer.writeObject(buf, Array.get(object, i));
    }
  }

  private static Class<?> getComponentType(Object object) {
    Class<?> c = object.getClass();
    if (!c.isArray()) {
      return null;
    }

    while (c.isArray()) {
      c = c.getComponentType();
    }
    return c;
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    if (clazz == null || !clazz.isArray()) {
      return false;
    }

    int dimensions = 0;
    while (clazz.isArray()) {
      dimensions += 1;
      clazz = clazz.getComponentType();
    }

    return dimensions == dimension;
  }
}
