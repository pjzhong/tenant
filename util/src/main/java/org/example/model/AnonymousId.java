package org.example.model;

import org.example.util.Identity;

public record AnonymousId(Object o) implements Identity {

  public static AnonymousId anonymousId() {
    return new AnonymousId(new Object());
  }

  public static AnonymousId anonymousId(Object o) {
    return new AnonymousId(o);
  }
}
