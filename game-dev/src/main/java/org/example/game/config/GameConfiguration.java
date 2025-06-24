package org.example.game.config;

import java.util.List;
import java.util.ServiceLoader;
import org.example.common.handler.ConnectionManagerHandler;
import org.example.exec.VirutalExecutors;
import org.example.net.ConnectionManager;
import org.example.net.DefaultDispatcher;
import org.example.net.HandlerRegister;
import org.example.net.handler.CallBackFacade;
import org.example.net.handler.DispatcherNettyInboundHandler;
import org.example.serde.DefaultSerializersRegister;
import org.example.serde.SerdeRegister;
import org.example.serde.Serdes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * 服务器环境配置
 *
 * @author zhongjianping
 * @since 2024/12/5 18:08
 */
@Configuration
@ComponentScan({"org.example.game", "org.example.common"})
@PropertySource("classpath:game.properties")
public class GameConfiguration {

  @Bean
  public Serdes commonSerializer() {
    Serdes serializer = new Serdes();
    new DefaultSerializersRegister().register(serializer);
    for (SerdeRegister register : ServiceLoader.load(SerdeRegister.class)) {
      register.register(serializer);
    }
    return serializer;
  }

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
    return VirutalExecutors.commonPool();
  }

  @Bean
  public DefaultDispatcher defaultDispatcher(List<HandlerRegister> handlers, Serdes s,
      ConnectionManager manager) {
    DefaultDispatcher d = new DefaultDispatcher();
    for (HandlerRegister h : handlers) {
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
