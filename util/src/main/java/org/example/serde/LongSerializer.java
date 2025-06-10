package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Integer序列化实现(使用varint32和ZigZag32进行编码)
 *
 * 与{@link Serdes} 组合使用,null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class LongSerializer implements Serializer<Long> {

  @Override
  public Long readObject(Serdes serializer, ByteBuf buf) {
    return serializer.readVarInt64(buf);
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Long object) {
    serializer.writeVarInt64(buf, object);
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return long.class == clazz || Long.class == clazz;
  }
}
