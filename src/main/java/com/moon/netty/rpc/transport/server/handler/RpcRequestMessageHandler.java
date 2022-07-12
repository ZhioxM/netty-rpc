package com.moon.netty.rpc.transport.server.handler;


import com.moon.netty.rpc.message.RpcRequestMessage;
import com.moon.netty.rpc.message.RpcResponseMessage;
import com.moon.netty.rpc.provider.ServiceProvider;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

@Slf4j
@ChannelHandler.Sharable
public class RpcRequestMessageHandler extends SimpleChannelInboundHandler<RpcRequestMessage> {

    /**
     * 通过反射调用RPC请求的方法
     *
     * @param ctx
     * @param message
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestMessage message) {
        RpcResponseMessage response = new RpcResponseMessage();
        response.setSequenceId(message.getSequenceId());
        try {
            // 通过服务名称从本地工厂获取本地注解了@RpcSerice的实例对象
            Object service = ServiceProvider.getService(message.getInterfaceName());
            // 获取调用的方法
            Method method = service.getClass().getMethod(message.getMethodName(), message.getParameterTypes());

            // 调用方法得到返回值
            Object invoke = method.invoke(service, message.getParameterValue());
            // 将方法返回值放入RPC响应消息
            response.setReturnValue(invoke);
        } catch (Exception e) {
            e.printStackTrace();
            // 这个异常信息态长了，超出了长度字段解码器设置的1024的最大限制，就会在帧解码器那里报错了，所以我们不需要返回那么长的错误信息
            // response.setExceptionValue(e);
            response.setExceptionValue(new Exception("远程调用出错:" + e.getCause().getMessage()));
        } finally {
            ctx.writeAndFlush(response); // 将响应对象返回给客户端，message对象在OutBound时会被编解码器编码成byteBuf
            ReferenceCountUtil.release(message); // 释放ByteBuf
        }

    }

    // 测试的代码
    /*
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        RpcRequestMessage message = new RpcRequestMessage(
                1,
                "com.moon.netty.rpc.server.service.HelloService",
                "sayHello",
                String.class,
                new Class[]{String.class},
                new Object[]{"张三"}
        );
        HelloService service = (HelloService)
                ServicesFactory.selectService(Class.forName(message.getInterfaceName()));
        Method method = service.getClass().getMethod(message.getMethodName(), message.getParameterTypes());
        Object invoke = method.invoke(service, message.getParameterValue());
        System.out.println(invoke);
    }
    */
}
