package com.moon.netty.rpc.loadBalancer.impl;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.moon.netty.rpc.loadBalancer.LoadBalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡算法
 *
 * @author chenlei
 */
public class RoundRobinRule implements LoadBalancer {

    /**
     * 使用院子计数器做轮循
     */
    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    /**
     * 防止Integer越界 超过Integer最大值
     *
     * @return
     */
    private final int getAndIncrement() {
        int current;
        int next;
        do {
            current = this.atomicInteger.get();
            next = current >= 2147483647 ? 0 : current + 1;
        } while (!this.atomicInteger.compareAndSet(current, next));
        return next;
    }


    /**
     * 轮询获取实例
     *
     * @param list
     * @return
     */
    @Override
    public Instance getInstance(List<Instance> list) {
        int index = getAndIncrement() % list.size();
        return list.get(index);
    }
}
