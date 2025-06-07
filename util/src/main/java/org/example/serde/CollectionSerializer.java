package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;


/**
 * 通用集合序列化，默认实现为{@link ArrayList}
 * <p>
 *
 * <pre>
 *   一维数组:
 *
 *    元素数量|唯一类型ID(非0需处理)|元素1|元素2|元素3|元素3|
 *
 *    元素数量:1-5字节, 使用varint32和ZigZga编码
 *    元素:实现决定
 * </pre>
 * <p>
 * 与{@link Serdes} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class CollectionSerializer implements Serializer<Object> {

  private final Class<?> type;

  /**
   * 集合提供者
   */
  private final IntFunction<Collection<Object>> factory;

  public Class<?> getType() {
    return type;
  }

  public CollectionSerializer() {
    this(List.class, ArrayList::new);
  }

  /**
   * @param factory 根据长度创建一个集合
   * @since 2024/8/8 22:36
   */
  public CollectionSerializer(Class<?> type, IntFunction<Collection<Object>> factory) {
    this.factory = factory;
    this.type = type;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);
    if (length < 0) {
      return null;
    }

    int typeId = serializer.readVarInt32(buf);

    Collection<Object> collection = factory.apply(length);

    if (typeId != 0) {
      Serializer<Object> ser = null;
      ser = (Serializer<Object>) Objects.requireNonNull(serializer.getSeriailizer(typeId),
          () -> "未注册的类型ID:%s".formatted(typeId));

      for (int i = 0; i < length; i++) {
        collection.add(ser.readObject(serializer, buf));
      }
    } else {
      for (int i = 0; i < length; i++) {
        collection.add(serializer.readObject(buf));
      }
    }

    return collection;
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Object object) {
    if (object == null) {
      serializer.writeVarInt32(buf, -1);
    } else {
      if (!(object instanceof Collection)) {
        throw new RuntimeException("类型:" + object.getClass() + ",不是集合");
      }
      @SuppressWarnings("unchecked") Collection<Object> collection = (Collection<Object>) object;

      serializer.writeVarInt32(buf, collection.size());
      serializer.writeVarInt32(buf, 0);

      for (Object o : collection) {
        serializer.writeObject(buf, o);
      }
    }
  }

  /**
   * 此序列化实现，能否为提供的{@code clazz}进行序列化和反序列化操作
   *
   * @since 2025/6/7 10:21
   */
  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return type.isAssignableFrom(clazz);
  }


}
