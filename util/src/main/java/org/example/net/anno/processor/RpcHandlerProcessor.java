package org.example.net.anno.processor;

import static org.example.net.Util.BYTE_BUF;
import static org.example.net.Util.CONNECTION_CLASS_NAME;
import static org.example.net.Util.FACADE_VAR_NAME;
import static org.example.net.Util.MESSAGE_CLASS_NAME;
import static org.example.net.Util.MSG_ID_VAR_NAME;
import static org.example.net.Util.SERIALIZER_VAR_NAME;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.example.net.DefaultDispatcher;
import org.example.net.HandlerRegister;
import org.example.net.Util;
import org.example.net.anno.Req;

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
public class RpcHandlerProcessor extends AbstractProcessor {

  private static final String CONNECTION_VAR_NAME = "c";
  private static final String MESSAGE_VAR_NAME = "m";
  private static final String BUF_VAR_NAME = "b";
  private static final String RUNNABLE_VAR_NAME = "r";

  private static final ParameterSpec CONNECTION_PARAM_SPEC = ParameterSpec.builder(
      CONNECTION_CLASS_NAME,
      CONNECTION_VAR_NAME).build();
  private static final ParameterSpec MESSAGE_PARAM_SPEC = ParameterSpec.builder(
      MESSAGE_CLASS_NAME,
      MESSAGE_VAR_NAME).build();


