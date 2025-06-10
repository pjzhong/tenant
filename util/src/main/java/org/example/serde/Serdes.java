package org.example.serde;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.example.serde.array.MultiDimensionsArraySerializer;
import org.example.serde.array.OneDimensionArraySerializer;
import org.example.util.NettyByteBufUtil;

/**
 * 序列化组合实现,业务主要入口
 * <p>
 *
 * @since 2021年07月17日 16:30:05
 **/
public final class Serdes {

  public static final int NULL_ID = 0;

  /**
   * 序列化注册 [类型ID -> 序列化实现]
   */
  private Int2ObjectOpenHashMap<SerializerPair> id2Serders;
  /**
   * 序列化注册 [目标类型 -> 序列化实现]
   */
  private Map<Class<?>, SerializerPair> type2Serders;

  public record SerializerPair(Serializer<?> serializer, Class<?> clz, int typeId) {

  }

  public Serdes() {
    type2Serders = new HashMap<>();
    id2Serders = new Int2ObjectOpenHashMap<>();
  }


  /**
   * 注册普通序列化
   *
   * @param clazz 类型
   * @since 2021年07月18日 11:37:14
   */
  public void registerObject(Class<?> clazz) {
    registerObject(clazz.getName().hashCode(), clazz);
  }

  /**
   * 注册对象序列化
   *
   * @param id    类型ID
   * @param clazz 类型
   * @since 2021年07月18日 11:37:14
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void registerObject(Integer id, Class clazz) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(clazz);

    if (clazz.isArray()) {
      registerSerializer(id, clazz, createArraySerializer(clazz));
    } else if (clazz.isRecord()) {
      Serializer<?> serializer = new RecordSerializer(clazz);
      registerSerializer(id, clazz, serializer);
    } else if (clazz.isEnum()) {
      registerSerializer(id, clazz, new EnumSerializer<>(clazz));
    } else {
      ObjectSerializer.checkClass(clazz);
      Serializer<?> serializer = new ObjectSerializer(clazz);
      registerSerializer(id, clazz, serializer);
    }
  }

  private Serializer<Object> createArraySerializer(Class<?> arrayType) {
    int dimension = 0;
    while (arrayType.isArray()) {
      dimension += 1;
      arrayType = arrayType.getComponentType();
    }

    if (dimension == 1) {
      return new OneDimensionArraySerializer();
    } else {
      return new MultiDimensionsArraySerializer(dimension);
    }
  }

  /**
   * 注册序列化
   *
   * @param clazz      类型
   * @param serializer 序列化实现
   * @since 2021年07月18日 11:37:14
   */
  public void registerSerializer(Class<?> clazz, Serializer<?> serializer) {
    registerSerializer(clazz.getName().hashCode(), clazz, serializer);
  }


  /**
   * 注册序列化
   *
   * @param id         类型ID
   * @param clazz      类型
   * @param serializer 序列化实现
   * @since 2021年07月18日 11:37:14
   */
  public void registerSerializer(int id, Class<?> clazz, Serializer<?> serializer) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(serializer);

    if (id2Serders.containsKey(id)) {
      throw new RuntimeException(
          String.format("%s,%s 类型ID:%s。发生冲突", id2Serders.get(id).clz(), clazz, id));
    }

    if (type2Serders.containsKey(clazz)) {
      SerializerPair pair = type2Serders.get(clazz);
      throw new RuntimeException(
          String.format(
              "clz:%s, id:%s, cls:%s,id:%s 类型ID不一致", pair.clz(), pair.typeId(), clazz, id));
    }

    SerializerPair pair = new SerializerPair(serializer, clazz, id);

