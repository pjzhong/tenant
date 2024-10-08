package org.example.proxy.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.Objects;
import org.example.net.Connection;
import org.example.net.Message;
import org.example.proxy.service.ProxyService;
import org.example.serde.NettyByteBufUtil;
import org.example.serde.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * 消息中转处理器,请配合一起使用
 *
 * @author zhongjianping
 * @since 2022/12/20 17:14
 */
public class ProxyMessageHandler extends ByteToMessageDecoder {

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  /** 序列化实现 */
  private Serializer<Object> serializer;
  private ProxyService proxyService;

  @Autowired
  public ProxyMessageHandler(Serializer<Object> serializer, ProxyService proxyService) {
    this.serializer = serializer;
    this.proxyService = proxyService;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    final int lengthFieldLength = Integer.BYTES;
    if (in.readableBytes() < lengthFieldLength) {
      return;
    }

    int readIdx = in.readerIndex();
    int length = in.getInt(readIdx);
    if (length <= 0) {
      throw new RuntimeException(String
          .format("address:%s, Body Size is incorrect:%s", ctx.channel().remoteAddress(),
              length));
    }

    if (in.readableBytes() < length) {
      return;
    }

    ByteBuf buf = in.slice(readIdx, length);
    in.skipBytes(length);
    buf.skipBytes(lengthFieldLength);

    int target = NettyByteBufUtil.readInt32(buf);
    int source = NettyByteBufUtil.readInt32(buf);
    int proto = NettyByteBufUtil.readInt32(buf);
    if (proxyService.getProxyServerConfig().getId() == target) {
      Message message = new Message();
//      message.target(target);
//      message.source(source);
      message.proto(proto);
      message.msgId(NettyByteBufUtil.readInt32(buf));
      message.packet(new byte[buf.readableBytes()]);
      buf.readBytes(message.packet());
      out.add(message);
    } else {
      Channel channel = proxyService.getChannels().get(target);
      if (channel != null) {
        channel.writeAndFlush(buf.retainedSlice(0, length));
      } else {
        logger.error("【Proxy Server】源:{}, 无法转发至目标:{},协议号:{}", source, target, proto);
      }
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    Channel channel = ctx.channel();
    if (channel.attr(Connection.CONNECTION).get() == null) {
      Connection connection = createConnection(channel);
      channel.attr(Connection.CONNECTION).set(connection);
    }

    ctx.fireChannelActive();
  }

  public Connection createConnection(Channel channel) {
    Objects.requireNonNull(channel, "channel can't not be null");
    Connection oldConn = channel.attr(Connection.CONNECTION).get();
    if (oldConn != null) {
      throw new IllegalStateException("duplicated create connection");
    }

    Connection connection = new Connection(channel, Connection.IdGenerator.incrementAndGet());
    channel.attr(Connection.CONNECTION).set(connection);
    return connection;
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    proxyService.channelInactive(ctx.channel());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    ctx.close();
  }
}
