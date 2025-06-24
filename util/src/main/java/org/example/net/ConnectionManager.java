package org.example.net;

import static org.example.model.AnonymousId.anonymousId;

import io.netty.channel.Channel;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.util.Identity;

/**
 * 链接管理者
 * <p>
 *
 * @author ZJP
 * @since 2021年08月15日 22:34:55
 **/
public class ConnectionManager implements AutoCloseable {

  private final ConcurrentHashMap<Identity, Connection> connections;

  private final AtomicInteger callBackMsgId;
  /** callBack future */
  private final ConcurrentHashMap<Integer, CompletableFuture<?>> invokeFutures = new ConcurrentHashMap<>();

  public ConnectionManager() {
    connections = new ConcurrentHashMap<>();
    callBackMsgId = new AtomicInteger();
  }

  public void anoymousChannel(Channel channel) {
    Connection connection = Connection.newConnection(
        anonymousId(channel.id()), channel);
    connections.put(connection.id(), connection);
  }

  public void channelInactive(Channel channel) {
    Connection connection = channel.attr(Connection.CONNECTION).getAndSet(null);
    if (connection != null) {
      connections.remove(connection.id());
    }
  }

  public Connection bindChannel(Identity id, Channel channel) {
    Connection oldConnection = channel.attr(Connection.CONNECTION).getAndSet(null);
    if (oldConnection != null) {
      connections.remove(oldConnection.id());
    }

    Connection connection = Connection.newConnection(id, channel);
    connections.put(connection.id(), connection);
    return connection;
  }


  public void removeConnection(Identity identity) {
    connections.remove(identity);
  }

  public Collection<Connection> connections() {
    return connections.values();
  }

  public Connection connection(Identity address) {
    return connections.get(address);
  }

  public <T> void addInvokeFuture(Integer id,
      CompletableFuture<T> future) {
    invokeFutures.putIfAbsent(id, future);
  }

  public int nextCallBackMsgId() {
    return callBackMsgId.incrementAndGet();
  }

  @SuppressWarnings("unchecked")
  public <T> CompletableFuture<T> removeInvokeFuture(int msgId) {
    return (CompletableFuture<T>) invokeFutures.remove(msgId);
  }

  public <T> boolean removeInvokeFuture(int msgId, CompletableFuture<T> future) {
    return invokeFutures.remove(msgId, future);
  }

  @Override
  public void close() {
    connections.values().removeIf(c -> {
      c.close();
      return true;
    });

    invokeFutures.values().removeIf(f -> {
      f.completeExceptionally(new IllegalStateException("Connection is close"));
      return true;
    });

  }
}
