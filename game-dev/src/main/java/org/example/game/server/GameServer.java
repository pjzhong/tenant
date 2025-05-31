package org.example.game.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.example.common.ThreadCommonResource;
import org.example.common.event.ServerStartEvent;
import org.example.common.handler.ConnectionManagerHandler;
import org.example.common.util.NettyEventLoopUtil;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.DispatcherNettyInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GameServer implements AutoCloseable {

  private static Logger logger = LoggerFactory.getLogger(GameServer.class);

  private GameInfo config;
  private ThreadCommonResource threadCommonResource;
  private DispatcherNettyInboundHandler defaultDispatcher;
  private ConnectionManagerHandler connectionManager;
  private Channel serverChannel;
  private ApplicationContext context;

  public GameServer(ApplicationContext context, GameInfo config,
      ThreadCommonResource threadCommonResource,
      DispatcherNettyInboundHandler defaultDispatcher, ConnectionManagerHandler connectionManager) {
    this.context = context;
    this.config = config;
    this.threadCommonResource = threadCommonResource;
    this.defaultDispatcher = defaultDispatcher;
    this.connectionManager = connectionManager;
  }

  @EventListener
  public void contextStart(ContextStartedEvent startedEvent) throws Exception {
    start();
  }

  public boolean isStart() {
    return serverChannel != null && serverChannel.isActive();
  }

  public ChannelFuture start() throws Exception {
    ChannelFuture f = bind();
    if (f.isSuccess()) {
      context.publishEvent(new ServerStartEvent());
    }
    return f;
  }

  public ChannelFuture startTest() throws InterruptedException {
    return bind();
  }

  private ChannelFuture bind() throws InterruptedException {
    ServerBootstrap b = new ServerBootstrap();
    b
        .option(ChannelOption.SO_BACKLOG, 1024)
        .option(ChannelOption.SO_REUSEADDR, true)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .group(threadCommonResource.getBoss(), threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getServerSocketChannelClass())
        .childHandler(new ChannelInitializer<>() {
          @Override
          protected void initChannel(Channel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("flush", new FlushConsolidationHandler());
            pipeline.addLast("idleStateHandler",
                new IdleStateHandler(0, 0, config.getIdleSec(), TimeUnit.SECONDS));
            pipeline.addLast("manager", connectionManager);
            pipeline.addLast("codec", new MessageCodec());
            pipeline.addLast("handler", defaultDispatcher);
          }
        });

    int prot = config.getPort();
    ChannelFuture channelFuture = b.bind(new InetSocketAddress(prot)).sync();
    serverChannel = channelFuture.channel();
    if (config.getPort() == 0 && channelFuture.isSuccess()) {
      InetSocketAddress address = (InetSocketAddress) channelFuture.channel().localAddress();
      prot = address.getPort();
    }
    logger.info("服务器：{}，启动成功：绑定端口： {}!", config.getId(), prot);
    return channelFuture;
  }


  @Override
  public void close() throws Exception {
    if (serverChannel != null) {
      serverChannel.close();
      logger.info("服务器：{}，关闭链接！！", config.getId());
    }
  }
}
