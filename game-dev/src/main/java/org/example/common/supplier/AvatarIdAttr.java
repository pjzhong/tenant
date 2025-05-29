package org.example.common.supplier;

import io.netty.util.AttributeKey;
import org.example.common.model.AvatarId;

public final class AvatarIdAttr {

  public static final AttributeKey<AvatarId> KEY = AttributeKey.valueOf("AvatarId");
}
