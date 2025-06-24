package org.example.net.anno.processor;

import static org.example.net.anno.processor.Util.FACADE_VAR_NAME;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.apache.commons.lang3.tuple.Pair;

final class ExecutorSupplierUtil {

  private ExecutorSupplierUtil() {
  }

  /**
   * 检查执行器接口并构建执行器方法
   *
   * @since 2025/6/23 21:45
   */
  public static void buildExecuotSupplerCode(ProcessingEnvironment processingEnv,
      TypeSpecInfo info) {
    List<Pair<TypeMirror, TypeName>> supplierInterfaces = executorSupplierInters(
        processingEnv, info);
    switch (supplierInterfaces.size()) {
      case 1 -> {
        Pair<TypeMirror, TypeName> pair = supplierInterfaces.getFirst();
        TypeName rootInter = pair.getRight();
        TypeName rawRootType = rootInter;
        ParameterizedTypeName paramTypeRootInter = null;
        if (rawRootType instanceof ParameterizedTypeName t) {
          rawRootType = t.rawType();
          paramTypeRootInter = t;
        }

        if (rawRootType.equals(Util.EXECUTOR_SUPPLIER_CLASS_NAME)) {
          info.executor = ignore -> CodeBlock.builder()
              .add("$L.get()", FACADE_VAR_NAME)
              .build();
        } else if (rawRootType.equals(Util.ARG_EXECUTOR_SUPPLIER_CLASS_NAME)) {
          argExecuteSupplier(processingEnv, info, paramTypeRootInter, pair);
        } else if (rawRootType.equals(Util.SYS_ARG_EXECUTOR_SUPPLIER_CLASS_NAME)) {
          sysArgExecuteSupplier(processingEnv, info, paramTypeRootInter, pair);
        } else if (rawRootType.equals(Util.SYS_ARGS_EXECUTOR_SUPPLIER_CLASS_NAME)) {
          sysArgsExecuteSupplier(processingEnv, info, paramTypeRootInter, pair);
        } else if (rootInter.equals(Util.RAW_EXECUTOR_SUPPLIER_CLASS_NAME)) {
          info.executor = null;
        } else {
          processingEnv.getMessager()
              .printError("不支持的ExecutorSuppler接口：%s, 请联系作者".formatted(rootInter),
                  info.typeElement);
        }
      }
      case 0 -> processingEnv.getMessager().printError(
          "缺少ExecutorSuppler接口，请选择实现其中之一：%s, %s, %s"
              .formatted(Util.EXECUTOR_SUPPLIER_CLASS_NAME,
                  Util.ARG_EXECUTOR_SUPPLIER_CLASS_NAME,
                  Util.SYS_ARG_EXECUTOR_SUPPLIER_CLASS_NAME
              ),
          info.typeElement);
      default -> processingEnv.getMessager().printError(
          "重复ExecutorSuppler接口，请选择实现其中之一保留：%s"
              .formatted(supplierInterfaces
                  .stream()
                  .map(Pair::getLeft)
                  .map(TypeMirror::toString)
                  .collect(Collectors.joining(", "))),
          info.typeElement);
    }
  }

  /**
   * 处理{@link org.example.net.handler.ArgExecSupplier}
   *
   * @since 2025/6/24 9:30
   */
  private static void argExecuteSupplier(ProcessingEnvironment processingEnv, TypeSpecInfo info,
      ParameterizedTypeName paramTypeRootInter, Pair<TypeMirror, TypeName> pair) {
    TypeMirror userProvideInter = pair.getLeft();
    if (paramTypeRootInter == null) {
      processingEnv.getMessager()
          .printError("接口：%s, 没有提供泛型参数".formatted(userProvideInter),
              info.typeElement);
      return;
    }
    List<TypeName> requiredParams = paramTypeRootInter.typeArguments();
    if (requiredParams.isEmpty()) {
      processingEnv.getMessager()
          .printError("接口：%s, 需要一个泛型参数".formatted(userProvideInter),
              info.typeElement);
      return;
    }

    argsExecutorSupplerCheck(processingEnv, info, userProvideInter, requiredParams);

    info.executor = method -> {
      return CodeBlock.builder()
          .add("$L.get($L)",
              FACADE_VAR_NAME,
              method.getParameters().getFirst().toString())
          .build();
    };
  }

