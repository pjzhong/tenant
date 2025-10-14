package org.example.common.model;

import java.util.Objects;
import org.example.serde.Serde;

/**
 * 移动结果
 *
 * @author ZJP
 * @since 2021年09月27日 15:26:14
 **/
@Serde
public final class ResMove {

  /** 对象ID */
  private long id;
  /** x轴 */
  private float x;
  /** y轴 */
  private float y;
  /** 方向 */
  private int dir;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public float getX() {
    return x;
  }

  public void setX(float x) {
    this.x = x;
  }

  public float getY() {
    return y;
  }

  public void setY(float y) {
    this.y = y;
  }

  public int getDir() {
    return dir;
  }

  public void setDir(int dir) {
    this.dir = dir;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResMove resMove = (ResMove) o;
    return getId() == resMove.getId() && Float.compare(getX(), resMove.getX()) == 0
        && Float.compare(getY(), resMove.getY()) == 0 && getDir() == resMove.getDir();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getX(), getY(), getDir());
  }

  @Override
  public String toString() {
    return "ResMove{" +
        "id=" + id +
        ", x=" + x +
        ", y=" + y +
        ", dir=" + dir +
        '}';
  }
}
