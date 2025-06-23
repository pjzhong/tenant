package org.example.serde.array;

import io.netty.buffer.ByteBuf;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import org.example.serde.Serdes;
import org.example.serde.Serializer;

public class StringArraySerializer implements Serializer<String[]> {

  public StringArraySerializer() {

  }


  @Override
  public String[] readObject(Serdes serializer, ByteBuf buf) {
    int length = serializer.readVarInt32(buf);
    String[] array = new String[length];
    for (int i = 0; i < length; ++i) {
      int strLength = serializer.readInt32(buf);
      if (0 <= strLength) {
        array[i] = buf.readCharSequence(strLength, StandardCharsets.UTF_8).toString();
      }
    }

    return array;
  }


  @Override
  public void writeObject(Serdes serializer, ByteBuf buf, String[] object) {
    final int length = Array.getLength(object);
    serializer.writeVarInt32(buf, length);

    for (String s : object) {
      if (s == null) {
        buf.writeInt(Integer.MIN_VALUE);
      } else {
        int strLenIdx = buf.writerIndex();
        serializer.writeInt32(buf, 0);

        int strStart = buf.writerIndex();
        buf.writeCharSequence(s, StandardCharsets.UTF_8);
        buf.setInt(strLenIdx, buf.writerIndex() - strStart);
      }
    }
  }
}
