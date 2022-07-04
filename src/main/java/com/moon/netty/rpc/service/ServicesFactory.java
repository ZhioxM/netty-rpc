package com.moon.netty.rpc.service;


import com.moon.netty.rpc.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 写死的服务工厂。暴露的服务接口是通过配置文件配置的
 */
@Deprecated
public class ServicesFactory {

    static Properties properties;
    static Map<Class<?>, Object> map = new ConcurrentHashMap<>();

    // TODO 暴露的服务在配置文件中配置好了
    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
            Set<String> names = properties.stringPropertyNames();
            for (String name : names) {
                if (name.endsWith("Service")) {
                    // 暴露的接口
                    Class<?> interfaceClass = Class.forName(name);
                    // 暴露的接口实现类
                    Class<?> instanceClass = Class.forName(properties.getProperty(name));
                    // 把接口的实现类进行实例化后放到Service中
                    map.put(interfaceClass, instanceClass.newInstance());
                }
            }
        } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static <T> T getService(Class<T> interfaceClass) {
        return (T) map.get(interfaceClass);
    }
}
