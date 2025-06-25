package org.example.net.anno.processor;

import static org.example.net.anno.processor.Util.FACADE_VAR_NAME;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import org.example.net.anno.LocalReq;

/**
 * 负责RPC方法的调用类和代理类
 * <p>
 *
 * @author zhongjianping
 * @since 2024/8/9 11:19
 */
@SupportedAnnotationTypes("org.example.net.anno.Rpc")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RpcLocalInvokerProcessor extends AbstractProcessor {

  private static final String LOCAL_SUBFIX = "Local";
  private static final String RUNNABLE_VAR_NAME = "r";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }
      for (Element clazz : annotationElements) {
        TypeElement typeElement = (TypeElement) clazz;
        try {
          List<ExecutableElement> elements = Util.getReqMethod(processingEnv, typeElement,
              LocalReq.class);

          buildInvoker(typeElement, elements);
        } catch (Exception e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build invoker error, %s".formatted(getClass(),
                      typeElement.getQualifiedName(),
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))), typeElement);
        }
      }
    }

    return false;
  }

  public void buildInvoker(TypeElement typeElement, List<ExecutableElement> elements)
      throws Exception {
    if (elements.isEmpty()) {
      return;
    }

    ClassName typeName = ClassName.get(typeElement);
    TypeSpec invoker = buildInvoker0(typeElement, elements).build();
    ClassName invokerClassName = ClassName.get(typeName.packageName(), invoker.name());

    JavaFile javaFile = JavaFile.builder(invokerClassName.packageName(), invoker).build();
    JavaFileObject file = processingEnv.getFiler()
        .createSourceFile(invokerClassName.canonicalName());
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  TypeSpec.Builder buildInvoker0(TypeElement typeElement, List<ExecutableElement> methods) {
    final String simpleName = typeElement.getSimpleName().toString() + LOCAL_SUBFIX;

    TypeSpec.Builder typeBuilder = TypeSpec
        .classBuilder(simpleName)
        .addAnnotation(Util.COMPONENT_ANNOTATION)
        .addJavadoc("{@link $T}\n", typeElement)
        .addJavadoc("@since $S", LocalDateTime.now())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    TypeSpecInfo info = new TypeSpecInfo(typeElement, typeBuilder, methods);
    ExecutorSupplierUtil.buildExecuotSupplerCode(processingEnv, info);
    buildConstructor(info);

    for (ExecutableElement method : methods) {
      Name methodName = method.getSimpleName();
      TypeMirror returnTypeMirror = method.getReturnType();
      TypeName returnTypeName = TypeName.get(returnTypeMirror);
      boolean hasReutrn = returnTypeMirror.getKind() != TypeKind.VOID;

      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(methodName.toString())

          .addModifiers(Modifier.PUBLIC)
          .addJavadoc("{@link $T#$L}", typeElement, methodName);

      for (VariableElement variableElement : method.getParameters()) {
        TypeName paramTypeName = TypeName.get(variableElement.asType());
        methodBuilder
            .addParameter(paramTypeName, variableElement.getSimpleName().toString());
      }

      if (info.executor == null) {
        if (hasReutrn) {
          methodBuilder.addCode("return ");
        }
        methodBuilder
            .returns(returnTypeName)
            .addStatement(
                buildInvokeCodeBlock(method)
                    .build()
            );
      } else if (hasReutrn) {
        ParameterizedTypeName returnType = ParameterizedTypeName
            .get(
                Util.NET_COMPLETE_ABLE_FUTURE_CLASS_NAME,
                returnTypeName.box()
            );

        ParameterizedTypeName supplierType = ParameterizedTypeName
            .get(
                Util.SUPPLIER_CLASS_NAME,
                returnTypeName.box()
            );

        methodBuilder
            .returns(returnType)
            .addCode("$T $L = () ->", supplierType, RUNNABLE_VAR_NAME)
            .beginControlFlow("")
            .addStatement("return $L", buildInvokeCodeBlock(method).build())
            .endControlFlow("")
            .addStatement("return $T.supplyAsync($L, $L)",
                Util.NET_COMPLETE_ABLE_FUTURE_CLASS_NAME,
                RUNNABLE_VAR_NAME, info.executor.apply(method))
        ;

      } else {
        methodBuilder
            .addCode("$T $L = () ->", Runnable.class, RUNNABLE_VAR_NAME)
            .beginControlFlow("")
            .addStatement(buildInvokeCodeBlock(method).build())
            .endControlFlow("")
            .addStatement("$L.execute($L)", info.executor.apply(method), RUNNABLE_VAR_NAME);
      }

      typeBuilder.addMethod(methodBuilder.build());
    }

    return typeBuilder;
  }

  void buildConstructor(TypeSpecInfo info) {
    TypeName typeName = TypeName.get(info.typeElement.asType());

    info.builder.addField(
        FieldSpec
            .builder(typeName, FACADE_VAR_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
    );

    info.builder.addMethod(
        info.constructor
            .addParameter(typeName, FACADE_VAR_NAME)
            .addStatement("this.$L = $L", FACADE_VAR_NAME, FACADE_VAR_NAME)
            .build()
    );
  }

  /**
   * 调用指定的方法
   *
   * @since 2024/12/4 11:01
   */
  private static CodeBlock.Builder buildInvokeCodeBlock(ExecutableElement method) {
    String paramStr = method.getParameters().stream()
        .map(p -> p.getSimpleName().toString())
        .collect(Collectors.joining(", "));

    CodeBlock.Builder codeBlock = CodeBlock.builder();
    codeBlock.add("this.$L.$L($L)", FACADE_VAR_NAME, method.getSimpleName().toString(), paramStr);

    return codeBlock;
  }
}
