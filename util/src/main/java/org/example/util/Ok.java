package org.example.util;

import org.apache.commons.lang3.ObjectUtils;

public record Ok<V, E>(V result) implements Result<V, E> {

  public static <E> Ok<ObjectUtils.Null, E> Ok() {
    return new Ok<>(ObjectUtils.NULL);
  }

  public static <V, E> Ok<V, E> Ok(V result) {
    return new Ok<>(result);
  }

}
