package com.moon.netty.rpc.transport.client;

import com.moon.netty.rpc.registery.ServiceDiscovery;
import io.netty.util.concurrent.Promise;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mzx
 */
public class RpcRequestFactory {
    /**
     * 用来异步处理RPC请求响应的Promise集合， 单例的
     * TODO 换成CompleteFuture
     */
    public static final Map<Integer, Promise<Object>> UNPROCESSED_RPC_REQUEST_PROMISES;
    /**
     * 保存各个请求的超时时间
     * messageId -> expireTime
     */
    public static final Map<Integer, Date> UNPROCESSED_RPC_REQUEST_TIMEOUT;

    static {
        UNPROCESSED_RPC_REQUEST_PROMISES = new ConcurrentHashMap<>();
        UNPROCESSED_RPC_REQUEST_TIMEOUT = new ConcurrentHashMap<>();
    }
}
