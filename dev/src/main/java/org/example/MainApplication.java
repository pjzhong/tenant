package org.example;

import org.example.config.CommonConfig;
import org.example.config.GameConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainApplication {

  public static void main(String[] args) throws Exception {
    AnnotationConfigApplicationContext root = new AnnotationConfigApplicationContext(
            CommonConfig.class);
    root.start();

    List<GenericApplicationContext> childContexts = new ArrayList<>();
    Runnable shutdownHook = () -> {
      closeContexts(childContexts);
      closeContexts(Collections.singleton(root));
    };

    try {
      for (String propertyLoc : Arrays.asList("/game1.properties", "/game2.properties")) {
        AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
        childContexts.add(child);

        child.setParent(root);
        child.register(GameConfig.class);

        Resource resource = child.getResource(propertyLoc);
        child.getEnvironment().getPropertySources()
            .addLast(
                new ResourcePropertySource(new EncodedResource(resource, StandardCharsets.UTF_8)));

        child.refresh();
        child.start();
      }

      Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
    } catch (Exception e) {
      e.printStackTrace();
      shutdownHook.run();
    }
  }


  private static void closeContexts(Iterable<GenericApplicationContext> contexts) {
    for (GenericApplicationContext ctx : contexts) {
      try {
        ctx.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


}
