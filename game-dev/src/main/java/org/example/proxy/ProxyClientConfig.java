package org.example.proxy;

/**
 * @author zhongjianping
 * @since 2022/12/14 23:26
 */
public class ProxyClientConfig {

  /** ID */
  private int id;
  /** 目标地址 */
  private String address;
  /** 端口 */
  private int port;


  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
