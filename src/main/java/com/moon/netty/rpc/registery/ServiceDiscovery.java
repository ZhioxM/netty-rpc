package com.moon.netty.rpc.registery;


import com.alibaba.nacos.api.exception.NacosException;

import java.net.InetSocketAddress;

/**
 * 服务发现接口
 * 由客户端使用
 *
 * @author chenlei
 */
public interface ServiceDiscovery {

    /**
     * 根据服务名找到InetSocketAddress
     */
    InetSocketAddress selectService(String serviceName) throws NacosException;

}
