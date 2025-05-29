package org.example.world;


import org.example.exec.VirutalExecutors;
import org.example.world.config.WorldConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class WorldStart {

  private static Logger logger = LoggerFactory.getLogger(WorldStart.class);

  public static void main(String[] args) throws Exception {
    try {
      AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();

      Thread thread = VirutalExecutors.commonPool()
          .execute(() -> {
            child.register(WorldConfiguration.class);
            child.refresh();
            child.start();
          });

      Runtime.getRuntime().addShutdownHook(new Thread(child::close));
      thread.join();
    } catch (Exception e) {
      logger.error("程序启动失败", e);
    }
  }


}
