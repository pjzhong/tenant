package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import org.example.serde.Serdes;
import org.example.serde.Serdes.SerializerPair;
import org.example.serde.Serializer;

/**
 * JAVA数组序列化
 *
 *
 * <pre>
 *   一维数组:
 *
 *    元素类型|长度|元素1|元素2|
 *
 *
 *  二维数组(都压缩成一维数组)
 *
 *    长度=N|元素1|......|元素N
 *
 *
 *    长度:1-5字节, 使用varint32和ZigZag编码
 *    元素:实现决定
 * </pre>
 * <p>1.数组长宽必须一致</p>
 * <p>2.暂时不支持PrimitiveWrapper数组，序列化时会全部转化为对应的基础类型</p>
 * <p>
 * 与{@link Serdes} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class OneDimensionArraySerializer implements Serializer<Object> {


  public OneDimensionArraySerializer() {
  }


  @Override
  public Object deserialize(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);
    Class<?> componentType;
    int typeId = serializer.readVarInt32(buf);
    if (serializer.isNullId(typeId)) {
      componentType = Object.class;
    } else {
      SerializerPair pair = serializer.getSerializerPair(typeId);
      if (pair == null) {
        throw new UnsupportedOperationException("类型ID:" + typeId + ",未注册");
      }

      componentType = pair.clz();
    }

    Object array = Array.newInstance(componentType, length);
    for (int i = 0; i < length; ++i) {
      Array.set(array, i, serializer.deserialize(buf));
    }
    return array;
  }


  @Override
  public void serialize(Serdes serializer, ByteBuf buf, Object object) {
    final int length = Array.getLength(object);
    serializer.writeVarInt32(buf, length);

    Class<?> componentType = object.getClass().getComponentType();
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
      serializer.serialize(buf, Array.get(object, i));
    }
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

    return dimensions == 1;
  }
}
