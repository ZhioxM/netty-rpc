package com.moon.netty.rpc.registery.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.moon.netty.rpc.loadBalancer.LoadBalancer;
import com.moon.netty.rpc.loadBalancer.impl.RoundRobinRule;
import com.moon.netty.rpc.registery.NacosUtils;
import com.moon.netty.rpc.registery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务发现
 * 供客户端使用
 */
@Slf4j
public class NacosServiceDiscovery implements ServiceDiscovery {

    private final LoadBalancer loadBalancer;

    /**
     * @param loadBalancer 负载均衡算法 默认使用轮循的方式
     */
    public NacosServiceDiscovery(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer == null ? new RoundRobinRule() : loadBalancer;
    }


    /**
     * 使用负载均衡策略
     * 根据服务名找到服务地址
     *
     * @param serviceName
     * @return
     */
    @Override
    public InetSocketAddress selectService(String serviceName) throws NacosException {
        // 获取服务实例
        List<Instance> instanceList = NacosUtils.getAllInstance(serviceName);
        System.out.println(serviceName);
        if (instanceList.size() == 0) {
            throw new RuntimeException("找不到对应服务");
        }
        Instance instance = loadBalancer.getInstance(instanceList); // 根据负载均衡策略选择服务器
        log.debug("客户端选择的服务提供者是{} : {}", instance.getIp(), instance.getPort());
        return new InetSocketAddress(instance.getIp(), instance.getPort());
    }

}