  private final Map<Integer, String> handlers = new HashMap<>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
  }


  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }
      for (Element clazz : annotationElements) {
        if (clazz.getKind() != ElementKind.CLASS) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@RpcModule must be applied to a Class", clazz);
          return false;
        }

        if (clazz.getModifiers().contains(Modifier.ABSTRACT)) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@RpcModule can't not applied to abstract class", clazz);
          return false;
        }
        TypeElement facade = (TypeElement) clazz;

        List<ExecutableElement> methodElements = Util.getReqMethod(processingEnv, facade,
            Req.class);
        if (methodElements.isEmpty()) {
          continue;
        }

        try {
          buildHandler(facade, methodElements);
          //generateCallBackHandler(facade, methodElements);
        } catch (Exception e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build Handler error, cause:%s, \n%s".formatted(getClass(),
                      facade.getQualifiedName(),
                      e,
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))
                  ),
                  facade);
        }
      }
    }
    return false;
  }

  private void buildHandler(TypeElement facade, List<ExecutableElement> elements)
      throws IOException {
    TypeName facdeTypeName = TypeName.get(facade.asType());
    String qualifiedName = facade.getQualifiedName().toString() + "Handler";
    int lastIdx = qualifiedName.lastIndexOf('.');
    String packet = qualifiedName.substring(0, lastIdx);
    String simpleName = qualifiedName.substring(lastIdx + 1);

    TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(simpleName)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(Util.HANDLER_INTERFACE)
        .addAnnotation(Util.COMPONENT_ANNOTATION)
        .addField(FieldSpec
            .builder(facdeTypeName, FACADE_VAR_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build())
        .addField(Util.COMMON_SERIALIZER_FIELD_SPEC)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(facdeTypeName, FACADE_VAR_NAME)
            .addParameter(Util.COMMON_SERIALIZER, SERIALIZER_VAR_NAME)
            .addStatement("this.$L = $L", FACADE_VAR_NAME, FACADE_VAR_NAME)
            .addStatement("this.$L = $L", SERIALIZER_VAR_NAME, SERIALIZER_VAR_NAME)
            .build());

    TypeSpecInfo info = buildTypeSpecInfo(facade, typeSpecBuilder, elements);

    buildHandlerMethod(info);

    JavaFile javaFile = JavaFile.builder(packet, typeSpecBuilder.build())
        .build();

    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  TypeSpecInfo buildTypeSpecInfo(TypeElement typeElement, TypeSpec.Builder builder,
      List<ExecutableElement> elements) {
    TypeSpecInfo info = new TypeSpecInfo(typeElement, builder, elements);

    ExecutorSupplierUtil.buildExecuotSupplerCode(processingEnv, info);

    return info;
  }

  void buildHandlerMethod(TypeSpecInfo info) {
    MethodSpec.Builder invoker = MethodSpec.methodBuilder("invoke")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(CONNECTION_PARAM_SPEC)
        .addParameter(MESSAGE_PARAM_SPEC)
        .addException(Exception.class);

    invoker.beginControlFlow("switch(m.proto())");
    IntList ids = buildHandlerMethod0(info, invoker);
    invoker
        .addStatement(
            "default -> throw new UnsupportedOperationException(\"【$L】无法处理消息，原因:【缺少对应方法】，消息ID:【%s】\".formatted($L.proto()))",
            info.typeElement.getSimpleName(), MESSAGE_VAR_NAME)
        .endControlFlow().addCode(";");

    info.builder
        .addMethod(buildRegisterMethod(info, ids))
        .addMethod(invoker.build());
  }

  private IntList buildHandlerMethod0(TypeSpecInfo info,
      MethodSpec.Builder handlerMethod) {
    IntList intList = new IntArrayList();
    for (ExecutableElement element : info.methods) {
      final int id = Util.calcProtoId(info.typeElement, element);
      Name methodName = element.getSimpleName();

      String name = String.format("%s.%s", info.typeElement, methodName);
      String prev = handlers.put(id, name);
      if (prev != null) {
        processingEnv.getMessager()
            .printError(
                "[%s]\n[%s]\nid:%s, hashID发生碰撞，请修改名字以避免".formatted(prev, name, id),
                element);
        continue;
      }

      MethodSpec.Builder methodBuilder = MethodSpec
          .methodBuilder(methodName.toString())
          .addParameter(CONNECTION_PARAM_SPEC)
          .addParameter(MESSAGE_PARAM_SPEC)
          .addModifiers(Modifier.PRIVATE);

      handlerMethod.addStatement("case $L -> $L($L, $L)", id, methodName, CONNECTION_VAR_NAME,
          MESSAGE_VAR_NAME);

      methodBuilder.addStatement("$T $L = $L.packet()", BYTE_BUF, BUF_VAR_NAME, MESSAGE_VAR_NAME);

      if (hasReturnValue(element)) {
        methodBuilder.addStatement("int $L = $L.readVarInt32($L)", MSG_ID_VAR_NAME,
            SERIALIZER_VAR_NAME,
            BUF_VAR_NAME);
      }

      List<? extends VariableElement> params = element.getParameters();
      for (VariableElement p : params) {
        final String pname = p.getSimpleName().toString();
        TypeMirror ptype = p.asType();
        switch (ptype.getKind()) {
          case BOOLEAN ->
              methodBuilder.addStatement("boolean $L = $L.readBoolean()", pname, BUF_VAR_NAME);
          case BYTE -> methodBuilder.addStatement("byte $L = $L.readByte()", pname, BUF_VAR_NAME);
          case SHORT ->
              methodBuilder.addStatement("short $L = $L.readShort()", pname, BUF_VAR_NAME);
          case CHAR -> methodBuilder.addStatement("char $L = $L.readChar()", pname, BUF_VAR_NAME);
          case FLOAT ->
              methodBuilder.addStatement("float $L = $L.readFloat()", pname, BUF_VAR_NAME);
          case DOUBLE ->
              methodBuilder.addStatement("double $L = $L.readDouble()", pname, BUF_VAR_NAME);
          case INT ->
              methodBuilder.addStatement("int $L = $L.readVarInt32($L)", pname, SERIALIZER_VAR_NAME,
                  BUF_VAR_NAME);
          case LONG -> methodBuilder.addStatement("long $L = $L.readVarInt64($L)", pname,
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          default -> {
            TypeMirror paramType = p.asType();
            TypeName paramTypeName = TypeName.get(paramType);

            if (paramTypeName.equals(CONNECTION_CLASS_NAME)) {
              methodBuilder.addStatement("$T $L = $L", CONNECTION_CLASS_NAME, pname,
                  CONNECTION_VAR_NAME);
            } else if (paramTypeName.equals(MESSAGE_CLASS_NAME)) {
              methodBuilder.addStatement("$T $L = $L", MESSAGE_CLASS_NAME, pname,
                  MESSAGE_VAR_NAME);
            } else {
              methodBuilder.addStatement("$T $L = $L.readObject($L)", TypeName.get(ptype), pname,
                  SERIALIZER_VAR_NAME, BUF_VAR_NAME);
            }
          }
        }
      }
      if (!params.isEmpty()) {
        methodBuilder.addCode("\n");
      }

      CodeBlock.Builder invokeCodeBlock = buildInvokeCodeBlock(element);
      if (info.executor != null) {
        methodBuilder
            .addCode("$T $L = () ->", Runnable.class, RUNNABLE_VAR_NAME)
            .beginControlFlow("")
            .addCode(invokeCodeBlock.build())
            .endControlFlow("")
            .addStatement("$L.execute($L)", info.executor.apply(element), RUNNABLE_VAR_NAME);
      } else {
        methodBuilder.addCode(invokeCodeBlock.build());
      }

      info.builder.addMethod(methodBuilder.build());

      intList.add(id);
    }
    return intList;
  }

  /**
   * 调用指定的方法
   *
   * @since 2024/12/4 11:01
   */
  private static CodeBlock.Builder buildInvokeCodeBlock(ExecutableElement executableElement) {
    String paramStr = executableElement.getParameters().stream()
        .map(p -> p.getSimpleName().toString())
        .collect(Collectors.joining(", "));
    Name methodName = executableElement.getSimpleName();

    CodeBlock.Builder codeBlock = CodeBlock.builder();
    if (hasReturnValue(executableElement)) {
      String resVarName = "res";
      String resBuf = "resBuf";
      TypeMirror returnTypeMirror = executableElement.getReturnType();
      switch (returnTypeMirror.getKind()) {
        case BOOLEAN ->
            codeBlock.addStatement("boolean $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
                methodName, paramStr);
        case BYTE -> codeBlock.addStatement("byte $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case SHORT -> codeBlock.addStatement("short $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case CHAR -> codeBlock.addStatement("char $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case FLOAT -> codeBlock.addStatement("float $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case DOUBLE -> codeBlock.addStatement("double $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case INT -> codeBlock.addStatement("int $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        case LONG -> codeBlock.addStatement("long $L = $L.$L($L)", resVarName, FACADE_VAR_NAME,
            methodName, paramStr);
        default -> codeBlock.addStatement("$T $L = $L.$L($L)", TypeName.get(returnTypeMirror),
            resVarName,
            FACADE_VAR_NAME,
            methodName, paramStr);
      }

      codeBlock
          .add("\n")
          .addStatement("$T $L = $T.DEFAULT.buffer()", BYTE_BUF, resBuf, Util.POOLED_UTIL)
          .beginControlFlow("try")
          .addStatement("$L.writeVarInt32($L, $L)", SERIALIZER_VAR_NAME, resBuf, MSG_ID_VAR_NAME)
          .addStatement("$L.writeObject($L, $L)", SERIALIZER_VAR_NAME, resBuf, resVarName)
          .addStatement("$L.channel().writeAndFlush($T.callBack($L))", CONNECTION_VAR_NAME,
              MESSAGE_CLASS_NAME, resBuf)
          .endControlFlow()
          .beginControlFlow("catch (Throwable t)")
          .addStatement("$T.release($L)", ReferenceCountUtil.class, resBuf)
          .addStatement("throw t")
          .endControlFlow();

    } else {
      codeBlock.addStatement("$L.$L($L)", FACADE_VAR_NAME, methodName, paramStr);
    }

    return codeBlock;
  }

  private MethodSpec buildRegisterMethod(TypeSpecInfo info, IntList intList) {
    final String dispatcherVarName = "dispatcher";
    StringJoiner joiner = new StringJoiner(",", "{", "}");
    for (int id : intList) {
      joiner.add(String.valueOf(id));
    }

    info.builder.addSuperinterface(HandlerRegister.class);
    MethodSpec.Builder registerMethod = MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(DefaultDispatcher.class, dispatcherVarName)
        .addStatement("$L.registeHandlers(new int[]$L, this)", dispatcherVarName, joiner);

    return registerMethod.build();
  }

  private static boolean hasReturnValue(ExecutableElement executableElement) {
    return executableElement.getReturnType().getKind() != TypeKind.VOID;
  }

}
