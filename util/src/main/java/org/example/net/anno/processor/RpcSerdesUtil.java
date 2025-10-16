package org.example.net.anno.processor;

import static org.example.net.anno.processor.Util.BUF_VAR_NAME;
import static org.example.net.anno.processor.Util.SERIALIZER_VAR_NAME;
import static org.example.serde.processor.SerdeProcessor.FAST_DESERIALZIER_IMPL;
import static org.example.serde.processor.SerdeProcessor.FAST_SERIALIZER_IMPL;
import static org.example.serde.processor.SerdeProcessor.SERDE_SUB_FIX;
import static org.example.serde.processor.SerdeProcessor.isFinalSerde;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * RPC模块和序列化模块的交集
 *
 * @author zhongjianping
 * @since 2025/10/16 15:13
 */
final class RpcSerdesUtil {

  private RpcSerdesUtil() {
  }

  public static CodeBlock.Builder tryFastDeSerde(ProcessingEnvironment processingEnv,
      CodeBlock.Builder paramDeSerde, VariableElement element) {
    Element fullElement = processingEnv.getTypeUtils().asElement(element.asType());
    Name name = element.getSimpleName();

    if (isFinalSerde(fullElement)) {
      TypeElement clazz = (TypeElement) fullElement;
      ClassName typeName = ClassName.get(clazz);
      ClassName serderTypeName = ClassName.get(typeName.packageName(),
          typeName.simpleName() + SERDE_SUB_FIX);
      paramDeSerde.addStatement("$T $L = $T.$L($L, $L)",
          TypeName.get(element.asType()),
          name,
          serderTypeName,
          FAST_DESERIALZIER_IMPL,
          SERIALIZER_VAR_NAME,
          BUF_VAR_NAME
      );
    } else {
      paramDeSerde.addStatement("$T $L = $L.deserialize($L)", TypeName.get(element.asType()), name,
          SERIALIZER_VAR_NAME, BUF_VAR_NAME);
    }
    return paramDeSerde;

  }

  public static CodeBlock.Builder tryFastSerde(ProcessingEnvironment processingEnv,
      CodeBlock.Builder paramSerde, VariableElement element) {
    Element fullElement = processingEnv.getTypeUtils().asElement(element.asType());
    Name name = element.getSimpleName();

    if (isFinalSerde(fullElement)) {
      TypeElement clazz = (TypeElement) fullElement;
      ClassName typeName = ClassName.get(clazz);
      ClassName serderTypeName = ClassName.get(typeName.packageName(),
          typeName.simpleName() + SERDE_SUB_FIX);
      paramSerde.addStatement("$T.$L($L, $L, $L)",
          serderTypeName,
          FAST_SERIALIZER_IMPL,
          SERIALIZER_VAR_NAME,
          BUF_VAR_NAME,
          name
      );
    } else {
      paramSerde.addStatement("$L.serialize(buf, $L)", SERIALIZER_VAR_NAME, name);
    }
    return paramSerde;

  }
}
