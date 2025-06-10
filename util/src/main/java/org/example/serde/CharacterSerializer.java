package org.example.serde;

import io.netty.buffer.ByteBuf;

/**
 * Character序列化实现
 *
 * 与{@link Serdes} 组合使用, null会被0代理
 *
 * @since 2021年07月17日 16:16:14
 **/
public class CharacterSerializer implements Serializer<Character> {

  @Override
  public Character readObject(Serdes serializer, ByteBuf buf) {
    return buf.readChar();
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, Character object) {
    buf.writeChar(object);
  }

  @Override
  public boolean isSupport(Serdes serdes, Class<?> clazz) {
    return char.class == clazz || Character.class == clazz;
  }
}
