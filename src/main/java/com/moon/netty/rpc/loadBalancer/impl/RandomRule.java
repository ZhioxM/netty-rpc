package com.moon.netty.rpc.loadBalancer.impl;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.moon.netty.rpc.loadBalancer.LoadBalancer;

import java.util.List;
import java.util.Random;

/**
 * 负载均衡随机算法
 *
 * @author chenlei
 */
public class RandomRule implements LoadBalancer {
    private final Random random = new Random();

    /**
     * 随机获取实例
     *
     * @param list
     * @return
     */
    @Override
    public Instance getInstance(List<Instance> list) {
        return list.get(random.nextInt(list.size()));
    }
}
