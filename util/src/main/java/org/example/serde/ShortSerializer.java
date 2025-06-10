package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Short序列化实现
 * <p>
 * 与{@link Serdes} 组合使用,null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class ShortSerializer implements Serializer<Short> {

  @Override
  public Short readObject(Serdes serializer, ByteBuf buf) {
    return buf.readShort();
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Short object) {
    buf.writeShort(object);
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return short.class == clazz || Short.class == clazz;
  }
}
