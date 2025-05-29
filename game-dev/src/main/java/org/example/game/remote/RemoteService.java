package org.example.game.remote;

import static org.example.common.model.ServerInfo.serInfo;
import static org.example.common.model.WorldId.worldId;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import org.example.common.ThreadCommonResource;
import org.example.common.model.ServerInfo;
import org.example.common.net.generated.invoker.RegisterFacadeInvoker;
import org.example.common.util.NettyEventLoopUtil;
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

  public void serverStart() throws Exception {
    connectSync(serInfo(worldId("world"), new InetSocketAddress("localhost", 8082)));
  }

  public ChannelFuture connectSync(ServerInfo server) throws Exception {
    Bootstrap b = new Bootstrap();
    ChannelFuture future = b
        .group(threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .handler(channelInitializer)
        .connect(server.addr()).sync();
    connectSync0(server, future);
    return future;
  }

  void connectSync0(ServerInfo server, ChannelFuture f) throws Exception {
    if (f.isSuccess()) {
      logger.info("目标服务器：{}，连接成功", server, f.cause());
      registerChannelSync(server, f.channel());
    } else {
      logger.error("目标服务器：{}，连接失败", server, f.cause());
    }
  }

  void registerChannelSync(ServerInfo server, Channel channel) throws Exception {
    Connection connection = channel.attr(Connection.CONNECTION).get();
    if (connection.id() instanceof AnonymousId) {
      boolean res = registerFacadeInvoker.of(connection).serverRegister(config.getId()).get();
      if (res) {
        logger.info("本服：{}与目标服务器{}，注册成功", config.getId(), server.id());
        connectionManager.bindChannel(server.id(), channel);
      } else {
        logger.error("本服：{}与目标服务器{}注册，结果：{},", config.getId(),
            server.id(), res);
      }
    } else {
      logger.error("服务器：{}，尝试重复注册", connection.id());
    }
  }


}

