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
import org.example.common.net.generated.invoker.RegisterServiceInvoker;
import org.example.common.util.NettyEventLoopUtil;
import org.example.exec.VirutalExecutors;
import org.example.game.server.GameInfo;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.util.Identity;
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
  private RegisterServiceInvoker registerFacadeInvoker;

  public RemoteService(GameInfo config, ThreadCommonResource threadCommonResource,
      ClientInitHandler channelInitializer, ConnectionManager connectionManager,
      RegisterServiceInvoker registerFacadeInvoker) {
    this.config = config;
    this.threadCommonResource = threadCommonResource;
    this.channelInitializer = channelInitializer;
    this.connectionManager = connectionManager;
    this.registerFacadeInvoker = registerFacadeInvoker;
  }

  public void serverStart() {
    ServerInfo info = serInfo(worldId("world"), new InetSocketAddress("localhost", 8082));
    try {
      ChannelFuture future = connectSync(info);
      if (future.isSuccess()) {
        registerChannelSync(config.getId(), info, future.channel());
      } else {
        logger.error("目标服务器：{}，连接失败", info, future.cause());
      }
    } catch (Exception e) {
      logger.error("", e);
      VirutalExecutors.commonPool().schedule(this::serverStart, Duration.ofSeconds(3));
    }

  }

  public ChannelFuture connectSync(ServerInfo server) throws Exception {
    Bootstrap b = new Bootstrap();
    return b
        .group(threadCommonResource.getWorker())
        .channel(NettyEventLoopUtil.getClientSocketChannelClass())
        .handler(channelInitializer)
        .connect(server.addr()).sync();
  }

  public boolean registerChannelSync(Identity selfIdentiy, ServerInfo server, Channel channel)
      throws Exception {
    Connection connection = channel.attr(Connection.CONNECTION).get();
    boolean res = registerFacadeInvoker.of(connection).serverRegister(selfIdentiy).get();
    if (res) {
      logger.info("本服：{}与目标服务器{}，注册成功", selfIdentiy, server.id());
      connectionManager.bindChannel(server.id(), channel);
    } else {
      logger.error("本服：{}与目标服务器{}注册，结果：{},", selfIdentiy,
          server.id(), res);
    }
    return res;

  }


}

