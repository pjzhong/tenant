package org.example.common;

import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.NettyRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 公用线程池
 *
 * @author ZJP
 * @since 2021年06月26日 14:53:18
 **/
@Component
public class ThreadCommonResource implements AutoCloseable {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /** BOSS线程 */
  private final MultiThreadIoEventLoopGroup boss;
  /** 工作线程 */
  private final MultiThreadIoEventLoopGroup worker;

  public ThreadCommonResource() {
    boss = new MultiThreadIoEventLoopGroup(1, new NamedThreadFactory("BOSS"),
        NioIoHandler.newFactory());
    worker = new MultiThreadIoEventLoopGroup(NettyRuntime.availableProcessors() / 2,
        new NamedThreadFactory("WORKER"), NioIoHandler.newFactory());
  }

  @Override
  public void close() {
    worker.shutdownGracefully();
    boss.shutdownGracefully();
    logger.info("threadCommonResource closing");
  }


  public MultiThreadIoEventLoopGroup getBoss() {
    return boss;
  }

  public MultiThreadIoEventLoopGroup getWorker() {
    return worker;
  }
}
