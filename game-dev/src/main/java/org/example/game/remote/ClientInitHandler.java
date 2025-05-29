package org.example.game.remote;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import org.example.common.handler.ConnectionManagerHandler;
import org.example.game.server.GameInfo;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.DispatcherNettyInboundHandler;
import org.springframework.stereotype.Component;

@Component
public class ClientInitHandler extends ChannelInitializer<Channel> {

  private GameInfo config;
  private DispatcherNettyInboundHandler defaultDispatcher;
  private ConnectionManagerHandler connectionManager;

  public ClientInitHandler(GameInfo config,
      DispatcherNettyInboundHandler defaultDispatcher, ConnectionManagerHandler connectionManager) {
    this.config = config;
    this.defaultDispatcher = defaultDispatcher;
    this.connectionManager = connectionManager;
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("idleStateHandler",
        new IdleStateHandler(0, 0, config.getIdleSec(), TimeUnit.SECONDS));
    pipeline.addLast("manager", connectionManager);
    pipeline.addLast("codec", new MessageCodec());
    pipeline.addLast("handler", defaultDispatcher);
  }
}
