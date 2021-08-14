package org.example.net.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.example.common.ThreadCommonResource;
import org.example.net.ServerIdleHandler;
import org.example.net.codec.MessageCodec;
import org.example.util.NettyEventLoopUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for rpc
 *
 * @author ZJP
 * @since 2021年08月13日 14:56:18
 **/
public class RpcServer implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private String ip;
  private int port;

  /** server bootstrap */
  private ServerBootstrap bootstrap;

  /** channelFuture of netty */
  private ChannelFuture channelFuture;

  /** connection handler */
  private ChannelHandler handler;


  public RpcServer() {
    this(0);
  }

  public RpcServer(int port) {
    this(new InetSocketAddress(port).getAddress().getHostAddress(), port);
  }

  public RpcServer(String ip, int port) {
    if (port < 0 || port > 65535) {
      throw new IllegalArgumentException(String.format(
          "Illegal port value: %d, which should between 0 and 65535.", port));
    }
    this.ip = ip;
    this.port = port;
  }

  public boolean start(ThreadCommonResource threadCommonResource)
      throws InterruptedException {
    Objects.requireNonNull(handler, "connection can't be null");

    ServerBootstrap b = new ServerBootstrap();
    b.option(ChannelOption.SO_BACKLOG, 1024);
    b.group(threadCommonResource.getBoss(), threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getServerSocketChannelClass())
        .option(ChannelOption.SO_REUSEADDR, true)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(handler);
    bootstrap = b;

    channelFuture = b.bind(new InetSocketAddress(ip, port)).sync();
    if (port == 0 && channelFuture.isSuccess()) {
      InetSocketAddress address = (InetSocketAddress) channelFuture.channel().localAddress();
      port = address.getPort();
      logger.info("rpc server start with random port: {}!", port);
    }
    return channelFuture.isSuccess();
  }

  public String ip() {
    return ip;
  }

  public int port() {
    return port;
  }

  public Channel channel() {
    return channelFuture.channel();
  }

  public ChannelHandler handler() {
    return handler;
  }

  public RpcServer handler(ChannelHandler handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public void close() {
    if (channelFuture != null) {
      logger.info("rpcServer:{}:{}, closing", ip, port);
      channelFuture.channel().close();
      channelFuture = null;
      logger.info("rpcServer:{}:{}, closed", ip, port);
    }
  }

  /**
   * Channel处理器
   *
   * @author ZJP
   * @since 2021年08月13日 21:41:18
   **/
  public static class ServerHandlerInitializer extends ChannelInitializer<SocketChannel> {

    private MessageCodec codec;
    private ServerIdleHandler idleHandler;

    private ChannelHandler handler;

    public ServerHandlerInitializer(ChannelHandler handler) {
      idleHandler = new ServerIdleHandler();
      this.handler = handler;
    }

    public ChannelHandler handler() {
      return handler;
    }

    public ServerHandlerInitializer handler(ChannelHandler handler) {
      this.handler = handler;
      return this;
    }

    public ServerIdleHandler idleHandler() {
      return idleHandler;
    }

    public ServerHandlerInitializer idleHandler(ServerIdleHandler idleHandler) {
      this.idleHandler = idleHandler;
      return this;
    }

    public MessageCodec codec() {
      return codec;
    }

    public ServerHandlerInitializer codec(MessageCodec codec) {
      this.codec = codec;
      return this;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
      Objects.requireNonNull(codec, "codec Handler can't not be null");

      ChannelPipeline pipeline = ch.pipeline();
      if (idleHandler != null) {
        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, ServerIdleHandler.IDLE_TIME,
            TimeUnit.MILLISECONDS));
        pipeline.addLast("serverIdleHandler", idleHandler);
      }
      pipeline.addLast("codec", codec);
      pipeline.addLast("handler", handler);
    }
  }
}
