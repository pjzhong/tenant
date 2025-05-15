package org.example.serde.processor;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
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
import org.example.serde.SerdeRegister;
import org.example.serde.Serdes;
import org.example.serde.Serializer;

@SupportedAnnotationTypes("org.example.serde.Serde")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class SerdeProcessor extends AbstractProcessor {

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
              MethodSpec deSer = BeanSerde.deSerializerCode(typename, fieldElements);
              MethodSpec serde = BeanSerde.serializerCode(typename, fieldElements);
              constructor(builder)
                  .addMethod(deSer)
                  .addMethod(serde)
              ;

              reigsterMethod(clz, typename, builder);

              JavaFileObject builderFile = processingEnv.getFiler()
                  .createSourceFile(serderTypeName.canonicalName());
              try (PrintWriter writer = new PrintWriter(builderFile.openWriter())) {
                JavaFile.builder(typename.packageName(), builder.build()).build().writeTo(writer);
              }
            }
            case RECORD -> {
              List<Element> fieldElements = RecordSerde.getAllFieldElements(clazz);
              MethodSpec deSer = RecordSerde.deSerializerCode(typename, fieldElements);
              MethodSpec serde = RecordSerde.serializerCode(typename, fieldElements);
              constructor(builder)
                  .addMethod(deSer)
                  .addMethod(serde)
              ;

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

    public static MethodSpec deSerializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("readObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .returns(typeName)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME);

      builder.addCode("return new $T(\n", typeName);
      Iterator<Element> elementIterator = fieldElements.iterator();
      while (elementIterator.hasNext()) {
        Element e = elementIterator.next();
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addCode("$L.readBoolean()", BUF_VAR_NAME);
          case BYTE -> builder.addCode("$L.readByte()", BUF_VAR_NAME);
          case SHORT -> builder.addCode("$L.readShort()", BUF_VAR_NAME);
          case CHAR -> builder.addCode("$L.readChar()", BUF_VAR_NAME);
          case FLOAT -> builder.addCode("$L.readFloat()", BUF_VAR_NAME);
          case DOUBLE -> builder.addCode("$T $L = $L.readDouble()", BUF_VAR_NAME);
          case INT -> builder.addCode("$L.readVarInt32($L)",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          case LONG -> builder.addCode("$L.readVarInt64($L)",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          default -> builder.addCode("$L.readObject($L)",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME
          );
        }

        if (elementIterator.hasNext()) {
          builder.addCode(",");
        }
        builder.addCode("\n");
      }
      builder.addCode(");");
      return builder.build();
    }

    public static MethodSpec serializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("writeObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addParameter(typeName, OBJECT_VAR_NAME, Modifier.FINAL)
          .returns(TypeName.VOID);

      fieldElements.forEach(e -> {
        String fieldName = e.getSimpleName().toString();
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addStatement("$L.writeBoolean($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case BYTE -> builder.addStatement("$L.writeByte($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case SHORT -> builder.addStatement("$L.writeShort($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case CHAR -> builder.addStatement("$L.writeChar($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case FLOAT -> builder.addStatement("$L.writeFloat($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case DOUBLE -> builder.addStatement("$L.writeDouble($L.$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case INT -> builder.addStatement("$L.writeVarInt32($L, $L.$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case LONG -> builder.addStatement("$L.writeVarInt64($L, $L.$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          default -> builder.addStatement("$L.writeObject($L, $L.$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
        }
      });

      return builder.build();
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
            .filter(e -> e.getKind() == ElementKind.FIELD).filter(
                e -> !(e.getModifiers().contains(Modifier.FINAL) || e.getModifiers()
                    .contains(Modifier.STATIC) || e.getModifiers().contains(Modifier.TRANSIENT)))
            .collect(Collectors.toUnmodifiableList());

        fields.addAll(fieldElements);
      }

      return fields;
    }

    public static MethodSpec deSerializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("readObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addStatement("$T $L = new $T()", typeName, OBJECT_VAR_NAME, typeName).returns(typeName);

      fieldElements.forEach(e -> {
        String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addStatement("$L.set$L($L.readBoolean())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case BYTE -> builder.addStatement("$L.set$L($L.readByte())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case SHORT -> builder.addStatement("$L.set$L($L.readShort())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case CHAR -> builder.addStatement("$L.set$L($L.readChar())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case FLOAT -> builder.addStatement("$L.set$L($L.readFloat())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case DOUBLE -> builder.addStatement("$L.set$L($L.readDouble())",
              OBJECT_VAR_NAME,
              fieldName,
              BUF_VAR_NAME);
          case INT -> builder.addStatement("$L.set$L($L.readVarInt32($L))",
              OBJECT_VAR_NAME,
              fieldName,
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          case LONG -> builder.addStatement("$L.set$L($L.readVarInt64($L))",
              OBJECT_VAR_NAME,
              fieldName,
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME);
          default -> builder.addStatement("$L.set$L($L.readObject(buf))",
              OBJECT_VAR_NAME,
              fieldName,
              SERIALIZER_VAR_NAME);
        }
      });

      builder.addStatement("return object");
      return builder.build();
    }

    public static MethodSpec serializerCode(TypeName typeName, List<Element> fieldElements) {
      MethodSpec.Builder builder = MethodSpec.methodBuilder("writeObject")
          .addAnnotation(Override.class)
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addParameter(Serdes.class, SERIALIZER_VAR_NAME)
          .addParameter(ByteBuf.class, BUF_VAR_NAME, Modifier.FINAL)
          .addParameter(typeName, OBJECT_VAR_NAME, Modifier.FINAL)
          .returns(TypeName.VOID);

      fieldElements.forEach(e -> {
        String fieldName = StringUtils.capitalize(e.getSimpleName().toString());
        switch (e.asType().getKind()) {
          case BOOLEAN -> builder.addStatement("$L.writeBoolean($L.is$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case BYTE -> builder.addStatement("$L.writeByte($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case SHORT -> builder.addStatement("$L.writeShort($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case CHAR -> builder.addStatement("$L.writeChar($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case FLOAT -> builder.addStatement("$L.writeFloat($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case DOUBLE -> builder.addStatement("$L.writeDouble($L.get$L())",
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case INT -> builder.addStatement("$L.writeVarInt32($L, $L.get$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          case LONG -> builder.addStatement("$L.writeVarInt64($L, $L.get$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
          default -> builder.addStatement("$L.writeObject($L, $L.get$L())",
              SERIALIZER_VAR_NAME,
              BUF_VAR_NAME,
              OBJECT_VAR_NAME,
              fieldName);
        }
      });

      return builder.build();
    }

  }


}