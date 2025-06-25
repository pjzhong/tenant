package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * 枚举类序列化
 * <p>
 * 与{@link Serdes} 组合使用
 *
 * @since 2022年03月13日 16:16:14
 */
public class EnumSerializer<E extends Enum<E>> implements Serializer<E> {

  /**
   * -1表示NULL
   */
  private static final int NULL_IDX = -1;

  /**
   * Enum的Class和内容
   */
  private final Class<E> enumClass;
  private final E[] values;

  public EnumSerializer(Class<E> clazz) {
    if (!clazz.isEnum()) {
      throw new IllegalArgumentException(clazz.getName() + " 不是枚举类");
    }

    values = clazz.getEnumConstants();
    enumClass = clazz;
  }


  public static <E extends Enum<E>> EnumSerializer<E> of(Class<E> enumuClass) {
    return new EnumSerializer<>(enumuClass);
  }

  @Override
  public E deserialize(Serdes serializer, ByteBuf buf) {
    int idx = serializer.readVarInt32(buf);
    if (idx == NULL_IDX) {
      return null;
    }

    if (idx < 0 || values.length <= idx) {
      throw new IllegalArgumentException(
          String.format("枚举类%s不存在下标为%s的元素", enumClass.getName(), idx));
    }

    return values[idx];
  }

  @Override
  public void serialize(Serdes serializer, ByteBuf buf, E object) {
    int idx = NULL_IDX;
    if (object != null) {
      idx = object.ordinal();
    }

    serializer.writeVarInt32(buf, idx);
  }
}
