package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Float序列化实现
 * <p>
 * 与{@link Serdes} 组合使用,null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class FloatSerializer implements Serializer<Float> {

  @Override
  public Float readObject(Serdes serializer, ByteBuf buf) {
    return buf.readFloat();
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Float object) {
    buf.writeFloat(object);
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return float.class == clazz || Float.class == clazz;
  }
}
