package org.example.net.anno.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.example.net.AsyncFuture;
import org.example.net.anno.Req;
import org.example.net.handler.ArgExecSupplier;
import org.example.net.handler.ExecSupplier;
import org.example.net.handler.RawExecSupplier;
import org.example.net.handler.SysArgExecSupplier;
import org.example.net.handler.SysArgsExecSupplier;
import org.example.serde.Serdes;
import org.example.util.NettyByteBufUtil;

final class Util {

  /** 常用类型 */

  public static final TypeName EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(ExecSupplier.class);
  public static final TypeName ARG_EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(
      ArgExecSupplier.class);
  public static final TypeName SYS_ARG_EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(
      SysArgExecSupplier.class);
  public static final TypeName SYS_ARGS_EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(
      SysArgsExecSupplier.class);
  public static final TypeName RAW_EXECUTOR_SUPPLIER_CLASS_NAME = TypeName.get(
      RawExecSupplier.class);
  public static final ClassName CONNECTION_CLASS_NAME = ClassName.get("org.example.net",
      "Connection");
  public static final ClassName CONNECTION_GETTER =
      ClassName.get("org.example.net", "ConnectionManager");
  public static final ClassName BASE_REMOTING = ClassName.get("org.example.net", "BaseRemoting");
  public static final ClassName COMMON_SERIALIZER = ClassName.get(Serdes.class);

  public static final String SERIALIZER_VAR_NAME = "serializer";
  public static final FieldSpec COMMON_SERIALIZER_FIELD_SPEC = FieldSpec
      .builder(COMMON_SERIALIZER, SERIALIZER_VAR_NAME)
      .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
      .build();
  public static final ClassName BYTE_BUF = ClassName.get("io.netty.buffer", "ByteBuf");
  public static final ClassName POOLED_UTIL = ClassName.get(NettyByteBufUtil.class);

  public static final ClassName UNNPOOLED_UTIL = ClassName.get("io.netty.buffer",
      "Unpooled");
  public static final ClassName MESSAGE_CLASS_NAME = ClassName.get("org.example.net", "Message");
  public static final ClassName LOGGER = ClassName.get("org.slf4j", "Logger");
  public static final ClassName LOGGER_FACTOR = ClassName.get("org.slf4j", "LoggerFactory");

  public static final ClassName NET_COMPLETE_ABLE_FUTURE_CLASS_NAME = ClassName.get(
      AsyncFuture.class);


  public static final ClassName SUPPLIER_CLASS_NAME = ClassName.get(
      "java.util.function",
      "Supplier");

  public static final ClassName COMPONENT_ANNOTATION = ClassName.get(
      "org.springframework.stereotype", "Component");

  public static final ClassName IDENTITY_CLASS_NAME = ClassName.get("org.example.util", "Identity");


  public static final String MSG_ID_VAR_NAME = "msgId";

  public static final String FACADE_VAR_NAME = "f";

  public static List<ExecutableElement> getReqMethod(ProcessingEnvironment processingEnv,
      TypeElement typeElement, Class<? extends Annotation> anno) {
    List<ExecutableElement> res = new ArrayList<>();

    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) {
        continue;
      }

      if (element.getAnnotation(anno) == null) {
        continue;
      }

      if (element.getModifiers().contains(Modifier.ABSTRACT)) {
        processingEnv.getMessager()
            .printMessage(Kind.ERROR, "@Req can't not applied to abstract method", element);
        continue;
      }

      if (element.getModifiers().contains(Modifier.STATIC)) {
        processingEnv.getMessager()
            .printMessage(Kind.ERROR, "@Req can't not applied to static method", element);
        continue;
      }

      if (!element.getModifiers().contains(Modifier.PUBLIC)) {
        processingEnv.getMessager()
            .printMessage(Kind.ERROR, "@Req  mnust applied to public method", element);
        continue;
      }

      res.add((ExecutableElement) element);
    }

    return res;
  }


  public static int calcProtoId(TypeElement facadeName, ExecutableElement method) {
    Req reqAnno = method.getAnnotation(Req.class);
    int id = reqAnno.value();

    if (id == 0) {
      // 类名哈希+方法名哈希的绝对值作为协议ID,
      id = Math.abs(
          facadeName.getQualifiedName().toString().hashCode()
              + method.getSimpleName().toString().hashCode());
    }

    return id;
  }


  private Util() {
  }
}
