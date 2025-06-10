package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
public class CollectionSerializer implements Serializer<Collection<Object>> {

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
  public Collection<Object> readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);
    if (length < 0) {
      return null;
    }

    Collection<Object> collection = factory.apply(length);
    for (int i = 0; i < length; i++) {
      collection.add(serializer.readObject(buf));
    }
    return collection;
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Collection<Object> collection) {
    serializer.writeVarInt32(buf, collection.size());
    for (Object o : collection) {
      serializer.writeObject(buf, o);
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
