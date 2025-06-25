package org.example.serde;

import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;


public class RecordSerializer implements Serializer<Object> {

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

  public RecordSerializer(Class<?> clazz) {
    this.clazz = clazz;
    register(clazz);
  }

  @Override
  public Object deserialize(Serdes serializer, ByteBuf buf) {
    Object[] args = null;
    if (0 < fields.length) {
      args = new Object[fields.length];
      for (int i = 0; i < fields.length; i++) {
        FieldInfo field = fields[i];
        try {
          args[i] = serializer.deserialize(buf);
        } catch (Throwable e) {
          throw new RuntimeException(
              String.format("反序列化:%s, 字段:%s 错误", clazz, field.name()), e);
        }
      }
    }
    Object o;
    try {
      if (args != null) {
        o = constructor.invokeWithArguments(args);
      } else {
        o = constructor.invoke();
      }
    } catch (Throwable e) {
      throw new RuntimeException("类型:" + clazz + ",创建失败", e);
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

  /**
   * 注册所有字段信息(排除静态，final和transient字段)
   *
   * @since 2021年07月18日 11:09:50
   */
  @SuppressWarnings("unchecked")
  public void register(Class<?> clazz) {
    MethodHandles.Lookup lookup = MethodHandles.lookup();

    RecordComponent[] components = clazz.getRecordComponents();
    Class<?>[] types = new Class[components.length];
    FieldInfo[] fieldInfos = new FieldInfo[components.length];
    for (int i = 0; i < components.length; i++) {
      RecordComponent component = components[i];
      types[i] = component.getType();

      try {
        fieldInfos[i] = new FieldInfo(
            lookup.unreflect(component.getAccessor()), component.getName());
      } catch (Exception e) {
        throw new RuntimeException(
            String.format("类型:%s, 字段：%s %s, 创建getter和setter失败", clazz.getName(),
                component.getType(), component.getName()), e);
      }
    }
    try {
      Constructor<?> temp = clazz.getDeclaredConstructor(types);
      temp.setAccessible(true);
      constructor = lookup.unreflectConstructor(temp);
    } catch (Exception e) {
      throw new RuntimeException("类型:" + clazz + ",缺少无参构造方法");
    }
    this.fields = fieldInfos;
  }


  record FieldInfo(MethodHandle getter, String name) {

  }
}
