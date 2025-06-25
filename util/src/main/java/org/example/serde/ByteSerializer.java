package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Byte序列化实现
 * <p>
 * 与{@link Serdes} 组合使用, null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class ByteSerializer implements Serializer<Byte> {

  @Override
  public Byte deserialize(Serdes serializer, ByteBuf buf) {
    return buf.readByte();
  }

  @Override
  public void serialize(Serdes serializer, ByteBuf buf, Byte object) {
    buf.writeByte(object);
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return byte.class == clazz || Byte.class == clazz;
  }
}
