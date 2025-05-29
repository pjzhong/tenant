package org.example.game.server;


import org.example.common.model.GameId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * 游戏服配置信息
 *
 * @author ZJP
 * @since 2021年06月30日 18:09:27
 **/
@Component
public class GameInfo {

  @Value("${game.id}")
  private GameId id;
  @Value("${game.port}")
  private int port;
  @Value("${game.idelSec:60}")
  public int idleSec = 60;

  public GameId getId() {
    return id;
  }

  public int getPort() {
    return port;
  }

  public int getIdleSec() {
    return idleSec;
  }
}
