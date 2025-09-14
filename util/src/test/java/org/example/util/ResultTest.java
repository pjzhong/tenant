package org.example.util;

import static org.example.util.Result.Err;
import static org.example.util.Result.Ok;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResultTest {


  @Test
  public void okAndErr() {
    Result<Boolean, Null> ok = Ok(true);
    switch (ok) {
      case Ok(Boolean a) -> Assertions.assertTrue(a);
      case Err(Null ignore) -> throw new IllegalArgumentException();
    }

    Result<Null, Boolean> err = Err(false);
    switch (err) {
      case Ok(Null ignore) -> throw new IllegalArgumentException();
      case Err(Boolean a) -> Assertions.assertFalse(a);
    }
  }
}
