package org.example.net.anno.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import org.example.net.anno.Req;

class Util {

  /** 常用类型 */
  public static final ClassName CONNECTION = ClassName.get("org.example.net", "Connection");
  public static final ClassName HANDLER_ANNOTATION = ClassName.get("org.example.common.generator.rpc", "Handler");
  public static final ParameterizedTypeName CONNECTION_GETTER = ParameterizedTypeName.get(
      ClassName.get("java.util.function", "Function"),
      TypeName.INT.box(), CONNECTION
  );
  public static final ClassName BASE_REMOTING = ClassName.get("org.example.net", "BaseRemoting");
  public static final ClassName COMMON_SERIALIZER = ClassName.get("org.example.serde",
      "CommonSerializer");

  public static final String SERIALIZER_VAR_NAME = "serializer";
  public static final FieldSpec COMMON_SERIALIZER_FIELD_SPEC = FieldSpec
      .builder(COMMON_SERIALIZER, SERIALIZER_VAR_NAME)
      .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build();
  public static final ClassName BYTE_BUF = ClassName.get("io.netty.buffer", "ByteBuf");
  public static final ClassName BYTEBUF_UTIL = ClassName.get("org.example.serde",
      "NettyByteBufUtil");
  public static final ClassName POOLED_UTIL = ClassName.get("io.netty.buffer",
      "PooledByteBufAllocator");

  public static final ClassName UNNPOOLED_UTIL = ClassName.get("io.netty.buffer",
      "Unpooled");
  public static final ClassName MESSAGE = ClassName.get("org.example.net", "Message");
  public static final ClassName LOGGER = ClassName.get("org.slf4j", "Logger");
  public static final ClassName LOGGER_FACTOR = ClassName.get("org.slf4j", "LoggerFactory");
  public static final ClassName HANDLER_INTERFACE = ClassName.get("org.example.net.handler",
      "Handler");

  public static final ClassName COMPLETE_ABLE_FUTURE_TYPE = ClassName.get("java.util.concurrent",
      "CompletableFuture");

  public static List<Element> getReqMethod(ProcessingEnvironment processingEnv,
      TypeElement typeElement) {
    List<Element> res = new ArrayList<>();

    for (Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) {
        continue;
      }

      if (element.getAnnotation(Req.class) == null) {
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

      res.add(element);
    }

    return res;
  }


  public static int calcProtoId(TypeElement facadeName, ExecutableElement method) {
    Req reqAnno = method.getAnnotation(Req.class);
    int id = reqAnno.value();

    if (id == 0) {
      // 类名+方法名的哈希的绝对值作为协议ID,
      id = Math.abs((facadeName.getQualifiedName() + "#" + method.getSimpleName().toString()).hashCode());
    }

    return id;
  }


  public static DeclaredType isCompleteAbleFuture(TypeMirror mirror) {
    if (mirror.getKind() != TypeKind.DECLARED) {
      return null;
    }
    DeclaredType declaredType = (DeclaredType) mirror;
    TypeElement returnTypeElement = (TypeElement) declaredType.asElement();
    if(returnTypeElement.getQualifiedName()
        .contentEquals(Util.COMPLETE_ABLE_FUTURE_TYPE.toString())) {
      return declaredType;
    } else {
      return null;
    }
  }

  private Util() {
  }
}
