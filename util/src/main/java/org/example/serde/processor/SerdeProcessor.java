package org.example.serde.processor;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeSpec.Builder;
import io.netty.buffer.ByteBuf;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import org.apache.commons.lang3.StringUtils;
import org.example.serde.Serde;
import org.example.serde.SerdeRegister;
import org.example.serde.Serdes;
import org.example.serde.Serializer;

@SupportedAnnotationTypes("org.example.serde.Serde")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class SerdeProcessor extends AbstractProcessor {

  private static final String DESERIALZIER_IMPL = "deserialzierImpl";
  private static final String FAST_DESERIALZIER_IMPL = "fastDeserialzier";
  private static final String SERIALIZER_IMPL = "serializerImpl";
  private static final String FAST_SERIALIZER_IMPL = "fastSerializer";
  private static final String SERDE_SUB_FIX = "Serde";
  private static final String BUF_VAR_NAME = "buf";
  private static final String SERIALIZER_VAR_NAME = "serializer";
  private static final String OBJECT_VAR_NAME = "object";

  private final Map<Integer, String> serdeObjects = new HashMap<>();

  public SerdeProcessor() {
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    processElements(annotations, roundEnv);
    return false;
  }

  private void processElements(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      Set<? extends Element> annotationElements = roundEnv.getElementsAnnotatedWith(annotation);
      if (annotationElements.isEmpty()) {
        continue;
      }

      for (Element clz : annotationElements) {
        if (clz.getKind() != ElementKind.CLASS && clz.getKind() != ElementKind.RECORD) {
          processingEnv.getMessager()
              .printMessage(Kind.ERROR, "@Serde must be applied to a Class", clz);
          return;
        }

        TypeElement clazz = (TypeElement) clz;
        ClassName typename = ClassName.get(clazz);
        ClassName serderTypeName = ClassName.get(typename.packageName(),
            typename.simpleName() + SERDE_SUB_FIX);

        try {

          TypeName genericInterface = ParameterizedTypeName.get(ClassName.get(Serializer.class),
              typename);
          Builder builder = TypeSpec.classBuilder(serderTypeName.simpleName())
              .addSuperinterface(genericInterface)
              .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

          switch (clazz.getKind()) {
            case CLASS -> {
              List<Element> fieldElements = BeanSerde.getAllFieldElements(this,
                  clazz);
              BeanSerde.deSerializerCode(this, builder, typename, fieldElements);
              BeanSerde.serializerCode(this, builder, typename, fieldElements);
              constructor(builder);

              reigsterMethod(clz, typename, builder);

              JavaFileObject builderFile = processingEnv.getFiler()
                  .createSourceFile(serderTypeName.canonicalName());
              try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
                JavaFile.builder(typename.packageName(), builder.build()).build().writeTo(writer);
              }
            }
            case RECORD -> {
              List<Element> fieldElements = RecordSerde.getAllFieldElements(clazz);
              RecordSerde.deSerializerCode(this, builder, typename, fieldElements);
              RecordSerde.serializerCode(this, builder, typename, fieldElements);
              constructor(builder);
              reigsterMethod(clz, typename, builder);

              JavaFileObject builderFile = processingEnv.getFiler()
                  .createSourceFile(serderTypeName.canonicalName());
              try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
                JavaFile.builder(typename.packageName(), builder.build()).build().writeTo(writer);
              }
            }
            default -> {
              processingEnv.getMessager()
                  .printMessage(Kind.ERROR, "@Serde must be applied to a Class", clazz);
              return;
            }
          }

        } catch (Throwable e) {
          processingEnv.getMessager()
              .printError(
                  "[%s] %s build Serde error, %s\n%s".formatted(getClass(),
                      clazz,
                      e,
                      Arrays.stream(
                              e.getStackTrace()).map(Objects::toString)
                          .collect(Collectors.joining("\n"))), clazz);
        }
      }
    }
  }

  /**
   * constructor and SerdeRegister
   *
   * @since 2025/5/14 13:04
   */
  private static TypeSpec.Builder constructor(TypeSpec.Builder builder) {
    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .build();

    return builder.addMethod(constructor);
  }

  private Builder reigsterMethod(Element clazz, ClassName type, Builder builder) {
    int protoId = type.toString().hashCode();
    String prev = serdeObjects.get(protoId);
    if (prev != null) {
      processingEnv.getMessager()
          .printError(
              "[%s]\n[%s]\nid:%s, hashID发生碰撞，请修改名字以避免".formatted(type, prev, protoId),
              clazz);
      return builder;
    }

    serdeObjects.put(protoId, type.toString());
    builder.addSuperinterface(SerdeRegister.class)
        .addAnnotation(AnnotationSpec
            .builder(AutoService.class)
            .addMember("value", "$T.class", SerdeRegister.class)
            .build());
    MethodSpec register = MethodSpec
        .methodBuilder("register")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
        .addStatement("$L.registerSerializer($L, $T.class, this)", SERIALIZER_VAR_NAME, protoId,
            type)
        .build();
    return builder.addMethod(register);
  }

  /**
   * 快速反序列化入口
   *
   * @since 2025/6/1 21:37
   */
  private static void buildFastDeSerialzier(Builder typeBuilder, TypeName typeName) {
    String typeIdVarName = "typeId";
    MethodSpec.Builder fastReadObject = MethodSpec.methodBuilder(FAST_DESERIALZIER_IMPL)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
        .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
        .addParameter(ByteBuf.class, BUF_VAR_NAME)
        .addStatement("int $L = $L.readVarInt32($L)", typeIdVarName, SERIALIZER_VAR_NAME,
            BUF_VAR_NAME)
        .beginControlFlow("if ($L.isNullId($L))", SERIALIZER_VAR_NAME, typeIdVarName)
        .addStatement("return null")
        .endControlFlow()
        .addStatement("return $L($L, $L)", DESERIALZIER_IMPL, SERIALIZER_VAR_NAME, BUF_VAR_NAME)
        .returns(typeName);

    typeBuilder.addMethod(fastReadObject.build());
  }

  /**
   * 快速序列化入口
   *
   * @since 2025/6/1 21:37
   */
  private static void buildFastSerializerCode(Builder typeBuilder, TypeName typeName) {
    MethodSpec.Builder fastReadObject = MethodSpec.methodBuilder(FAST_SERIALIZER_IMPL)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
        .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
        .addParameter(ByteBuf.class, BUF_VAR_NAME)
        .addParameter(typeName, OBJECT_VAR_NAME)
        .beginControlFlow("if ($L == null)", OBJECT_VAR_NAME)
        .addStatement("$L.writeNull($L)", SERIALIZER_VAR_NAME, BUF_VAR_NAME)
        .addStatement("return")
        .endControlFlow()
        .addStatement("$L.writeVarInt32($L, $L)", SERIALIZER_VAR_NAME, BUF_VAR_NAME,
            typeName.toString().hashCode())
        .addStatement("$L($L, $L, $L)", SERIALIZER_IMPL, SERIALIZER_VAR_NAME, BUF_VAR_NAME,
            OBJECT_VAR_NAME)
        .returns(TypeName.VOID);

    typeBuilder.addMethod(fastReadObject.build());
  }

  private static CodeBlock tryFastDeSerialzier(SerdeProcessor processor, Element element) {
    Element fullElement = processor.processingEnv.getTypeUtils().asElement(element.asType());

    CodeBlock.Builder builder = CodeBlock.builder();
    if (fullElement.getAnnotation(Serde.class) != null) {
      TypeElement clazz = (TypeElement) fullElement;
      ClassName typeName = ClassName.get(clazz);
      ClassName serderTypeName = ClassName.get(typeName.packageName(),
          typeName.simpleName() + SERDE_SUB_FIX);
      builder.add("$T.$L($L, $L)",
          serderTypeName,
          FAST_DESERIALZIER_IMPL,
          SERIALIZER_VAR_NAME,
          BUF_VAR_NAME
      );
    } else {
      builder.add("$L.readObject($L)",
          SERIALIZER_VAR_NAME,
          BUF_VAR_NAME
      );
    }
    return builder.build();
  }

  private static CodeBlock tryFastSerialzier(SerdeProcessor processor, Element element,
      CodeBlock getter) {
    Element fullElement = processor.processingEnv.getTypeUtils().asElement(element.asType());

    CodeBlock.Builder builder = CodeBlock.builder();
    if (fullElement.getAnnotation(Serde.class) != null) {
      TypeElement clazz = (TypeElement) fullElement;
      ClassName typeName = ClassName.get(clazz);
      ClassName serderTypeName = ClassName.get(typeName.packageName(),
          typeName.simpleName() + SERDE_SUB_FIX);
      builder.add("$T.$L($L, $L, $L)",
          serderTypeName,
          FAST_SERIALIZER_IMPL,
          SERIALIZER_VAR_NAME,
          BUF_VAR_NAME,
          getter
      );
    } else {
      builder.add("$L.writeObject($L, $L)",
          SERIALIZER_VAR_NAME,
          BUF_VAR_NAME,
          getter
      );
    }
    return builder.build();
  }


  /**
   * class代码生成
   *
   * @author zhongjianping
   * @since 2024/11/19 15:08
   */
  private static final class RecordSerde {

    public static List<Element> getAllFieldElements(TypeElement element) {
      return element.getEnclosedElements().stream()
          .filter(e -> e.getKind() == ElementKind.RECORD_COMPONENT)
          .collect(Collectors.toUnmodifiableList());
    }

    public static void deSerializerCode(SerdeProcessor processor, TypeSpec.Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {
      buildDeSerializerCode(processor, typeBuilder, typeName, fieldElements);
      buildFastDeSerialzier(typeBuilder, typeName);
    }

    private static void buildDeSerializerCode(SerdeProcessor processor, Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {
      MethodSpec.Builder impl = MethodSpec.methodBuilder(DESERIALZIER_IMPL)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
          .returns(typeName)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME);

      impl.addCode("return new $T(\n", typeName);
      Iterator<Element> elementIterator = fieldElements.iterator();
      while (elementIterator.hasNext()) {
        Element e = elementIterator.next();
        switch (e.asType().getKind()) {
          case BOOLEAN -> impl.addCode("$L.readBoolean()", BUF_VAR_NAME);
          case BYTE -> impl.addCode("$L.readByte()", BUF_VAR_NAME);
          case SHORT -> impl.addCode("$L.readShort()", BUF_VAR_NAME);
          case CHAR -> impl.addCode("$L.readChar()", BUF_VAR_NAME);
          case FLOAT -> impl.addCode("$L.readFloat()", BUF_VAR_NAME);
          case DOUBLE -> impl.addCode("$T $L = $L.readDouble()", BUF_VAR_NAME);
          case INT -> impl.addCode("$L.readVarInt32($L)",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          case LONG -> impl.addCode("$L.readVarInt64($L)",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          default -> impl.addCode("$L", tryFastDeSerialzier(processor, e));
        }

        if (elementIterator.hasNext()) {
          impl.addCode(",");
        }
        impl.addCode("\n");
      }
      impl.addCode(");");

      MethodSpec.Builder readObject = MethodSpec.methodBuilder("readObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME)
          .addStatement("return $L($L, $L)", DESERIALZIER_IMPL, SERIALIZER_VAR_NAME, BUF_VAR_NAME)
          .returns(typeName);

      typeBuilder
          .addMethod(impl.build())
          .addMethod(readObject.build());
    }

    public static void serializerCode(SerdeProcessor processor, TypeSpec.Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {
      buildSerializerCode(processor, typeBuilder, typeName, fieldElements);
      buildFastSerializerCode(typeBuilder, typeName);
    }

    private static void buildSerializerCode(SerdeProcessor processor, Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {
      MethodSpec.Builder impl = MethodSpec.methodBuilder(SERIALIZER_IMPL)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addParameter(typeName, OBJECT_VAR_NAME, Modifier.FINAL)
          .returns(TypeName.VOID);

      fieldElements.forEach(e -> {
        String fieldName = e.getSimpleName().toString();
        switch (e.asType().getKind()) {
          case BOOLEAN -> impl.addStatement("$L.writeBoolean($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case BYTE -> impl.addStatement("$L.writeByte($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case SHORT -> impl.addStatement("$L.writeShort($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case CHAR -> impl.addStatement("$L.writeChar($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case FLOAT -> impl.addStatement("$L.writeFloat($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case DOUBLE -> impl.addStatement("$L.writeDouble($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case INT -> impl.addStatement("$L.writeVarInt32($L, $L.$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case LONG -> impl.addStatement("$L.writeVarInt64($L, $L.$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          default -> impl.addStatement(tryFastSerialzier(processor, e,
              CodeBlock
                  .builder()
                  .add("$L.$L()", OBJECT_VAR_NAME, fieldName)
                  .build()
          ));
        }
      });

      MethodSpec.Builder writeObject = MethodSpec.methodBuilder("writeObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME)
          .addParameter(typeName, OBJECT_VAR_NAME)
          .addStatement("$L($L, $L, $L)", SERIALIZER_IMPL, SERIALIZER_VAR_NAME, BUF_VAR_NAME,
              OBJECT_VAR_NAME)
          .returns(TypeName.VOID);

      typeBuilder
          .addMethod(impl.build())
          .addMethod(writeObject.build());
    }

  }


  /**
   * class代码生成
   *
   * @author zhongjianping
   * @since 2024/11/19 15:08
   */
  private static final class BeanSerde {

    private static TypeElement getSuperclass(SerdeProcessor processor, TypeElement type) {
      if (type.getSuperclass().getKind() == TypeKind.DECLARED) {
        TypeElement superclass = (TypeElement) processor.processingEnv.getTypeUtils()
            .asElement(type.getSuperclass());
        String name = superclass.getQualifiedName().toString();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("android.")) {
          // Skip system classes, this just degrades performance
          return null;
        } else {
          return superclass;
        }
      } else {
        return null;
      }
    }


    public static List<Element> getAllFieldElements(SerdeProcessor processor, TypeElement element) {
      List<TypeElement> clazzs = new ArrayList<>();
      clazzs.add(element);

      TypeElement parent = element;
      while (true) {
        parent = getSuperclass(processor, parent);
        if (parent == null) {
          break;
        }

        clazzs.addFirst(parent);
      }

      List<Element> fields = new ArrayList<>();
      for (TypeElement typeElement : clazzs) {
        List<Element> fieldElements = typeElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.FIELD)
            .filter(e -> !(
                e.getModifiers().contains(Modifier.FINAL) ||
                    e.getModifiers().contains(Modifier.STATIC) ||
                    e.getModifiers().contains(Modifier.TRANSIENT)
            ))
            .collect(Collectors.toUnmodifiableList());

        fields.addAll(fieldElements);
      }

      return fields;
    }

    public static void deSerializerCode(SerdeProcessor processor, TypeSpec.Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {

      buildDeSerialzier(processor, typeBuilder, typeName, fieldElements);
      buildFastDeSerialzier(typeBuilder, typeName);
    }

    private static void buildDeSerialzier(SerdeProcessor processor, Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {
      MethodSpec.Builder impl = MethodSpec.methodBuilder(DESERIALZIER_IMPL)
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME, Modifier.FINAL)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addStatement("$T $L = new $T()", typeName, OBJECT_VAR_NAME, typeName)
          .returns(typeName);

      for (Element e : fieldElements) {
        String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
        switch (e.asType().getKind()) {
          case BOOLEAN -> impl.addStatement("$L.set$L($L.readBoolean())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case BYTE -> impl.addStatement("$L.set$L($L.readByte())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case SHORT -> impl.addStatement("$L.set$L($L.readShort())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case CHAR -> impl.addStatement("$L.set$L($L.readChar())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case FLOAT -> impl.addStatement("$L.set$L($L.readFloat())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case DOUBLE -> impl.addStatement("$L.set$L($L.readDouble())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case INT -> impl.addStatement("$L.set$L($L.readVarInt32($L))",
              OBJECT_VAR_NAME,
              fieldName,
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          case LONG -> impl.addStatement("$L.set$L($L.readVarInt64($L))",
              OBJECT_VAR_NAME,
              fieldName,
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          default -> impl.addStatement("$L.set$L($L)",
              OBJECT_VAR_NAME,
              fieldName,
              tryFastDeSerialzier(processor, e)
          );
        }
      }

      impl.addStatement("return object");

      MethodSpec.Builder readObject = MethodSpec.methodBuilder("readObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME)
          .addStatement("return $L($L, $L)", DESERIALZIER_IMPL, SERIALIZER_VAR_NAME, BUF_VAR_NAME)
          .returns(typeName);

      typeBuilder
          .addMethod(impl.build())
          .addMethod(readObject.build());
    }


    public static void serializerCode(SerdeProcessor processor, TypeSpec.Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {
      buildSerializerCode(processor, typeBuilder, typeName, fieldElements);
      buildFastSerializerCode(typeBuilder, typeName);
    }

    private static void buildSerializerCode(SerdeProcessor processor, Builder typeBuilder,
        TypeName typeName,
        List<Element> fieldElements) {
      MethodSpec.Builder impl = MethodSpec.methodBuilder(SERIALIZER_IMPL)
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME)
          .addParameter(typeName, OBJECT_VAR_NAME)
          .returns(TypeName.VOID);

      fieldElements.forEach(e -> {
        String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
        switch (e.asType().getKind()) {
          case BOOLEAN -> impl.addStatement("$L.writeBoolean($L.is$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case BYTE -> impl.addStatement("$L.writeByte($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case SHORT -> impl.addStatement("$L.writeShort($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case CHAR -> impl.addStatement("$L.writeChar($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case FLOAT -> impl.addStatement("$L.writeFloat($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case DOUBLE -> impl.addStatement("$L.writeDouble($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case INT -> impl.addStatement("$L.writeVarInt32($L, $L.get$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case LONG -> impl.addStatement("$L.writeVarInt64($L, $L.get$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          default -> tryFastSerialzier(processor, e,
              CodeBlock
                  .builder()
                  .add("$L.get$L()", OBJECT_VAR_NAME, fieldName)
                  .build()
          );
        }
      });

      MethodSpec.Builder writeObject = MethodSpec.methodBuilder("writeObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME)
          .addParameter(typeName, OBJECT_VAR_NAME)
          .addStatement("$L($L, $L, $L)", SERIALIZER_IMPL, SERIALIZER_VAR_NAME, BUF_VAR_NAME,
              OBJECT_VAR_NAME)
          .returns(TypeName.VOID);

      typeBuilder
          .addMethod(impl.build())
          .addMethod(writeObject.build());
    }
  }


}