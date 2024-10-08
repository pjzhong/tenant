package org.example;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.example.common.generator.SerdeConfigGenerator;
import org.example.common.generator.rpc.GameRpcConfigGenerator;

public final class SourceCodeGenerator {

  /**
   * @return 所有代码生成器，因为会并发执行所以写逻辑请不要相互依赖
   * @since 2024/8/8 20:42
   */
  private static List<Consumer<Path>> codeGenerators() {
    return List.of(SerdeConfigGenerator::serdeConfig, GameRpcConfigGenerator::rpcConfig);
  }

  /**
   * @param outputDir  所有生成的代码，请放在这个目录
   * @since 2024/8/8 20:37
   */
  private static void generateJavaSourceFile(Path outputDir) {
    codeGenerators().forEach(c -> c.accept(outputDir));
  }


  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: SourceCodeGenerator <output-directory>");
      return;
    }

    Path outputDir = Path.of(args[0]);
    ExecutorService executors = Executors.newVirtualThreadPerTaskExecutor();
    try {
      for (Consumer<Path> consumer : codeGenerators()) {
        executors.execute(() -> consumer.accept(outputDir));
      }
    } finally {
      executors.shutdown();
    }
  }


}