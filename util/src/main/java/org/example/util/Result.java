package org.example.util;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.ObjectUtils.Null;

public sealed interface Result<V, E> permits Ok, Err {

  default boolean isOk() {
    return switch (this) {
      case Ok<V, E> ignore -> true;
      case Err<V, E> ignore -> false;
    };

  }

  static <V, E> Ok<V, E> Ok(V result) {
    return new Ok<>(result);
  }

  static <E> Ok<Null, E> Ok() {
    return new Ok<>(ObjectUtils.NULL);
  }

  static <V, E> Err<V, E> Err(E result) {
    return new Err<>(result);
  }

  static <V> Err<V, Null> Err() {
    return new Err<>(ObjectUtils.NULL);
  }


  default boolean isErr() {
    return !isOk();
  }

}
