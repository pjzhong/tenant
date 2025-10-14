package org.example.common.model;

import java.util.Objects;
import org.example.serde.Serde;

@Serde
public class CommonRes<T> {

  /** 是否成功 */
  private boolean suc;
  /** 结果 */
  private T res;

  public CommonRes() {
  }

  public CommonRes(boolean suc, T res) {
    this.suc = suc;
    this.res = res;
  }

  public boolean isSuc() {
    return suc;
  }

  public CommonRes<T> setSuc(boolean suc) {
    this.suc = suc;
    return this;
  }

  public T getRes() {
    return res;
  }

  public CommonRes<T> setRes(T res) {
    this.res = res;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CommonRes<?> commonRes)) {
      return false;
    }
    return suc == commonRes.suc && Objects.equals(res, commonRes.res);
  }

  @Override
  public int hashCode() {
    return Objects.hash(suc, res);
  }
}