  /**
   * 处理{@link org.example.net.handler.SysArgExecSupplier}
   *
   * @since 2025/6/24 9:27
   */
  private static void sysArgExecuteSupplier(ProcessingEnvironment processingEnv, TypeSpecInfo info,
      ParameterizedTypeName paramTypeRootInter, Pair<TypeMirror, TypeName> pair) {
    TypeMirror userProvideInter = pair.getLeft();
    if (paramTypeRootInter == null) {
      processingEnv.getMessager()
          .printError("接口：%s, 没有提供泛型参数".formatted(userProvideInter),
              info.typeElement);
      return;
    }

    List<TypeName> requiredParams = paramTypeRootInter.typeArguments();
    if (requiredParams.size() < 2) {
      processingEnv.getMessager()
          .printError("接口：%s, 需要两个泛型参数".formatted(userProvideInter),
              info.typeElement);
      return;
    }

    argsExecutorSupplerCheck(processingEnv, info, userProvideInter,
        requiredParams.subList(1, requiredParams.size()));
    if (paramTypeRootInter.typeArguments().getFirst() instanceof ClassName c) {
      String sysName = info.fieldName(c);

      info.builder
          .addField(c, sysName, Modifier.PRIVATE, Modifier.FINAL);

      info.constructor
          .addParameter(c, sysName)
          .addStatement("this.$L = $L", sysName, sysName);

      info.executor = method -> {
        return CodeBlock.builder()
            .add("$L.get($L, $L)",
                FACADE_VAR_NAME,
                sysName,
                method.getParameters().getFirst().toString())
            .build();
      };
    } else {
      processingEnv.getMessager()
          .printError("接口：%s, 首个参数必须具体的Class, 否则无法识别".formatted(userProvideInter),
              info.typeElement);
    }
  }

  /**
   * 处理{@link org.example.net.handler.SysArgsExecSupplier}
   *
   * @since 2025/6/24 9:27
   */
  private static void sysArgsExecuteSupplier(ProcessingEnvironment processingEnv, TypeSpecInfo info,
      ParameterizedTypeName paramTypeRootInter, Pair<TypeMirror, TypeName> pair) {
    TypeMirror userProvideInter = pair.getLeft();
    if (paramTypeRootInter == null) {
      processingEnv.getMessager()
          .printError("接口：%s, 没有提供泛型参数".formatted(userProvideInter),
              info.typeElement);
      return;
    }

    List<TypeName> requiredParams = paramTypeRootInter.typeArguments();
    if (requiredParams.size() < 3) {
      processingEnv.getMessager()
          .printError("接口：%s, 需要两个泛型参数".formatted(userProvideInter),
              info.typeElement);
      return;
    }

    argsExecutorSupplerCheck(processingEnv, info, userProvideInter,
        requiredParams.subList(1, requiredParams.size()));
    if (paramTypeRootInter.typeArguments().getFirst() instanceof ClassName c) {
      String sysName = info.fieldName(c);

      info.builder
          .addField(c, sysName, Modifier.PRIVATE, Modifier.FINAL);

      info.constructor
          .addParameter(c, sysName)
          .addStatement("this.$L = $L", sysName, sysName);

      info.executor = method -> {
        List<? extends VariableElement> parameters = method.getParameters();
        return CodeBlock.builder()
            .add("$L.get($L, $L, $L)",
                FACADE_VAR_NAME,
                sysName,
                parameters.getFirst().toString(),
                parameters.get(1).toString()
            )
            .build();
      };
    } else {
      processingEnv.getMessager()
          .printError("接口：%s, 首个参数必须具体的Class, 否则无法识别".formatted(userProvideInter),
              info.typeElement);
    }
  }

