package org.example.net.proxy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.common.ThreadCommonResource;
import org.example.net.DefaultDispatcher;
import org.example.net.HelloWorld;
import org.example.net.InvokeFuture;
import org.example.net.Message;
import org.example.net.MessageStatus;
import org.example.net.ReqMethod;
import org.example.net.RespMethod;
import org.example.net.ResultInvokeFuture;
import org.example.net.RpcModule;
import org.example.net.client.DefaultClient;
import org.example.net.handler.HandlerRegistry;
import org.example.net.server.DefaultServer;
import org.example.serde.CommonSerializer;
import org.example.serde.ObjectSerializer;
import org.example.serde.Serializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReqCliProxyTest {

  private static ThreadCommonResource resource;

  /** 服务端消息处理 */
  private SerHelloWorldFacade serFacade;
  /**
   * 客户端消息处理
   */
  private CliHelloWorldFacade cliFacade;
  /**
   * 服务端
   */
  private DefaultServer rpcServer;
  /**
   * 客户端
   */
  private DefaultClient rpcClient;
  /**
   * 请求代理
   */
  private ReqCliProxy proxy;
  /** 服务端地址 */
  private Integer id;

  private final int invokeTimes = 5;


  @BeforeAll
  public static void beforeAll() {
    resource = new ThreadCommonResource();
  }

  @AfterAll
  public static void afterAll() {
    if (resource != null) {
      resource.close();
    }
  }

  @BeforeEach
  public void beforeEach() throws Exception {
    Serializer<Object> serializer = createSerializer();

    rpcServer = new DefaultServer();
    serFacade = new SerHelloWorldFacade();
    HandlerRegistry serverRegistry = new HandlerRegistry();
    serverRegistry.registerHandlers(serFacade);
    rpcServer.handler(new DefaultDispatcher(serverRegistry));
    rpcServer.serializer(serializer);
    rpcServer.start(resource);

    rpcClient = new DefaultClient();
    cliFacade = new CliHelloWorldFacade();
    HandlerRegistry clientRegistry = new HandlerRegistry();
    clientRegistry.registerHandlers(cliFacade);
    rpcClient.handler(new DefaultDispatcher(clientRegistry));
    rpcClient.serializer(serializer);
    rpcClient.init(resource.getBoss());
    id = rpcClient.connection(rpcServer.ip(), rpcServer.port()).id();

    //链接的创建和管理交给client，proxy不要管，直接用就行了
    proxy = new ReqCliProxy(rpcClient.manager());
  }

  private CommonSerializer createSerializer() {
    CommonSerializer serializer = new CommonSerializer();
    serializer.registerSerializer(10, Object.class, new ObjectSerializer(Object.class, serializer));
    serializer.registerSerializer(11, Message.class);
    return serializer;
  }

  @AfterEach
  public void close() {
    if (rpcServer != null) {
      rpcServer.close();
    }

    if (rpcClient != null) {
      rpcClient.close();
    }
  }

  @Test
  public void doNothingTest() throws Exception {
    HelloWorld world = proxy.getProxy(id, HelloWorld.class);
    for (int i = 0; i < invokeTimes; i++) {
      world.doNothing();
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void echoTest() throws Exception {
    HelloWorld world = proxy.getProxy(id, HelloWorld.class);
    for (int i = 0; i < invokeTimes; i++) {
      world.echo("hi");
    }

    TimeUnit.MILLISECONDS.sleep(100);
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
    Assertions.assertEquals(invokeTimes, cliFacade.integer.get());
  }

  @Test
  public void callBackArgsTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(id, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    long answer = 2012;
    for (int i = 0; i < invokeTimes; i++) {
      req.callBackArgs("Hi", i, answer).onSuc(l -> {
        Assertions.assertEquals(answer, l);
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void callBackArgsMessageTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(id, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    long answer = 2012;
    for (int i = 0; i < invokeTimes; i++) {
      req.callBackArgs("Hi", i, answer).onSuc(msg -> {
        Assertions.assertEquals(answer, msg);
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void callBackArrayMessageTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(id, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    long[] longs = {1, 2, 3, 4, 5, 6};
    for (int i = 0; i < invokeTimes; i++) {
      req.callBackArray(longs).onSuc(msg -> {
        Assertions.assertArrayEquals(longs, msg);
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @Test
  public void calErrMessageTest() throws InterruptedException {
    CallBackReq req = proxy.getProxy(id, CallBackReq.class);
    CountDownLatch latch = new CountDownLatch(invokeTimes);
    for (int i = 0; i < invokeTimes; i++) {
      req.errMsg().onErr(msg -> {
        Assertions.assertTrue(msg.isErr());
        Assertions.assertEquals(MessageStatus.SERVER_EXCEPTION.status(), msg.status());
        latch.countDown();
      }).invoke();
    }

    Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    Assertions.assertEquals(invokeTimes, serFacade.integer.get());
  }

  @RpcModule
  interface CallBackReq {

    @ReqMethod
    InvokeFuture<Long> callBackArgs(String str, Integer i, Long a);

    @ReqMethod
    InvokeFuture<long[]> callBackArray(long[] longs);

    @ReqMethod
    InvokeFuture<Message> errMsg();
  }

  /**
   * 世界你好，门面
   *
   * @author ZJP
   * @since 2021年07月22日 21:58:02
   **/
  @RpcModule
  private static class SerHelloWorldFacade implements HelloWorld, CallBackReq {

    public AtomicInteger integer = new AtomicInteger();

    @Override
    public Object echo(Object o) {
      integer.incrementAndGet();
      return o;
    }

    @Override
    public void doNothing() {
      integer.incrementAndGet();
    }

    @Override
    public InvokeFuture<Long> callBackArgs(String str, Integer i, Long a) {
      integer.incrementAndGet();
      return ResultInvokeFuture.withResult(a);
    }

    @Override
    public InvokeFuture<long[]> callBackArray(long[] longs) {
      integer.incrementAndGet();
      return ResultInvokeFuture.withResult(longs);
    }

    @Override
    public InvokeFuture<Message> errMsg() {
      integer.incrementAndGet();
      Message message = Message.of().status(MessageStatus.SERVER_EXCEPTION);
      return ResultInvokeFuture.withResult(message);
    }
  }

  @RpcModule
  private static class CliHelloWorldFacade {

    public AtomicInteger integer = new AtomicInteger();

    @RespMethod(HelloWorld.ECHO)
    public void echoRes(Object o) {
      integer.incrementAndGet();
    }
  }

}
