package org.example.common.model;

import java.util.Objects;
import org.example.serde.Serde;

@Serde
public class ComposeObject {

  private AvatarId id;
  private ReqMove move;
  private CommonRes<String> res;

  public AvatarId getId() {
    return id;
  }

  public void setId(AvatarId id) {
    this.id = id;
  }

  public ReqMove getMove() {
    return move;
  }

  public void setMove(ReqMove move) {
    this.move = move;
  }

  public CommonRes<String> getRes() {
    return res;
  }

  public void setRes(CommonRes<String> res) {
    this.res = res;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ComposeObject that)) {
      return false;
    }
    return Objects.equals(id, that.id) && Objects.equals(move, that.move)
        && Objects.equals(res, that.res);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, move, res);
  }
}
