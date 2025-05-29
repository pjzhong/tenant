package org.example.game.remote;

import static org.example.common.model.ServerInfo.serInfo;
import static org.example.common.model.WorldId.worldId;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.example.common.ThreadCommonResource;
import org.example.common.model.ServerInfo;
import org.example.common.net.generated.invoker.RegisterFacadeInvoker;
import org.example.common.util.NettyEventLoopUtil;
import org.example.exec.VirutalExecutors;
import org.example.game.server.GameInfo;
import org.example.model.AnonymousId;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteService {

  private static final Logger logger = LoggerFactory.getLogger(RemoteService.class);

  private GameInfo config;

  private ThreadCommonResource threadCommonResource;
  private ClientInitHandler channelInitializer;
  private ConnectionManager connectionManager;
  private RegisterFacadeInvoker registerFacadeInvoker;

  public RemoteService(GameInfo config, ThreadCommonResource threadCommonResource,
      ClientInitHandler channelInitializer, ConnectionManager connectionManager,
      RegisterFacadeInvoker registerFacadeInvoker) {
    this.config = config;
    this.threadCommonResource = threadCommonResource;
    this.channelInitializer = channelInitializer;
    this.connectionManager = connectionManager;
    this.registerFacadeInvoker = registerFacadeInvoker;
  }

  public void serverStart() {
    connect(serInfo(worldId("world"), new InetSocketAddress("localhost", 8082)));
  }

  public void connect(ServerInfo server) {
    Bootstrap b = new Bootstrap();
    b
        .group(threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .handler(channelInitializer)
        .connect(server.addr())
        .addListener((ChannelFuture f) -> {
          VirutalExecutors.commonPool().execute(() -> connect0(server, f));
        });
  }

  void connect0(ServerInfo server, ChannelFuture f) {
    if (f.isSuccess()) {
      logger.info("目标服务器：{}，连接成功", server, f.cause());
      registerChannel(server, f.channel());
    } else {
      logger.error("目标服务器：{}，连接失败", server, f.cause());
      VirutalExecutors.commonPool()
          .schedule(() -> connect(server), Duration.ofSeconds(3));
    }
  }

  void registerChannel(ServerInfo server, Channel channel) {
    Connection connection = channel.attr(Connection.CONNECTION).get();
    if (connection.id() instanceof AnonymousId) {
      registerFacadeInvoker.of(connection).serverRegister(config.getId())
          .async((res, ex) -> {
            if (ex != null || !res) {
              logger.error("本服：{}与目标服务器{}注册，结果：{},", config.getId(),
                  server.id(), res, ex);
            } else {
              logger.info("本服：{}与目标服务器{}，注册成功", config.getId(), server.id());
              connectionManager.bindChannel(server.id(), channel);
            }
          });
    } else {
      logger.error("服务器：{}，尝试重复注册", connection.id());
    }
  }


}

