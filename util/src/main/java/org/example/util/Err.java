package org.example.util;

public record Err<V, E>(E error) implements Result<V, E> {

}
