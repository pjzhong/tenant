/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.net;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server Idle handler.
 *
 * In the server side, the connection will be closed if it is idle for a certain period of time.
 *
 * @author jiangping
 * @version $Id: ServerIdleHandler.java, v 0.1 Nov 3, 2015 05:23:19 PM tao Exp $
 */
@Sharable
public class ServerIdleHandler extends ChannelDuplexHandler {

  public static final Integer IDLE_TIME = 90000;

  private static final Logger logger = LoggerFactory.getLogger(ServerIdleHandler.class);

  /**
   * @see io.netty.channel.ChannelInboundHandlerAdapter#userEventTriggered(ChannelHandlerContext,
   * Object)
   */
  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      try {
        logger.warn("Connection idle, close it from server side: {}",
            ctx.channel().remoteAddress());
        ctx.close();
      } catch (Exception e) {
        logger.warn("Exception caught when closing connection in ServerIdleHandler.", e);
      }
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }
}