package org.example.exec;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * 当Actors数量少的时候，两种模式没有明显的区别
 *
 * @author ZJP
 * @since 2021年10月07日 16:53:57
 **/
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConcurrentLinkQueueExecutorTest extends ExecutorTest {

  @Override
  public DelegateExecutor[] createActors(ExecutorService service, int size) {
    DelegateExecutor[] actros = new DelegateExecutor[size];
    for (int i = 0; i < size; i++) {
      actros[i] = new DelegateExecutor(new ConcurrentLinkedQueue<>(), service);
    }

    return actros;
  }


}
