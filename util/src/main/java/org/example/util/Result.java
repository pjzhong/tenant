package org.example.util;

public sealed interface Result<V, E> permits Ok, Err {

  default boolean isOk() {
    return switch (this) {
      case Ok<V, E> ignore -> true;
      case Err<V, E> ignore -> false;
    };

  }

  default boolean isErr() {
    return !isOk();
  }

}
