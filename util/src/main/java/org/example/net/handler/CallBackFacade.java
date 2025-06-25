package org.example.net.handler;

import com.google.auto.service.AutoService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.CompletableFuture;
import org.example.net.Connection;
import org.example.net.ConnectionManager;
import org.example.net.DefaultDispatcher;
import org.example.net.HandlerRegister;
import org.example.net.Message;
import org.example.serde.Serdes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 回调处理实现
 *
 * @author zhongjianping
 * @since 2025/5/15 21:49
 */
@AutoService(HandlerRegister.class)
public class CallBackFacade implements Handler, HandlerRegister {

  static final Logger logger = LoggerFactory.getLogger(CallBackFacade.class);

  private final Serdes serializer;
  private final ConnectionManager manager;

  public CallBackFacade(ConnectionManager manager, Serdes serializer) {
    this.serializer = serializer;
    this.manager = manager;
  }

  void callback(Connection c, Message m) throws Exception {
    final int msgId = serializer.readVarInt32(m.packet());
    final CompletableFuture<?> futureVar = manager.removeInvokeFuture(msgId);
    if (futureVar == null) {
      logger.error(
          "寻找回调函数失败, 可能原因：【回调函数过期/回调函数不存在】, 消息ID:{}, 链接地址:{} ",
          msgId,
          c.channel().remoteAddress());
      return;
    }
    ByteBuf buf = Unpooled.wrappedBuffer(m.packet());
    futureVar.complete(serializer.deserialize(buf));
  }

  public int callBackId() {
    //回调ID设置0
    return 0;
  }

  @Override
  public void invoke(Connection c, Message m) throws Exception {
    callback(c, m);
  }

  @Override
  public void register(DefaultDispatcher defaultDispatcher) {
    defaultDispatcher.registeHandler(callBackId(), this);
  }
}