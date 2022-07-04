package com.moon.netty.rpc.transport.client;

import com.alibaba.nacos.api.exception.NacosException;
import com.moon.netty.rpc.loadBalancer.impl.RoundRobinRule;
import com.moon.netty.rpc.message.RpcRequestMessage;
import com.moon.netty.rpc.registery.ServiceDiscovery;
import com.moon.netty.rpc.registery.impl.NacosServiceDiscovery;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcClient {
    public static final Map<Integer, Promise<Object>> UNPROCESSED_RPC_REQUEST_PROMISES; // 用来异步处理RPC请求响应的Promise集合， 单例的
    private final ServiceDiscovery serviceDiscovery; // 服务发现表

    static {
        UNPROCESSED_RPC_REQUEST_PROMISES = new ConcurrentHashMap<>();
    }

    public RpcClient() {
        this.serviceDiscovery = new NacosServiceDiscovery(new RoundRobinRule());
    }

    /**
     * 发送Rpc请求消息
     *
     * @param msg
     */
    public void sendRpcRequest(RpcRequestMessage msg) throws NacosException {
        InetSocketAddress serviceAddr = serviceDiscovery.selectService(msg.getInterfaceName()); // 根据配置的负载均衡策略，获取服务器的地址
        Channel channel = ChannelProvider.get(serviceAddr); // 获取客户端与该服务器的Channel
        if (!channel.isActive() || !channel.isRegistered()) {
            log.debug("通道已关闭");
            return;
            // TODO 通道
        }
        channel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("客户端发送消息成功");
            }
        });
    }
}
