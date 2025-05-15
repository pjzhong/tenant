package org.example.game.remote;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import org.example.common.handler.ConnectionManagerHandler;
import org.example.game.GameConfig;
import org.example.net.codec.MessageCodec;
import org.example.net.handler.DispatcherNettyInboundHandler;
import org.springframework.stereotype.Component;

@Component
public class ClientInitHandler extends ChannelInitializer<Channel> {

  private GameConfig config;
  private DispatcherNettyInboundHandler defaultDispatcher;
  private ConnectionManagerHandler connectionManager;
  private LoggingHandler loggingHandler;

  public ClientInitHandler(GameConfig config,
      DispatcherNettyInboundHandler defaultDispatcher, ConnectionManagerHandler connectionManager) {
    this.config = config;
    this.defaultDispatcher = defaultDispatcher;
    this.connectionManager = connectionManager;
    loggingHandler = new LoggingHandler(LogLevel.INFO);
  }

  @Override
  protected void initChannel(Channel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("idleStateHandler",
        new IdleStateHandler(0, 0, config.getIdleSec(), TimeUnit.SECONDS));
    pipeline.addLast("logger", loggingHandler);
    pipeline.addLast("manager", connectionManager);
    pipeline.addLast("codec", new MessageCodec());
    pipeline.addLast("handler", defaultDispatcher);
  }
}
