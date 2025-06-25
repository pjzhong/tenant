package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用对象序列化实现
 *
 * <p>2.字段类型也需要注册进{@link Serdes}, 顺序无关</p>
 * <p>3.因为接口和抽象类的存在，无法确定具体类型，所以不提供自动注册</p>
 * <p>
 * 与{@link Serdes} 组合使用,本体功能并不完整
 *
 * @since 2021年07月17日 16:16:14
 **/
public class ObjectSerializer implements Serializer<Object> {

  private static final FieldInfo[] EMPTY_FILE_INFO = new FieldInfo[0];

  /**
   * 目标类型
   */
  private Class<?> clazz;
  /**
   * 默认无参构造
   */
  private MethodHandle constructor;
  /**
   * 字段信息
   */
  private FieldInfo[] fields;

  public ObjectSerializer(Class<?> clazz) {
    this.clazz = clazz;
    register(clazz);
  }

  /**
   * 接口，抽象类，标记，无法进行注册
   * <p>
   * 必须提供无参构造方法
   *
   * @param clazz 注册类型
   * @since 2021年07月18日 11:02:39
   */
  public static void checkClass(Class<?> clazz) {
    if (clazz.isInterface() || clazz.isAnnotation() || clazz.isPrimitive() || Modifier.isAbstract(
        clazz.getModifiers())) {
      throw new RuntimeException("类型:" + clazz + ",无法序列化");
    }

    try {
      clazz.getDeclaredConstructor();
    } catch (Exception e) {
      throw new RuntimeException("类型:" + clazz + ",缺少无参构造方法");
    }
  }

  /**
   * 注册所有字段信息(排除静态，final和transient字段)
   *
   * @since 2021年07月18日 11:09:50
   */
  public void register(Class<?> clazz) {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      Constructor<?> temp = clazz.getDeclaredConstructor();
      temp.setAccessible(true);
      constructor = lookup.unreflectConstructor(temp);
    } catch (Exception e) {
      throw new RuntimeException("类型:" + clazz + ",缺少无参构造方法");
    }

    //所有字段(Getter, Setter)
    List<FieldInfo> fields = new ArrayList<>();

    for (Class<?> cls = clazz; cls != Object.class; cls = cls.getSuperclass()) {
      for (Field f : cls.getDeclaredFields()) {
        int modifier = f.getModifiers();
        if (Modifier.isStatic(modifier) || Modifier.isFinal(modifier) || Modifier.isTransient(
            modifier)) {
          continue;
        }

        f.setAccessible(true);
        try {
          FieldInfo fieldInfo = new FieldInfo(
              lookup.unreflectSetter(f), lookup.unreflectGetter(f), f.getName());
          fields.add(fieldInfo);
        } catch (Exception e) {
          throw new RuntimeException(
              String.format("类型:%s.%s, 创建getter和setter失败", clazz.getName(), f.getName()));
        }
      }
    }

    this.fields = fields.toArray(EMPTY_FILE_INFO);
  }

  @Override
  public Object deserialize(Serdes serializer, ByteBuf buf) {
    Object o;
    try {
      o = constructor.invoke();
    } catch (Throwable e) {
      throw new RuntimeException("类型:" + clazz + ",创建失败", e);
    }

    for (FieldInfo field : fields) {
      try {
        Object value = serializer.deserialize(buf);
        MethodHandle setter = field.setter();
        setter.invoke(o, value);
      } catch (Throwable e) {
        throw new RuntimeException(String.format("反序列化:%s, 字段:%s 错误", clazz, field.name()),
            e);
      }
    }
    return o;
  }

  @Override
  public void serialize(Serdes serializer, ByteBuf buf, Object object) {
    for (FieldInfo field : fields) {
      try {
        Object value = field.getter().invoke(object);
        serializer.serialize(buf, value);
      } catch (Throwable e) {
        throw new RuntimeException(String.format("序列化:%s, 字段:%s 错误", clazz, field.name()),
            e);
      }
    }
  }

  record FieldInfo(MethodHandle setter, MethodHandle getter,
                   String name) {

  }
}
