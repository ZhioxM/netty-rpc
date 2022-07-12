package com.moon.netty.rpc.transport.client;

import com.alibaba.nacos.api.exception.NacosException;
import com.moon.netty.rpc.loadBalancer.LoadBalancer;
import com.moon.netty.rpc.loadBalancer.impl.RoundRobinRule;
import com.moon.netty.rpc.message.RpcRequestMessage;
import com.moon.netty.rpc.registery.ServiceDiscovery;
import com.moon.netty.rpc.registery.impl.NacosServiceDiscovery;
import com.moon.netty.rpc.schedule.TimeoutCheckJob;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcClient {
    /**
     * 服务发现表
     */
    private final ServiceDiscovery serviceDiscovery;

    /**
     * 使用无参构造，则默认使用轮询的方式进行服务发现
     */
    public RpcClient() {
        // 开启开启定时任务，去监测超时
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(new TimeoutCheckJob(),0, 5, TimeUnit.SECONDS);
        this.serviceDiscovery = new NacosServiceDiscovery(new RoundRobinRule());
    }

    /**
     * @param loadBalancer 负载均衡策略
     */
    public RpcClient(LoadBalancer loadBalancer) {
        this.serviceDiscovery = new NacosServiceDiscovery(loadBalancer);
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
