package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
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
  @SuppressWarnings("unchecked")
  public Object readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);
    if (length < 0) {
      return null;
    }

    Class<?> componentType;
    Serializer<Object> ser = null;
    int typeId = serializer.readVarInt32(buf);
    if (serializer.isNullId(typeId)) {
      componentType = Object.class;
    } else {
      SerializerPair pair = serializer.getSerializerPair(typeId);
      if (pair == null) {
        throw new UnsupportedOperationException("类型ID:" + typeId + ",未注册");
      }

      componentType = pair.clz();
      if (Modifier.isFinal(componentType.getModifiers())) {
        ser = (Serializer<Object>) pair.serializer();
      }
    }

    Object array = Array.newInstance(componentType, length);
    if (ser != null) {
      for (int i = 0; i < length; ++i) {
        Array.set(array, i, ser.readObject(serializer, buf));
      }
    } else {
      for (int i = 0; i < length; ++i) {
        Array.set(array, i, serializer.readObject(buf));
      }
    }

    return array;
  }


  @Override
  @SuppressWarnings("unchecked")
  public void writeObject(Serdes serializer, ByteBuf buf, Object object) {
    final int length = Array.getLength(object);
    serializer.writeVarInt32(buf, length);

    Class<?> componentType = object.getClass().getComponentType();
    Serializer<Object> ser = null;
    if (componentType == Object.class) {
      serializer.writeNull(buf);
    } else {
      SerializerPair pair = serializer.trySerachAndBindSerializer(componentType);
      if (pair == null) {
        throw new UnsupportedOperationException("类型:" + componentType + ",未注册");
      }

      serializer.writeVarInt32(buf, pair.typeId());
      if (Modifier.isFinal(componentType.getModifiers())) {
        ser = (Serializer<Object>) pair.serializer();
      }
    }

    if (ser != null) {
      for (int i = 0; i < length; i++) {
        ser.writeObject(serializer, buf, Array.get(object, i));
      }
    } else {
      for (int i = 0; i < length; i++) {
        serializer.writeObject(buf, Array.get(object, i));
      }
    }
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return clazz != null && clazz.isArray() && !clazz.getComponentType().isArray();
  }
}
