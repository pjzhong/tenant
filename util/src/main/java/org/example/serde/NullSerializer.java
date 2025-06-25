package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * null默认实现
 * <p>
 * 与{@link Serdes} 组合使用，提供完成功能
 *
 * @since 2021年07月17日 17:02:35
 **/
public final class NullSerializer implements Serializer<Object> {

  public NullSerializer() {

  }

  @Override
  public Object deserialize(Serdes serializer, ByteBuf buf) {
    return null;
  }

  @Override
  public void serialize(Serdes serializer, ByteBuf buf, Object object) {
  }
}