    id2Serders.put(id, pair);
    type2Serders.put(clazz, pair);
  }

  /**
   * 根据类型获取类型ID
   *
   * @param cls 类型
   * @since 2021年07月18日 16:18:08
   */
  public SerializerPair getSerializerPair(Class<?> cls) {
    return type2Serders.get(cls);
  }

  public SerializerPair getSerializerPair(int typeId) {
    return id2Serders.get(typeId);
  }

  /**
   * 根据类型ID获取类型
   *
   * @param typeId 类型ID
   * @since 2021年07月18日 16:18:08
   */
  public Serializer<?> getSeriailizer(int typeId) {
    SerializerPair typePair = id2Serders.get(typeId);
    return typePair != null ? typePair.serializer : null;
  }

  public boolean isNullId(int id) {
    return id == NULL_ID;
  }

  public void writeNull(ByteBuf buf) {
    writeVarInt32(buf, NULL_ID);
  }

  public void writeInt32(ByteBuf buf, int value) {
    buf.writeInt(value);
  }

  /**
   * {@link NettyByteBufUtil#writeVarInt32(ByteBuf, int)}
   *
   * @since 2025/1/21 18:46
   */
  public void writeVarInt32(ByteBuf buf, int value) {
    NettyByteBufUtil.writeVarInt32(buf, value);
  }

  public int readInt32(ByteBuf buf) {
    return buf.readInt();
  }

  /**
   * {@link NettyByteBufUtil#readVarInt32(ByteBuf)} (ByteBuf, int)}
   *
   * @since 2025/1/21 18:46
   */
  public int readVarInt32(ByteBuf buf) {
    return NettyByteBufUtil.readVarInt32(buf);
  }


  public void writeInt64(ByteBuf buf, long value) {
    buf.writeLong(value);
  }


  /**
   * {@link NettyByteBufUtil#writeVarInt64(ByteBuf, long)} (ByteBuf, int)}
   *
   * @since 2025/1/21 18:46
   */
  public void writeVarInt64(ByteBuf buf, long value) {
    NettyByteBufUtil.writeVarInt64(buf, value);
  }

  public long readInt64(ByteBuf buf) {
    return buf.readLong();
  }

  /**
   * {@link NettyByteBufUtil#readVarInt64(ByteBuf)} (ByteBuf)}
   *
   * @since 2025/1/21 18:46
   */
  public long readVarInt64(ByteBuf buf) {
    return NettyByteBufUtil.readVarInt64(buf);
  }

  @SuppressWarnings("unchecked")
  public <V> V readObject(ByteBuf buf) {
    return (V) read(buf);
  }

  private Object read(ByteBuf buf) {
    int readerIndex = buf.readerIndex();
    try {
      int typeId = readVarInt32(buf);
      if (typeId == NULL_ID) {
        return null;
      }

      SerializerPair clazz = getSerializerPair(typeId);
      if (clazz == null) {
        throw new NullPointerException("类型ID:" + typeId + "，未注册");
      }

      return clazz.serializer.readObject(this, buf);
    } catch (Exception e) {
      buf.readerIndex(readerIndex);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public void writeObject(ByteBuf buf, Object object) {
    if (object == null) {
      writeNull(buf);
      return;
    }

    Class<?> clazz = object.getClass();
    SerializerPair pair = getSerializerPair(clazz);
    if (pair == null) {
      pair = trySerachAndBindSerializer(clazz);
    }

    if (pair == null) {
      throw new RuntimeException("类型:" + clazz + "，未注册");
    }

    int writeIdx = buf.writerIndex();
    try {
      Serializer<Object> serializer = (Serializer<Object>) pair.serializer;
      writeVarInt32(buf, pair.typeId);
      serializer.writeObject(this, buf, object);
    } catch (Exception e) {
      buf.writerIndex(writeIdx);
      throw new RuntimeException("类型:" + clazz + ",序列化错误", e);
    }
  }

  public SerializerPair trySerachAndBindSerializer(Class<?> clazz) {
    SerializerPair pair = type2Serders.get(clazz);
    if (pair != null) {
      return pair;
    }

    SerializerPair res = id2Serders.values().stream()
        .filter(id2Serde -> id2Serde.serializer.isSupport(this, clazz))
        .min(Comparator.comparingInt(SerializerPair::typeId))
        .orElse(null);

    if (res != null) {
      type2Serders.put(clazz, pair = res);
    }
    return pair;
  }
}
