package org.example.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.apache.commons.lang3.ArrayUtils;
import org.example.net.Message;
import org.example.serde.Serdes;

/**
 * 编码，反编码工具类。代码部分从ProtoBuf那里复制过来。方便实现自己的需求
 *
 * @author ZJP
 * @since 2021年07月17日 09:26:40
 **/
public final class NettyByteBufUtil {

  /** 测试使用中 */
  public static final ByteBufAllocator DEFAULT = ByteBufAllocator.DEFAULT;

  private NettyByteBufUtil() {
  }

  /**
   * Write a int32 field to the {@param buf}.
   */
  public static void writeVarInt32(ByteBuf buf, int value) {
    writeRawVarint32(buf, encodeZigZag32(value));
  }

  /**
   * read a int32 field from the {@param buf}.
   */
  public static int readVarInt32(ByteBuf buf) {
    return decodeZigZag32(readRawVarint32(buf));
  }

  /**
   * Write a  uint32 field to the {@param buf}.
   */
  public static void writeRawVarint32(ByteBuf buf, int value) {
    while (true) {
      if ((value & ~0x7F) == 0) {
        buf.writeByte((byte) value);
        return;
      } else {
        buf.writeByte((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }
  }


  /**
   * Read a raw Varint from the stream. If larger than 32 bits, discard the upper bits. *
   *
   * @since 2021年07月17日 15:01:11
   */
  public static int readRawVarint32(ByteBuf buf) {
    // See implementation notes for readRawVarint64
    fastpath:
    {

      int x;
      if ((x = buf.readByte()) >= 0) {
        return x;
      } else if ((x ^= (buf.readByte() << 7)) < 0) {
        x ^= (~0 << 7);
      } else if ((x ^= (buf.readByte() << 14)) >= 0) {
        x ^= (~0 << 7) ^ (~0 << 14);
      } else if ((x ^= (buf.readByte() << 21)) < 0) {
        x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
      } else {
        int y = buf.readByte();
        x ^= y << 28;
        x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
        if (y < 0
            && buf.readByte() < 0
            && buf.readByte() < 0
            && buf.readByte() < 0
            && buf.readByte() < 0
            && buf.readByte() < 0) {
          break fastpath; // Will throw malformedVarint()
        }
      }
      return x;
    }
    return (int) readRawVarint64SlowPath(buf);
  }

  /**
   * Encode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 32-bit integer.
   * @return An unsigned 32-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   */
  public static int encodeZigZag32(final int n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 31);
  }

  /**
   * Decode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n An unsigned 32-bit integer, stored in a signed int because Java has no explicit
   *          unsigned support.
   * @return A signed 32-bit integer.
   */
  public static int decodeZigZag32(final int n) {
    return (n >>> 1) ^ -(n & 1);
  }

  /**
   * 写入long(使用ZigZag优化和varint64)
   *
   * @param buf   目前buff
   * @param value 写入的内容
   * @since 2021年07月17日 09:40:01
   */
  public static void writeVarInt64(ByteBuf buf, long value) {
    writeRawVarint64(buf, encodeZigZag64(value));
  }

  /**
   * 读一个long(使用ZigZag优化和varint64)
   *
   * @param buf 目前buff
   * @since 2021年07月17日 09:40:01
   */
  public static long readVarInt64(ByteBuf buf) {
    return decodeZigZag64(readRawVarint64(buf));
  }

  /**
   * Encode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n A signed 64-bit integer.
   * @return An unsigned 64-bit integer, stored in a signed int because Java has no explicit
   * unsigned support.
   */
  public static long encodeZigZag64(final long n) {
    // Note:  the right-shift must be arithmetic
    return (n << 1) ^ (n >> 63);
  }

  /**
   * Decode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
   * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
   * to be varint encoded, thus always taking 10 bytes on the wire.)
   *
   * @param n An unsigned 64-bit integer, stored in a signed int because Java has no explicit
   *          unsigned support.
   * @return A signed 64-bit integer.
   */
  public static long decodeZigZag64(final long n) {
    return (n >>> 1) ^ -(n & 1);
  }

  /**
   * 写入varint64
   *
   * @param buf   目前buff
   * @param value 写入的内容
   * @since 2021年07月17日 09:40:01
   */
  public static void writeRawVarint64(ByteBuf buf, long value) {
    while (true) {
      if ((value & ~0x7F) == 0) {
        buf.writeByte((byte) value);
        return;
      } else {
        buf.writeByte((byte) ((value & 0x7F) | 0x80));
        value >>>= 7;
      }
    }
  }

  /**
   * 从buf读一个 raw Varint64
   *
   * @param buf 目标buf
   * @since 2021年07月17日 10:24:55
   */
  public static long readRawVarint64(ByteBuf buf) {
    // Implementation notes:
    //
    // Optimized for one-byte values, expected to be common.
    // The particular code below was selected from various candidates
    // empirically, by winning VarintBenchmark.
    //
    // Sign extension of (signed) Java bytes is usually a nuisance, but
    // we exploit it here to more easily obtain the sign of bytes read.
    // Instead of cleaning up the sign extension bits by masking eagerly,
    // we delay until we find the final (positive) byte, when we clear all
    // accumulated bits with one xor.  We depend on javac to constant fold.
    fastpath:
    {

      long x;
      int y;
      if ((y = buf.readByte()) >= 0) {
        return y;
      } else if ((y ^= (buf.readByte() << 7)) < 0) {
        x = y ^ (~0 << 7);
      } else if ((y ^= (buf.readByte() << 14)) >= 0) {
        x = y ^ ((~0 << 7) ^ (~0 << 14));
      } else if ((y ^= (buf.readByte() << 21)) < 0) {
        x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
      } else if ((x = y ^ ((long) buf.readByte() << 28)) >= 0L) {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
      } else if ((x ^= ((long) buf.readByte() << 35)) < 0L) {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
      } else if ((x ^= ((long) buf.readByte() << 42)) >= 0L) {
        x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
      } else if ((x ^= ((long) buf.readByte() << 49)) < 0L) {
        x ^=
            (~0L << 7)
                ^ (~0L << 14)
                ^ (~0L << 21)
                ^ (~0L << 28)
                ^ (~0L << 35)
                ^ (~0L << 42)
                ^ (~0L << 49);
      } else {
        x ^= ((long) buf.readByte() << 56);
        x ^=
            (~0L << 7)
                ^ (~0L << 14)
                ^ (~0L << 21)
                ^ (~0L << 28)
                ^ (~0L << 35)
                ^ (~0L << 42)
                ^ (~0L << 49)
                ^ (~0L << 56);
        if (x < 0L) {
          if (buf.readByte() < 0L) {
            break fastpath; // Will throw malformedVarint()
          }
        }
      }
      return x;
    }
    return readRawVarint64SlowPath(buf);
  }

  /**
   * 从buf读一个 raw Varint64
   *
   * @param buf 目标buf
   * @since 2021年07月17日 10:24:55
   */
  public static long readRawVarint64SlowPath(ByteBuf buf) {
    long result = 0;
    for (int shift = 0; shift < 64; shift += 7) {
      final byte b = buf.readByte();
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new RuntimeException();
  }

  /**
   * 读取指定ByteBuf全部的全部数据
   *
   * @since 2024/8/9 15:15
   */
  public static byte[] readBytes(ByteBuf buf) {
    if (buf.isReadable()) {
      byte[] bytes = new byte[buf.readableBytes()];
      buf.readBytes(bytes);
      return bytes;
    } else {
      return ArrayUtils.EMPTY_BYTE_ARRAY;
    }
  }

  /**
   * Add the given {@link ByteBuf} and increase the {@code writerIndex} if
   * {@code increaseWriterIndex} is {@code true}.
   * <p>
   * {@link ByteBuf#release()} ownership of {@code buffer} is not transferred.
   *
   * @param buffer the {@link ByteBuf} to add. {@link ByteBuf#release()} ownership is not
   *               transferred {@link CompositeByteBuf}.
   */
  public static ByteBuf encode(Serdes serdes, Message message, ByteBuf buffer) {
    CompositeByteBuf composite = ByteBufAllocator.DEFAULT.compositeBuffer();
    try {
      int writerIndex = composite.writerIndex();
      composite.writeInt(0);

      serdes.serialize(composite, message);
      composite.addComponent(true, buffer.retain());
      composite.setInt(writerIndex, composite.writerIndex() - writerIndex);
      return composite;
    } catch (Throwable throwable) {
      composite.release();
      throw throwable;
    }
  }


}
