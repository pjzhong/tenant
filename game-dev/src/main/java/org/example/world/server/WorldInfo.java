package org.example.world.server;

import org.example.common.model.WorldId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 子容器配置类
 *
 * @author ZJP
 * @since 2021年06月30日 18:09:27
 **/
@Component
public class WorldInfo {

  @Value("${world.id}")
  private WorldId id;
  @Value("${world.port}")
  private int port;
  @Value("${world.idelSec:60}")
  public int idleSec = 60;

  public WorldId getId() {
    return id;
  }

  public int getPort() {
    return port;
  }

  public int getIdleSec() {
    return idleSec;
  }
}
