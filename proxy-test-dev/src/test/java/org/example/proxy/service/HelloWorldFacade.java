package org.example.proxy.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.example.net.anno.ReqMethod;
import org.example.net.anno.RespMethod;
import org.example.net.anno.RpcModule;

@RpcModule
public class HelloWorldFacade {

  public AtomicInteger integer = new AtomicInteger();

  public static final int echo = 1;
  public static final int callBackArgs = 2;
  public static final int callBackArray = 3;

  @ReqMethod(echo)
  public Object echo(Object o) {
    integer.incrementAndGet();
    return o;
  }

  @RespMethod(echo)
  public void echoRes(Object o) {
    integer.incrementAndGet();
  }

  @ReqMethod
  public void doNothing() {
    integer.incrementAndGet();
  }

}
