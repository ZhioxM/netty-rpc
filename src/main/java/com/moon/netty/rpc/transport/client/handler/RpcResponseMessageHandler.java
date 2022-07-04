package com.moon.netty.rpc.transport.client.handler;

import com.moon.netty.rpc.message.RpcResponseMessage;
import com.moon.netty.rpc.transport.client.RpcClient;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class RpcResponseMessageHandler extends SimpleChannelInboundHandler<RpcResponseMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponseMessage msg) throws Exception {
        log.debug("{}", msg); // 当前线程是某个EventLoop
        // 拿到当前消息的promise（并从MAP中删除, 因为它处理完成了）
        Promise<Object> promise = RpcClient.UNPROCESSED_RPC_REQUEST_PROMISES.remove(msg.getSequenceId());
        if (promise != null) {
            Object returnValue = msg.getReturnValue();
            Exception exceptionValue = msg.getExceptionValue();
            if (exceptionValue != null) {
                promise.setFailure(exceptionValue);
            } else {
                promise.setSuccess(returnValue);
            }
        }
    }
}
