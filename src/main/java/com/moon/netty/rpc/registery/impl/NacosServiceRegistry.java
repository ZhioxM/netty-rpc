package com.moon.netty.rpc.registery.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.moon.netty.rpc.registery.NacosUtils;
import com.moon.netty.rpc.registery.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * nacos注册
 * 供服务端使用
 */
@Slf4j
public class NacosServiceRegistry implements ServiceRegistry {


    /**
     * 服务注册
     *
     * @param serviceName
     * @param inetSocketAddress
     */
    @Override
    public void register(String serviceName, InetSocketAddress inetSocketAddress) {
        try {
            NacosUtils.registerServer(serviceName, inetSocketAddress);
            log.debug(serviceName + " 服务成功注册到远程服务中心");
        } catch (NacosException e) {
            throw new RuntimeException("注册Nacos出现异常");
        }
    }
}
