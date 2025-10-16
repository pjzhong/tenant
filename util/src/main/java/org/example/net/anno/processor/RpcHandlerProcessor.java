package org.example.net.anno.processor;

import static org.example.net.anno.processor.Util.BUF_VAR_NAME;
import static org.example.net.anno.processor.Util.BYTE_BUF;
import static org.example.net.anno.processor.Util.CONNECTION_CLASS_NAME;
import static org.example.net.anno.processor.Util.FACADE_VAR_NAME;
import static org.example.net.anno.processor.Util.MESSAGE_CLASS_NAME;
import static org.example.net.anno.processor.Util.MSG_ID_VAR_NAME;
import static org.example.net.anno.processor.Util.SERIALIZER_VAR_NAME;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.netty.util.ReferenceCountUtil;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

  public static final String DISPATCHER_VAR_NAME = "dispatcher";
  private static final String CONNECTION_VAR_NAME = "c";
  private static final String MESSAGE_VAR_NAME = "m";
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
        //.addSuperinterface(Util.HANDLER_INTERFACE)
        .addAnnotation(Util.COMPONENT_ANNOTATION)
        .addField(FieldSpec
            .builder(facdeTypeName, FACADE_VAR_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build())
        .addField(Util.COMMON_SERIALIZER_FIELD_SPEC);

    TypeSpecInfo info = new TypeSpecInfo(facade, typeSpecBuilder, elements);
    ExecutorSupplierUtil.buildExecuotSupplerCode(processingEnv, info);

    buildHandlerMethod(info);

    typeSpecBuilder.addMethod(info.constructor
        .addModifiers(Modifier.PUBLIC)
        .addParameter(facdeTypeName, FACADE_VAR_NAME)
        .addParameter(Util.COMMON_SERIALIZER, SERIALIZER_VAR_NAME)
        .addStatement("this.$L = $L", FACADE_VAR_NAME, FACADE_VAR_NAME)
        .addStatement("this.$L = $L", SERIALIZER_VAR_NAME, SERIALIZER_VAR_NAME)
        .build());

    JavaFile javaFile = JavaFile.builder(packet, typeSpecBuilder.build())
        .build();

    JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
    try (PrintWriter writer = new PrintWriter(file.openWriter())) {
      javaFile.writeTo(writer);
    }
  }

  void buildHandlerMethod(TypeSpecInfo info) {
    MethodSpec.Builder registerMethod = buildRegisterMethod(info);

    //invoker.beginControlFlow("switch(m.proto())");
    buildHandlerMethod0(info, registerMethod);
//

    info.builder
        .addMethod(registerMethod.build());
  }

  private void buildHandlerMethod0(TypeSpecInfo info,
      MethodSpec.Builder registerMethod) {

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

      registerMethod.addStatement("$L.registeHandler($L, this::$L)", DISPATCHER_VAR_NAME, id,
          methodName);

      methodBuilder.addStatement("$T $L = $L.packet()", BYTE_BUF, BUF_VAR_NAME, MESSAGE_VAR_NAME);

      if (hasReturnValue(element)) {
        methodBuilder.addStatement("int $L = $L.readVarInt32($L)", MSG_ID_VAR_NAME,
            SERIALIZER_VAR_NAME,
            BUF_VAR_NAME);
      }

      CodeBlock paramDeSerde = paramDeSerde(element);

      methodBuilder.addCode(paramDeSerde);

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


    }
  }

  private CodeBlock paramDeSerde(ExecutableElement element) {
    CodeBlock.Builder paramDeSerde = CodeBlock.builder();
    List<? extends VariableElement> params = element.getParameters();
    for (VariableElement p : params) {
      final Name pname = p.getSimpleName();
      TypeMirror ptype = p.asType();
      switch (ptype.getKind()) {
        case BOOLEAN ->
            paramDeSerde.addStatement("boolean $L = $L.readBoolean()", pname, BUF_VAR_NAME);
        case BYTE -> paramDeSerde.addStatement("byte $L = $L.readByte()", pname, BUF_VAR_NAME);
        case SHORT -> paramDeSerde.addStatement("short $L = $L.readShort()", pname, BUF_VAR_NAME);
        case CHAR -> paramDeSerde.addStatement("char $L = $L.readChar()", pname, BUF_VAR_NAME);
        case FLOAT -> paramDeSerde.addStatement("float $L = $L.readFloat()", pname, BUF_VAR_NAME);
        case DOUBLE ->
            paramDeSerde.addStatement("double $L = $L.readDouble()", pname, BUF_VAR_NAME);
        case INT ->
            paramDeSerde.addStatement("int $L = $L.readVarInt32($L)", pname, SERIALIZER_VAR_NAME,
                BUF_VAR_NAME);
        case LONG -> paramDeSerde.addStatement("long $L = $L.readVarInt64($L)", pname,
            SERIALIZER_VAR_NAME,
            BUF_VAR_NAME);
        default -> {
          TypeMirror paramType = p.asType();
          TypeName paramTypeName = TypeName.get(paramType);

          if (paramTypeName.equals(CONNECTION_CLASS_NAME)) {
            paramDeSerde.addStatement("$T $L = $L", CONNECTION_CLASS_NAME, pname,
                CONNECTION_VAR_NAME);
          } else if (paramTypeName.equals(MESSAGE_CLASS_NAME)) {
            paramDeSerde.addStatement("$T $L = $L", MESSAGE_CLASS_NAME, pname,
                MESSAGE_VAR_NAME);
          } else {
            RpcSerdesUtil.tryFastDeSerde(processingEnv, paramDeSerde, p);
          }
        }
      }
    }

    if (!paramDeSerde.isEmpty()) {
      paramDeSerde.add("\n");
    }

    return paramDeSerde.build();
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
      final String resVarName = "res";
      final String resBufVarName = "resBuf";
      final String starVarName = "start";
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
          .addStatement("$T $L = $T.DEFAULT.buffer()", BYTE_BUF, resBufVarName, Util.POOLED_UTIL)
          .beginControlFlow("try")
          .addStatement("final int $L = $L.writerIndex()", starVarName, resBufVarName)
          .addStatement("$L.writerIndex($L + Integer.BYTES)", resBufVarName, starVarName)
          .addStatement("$L.writeVarInt32($L, 0)", SERIALIZER_VAR_NAME, resBufVarName)
          .addStatement("$L.writeVarInt32($L, $L)", SERIALIZER_VAR_NAME, resBufVarName,
              MSG_ID_VAR_NAME)
          .addStatement("$L.serialize($L, $L)", SERIALIZER_VAR_NAME, resBufVarName, resVarName)
          .addStatement("$L.setInt($L, $L.readableBytes())", resBufVarName, starVarName,
              resBufVarName)
          .addStatement("$L.channel().writeAndFlush($L)", CONNECTION_VAR_NAME, resBufVarName)
          .endControlFlow()
          .beginControlFlow("catch (Throwable t)")
          .addStatement("$T.release($L)", ReferenceCountUtil.class, resBufVarName)
          .addStatement("throw t")
          .endControlFlow();

    } else {
      codeBlock.addStatement("$L.$L($L)", FACADE_VAR_NAME, methodName, paramStr);
    }

    return codeBlock;
  }

  private MethodSpec.Builder buildRegisterMethod(TypeSpecInfo info) {

    info.builder.addSuperinterface(HandlerRegister.class);

    return MethodSpec.methodBuilder("register")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(DefaultDispatcher.class, DISPATCHER_VAR_NAME);
  }

  private static boolean hasReturnValue(ExecutableElement executableElement) {
    return executableElement.getReturnType().getKind() != TypeKind.VOID;
  }

}
