package org.example.net.handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.net.ReqMethod;
import org.example.net.proxy.ReqUtil;
import org.example.util.Pair;

/**
 * 根据协议号int,提供获取和注册处理者的方法。
 *
 * 获取之后如何验证Handler是否合法，交由业务决定。此类只提供基础的基础和获取。调用方式则交给业务决定
 *
 * @author ZJP
 * @since 2021年07月22日 22:06:22
 **/
public class HandlerRegistry {

  /** 协议编号 -> 请求处理者 */
  private final Map<Integer, Handler> handles;

  public HandlerRegistry() {
    handles = new ConcurrentHashMap<>();
  }

  /**
   * 寻找被{@link ReqMethod}标记的非静态公共方法
   *
   * @param object 需要被注册的对象
   * @since 2021年07月24日 10:04:05
   */
  public List<Handler> findHandler(Object object) {
    List<Handler> res = new ArrayList<>();
    Class<?> clazz = object.getClass();

    {
      List<Pair<Integer, Method>> methods = ReqUtil.calcFacadeMethods(clazz);
      for (Pair<Integer, Method> pair : methods) {
        Integer req = pair.first();
        Method method = pair.second();

        Handler handler = Handler.of(object, method, req);
        res.add(handler);
      }
    }

    {
      Class<?>[] interfaces = clazz.getInterfaces();
      for (Class<?> inter : interfaces) {
        List<Pair<Integer, Method>> interMethods = ReqUtil.calcModuleMethods(inter);
        for (Pair<Integer, Method> pair : interMethods) {
          Integer req = pair.first();
          Method method = pair.second();

          Handler handler = Handler.of(object, method, req);
          res.add(handler);
        }
      }
    }

    return res;
  }

  /**
   * 根据{@link Handler#reqId()}获取协议ID然后进行注册，协议ID不允许重复
   *
   * @param hs 需要注册处理者
   * @since 2021年07月24日 10:25:05
   */
  public void registeHandlers(List<Handler> hs) {
    for (Handler h : hs) {
      final Handler old = handles.get(h.reqId());
      if (old != null) {
        throw new RuntimeException(String
            .format("协议ID:【%s】,%s和%s发生重读", h.reqId(), h.method().getName(), h.method().getName()));
      }

      handles.put(h.reqId(), h);
    }
  }


  /**
   * 根据协议编号，获取处理者
   *
   * @param proto 协议编号
   * @since 2021年07月22日 23:35:25
   */
  public Handler getHandler(int proto) {
    return handles.get(proto);
  }
}