  private static List<Pair<TypeMirror, TypeName>> executorSupplierInters(
      ProcessingEnvironment processingEnv,
      TypeSpecInfo info) {
    List<Pair<TypeMirror, TypeName>> supplierInterface = new ArrayList<>();

    for (TypeMirror inter : info.typeElement.getInterfaces()) {
      List<TypeName> supplier = getExecutorSupplierInter(processingEnv, inter);
      switch (supplier.size()) {
        case 0 -> {
          continue;
        }
        case 1 -> supplierInterface.add(Pair.of(inter, supplier.getFirst()));
        default -> {
          processingEnv.getMessager().printError(
              "重复ExecutorSuppler接口，请选择实现其中之一保留：%s"
                  .formatted(supplier.stream().map(TypeName::toString)
                      .collect(Collectors.joining(", "))),
              processingEnv.getTypeUtils().asElement(inter));
          continue;
        }
      }

    }
    return supplierInterface;
  }

  private static List<TypeName> getExecutorSupplierInter(ProcessingEnvironment processingEnv,
      TypeMirror orginInter) {
    Queue<TypeMirror> typeMirrors = new ArrayDeque<>();
    typeMirrors.add(orginInter);

    List<TypeName> supplierInterface = new ArrayList<>();
    while (!typeMirrors.isEmpty()) {
      TypeMirror inter = typeMirrors.poll();
      TypeName typeName = TypeName.get(inter);
      TypeName rawType = typeName;

      if (typeName instanceof ParameterizedTypeName t) {
        rawType = t.rawType();
      }

      if (rawType.equals(Util.EXECUTOR_SUPPLIER_CLASS_NAME)
          || rawType.equals(Util.ARG_EXECUTOR_SUPPLIER_CLASS_NAME)
          || rawType.equals(Util.SYS_ARG_EXECUTOR_SUPPLIER_CLASS_NAME)
          || rawType.equals(Util.SYS_ARGS_EXECUTOR_SUPPLIER_CLASS_NAME)
          || rawType.equals(Util.RAW_EXECUTOR_SUPPLIER_CLASS_NAME)
      ) {
        supplierInterface.add(typeName);
      } else {
        typeMirrors.addAll(processingEnv.getTypeUtils().directSupertypes(inter));
      }
    }

    return supplierInterface;
  }

  /**
   * 检查方法的参数类型是否符合接口的要求
   *
   * @since 2025/6/24 9:36
   */
  private static void argsExecutorSupplerCheck(ProcessingEnvironment processingEnv,
      TypeSpecInfo info, TypeMirror userProviderInter, List<TypeName> requiredParams) {

    for (ExecutableElement element : info.methods) {
      List<? extends VariableElement> parameters = element.getParameters();
      if (parameters.isEmpty()) {
        processingEnv.getMessager()
            .printError(
                "缺少参数：%s, 详细定义请查询：%s".formatted(requiredParams,
                    userProviderInter),
                element);
        continue;
      }

      if (parameters.size() < requiredParams.size()) {
        processingEnv.getMessager()
            .printError(
                "方法参数数量不足, 详细定义请查询：%s".formatted(userProviderInter),
                element);
        continue;
      }

      for (int i = 0, size = requiredParams.size(); i < size; i++) {
        VariableElement p0 = parameters.get(i);
        TypeName paramTypeName = TypeName.get(p0.asType());
        TypeName requiredParam = requiredParams.get(i);
        if (!paramTypeName.equals(requiredParam)) {
          processingEnv.getMessager()
              .printError(
                  "参数：%s，提供的类型：%s,需要的类型：%s, 详细定义请查询：%s".formatted(
                      p0.getSimpleName(),
                      paramTypeName, requiredParam, userProviderInter
                  ),
                  p0);
        }
      }


    }
  }

}
