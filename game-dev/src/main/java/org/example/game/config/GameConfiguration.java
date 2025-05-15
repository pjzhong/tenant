package org.example.game.config;

import java.util.List;
import org.example.common.handler.ConnectionManagerHandler;
import org.example.exec.VirutalExecutors;
import org.example.net.ConnectionManager;
import org.example.net.DefaultDispatcher;
import org.example.net.handler.CallBackFacade;
import org.example.net.handler.DispatcherNettyInboundHandler;
import org.example.net.handler.Handler;
import org.example.serde.Serdes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 服务器环境配置
 *
 * @author zhongjianping
 * @since 2024/12/5 18:08
 */
@Configuration
public class GameConfiguration {

  @Bean
  public ConnectionManager connectionManager() {
    return new ConnectionManager();
  }

  @Bean
  public ConnectionManagerHandler connectionManagerHandler(ConnectionManager manager) {
    return new ConnectionManagerHandler(manager);
  }

  @Bean
  public VirutalExecutors virtualThreadExecutor() {
    return new VirutalExecutors();
  }


  @Bean
  public DefaultDispatcher defaultDispatcher(List<Handler> handlers, Serdes s,
      ConnectionManager manager) {
    DefaultDispatcher d = new DefaultDispatcher();
    for (Handler h : handlers) {
      h.register(d);
    }
    new CallBackFacade(manager, s).register(d);
    return d;
  }

  @Bean
  public DispatcherNettyInboundHandler dispatcherHandler(DefaultDispatcher defaultDispatcher) {
    return new DispatcherNettyInboundHandler(defaultDispatcher);
  }

}
