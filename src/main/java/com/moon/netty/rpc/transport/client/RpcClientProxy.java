package com.moon.netty.rpc.transport.client;

import com.moon.netty.rpc.message.RpcRequestMessage;
import com.moon.netty.rpc.protocol.SequenceIdGenerator;
import io.netty.util.concurrent.DefaultPromise;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;

/**
 * 代理客户端，将方法调用封装成RPC请求，隐藏RPC通信的细节。让使用者觉得就像在本地调用一样
 *
 * @Author: Mzx
 * @Date: 2022/6/11 21:27
 */
@Slf4j
public class RpcClientProxy {
    private RpcClient rpcClient;

    public RpcClientProxy(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    /**
     * 获取远程服务的代理对象
     *
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked") // 抑制unchecked警告
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                log.debug("调用方法: {}#{}", method.getDeclaringClass().getName(), method.getName());
                long timeout = 1000;
                // 1. 将方法调用转换为 消息对象
                int sequenceId = SequenceIdGenerator.nextId();
                RpcRequestMessage msg = new RpcRequestMessage(
                        sequenceId,
                        timeout,
                        clazz.getName(),
                        method.getName(),
                        method.getReturnType(),
                        method.getParameterTypes(),
                        args
                );
                // 2. 准备一个空 Promise 对象，来接收结果 存入集合, 指定一个线程来执行监听器代码
                DefaultPromise<Object> promise = new DefaultPromise<Object>(ChannelProvider.eventLoopGroup.next());
                RpcRequestFactory.UNPROCESSED_RPC_REQUEST_PROMISES.put(sequenceId, promise);
                RpcRequestFactory.UNPROCESSED_RPC_REQUEST_TIMEOUT.put(sequenceId, new Date(System.currentTimeMillis() + timeout));
                // 3. 进行网络通信，发起RPC调用，将消息对象发送出去
                rpcClient.sendRpcRequest(msg);
                // 4. 阻塞等待 promise 结果
                promise.await();
                if (promise.isSuccess()) {
                    // 调用正常
                    return promise.getNow();
                } else {
                    // 调用失败
                    throw new RuntimeException(promise.cause());
                }
            }
        });
    }
}
