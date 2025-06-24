package org.example.net.anno.processor;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.apache.commons.lang3.StringUtils;

/**
 * Hanlder构建信息
 *
 * @author zhongjianping
 * @since 2024/12/4 12:17
 */
class TypeSpecInfo {

  public final TypeElement typeElement;
  /** Hanlder构建者 */
  public final TypeSpec.Builder builder;
  /** 请求方法 */
  public final List<ExecutableElement> methods;
  /** 构造器 */
  public final MethodSpec.Builder constructor;

  /** 执行器代码块 */
  public Function<ExecutableElement, CodeBlock> executor;

  TypeSpecInfo(TypeElement typeElement, TypeSpec.Builder builder,
      List<ExecutableElement> methods) {
    this.typeElement = typeElement;
    this.builder = builder;
    this.methods = new ArrayList<>(methods);
    constructor = MethodSpec.constructorBuilder();
  }

  public String fieldName(ClassName typeName) {
    return StringUtils.uncapitalize(typeName.simpleName());
  }
}
