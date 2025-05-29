package org.example.common.register;

import java.util.Objects;
import org.example.model.AnonymousId;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.util.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RegisterService {

  private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

  private ConnectionManager connectionManager;

  public RegisterService(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  public boolean serverRegister(Identity identity, Connection connection) {
    if (Objects.requireNonNull(connection.id()) instanceof AnonymousId) {
      connectionManager.bindChannel(identity, connection.channel());
      logger.info("服务器：{}， 注册成功", identity);
      return true;
    } else {
      logger.error("服务器：{}， 重复注册", connection.id());
      return false;
    }
  }
}
