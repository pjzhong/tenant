package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Map集合序列化，默认实现为{@link HashMap},反序列化不保持顺序
 *
 * <pre>
 *   一维数组:
 *
 *    元素数量|唯一key类型ID(非0需处理)|唯一val类型ID(非0需处理)|KEY1|VALUE1|KEY2|VALUE2|
 *
 *    元素数量:1-5字节, 使用varint32和ZigZga编码
 *    元素:实现决定
 * </pre>
 * <p>
 * 与{@link Serdes} 组合使用
 *
 * @since 2021年07月18日 14:17:04
 **/
public class MapSerializer<K, V> implements Serializer<Map<K, V>> {

  private final Class<?> type;
  /**
   * Map工厂
   */
  private final IntFunction<Map<K, V>> supplier;

  public MapSerializer() {
    this(Map.class, HashMap::new);
  }

  public MapSerializer(Class<?> type, IntFunction<Map<K, V>> mapSupplier) {
    this.type = type;
    this.supplier = mapSupplier;
  }


  public Class<?> getType() {
    return type;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Map<K, V> readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);
    if (length < 0) {
      return null;
    }

    int keyTypeId = serializer.readVarInt32(buf);
    int valueTypeId = serializer.readVarInt32(buf);
    if (keyTypeId != 0 && valueTypeId != 0) {
      Serializer<Object> keySer = null;
      Serializer<Object> valSer = null;

      keySer = (Serializer<Object>) Objects.requireNonNull(serializer.getSeriailizer(keyTypeId),
          () -> "未注册的类型ID:%s".formatted(keyTypeId));

      valSer = (Serializer<Object>) Objects.requireNonNull(serializer.getSeriailizer(valueTypeId),
          () -> "未注册的类型ID:%s".formatted(valueTypeId));

      Map<K, V> map = supplier.apply(length);
      for (int i = 0; i < length; i++) {
        K key = (K) keySer.readObject(serializer, buf);
        V val = (V) valSer.readObject(serializer, buf);
        map.put(key, val);
      }
      return map;
    } else {
      Map<K, V> map = supplier.apply(length);
      for (int i = 0; i < length; i++) {
        K key = serializer.readObject(buf);
        V val = serializer.readObject(buf);
        map.put(key, val);
      }
      return map;
    }
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Map<K, V> object) {
    if (object == null) {
      serializer.writeVarInt32(buf, -1);
      return;
    }

    int length = object.size();
    serializer.writeVarInt32(buf, length);
    serializer.writeVarInt32(buf, 0);
    serializer.writeVarInt32(buf, 0);

    for (Entry<K, V> e : object.entrySet()) {
      Object key = e.getKey();
      Object val = e.getValue();
      serializer.writeObject(buf, key);
      serializer.writeObject(buf, val);
    }
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return type.isAssignableFrom(clazz);
  }

}
