package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Byte序列化实现
 *
 * 与{@link Serdes} 组合使用, null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class BooleanSerializer implements Serializer<Boolean> {

  @Override
  public Boolean readObject(Serdes serializer, ByteBuf buf) {
    return buf.readBoolean();
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Boolean object) {
    buf.writeBoolean(object);
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return boolean.class == clazz || Boolean.class == clazz;
  }
}
