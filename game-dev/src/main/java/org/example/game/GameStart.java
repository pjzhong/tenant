package org.example.game;


import org.example.exec.VirutalExecutors;
import org.example.game.config.GameConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class GameStart {

  private static Logger logger = LoggerFactory.getLogger(GameStart.class);

  public static void main(String[] args) throws Exception {
    try {
      AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();

      Thread thread = VirutalExecutors.commonPool()
          .execute(() -> {
            child.register(GameConfiguration.class);
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
