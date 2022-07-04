package com.moon.netty.rpc.provider;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端本地的服务工厂
 *
 * @author chenlei
 */
@Slf4j
public class ServiceProvider {

    //保存所有有注解@RpcService的类的实例化对象的集合
    // Key 是服务的名字， Value
    // 服务工厂是单例的
    public static final Map<String, Object> serviceProviders = new ConcurrentHashMap<>();

    //添加已注解的类对象进入工厂
    public <T> void addServiceProvider(T service, String serviceName) {
        if (serviceProviders.containsKey(serviceName)) {
            return;
        }
        serviceProviders.put(serviceName, service);
        log.debug("{} 服务类的实例对象成功添加进服务端本地工厂", serviceName);
    }

    //远程调用接口从该方法获取
    public static Object getService(String serviceName) {
        Object service = serviceProviders.get(serviceName);
        if (service == null) {
            throw new RuntimeException("服务端未发现该服务");
        }
        return service;
    }
}
