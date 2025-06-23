package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

/**
 * String序列化实现,UTF_8编码
 * <p>
 * 长度|内容
 * <p>
 * 长度:varint和ZigZag编码 内容:bytes
 * <p>
 * 与{@link Serdes} 组合使用
 *
 * @since 2021年07月17日 16:16:14
 **/
public class StringSerializer implements Serializer<String> {

  @Override
  public String readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readInt32(buf);
    return buf.readCharSequence(length, StandardCharsets.UTF_8).toString();
  }

  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, String object) {
    int strLenIdx = buf.writerIndex();
    serializer.writeInt32(buf, 0);

    int strStart = buf.writerIndex();
    buf.writeCharSequence(object, StandardCharsets.UTF_8);
    buf.setInt(strLenIdx, buf.writerIndex() - strStart);
  }
}
